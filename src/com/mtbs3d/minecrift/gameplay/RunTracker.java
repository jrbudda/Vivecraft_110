package com.mtbs3d.minecrift.gameplay;

import com.mtbs3d.minecrift.api.IRoomscaleAdapter;
import com.mtbs3d.minecrift.provider.MCOpenVR;
import com.mtbs3d.minecrift.settings.VRSettings;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class RunTracker {


	public boolean isActive(EntityPlayerSP p){
		if(!Minecraft.getMinecraft().vrSettings.vrFreeMove || Minecraft.getMinecraft().vrSettings.seated)
			return false;
		if(Minecraft.getMinecraft().vrSettings.vrFreeMoveMode != VRSettings.FREEMOVE_RUNINPLACE)
			return false;
		if(p==null || p.isDead)
			return false;
		if(p.isInWater() || p.isInLava())
			return false;
		if(p.isOnLadder()) 
			return false;
		return true;
	}

	private double direction = 0;
	private double speed = 0;
	
	public double getYaw(){
		return direction;
	}
	
	public double getSpeed(){
		return speed;
	}
	public void doProcess(Minecraft minecraft, EntityPlayerSP player){
		if(!isActive(player)) {
			speed = 0;
			return;
		}

		Vec3d controllerR= minecraft.roomScale.getControllerPos_World(0);
		Vec3d controllerL= minecraft.roomScale.getControllerPos_World(1);

		Vec3d middle= controllerL.subtract(controllerR).scale(0.5).add(controllerR);

		Vec3d hmdPos = minecraft.roomScale.getHMDPos_World();

		Vec3d movedir= new  Vec3d(middle.xCoord - hmdPos.xCoord, 0, middle.zCoord - hmdPos.zCoord);
		//TODO: do this betterer. Use actual controller movement dir in x-z plane.
		movedir = movedir.normalize();
		
		direction = Math.toDegrees(Math.atan2(-movedir.xCoord, movedir.zCoord));    

		double c0move = MCOpenVR.controllerHistory[0].averageSpeed(.25).lengthVector();
		double c1move = MCOpenVR.controllerHistory[1].averageSpeed(.25).lengthVector();

		if(c0move < 0.4 && c1move < 0.4){
			speed = 0;
			return;
		}
		
		if(Math.abs(c0move - c1move) > 1.0){
			speed = 0;
			return;
		} //2 hands plz.
			
		
		//TODO: tweak values, I guessed all these.
		
		double spd = (c0move + c1move) / 2;	
		this.speed = spd * 1.5 * 1.3;
		if(this.speed > 1.3) this.speed = 1.3f;
		
	}

}
