package com.pg85.otg.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.pg85.otg.OTG;
import com.pg85.otg.logging.LogMarker;

public class DataUtil
{
    public interface IOConsumer<T>
    {
        void accept(T t) throws IOException;
    }

    public interface IOBiConsumer<T, U>
    {
        void accept(T t, U u) throws IOException;
    }

    public interface IOTriConsumer<T, U, V>
    {
        void accept(T t, U u, V v) throws IOException;
    }

    public interface IOFunction<T, R>
    {
        R apply(T t) throws IOException;
    }

    public static void writeCompressed(Path file, Path backup, IOConsumer<DataOutputStream> writer)
    {
        Path temp = null;
        try
        {
            Path directory = file.toAbsolutePath().getParent();
            Files.createDirectories(directory);

            temp = Files.createTempFile(directory, file.getFileName().toString() + ".", ".tmp");
            try(DataOutputStream out = new DataOutputStream(new BufferedOutputStream(CompressionUtils.newDeflaterOutputStream(temp))))
            {
                writer.accept(out);
            }

            if(Files.isRegularFile(file))
            {
                Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            }

            try
            {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch(AtomicMoveNotSupportedException e)
            {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
            OTG.log(LogMarker.INFO, "OTG encountered an error writing " + file.toAbsolutePath() + ", skipping.");
        }
        finally
        {
            if(temp != null)
            {
                try
                {
                    Files.deleteIfExists(temp);
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void readCompressed(Path file, Path backup, IOConsumer<DataInputStream> reader)
    {
        readCompressed(file, backup, (p, in) -> reader.accept(in));
    }

    public static void readCompressed(Path directory, String extension, String backupExtension, IOBiConsumer<Path, DataInputStream> reader)
    {
        try(Stream<Path> files = Files.find(directory, 1, (p, a) -> a.isRegularFile()))
        {
            files.map(directory::relativize)
                    .map(Path::toString)
                    .filter(p -> p.endsWith(extension) || p.endsWith(backupExtension))
                    .map(p -> p.endsWith(backupExtension) ? StringUtils.removeEnd(p, backupExtension) : StringUtils.removeEnd(p, extension))
                    .distinct()
                    .forEach(p -> readCompressed(directory.resolve(p + extension), directory.resolve(p + backupExtension), reader));
        }
        catch(NoSuchFileException e)
        {
            return;
        }
        catch(IOException | UncheckedIOException e)
        {
            OTG.log(LogMarker.INFO, "OTG encountered an error listing " + directory.toAbsolutePath() + ", skipping.");
            e.printStackTrace();
        }
    }

    public static void readCompressed(Path file, Path backup, IOBiConsumer<Path, DataInputStream> reader)
    {
        try
        {
            readCompressedFile(file, reader);
        }
        catch(IOException e)
        {
            OTG.log(LogMarker.INFO, "Failed to load " + file.toAbsolutePath() + ", trying to load backup.");
            e.printStackTrace();

            try
            {
                readCompressedFile(backup, reader);
            }
            catch(IOException e1)
            {
                OTG.log(LogMarker.INFO, "Failed to load backup " + backup.toAbsolutePath() + ".");
                e1.printStackTrace();
                OTG.log(LogMarker.INFO, "OTG encountered an error loading " + file.toAbsolutePath() + " and could not load a backup, skipping.");
            }
        }
    }

    private static void readCompressedFile(Path file, IOBiConsumer<Path, DataInputStream> reader) throws IOException
    {
        try(DataInputStream in = new DataInputStream(new BufferedInputStream(CompressionUtils.newInflaterInputStream(file))))
        {
            reader.accept(file, in);
        }
    }

    public static <T> IOBiConsumer<DataOutputStream, T> mappingWriter(DataOutputStream out, UnaryOperator<T> mappingFunction, IOBiConsumer<DataOutputStream, T> mappedWriter)
    {
        return (out1, t) -> mappedWriter.accept(out1, mappingFunction.apply(t));
    }

    public static <T, C extends Collection<T>> IOBiConsumer<DataOutputStream, C> collectionWriter(IOBiConsumer<DataOutputStream, T> elementWriter)
    {
        return (out, collection) -> writeCollection(collection, out, elementWriter);
    }

    public static <K, V, M extends Map<K, V>> IOBiConsumer<DataOutputStream, M> mapWriter(IOBiConsumer<DataOutputStream, K> keyWriter, IOBiConsumer<DataOutputStream, V> valueWriter)
    {
        return (out, map) -> writeMap(map, out, keyWriter, valueWriter);
    }

    public static <K, V, M extends Map<K, V>> IOBiConsumer<DataOutputStream, M> mapWriter(IOTriConsumer<DataOutputStream, K, V> entryWriter)
    {
        return (out, map) -> writeMap(map, out, entryWriter);
    }

    public static <T> void writeCollection(Collection<T> collection, DataOutputStream out, IOBiConsumer<DataOutputStream, T> elementWriter) throws IOException
    {
        out.writeInt(collection.size());
        for(T t : collection)
        {
            elementWriter.accept(out, t);
        }
    }

    public static <K, V> void writeMap(Map<K, V> map, DataOutputStream out, IOBiConsumer<DataOutputStream, K> keyWriter, IOBiConsumer<DataOutputStream, V> valueWriter) throws IOException
    {
        out.writeInt(map.size());
        for(Entry<K, V> entry : map.entrySet())
        {
            keyWriter.accept(out, entry.getKey());
            valueWriter.accept(out, entry.getValue());
        }
    }

    public static <K, V> void writeMap(Map<K, V> map, DataOutputStream out, IOTriConsumer<DataOutputStream, K, V> entryWriter) throws IOException
    {
        out.writeInt(map.size());
        for(Entry<K, V> entry : map.entrySet())
        {
            entryWriter.accept(out, entry.getKey(), entry.getValue());
        }
    }

    public static <T, C extends Collection<T>> IOFunction<DataInputStream, C> collectionReader(Supplier<C> collectionSupplier, IOFunction<DataInputStream, T> elementReader)
    {
        return in -> {
            C collection = collectionSupplier.get();
            readCollection(collection, in, elementReader);
            return collection;
        };
    }

    public static <K, V, M extends Map<K, V>> IOFunction<DataInputStream, M> mapReader(Supplier<M> mapSupplier, IOFunction<DataInputStream, K> keyReader, IOFunction<DataInputStream, V> valueReader)
    {
        return in -> {
            M map = mapSupplier.get();
            readMap(map, in, keyReader, valueReader);
            return map;
        };
    }

    public static <K, V, M extends Map<K, V>> IOFunction<DataInputStream, M> mapReader(Supplier<M> mapSupplier, IOFunction<DataInputStream, Entry<K, V>> entryReader)
    {
        return in -> {
            M map = mapSupplier.get();
            readMap(map, in, entryReader);
            return map;
        };
    }

    public static <T, C extends Collection<T>> C readCollection(C collection, DataInputStream in, IOFunction<DataInputStream, T> elementReader) throws IOException
    {
        for(int i = in.readInt(); i > 0; i--)
        {
            collection.add(elementReader.apply(in));
        }
        return collection;
    }

    public static <T, C extends Collection<T>> C readCollection(C collection, DataInputStream in, IOBiConsumer<C, DataInputStream> elementReader) throws IOException
    {
        for(int i = in.readInt(); i > 0; i--)
        {
            elementReader.accept(collection, in);
        }
        return collection;
    }

    public static <K, V, M extends Map<K, V>> M readMap(M map, DataInputStream in, IOFunction<DataInputStream, K> keyReader, IOFunction<DataInputStream, V> valueReader) throws IOException
    {
        for(int i = in.readInt(); i > 0; i--)
        {
            map.put(keyReader.apply(in), valueReader.apply(in));
        }
        return map;
    }

    public static <K, V, M extends Map<K, V>> M readMap(M map, DataInputStream in, IOFunction<DataInputStream, Entry<K, V>> entryReader) throws IOException
    {
        for(int i = in.readInt(); i > 0; i--)
        {
            Entry<K, V> entry = entryReader.apply(in);
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    public static <K, V, M extends Map<K, V>> M readMap(M map, DataInputStream in, IOBiConsumer<M, DataInputStream> entryReader) throws IOException
    {
        for(int i = in.readInt(); i > 0; i--)
        {
            entryReader.accept(map, in);
        }
        return map;
    }

    public static <T, R> void writeOptional(T t, Function<T, R> mappingFunction, Predicate<R> filter, DataOutputStream out, IOBiConsumer<DataOutputStream, R> valueWriter) throws IOException
    {
        R r;
        boolean matches = filter.test(r = mappingFunction.apply(t));
        out.writeBoolean(matches);
        if(matches)
        {
            valueWriter.accept(out, r);
        }
    }

    public static <T, U extends T, R> void writeOptional(T t, Class<U> c, Function<U, R> mappingFunction, Predicate<R> filter, DataOutputStream out, IOBiConsumer<DataOutputStream, R> valueWriter) throws IOException
    {
        if(!c.isInstance(t))
        {
            out.writeBoolean(false);
        }
        else
        {
            writeOptional(c.cast(t), mappingFunction, filter, out, valueWriter);
        }
    }

    public static <T> T readOptional(T t, DataInputStream in, IOFunction<DataInputStream, T> reader) throws IOException
    {
        if(in.readBoolean())
        {
            return reader.apply(in);
        }
        return t;
    }

    public static <T> UnaryOperator<Collection<T>> filter(Predicate<T> filter)
    {
        return collection -> collection.stream().filter(filter).collect(Collectors.toList());
    }

    public static <K, V> UnaryOperator<Map<K, V>> filterByKey(Predicate<K> keyFilter)
    {
        return filterByEntry((k, v) -> keyFilter.test(k));
    }

    public static <K, V> UnaryOperator<Map<K, V>> filterByValue(Predicate<V> valueFilter)
    {
        return filterByEntry((k, v) -> valueFilter.test(v));
    }

    public static <K, V> UnaryOperator<Map<K, V>> filterByEntry(BiPredicate<K, V> entryFilter)
    {
        return map -> map.entrySet().stream().filter(e -> entryFilter.test(e.getKey(), e.getValue())).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    public static boolean nonEmpty(Collection<?> collection)
    {
        return !collection.isEmpty();
    }

    public static boolean nonEmpty(Map<?, ?> map)
    {
        return !map.isEmpty();
    }
}
