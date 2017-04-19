package com.mtbs3d.minecrift.control;

import java.awt.event.KeyEvent;

import com.mtbs3d.minecrift.utils.KeyboardSimulator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

public class VRControllerButtonMapping {

	public ViveButtons Button;
	public String FunctionDesc = "none";
	public char FunctionExt = 0;
	public KeyBinding key;
	
	public VRControllerButtonMapping(ViveButtons button, String function) {
		this.Button = button;
		this.FunctionDesc = function;		
	}
	
	@Override
	public String toString() {
		return Button.toString() + ":" + FunctionDesc + ( FunctionExt !=0  ? "_" + FunctionExt:"");
	};

	public void press(){	
		if(this.FunctionDesc.equals("none")) return;
		if(key!=null){
			key.pressKey();
			return;
		}
		if(FunctionExt!=0){
			if(FunctionDesc.contains("(hold)")){
				KeyboardSimulator.press(FunctionExt);
			} else {
				KeyboardSimulator.type(FunctionExt);	
			}		
			return;
		}	
		if(FunctionDesc.equals("keyboard-shift")){
			KeyboardSimulator.robot.keyPress(KeyEvent.VK_SHIFT);
			return;
		}
		if(FunctionDesc.equals("keyboard-ctrl")){
			KeyboardSimulator.robot.keyPress(KeyEvent.VK_CONTROL);
			return;
		}
		if(FunctionDesc.equals("keyboard-alt")){
			KeyboardSimulator.robot.keyPress(KeyEvent.VK_ALT);
			return;
		}
	}
	
	public void unpress(){
		if(this.FunctionDesc.equals("none")) return;
		if(key!=null) {
			key.unpressKey();
			return ;
		}
		if(FunctionExt!=0){
			if(FunctionDesc.contains("(hold)")){
				KeyboardSimulator.unpress(FunctionExt);
			} else {
				//nothing
			}		
			return;
		}	
		if(FunctionDesc.equals("keyboard-shift")){
			KeyboardSimulator.robot.keyRelease(KeyEvent.VK_SHIFT);
			return;
		}
		if(FunctionDesc.equals("keyboard-ctrl")){
			KeyboardSimulator.robot.keyRelease(KeyEvent.VK_CONTROL);
			return;
		}
		if(FunctionDesc.equals("keyboard-alt")){
			KeyboardSimulator.robot.keyRelease(KeyEvent.VK_ALT);
			return;
		}
	}
}
