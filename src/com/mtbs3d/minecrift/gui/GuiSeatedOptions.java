package com.mtbs3d.minecrift.gui;

import com.mtbs3d.minecrift.gui.framework.BaseGuiSettings;
import com.mtbs3d.minecrift.gui.framework.GuiButtonEx;
import com.mtbs3d.minecrift.gui.framework.GuiSliderEx;
import com.mtbs3d.minecrift.gui.framework.GuiSmallButtonEx;
import com.mtbs3d.minecrift.provider.MCOpenVR;
import com.mtbs3d.minecrift.settings.VRSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class GuiSeatedOptions extends BaseGuiSettings
{
	static VRSettings.VrOptions[] seatedOptions = new VRSettings.VrOptions[] {
			VRSettings.VrOptions.X_SENSITIVITY,
			VRSettings.VrOptions.Y_SENSITIVITY,
			VRSettings.VrOptions.KEYHOLE,
			VRSettings.VrOptions.RESET_ORIGIN,
	};
	// VIVE END - hide options not supported by tracked controller UI

	public GuiSeatedOptions(GuiScreen guiScreen, VRSettings guivrSettings) {
		super( guiScreen, guivrSettings );
		screenTitle = "Seated Settings";
	}

	/**
	 * Adds the buttons (and other controls) to the screen in question.
	 */
	public void initGui()
	{
		this.buttonList.clear();
		this.buttonList.add(new GuiButtonEx(ID_GENERIC_DEFAULTS, this.width / 2 - 155 ,  this.height -25 ,150,20, "Reset To Defaults"));
		this.buttonList.add(new GuiButtonEx(ID_GENERIC_DONE, this.width / 2 - 155  + 160, this.height -25,150,20, "Done"));


		VRSettings.VrOptions[] buttons = seatedOptions;

		for (int var12 = 2; var12 < buttons.length + 2; ++var12)
		{
			VRSettings.VrOptions var8 = buttons[var12 - 2];
			int width = this.width / 2 - 155 + var12 % 2 * 160;
			int height = this.height / 6 + 21 * (var12 / 2) - 10;

			if (var8 == VRSettings.VrOptions.DUMMY)
				continue;

			if (var8.getEnumFloat())
			{
				float minValue = 0.0f;
				float maxValue = 1.0f;
				float increment = 0.01f;

				if (var8 == VRSettings.VrOptions.X_SENSITIVITY)
				{
					minValue = 0.1f;
					maxValue = 5f;
					increment = 0.01f;
				}
				else if (var8 == VRSettings.VrOptions.Y_SENSITIVITY)
				{
					minValue = 0.1f;
					maxValue = 5f;
					increment = 0.01f;
				}
				else if (var8 == VRSettings.VrOptions.KEYHOLE)
				{
					minValue = 0f;
					maxValue = 40f;
					increment = 5f;
				}

				this.buttonList.add(new GuiSliderEx(var8.returnEnumOrdinal(), width, height, var8, this.guivrSettings.getKeyBinding(var8), minValue, maxValue, increment, this.guivrSettings.getOptionFloatValue(var8)));
			}
			else
			{
				this.buttonList.add(new GuiSmallButtonEx(var8.returnEnumOrdinal(), width, height, var8, this.guivrSettings.getKeyBinding(var8)));
			}
		}
	}

	/**
	 * Fired when a control is clicked. This is the equivalent of ActionListener.actionPerformed(ActionEvent e).
	 */
	protected void actionPerformed(GuiButton par1GuiButton)
	{
		if (par1GuiButton.enabled)
		{
			if (par1GuiButton.id == ID_GENERIC_DONE)
			{
				Minecraft.getMinecraft().vrSettings.saveOptions();
				this.mc.displayGuiScreen(this.parentGuiScreen);
			}
			else if (par1GuiButton.id == ID_GENERIC_DEFAULTS)
			{
				VRSettings vrSettings=Minecraft.getMinecraft().vrSettings;
				vrSettings.keyholeX=15;
				vrSettings.xSensitivity=1;
				vrSettings.ySensitivity=1;
				MCOpenVR.clearOffset();
				
				Minecraft.getMinecraft().vrSettings.saveOptions();
				this.reinit = true;
			}
			else if(par1GuiButton.id == VRSettings.VrOptions.RESET_ORIGIN.ordinal()){
				MCOpenVR.resetPosition();
				Minecraft.getMinecraft().vrSettings.saveOptions();
				this.mc.displayGuiScreen(null);
				this.mc.setIngameFocus();
			}
			else if (par1GuiButton instanceof GuiSmallButtonEx)
			{
				VRSettings.VrOptions num = VRSettings.VrOptions.getEnumOptions(par1GuiButton.id);
				this.guivrSettings.setOptionValue(((GuiSmallButtonEx)par1GuiButton).returnVrEnumOptions(), 1);
				par1GuiButton.displayString = this.guivrSettings.getKeyBinding(VRSettings.VrOptions.getEnumOptions(par1GuiButton.id));
			}
		}
	}

	@Override
	protected String[] getTooltipLines(String displayString, int buttonId)
	{
        VRSettings.VrOptions e = VRSettings.VrOptions.getEnumOptions(buttonId);
        if( e != null )
            switch(e)
            {
            case KEYHOLE:
                return new String[] {
                        "The number of degrees to the left and right of center",
                        "Where the view will begin to rotate."
                };
            case X_SENSITIVITY:
                return new String[] {
                        "Speed the view will rotate when pushed on the edge of the keyhole"
                };
            case Y_SENSITIVITY:
                return new String[] {
                        "Vertical speed of the crosshair related to the mouse"
                };
            default:
                return null;
            }
        else{
        	if(buttonId == VRSettings.VrOptions.RESET_ORIGIN.ordinal())
                    return new String[] {
                            "Recenter the player's feet in the world to 1.62m below the current",
                            "HMD position. For non-lighthouse tracking systems."
                    };
        }
		return null;
    }
}
