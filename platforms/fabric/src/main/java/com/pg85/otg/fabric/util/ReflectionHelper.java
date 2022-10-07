package com.pg85.otg.fabric.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ReflectionHelper {
    public static Object getField(Class clazz, String field, Object o) throws NoSuchFieldException, IllegalAccessException {
        Field f = clazz.getDeclaredField(field);
        f.setAccessible(true);
        return f.get(o);
    }

    public static void setField(Class clazz, String field, Object o, Object value) throws IllegalAccessException, NoSuchFieldException {
        Field f = clazz.getDeclaredField(field);
        f.setAccessible(true);
        f.set(o, value);
    }

}
