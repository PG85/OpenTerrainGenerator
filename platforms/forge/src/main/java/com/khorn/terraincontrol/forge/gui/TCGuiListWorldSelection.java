package com.khorn.terraincontrol.forge.gui;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.AnvilConverterException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.WorldSummary;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SideOnly(Side.CLIENT)
public class TCGuiListWorldSelection extends GuiListExtended
{
	// Taken from net.minecraft.client.gui.GuiListWorldSelection. Only changed GuiWorldSelection to TCGuiWorldSelection and GuiListWorldSelectionEntry to TCGuiListWorldSelectionEntry
	
    private static final Logger LOGGER = LogManager.getLogger();
    private final TCGuiWorldSelection worldSelectionObj;
    private final List<TCGuiListWorldSelectionEntry> entries = Lists.<TCGuiListWorldSelectionEntry>newArrayList();
    /** Index to the currently selected world */
    private int selectedIdx = -1;

    public TCGuiListWorldSelection(TCGuiWorldSelection p_i46590_1_, Minecraft clientIn, int p_i46590_3_, int p_i46590_4_, int p_i46590_5_, int p_i46590_6_, int p_i46590_7_)
    {
        super(clientIn, p_i46590_3_, p_i46590_4_, p_i46590_5_, p_i46590_6_, p_i46590_7_);
        this.worldSelectionObj = p_i46590_1_;
        this.refreshList();
    }

    public void refreshList()
    {
        ISaveFormat isaveformat = this.mc.getSaveLoader();
        List<WorldSummary> list;

        try
        {
            list = isaveformat.getSaveList();
        }
        catch (AnvilConverterException anvilconverterexception)
        {
            LOGGER.error((String)"Couldn\'t load level list", (Throwable)anvilconverterexception);
            this.mc.displayGuiScreen(new GuiErrorScreen("Unable to load worlds", anvilconverterexception.getMessage()));
            return;
        }

        Collections.sort(list);

        for (WorldSummary worldsummary : list)
        {
            this.entries.add(new TCGuiListWorldSelectionEntry(this, worldsummary, this.mc.getSaveLoader()));
        }
    }

    /**
     * Gets the IGuiListEntry object for the given index
     */
    public TCGuiListWorldSelectionEntry getListEntry(int index)
    {
        return (TCGuiListWorldSelectionEntry)this.entries.get(index);
    }

    protected int getSize()
    {
        return this.entries.size();
    }

    protected int getScrollBarX()
    {
        return super.getScrollBarX() + 20;
    }

    /**
     * Gets the width of the list
     */
    public int getListWidth()
    {
        return super.getListWidth() + 50;
    }

    public void selectWorld(int idx)
    {
        this.selectedIdx = idx;
        this.worldSelectionObj.selectWorld(this.getSelectedWorld());
    }

    /**
     * Returns true if the element passed in is currently selected
     */
    protected boolean isSelected(int slotIndex)
    {
        return slotIndex == this.selectedIdx;
    }

    @Nullable
    public TCGuiListWorldSelectionEntry getSelectedWorld()
    {
        return this.selectedIdx >= 0 && this.selectedIdx < this.getSize() ? this.getListEntry(this.selectedIdx) : null;
    }

    public TCGuiWorldSelection getGuiWorldSelection()
    {
        return this.worldSelectionObj;
    }
}