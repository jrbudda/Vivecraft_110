package com.mtbs3d.minecrift.gameplay;

import com.mtbs3d.minecrift.provider.MCOpenVR;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Created by Hendrik on 02-Aug-16.
 */
public class SwimTracker {

	Vec3d motion=Vec3d.ZERO;
	double friction=0.9f;

	double lastDist;

	final double riseSpeed=0.005f;
	double swimspeed=0.8f;

	public boolean isActive(EntityPlayerSP p){
		if(!Minecraft.getMinecraft().vrSettings.vrFreeMove || Minecraft.getMinecraft().vrSettings.seated)
			return false;
		if(p==null || p.isDead)
			return false;
		if(!p.isInWater() && !p.isInLava())
			return false;
		
		//Block block=p.worldObj.getBlockState(p.getPosition().add(0,0.7,0)).getBlock();
		return true;
	}

	public void doProcess(Minecraft minecraft, EntityPlayerSP player){
		if(!isActive(player)) {
			return;
		}

		Vec3d face = minecraft.roomScale.getHMDPos_World();
		Vec3d feets = face.subtract(0,minecraft.roomScale.getHMDPos_Room().yCoord * 0.9, 0);
		double waterLine=256;

		BlockPos bp = new BlockPos(feets);
		for (int i = 0; i < 4; i++) {
			Material mat=player.worldObj.getBlockState(bp).getMaterial();
			if(!mat.isLiquid())
			{
				waterLine=bp.getY();
				break;
			}
			bp = bp.up();
		}

		double percent = (waterLine - feets.yCoord) / (face.yCoord - feets.yCoord);

		if(percent < 0){
			//how did u get here, drybones?
			return;
		}

		if(percent < 0.5 && player.onGround){
			return;
			//no diving in the kiddie pool.
		}
		
		player.addVelocity(0, 0.018D , 0); //counteract most gravity.
		
		double neutal = player.isCollidedHorizontally? 0.5 : 1;
		
		if(percent > neutal && percent < 2){ //between halfway submerged and 1 body length under.
			//rise!
			double buoyancy = 2 - percent;
			if(player.isCollidedHorizontally)  player.addVelocity(0, 00.03f, 0);	
	        player.addVelocity(0, 0.0015 + buoyancy/100 , 0);		
		}


//		gravityOverride=true;
//		player.setNoGravity(true);
//
//		Vec3d playerpos=player.getPositionVector();
//
//		double swimHeight= MCOpenVR.hmdPivotHistory.latest().yCoord;//new Vec3d(0,1.5,0);
//		double maxSwim= swimHeight/2;
//
//		double depth=2;
//
//		for (int i = 0; i < 4; i++) {
//			BlockPos blockpos=new BlockPos(playerpos.add(new Vec3d(0,i+0.5,0)));
//			Material block=player.worldObj.getBlockState(blockpos).getMaterial();
//			if(!block.isLiquid())
//			{
//				depth=blockpos.getY()-playerpos.yCoord-2;
//				break;
//			}
//		}
//
//		if (depth > 2)
//				depth = 2;
//
//		double buoyancy=(1-depth);
//		
//		Material block1=player.worldObj.getBlockState(new BlockPos(playerpos.addVector(0,swimHeight,0))).getMaterial();
//		if(!block1.isLiquid()){
//			//we are at the surface
//			Material block2=player.worldObj.getBlockState(new BlockPos(playerpos.addVector(0,maxSwim,0))).getMaterial();
//			if(!block2.isLiquid()){
//				//Too high
//				player.setNoGravity(false);
//			}
//
//		}else{
//			player.addVelocity(0, buoyancy<0 ? sinkspeed*buoyancy : riseSpeed*buoyancy, 0);
//		}

		Vec3d controllerR= minecraft.roomScale.getControllerPos_World(0);
		Vec3d controllerL= minecraft.roomScale.getControllerPos_World(1);

		Vec3d middle= controllerL.subtract(controllerR).scale(0.5).add(controllerR);

		Vec3d hmdPos=minecraft.roomScale.getHMDPos_World().subtract(0,0.3,0);

		Vec3d movedir=middle.subtract(hmdPos).normalize().add(
				minecraft.roomScale.getHMDDir_World()).scale(0.5);

		Vec3d contollerDir= minecraft.roomScale.getCustomControllerVector(0,new Vec3d(0,0,-1)).add(
				minecraft.roomScale.getCustomControllerVector(1,new Vec3d(0,0,-1))).scale(0.5);
		double dirfactor=contollerDir.add(movedir).lengthVector()/2;

		double distance= hmdPos.distanceTo(middle);
		double distDelta=lastDist-distance;

		if(distDelta>0){
			Vec3d velo=movedir.scale(distDelta*swimspeed*dirfactor);
			motion=motion.add(velo.scale(0.15));
		}

		lastDist=distance;
		player.addVelocity(motion.xCoord,motion.yCoord,motion.zCoord);
		motion=motion.scale(friction);

	}

}
