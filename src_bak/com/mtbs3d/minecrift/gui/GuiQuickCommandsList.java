package com.mtbs3d.minecrift.gui;

import java.awt.TextField;
import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.gui.GuiScreen;
import org.apache.commons.lang3.ArrayUtils;

import com.mtbs3d.minecrift.control.VRControllerButtonMapping;
import com.mtbs3d.minecrift.control.ViveButtons;
import com.mtbs3d.minecrift.gui.framework.GuiEnterText;
import com.mtbs3d.minecrift.settings.VRSettings;

public class GuiQuickCommandsList extends GuiListExtended
{
    private final GuiQuickCommandEditor parent;
    private final Minecraft mc;
    private final GuiListExtended.IGuiListEntry[] listEntries;
    private int maxListLabelWidth = 0;
    private static final String __OBFID = "CL_00000732";

        
    public GuiQuickCommandsList(GuiQuickCommandEditor parent, Minecraft mc)
    {
        super(mc, parent.width, parent.height, 63, parent.height - 32, 20);
        this.parent = parent;
        this.mc = mc;
        
        String[] commands = mc.vrSettings.vrQuickCommands;
        
        this.listEntries = new GuiListExtended.IGuiListEntry[commands.length];
        
      //  Arrays.sort(bindings);
        String var5 = null;
        int var4 = 0;
        int var7 = commands.length;
        for (int i = 0; i < var7; i++)
        {
        	String kb = commands[i];

            int width = mc.fontRendererObj.getStringWidth(kb);

            if (width > this.maxListLabelWidth)
            {
                this.maxListLabelWidth = width;
            }

            this.listEntries[i] = new GuiQuickCommandsList.CommandEntry(kb, this);
        }
    }

    protected int getSize()
    {
        return this.listEntries.length;
    }

    /**
     * Gets the IGuiListEntry object for the given index
     */
    public GuiListExtended.IGuiListEntry getListEntry(int i)
    {
        return this.listEntries[i];
    }

    protected int getScrollBarX()
    {
        return super.getScrollBarX() + 15;
    }

    /**
     * Gets the width of the list
     */
    public int getListWidth()
    {
        return super.getListWidth() + 32;
    }

    public class CommandEntry implements GuiListExtended.IGuiListEntry
    {
        private final GuiButton btnDelete;
        public final GuiTextField txt;
        
        private CommandEntry(String command, GuiQuickCommandsList parent)
        {
            this.btnDelete = new GuiButton(0, 0, 0, 48, 18, "X");
            txt = new GuiTextField(0,parent.parent.fontRendererObj, parent.width / 2 - 100, 60, 200, 20);
            txt.setText(command);
        }

        public void drawEntry(int p_148279_1_, int x, int y, int p_148279_4_, int p_148279_5_, int p_148279_7_, int p_148279_8_, boolean p_148279_9_)
        {
        	txt.xPosition = 16;
        	txt.yPosition = y;
        	
        	txt.drawTextBox();
        	//GuiQuickCommandsList.this.mc.fontRendererObj.drawString(command, x + 40  - GuiQuickCommandsList.this.maxListLabelWidth, y + p_148279_5_ / 2 - GuiQuickCommandsList.this.mc.fontRendererObj.FONT_HEIGHT / 2, 16777215);

        	this.btnDelete.xPosition = x+140;
        	this.btnDelete.yPosition = y;
        	this.btnDelete.visible = true;
        	this.btnDelete.drawButton(mc, x, y);
        }
        
        
        public boolean mousePressed(int p_148278_1_, int p_148278_2_, int p_148278_3_, int p_148278_4_, int p_148278_5_, int p_148278_6_)
        {
        	txt.setFocused(true);
            if (this.btnDelete.mousePressed(GuiQuickCommandsList.this.mc, p_148278_2_, p_148278_3_))
            {
            	txt.setText("");
                return true;
            }

            else
            {
                return false;
            }
        }

        public void mouseReleased(int p_148277_1_, int p_148277_2_, int p_148277_3_, int p_148277_4_, int p_148277_5_, int p_148277_6_)
        {

        }

		@Override
		public void setSelected(int p_178011_1_, int p_178011_2_, int p_178011_3_) {
			// TODO Auto-generated method stub
			
		}

    }
}
