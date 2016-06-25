/**
* Copyright 2013 Mark Browning, StellaArtois
* Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
*/
package com.mtbs3d.minecrift.settings;

import com.mtbs3d.minecrift.provider.OpenVRPlayer;
import com.mtbs3d.minecrift.settings.VRSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiWinGame;

import org.lwjgl.input.Keyboard;

public class VRHotkeys {

	static long nextRead = 0;
	static final long COOLOFF_PERIOD_MILLIS = 500;

	public static void handleKeyboardInputs(Minecraft mc)
	{
		// Support cool-off period for key presses - otherwise keys can get spammed...
		if (nextRead != 0 && System.currentTimeMillis() < nextRead)
		return;

		// Capture Minecrift key events
		boolean gotKey = false;

		// Debug aim
		if (Keyboard.getEventKey() == Keyboard.KEY_RSHIFT && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
		{
			mc.vrSettings.storeDebugAim = true;
			mc.printChatMessage("Show aim (RCTRL+RSHIFT): done");
			gotKey = true;
		}

		// Debug pos
		if (Keyboard.getEventKey() == Keyboard.KEY_P && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
		{
			mc.vrSettings.debugPos = !mc.vrSettings.debugPos;
			mc.printChatMessage("Debug position (RCTRL+P): " + mc.vrSettings.debugPos);
			gotKey = true;
		}

		// Pause pose updates for Timewarp test
		if (Keyboard.getEventKey() == Keyboard.KEY_T && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
		{
			mc.vrSettings.testTimewarp = !mc.vrSettings.testTimewarp;
			mc.printChatMessage("Test Timewarp (RCTRL+T): " + mc.vrSettings.testTimewarp);
			gotKey = true;
		}

		// Walk up blocks
		if (Keyboard.getEventKey() == Keyboard.KEY_B && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
		{
			mc.vrSettings.walkUpBlocks = !mc.vrSettings.walkUpBlocks;
			mc.vrSettings.saveOptions();
			mc.printChatMessage("Walk up blocks (RCTRL+B): " + (mc.vrSettings.walkUpBlocks ? "YES" : "NO"));
			gotKey = true;
		}

		// Player inertia
		if (Keyboard.getEventKey() == Keyboard.KEY_I && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL))
		{
			mc.vrSettings.inertiaFactor += 1;
			if (mc.vrSettings.inertiaFactor > VRSettings.INERTIA_MASSIVE)
			mc.vrSettings.inertiaFactor = VRSettings.INERTIA_NONE;
			mc.vrSettings.saveOptions();
			switch (mc.vrSettings.inertiaFactor)
			{
			case VRSettings.INERTIA_NONE:
				mc.printChatMessage("Player player movement inertia (LCTRL-I): None");
				break;
			case VRSettings.INERTIA_NORMAL:
				mc.printChatMessage("Player player movement inertia (LCTRL-I): Normal");
				break;
			case VRSettings.INERTIA_LARGE:
				mc.printChatMessage("Player player movement inertia (LCTRL-I): Large");
				break;
			case VRSettings.INERTIA_MASSIVE:
				mc.printChatMessage("Player player movement inertia (LCTRL-I): Massive");
				break;
			}
			gotKey = true;
		}

		// Render full player model or just an disembodied hand...
		if (Keyboard.getEventKey() == Keyboard.KEY_H && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
		{
			mc.vrSettings.renderFullFirstPersonModelMode++;
			if (mc.vrSettings.renderFullFirstPersonModelMode > VRSettings.RENDER_FIRST_PERSON_NONE)
			mc.vrSettings.renderFullFirstPersonModelMode = VRSettings.RENDER_FIRST_PERSON_FULL;

			mc.vrSettings.saveOptions();
			switch (mc.vrSettings.renderFullFirstPersonModelMode)
			{
			case VRSettings.RENDER_FIRST_PERSON_FULL:
				mc.printChatMessage("First person model (RCTRL-H): Full");
				break;
			case VRSettings.RENDER_FIRST_PERSON_HAND:
				mc.printChatMessage("First person model (RCTRL-H): Hand");
				break;
			case VRSettings.RENDER_FIRST_PERSON_NONE:
				mc.printChatMessage("First person model (RCTRL-H): None");
				break;
			}
			gotKey = true;
		}
		// VIVE START - hotkeys

		// Testing different movement styles
		if (Keyboard.getEventKey() == Keyboard.KEY_M && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
		{
			// JRBUDDA ADDED all dis.
			if (mc.vrPlayer.getFreeMoveMode()) {
				//cycle restricted movement styles
				if (mc.vrPlayer.useLControllerForRestricedMovement) {
					mc.vrPlayer.useLControllerForRestricedMovement = false;
					mc.printChatMessage("Restricted movement mode set to gaze");
				} else {
					mc.vrPlayer.useLControllerForRestricedMovement = true;
					mc.printChatMessage("Restricted movement mode set to left controller");
				}
			} else {
				
				OpenVRPlayer vrp = mc.vrPlayer;
				
				// cycle VR movement styles
				if (vrp.vrMovementStyle.name == "Minimal") vrp.vrMovementStyle.setStyle("Beam");
				else if (vrp.vrMovementStyle.name == "Beam") vrp.vrMovementStyle.setStyle("Tunnel");
				else if (vrp.vrMovementStyle.name == "Tunnel") vrp.vrMovementStyle.setStyle("Grapple");
				else if (vrp.vrMovementStyle.name == "Grapple") vrp.vrMovementStyle.setStyle("Arc");
				else vrp.vrMovementStyle.setStyle("Minimal");
			
						
			}
					
			gotKey = true;
		}

		if (Keyboard.getEventKey() == Keyboard.KEY_R && Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
		{
			// for testing restricted client mode
			
			if (mc.vrPlayer.getFreeMoveMode()) {
				mc.vrPlayer.setFreeMoveMode(false);
							mc.printChatMessage("Restricted movement disabled (teleporting allowed)");
				} else {
				mc.vrPlayer.setFreeMoveMode(true);
				mc.printChatMessage("Restricted movement enabled (no teleporting)");
			}
			
			gotKey = true;
		}
		// VIVE END - hotkeys

		if (gotKey) {
			nextRead = System.currentTimeMillis() + COOLOFF_PERIOD_MILLIS;
		}
	}
}
