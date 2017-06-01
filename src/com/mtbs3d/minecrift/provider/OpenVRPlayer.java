package com.mtbs3d.minecrift.provider;


import com.mtbs3d.minecrift.render.PlayerModelController;
import com.mtbs3d.minecrift.settings.AutoCalibration;

import de.fruitfly.ovr.enums.EyeType;
import de.fruitfly.ovr.structs.EulerOrient;
import de.fruitfly.ovr.structs.Matrix4f;
import de.fruitfly.ovr.structs.Quatf;
import de.fruitfly.ovr.structs.Vector3f;
import de.fruitfly.ovr.util.BufferUtil;
import io.netty.util.concurrent.GenericFutureListener;
import jopenvr.OpenVRUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockVine;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Minecraft.renderPass;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.src.Reflector;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Random;

import javax.swing.plaf.multi.MultiViewportUI;

import org.lwjgl.util.vector.Quaternion;

import com.google.common.base.Charsets;
import com.mtbs3d.minecrift.api.IRoomscaleAdapter;
import com.mtbs3d.minecrift.api.NetworkHelper;
import com.mtbs3d.minecrift.api.NetworkHelper.PacketDiscriminators;
import com.mtbs3d.minecrift.gameplay.ParticleVRTeleportFX;
import com.mtbs3d.minecrift.gameplay.VRMovementStyle;
import com.mtbs3d.minecrift.render.QuaternionHelper;
import com.mtbs3d.minecrift.settings.VRSettings;
import com.mtbs3d.minecrift.utils.MCReflection;

// VIVE
public class OpenVRPlayer implements IRoomscaleAdapter
{
    public double lastRoomUpdateTime = 0;
    public Vec3d movementTeleportDestination = new Vec3d(0.0,0.0,0.0);
    public EnumFacing movementTeleportDestinationSideHit;
    public double movementTeleportProgress;
    public double movementTeleportDistance;
        
    public Vec3d roomOrigin = new Vec3d(0,0,0);
    public Vec3d lastroomOrigin = new Vec3d(0,0,0);
    
    public VRMovementStyle vrMovementStyle = new VRMovementStyle();
    public Vec3d[] movementTeleportArc = new Vec3d[50];
    public int movementTeleportArcSteps = 0;
    private boolean isFreeMoveCurrent = true;        // true when connected to another server that doesn't have this mod
    public double lastTeleportArcDisplayOffset = 0;
    public boolean noTeleportClient = true;
    
    private float teleportEnergy;

	private Vec3d walkMultOffset=Vec3d.ZERO;
    
	public double wfMode = 0;
	public int wfCount = 0;

	public static OpenVRPlayer get()
    {
        return Minecraft.getMinecraft().vrPlayer;
    }

    public OpenVRPlayer()
    {

    }
   
    public boolean debugSpawn;
    
    public void setRoomOrigin(double x, double y, double z, boolean reset, boolean onframe ) { 
  	    if(!onframe){
	    	if (reset){
		    		lastroomOrigin = new Vec3d(x, y, z);
		    		Minecraft.getMinecraft().entityRenderer.interPolatedRoomOrigin = new Vec3d(x, y, z);
		    	} else {
		    	}
	    }
	    roomOrigin = new Vec3d(x, y, z);
        lastRoomUpdateTime = Minecraft.getMinecraft().stereoProvider.getCurrentTimeSecs();
        Minecraft.getMinecraft().entityRenderer.irpUpdatedThisFrame = onframe;
        if(debugSpawn){
        	System.out.println("room origin " + x + " " + y + " " + z + " " + onframe + " " + reset);
        	Thread.dumpStack();
        }
    } 
    
    private int roomScaleMovementDelay = 10;
    
    //set room 
    public void snapRoomOriginToPlayerEntity(EntityPlayerSP player, boolean reset, boolean onFrame, float nano)
    {
        if (Thread.currentThread().getName().equals("Server thread"))
            return;
       
        if(player.posX == 0 && player.posY == 0 &&player.posZ == 0) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        
        Vec3d campos = mc.roomScale.getHMDPos_Room();
        
        campos = campos.rotateYaw(worldRotationRadians);
                
        double x,y,z;

        if(onFrame){
        	x = mc.entityRenderer.interpolatedPlayerPos.xCoord - campos.xCoord;
         
        	if(player.isRiding()){	
        			y = player.getRidingEntity().getPositionEyes(nano).yCoord - player.getRidingEntity().getEyeHeight() + player.getRidingEntity().getMountedYOffset();
        	} else
            	y = mc.entityRenderer.interpolatedPlayerPos.yCoord;

          	z = mc.entityRenderer.interpolatedPlayerPos.zCoord - campos.zCoord;
        } else {
        	x = player.posX - campos.xCoord;
        	if(player.isRiding()){
        			y = player.getRidingEntity().posY + player.getRidingEntity().getMountedYOffset();        	}
        	else
        		y = player.posY;
        	z = player.posZ - campos.zCoord;
        }
        
        setRoomOrigin(x, y, z, reset, onFrame);
        this.roomScaleMovementDelay = 3;
        
    }
    
    public  double topofhead = 1.62;
    
    
    private float lastworldRotation= 0f;
	private float lastWorldScale;
    
	public void checkandUpdateRotateScale(boolean onFrame, float nano){
		Minecraft mc = Minecraft.getMinecraft();
		if(mc.player == null || mc.player.initFromServer == false) return;
		if(!onFrame && mc.currentScreen!=null) return;
		
		if(!onFrame) {
			if(this.wfCount > 0){
				if(this.wfCount < 40){
					this.worldScale-=this.wfMode / 2;
					if(this.worldScale >  mc.vrSettings.vrWorldScale && this.wfMode <0) this.worldScale = mc.vrSettings.vrWorldScale;
					if(this.worldScale <  mc.vrSettings.vrWorldScale && this.wfMode >0) this.worldScale = mc.vrSettings.vrWorldScale;
				}else {
					this.worldScale+=this.wfMode / 2;
					if(this.worldScale >  mc.vrSettings.vrWorldScale*20) this.worldScale = 20;
					if(this.worldScale <  mc.vrSettings.vrWorldScale/10) this.worldScale = 0.1f;				
				}
				this.wfCount--;
			} else 	this.worldScale =  mc.vrSettings.vrWorldScale;
		} else {

		}
		
		this.interpolatedWorldScale = this.worldScale*nano + this.lastWorldScale*(1-nano);
		
	    this.worldRotationRadians = (float) Math.toRadians(mc.vrSettings.vrWorldRotation);
	    
	    if (worldRotationRadians!= lastworldRotation || worldScale != lastWorldScale) {
	    	if(mc.player!=null) 
	    		snapRoomOriginToPlayerEntity(mc.player, true, onFrame, nano);
	    }
	}

	
	boolean initdone =false;
	
	public void preTick(){
		lastWorldScale = worldScale;	
	    lastworldRotation = worldRotationRadians;
		lastroomOrigin = new Vec3d(roomOrigin.xCoord, roomOrigin.yCoord, roomOrigin.zCoord);
	}
	
    public void onLivingUpdate(EntityPlayerSP player, Minecraft mc, Random rand)
    {
    	if(!player.initFromServer) return;
    	
	    if(!initdone){
		    initdone =true;
	    }

	    mc.runTracker.doProcess(mc, player);

	    mc.rowTracker.doProcess(mc, player);

	    mc.jumpTracker.doProcess(player);
   
	    mc.sneakTracker.doProcess(mc, player);
	    
	    mc.backpackTracker.doProcess(mc, player);
	    
		mc.autoFood.doProcess(mc,player);
           		
		this.checkandUpdateRotateScale(false,1);

        mc.swimTracker.doProcess(mc,player);

	    mc.climbTracker.doProcess(player);
	    
        updateSwingAttack();

	    AutoCalibration.logHeadPos(MCOpenVR.hmdPivotHistory.latest());

    	NetworkHelper.sendVRPlayerPositions(this);
	    
       if(mc.vrSettings.vrAllowCrawling){         //experimental
//           topofhead = (double) (mc.roomScale.getHMDPos_Room().yCoord + .05);
//           
//           if(topofhead < .5) {topofhead = 0.5f;}
//           if(topofhead > 1.8) {topofhead = 1.8f;}
//           
//           player.height = (float) topofhead - 0.05f;
//           player.spEyeHeight = player.height - 1.62f;
//           player.boundingBox.setMaxY( player.boundingBox.minY +  topofhead);  	   
       } else {
    	   player.height = 1.8f;
    	   player.spEyeHeight = 0.12f;
       }

       
        // don't do teleport movement if on a server that doesn't have this mod installed
        if (getFreeMove()) {
        	
        		if(player.movementInput.moveForward ==0) doPlayerMoveInRoom(player);
	
			  return; //let mc handle look direction movement
			// controller vs gaze movement is handled in Entity.java > moveFlying
          }
				
        mc.mcProfiler.startSection("VRPlayerOnLivingUpdate");

        if (teleportEnergy < 100) { teleportEnergy++;}
        
        boolean doTeleport = false;
        Vec3d dest = null;

        if (player.movementInput.moveForward != 0 && !player.isRiding()) //holding down Ltrigger
        {
            dest = movementTeleportDestination;

            if (vrMovementStyle.teleportOnRelease)
            {
                if (player.movementTeleportTimer==0)
                {
                    String sound = vrMovementStyle.startTeleportingSound;
                    if (sound != null)
                    {
                        player.playSound(SoundEvents.getRegisteredSoundEvent(sound), vrMovementStyle.startTeleportingSoundVolume,
                                1.0F / (rand.nextFloat() * 0.4F + 1.2F) + 1.0f * 0.5F);
                    }
                }
                player.movementTeleportTimer++;
                if (player.movementTeleportTimer > 0)
                {
                    movementTeleportProgress = (float) player.movementTeleportTimer / 4.0f;
                    if (movementTeleportProgress>=1.0f)
                    {
                        movementTeleportProgress = 1.0f;
                    }

                    if (dest.xCoord != 0 || dest.yCoord != 0 || dest.zCoord != 0)
                    {
                        Vec3d eyeCenterPos = getHMDPos_World();

                        // cloud of sparks moving past you
                        Vec3d motionDir = dest.addVector(-eyeCenterPos.xCoord, -eyeCenterPos.yCoord, -eyeCenterPos.zCoord).normalize();
                        Vec3d forward;
						
						forward	= player.getLookVec();

                        Vec3d right = forward.crossProduct(new Vec3d(0, 1, 0));
                        Vec3d up = right.crossProduct(forward);

                        if (vrMovementStyle.airSparkles)
                        {
                            for (int iParticle = 0; iParticle < 3; iParticle++)
                            {
                                double forwardDist = rand.nextDouble() * 1.0 + 3.5;
                                double upDist = rand.nextDouble() * 2.5;
                                double rightDist = rand.nextDouble() * 4.0 - 2.0;

                                Vec3d sparkPos = new Vec3d(eyeCenterPos.xCoord + forward.xCoord * forwardDist,
                                        eyeCenterPos.yCoord + forward.yCoord * forwardDist,
                                        eyeCenterPos.zCoord + forward.zCoord * forwardDist);
                                sparkPos = sparkPos.addVector(right.xCoord * rightDist, right.yCoord * rightDist, right.zCoord * rightDist);
                                sparkPos = sparkPos.addVector(up.xCoord * upDist, up.yCoord * upDist, up.zCoord * upDist);

                                double speed = -0.6;
//                                EntityFX particle = new ParticleVRTeleportFX(
//                                        player.world,
//                                        sparkPos.xCoord, sparkPos.yCoord, sparkPos.zCoord,
//                                        motionDir.xCoord * speed, motionDir.yCoord * speed, motionDir.zCoord * speed,
//                                        1.0f);
//                                mc.effectRenderer.addEffect(particle);
                            }
                        }
                    }
                }
            }
            else
            {
                if (player.movementTeleportTimer >= 0 && (dest.xCoord != 0 || dest.yCoord != 0 || dest.zCoord != 0))
                {
                    if (player.movementTeleportTimer == 0)
                    {
                        String sound = vrMovementStyle.startTeleportingSound;
                        if (sound != null)
                        {
                            player.playSound(SoundEvents.getRegisteredSoundEvent(sound), vrMovementStyle.startTeleportingSoundVolume,
                                    1.0F / (rand.nextFloat() * 0.4F + 1.2F) + 1.0f * 0.5F);
                        }
                    }
                    player.movementTeleportTimer++;

                    Vec3d playerPos = new Vec3d(player.posX, player.posY, player.posZ);
                    double dist = dest.distanceTo(playerPos);
                    double progress = (player.movementTeleportTimer * 1.0) / (dist + 3.0);

                    if (player.movementTeleportTimer > 0)
                    {
                        movementTeleportProgress = progress;

                        // spark at dest point
                        if (vrMovementStyle.destinationSparkles)
                        {
                          //  player.world.spawnParticle("instantSpell", dest.xCoord, dest.yCoord, dest.zCoord, 0, 1.0, 0);
                        }

                        // cloud of sparks moving past you
                        Vec3d motionDir = dest.addVector(-player.posX, -player.posY, -player.posZ).normalize();
                        Vec3d forward = player.getLookVec();
                        Vec3d right = forward.crossProduct(new Vec3d(0, 1, 0));
                        Vec3d up = right.crossProduct(forward);

                        if (vrMovementStyle.airSparkles)
                        {
                            for (int iParticle = 0; iParticle < 3; iParticle++)
                            {
                                double forwardDist = rand.nextDouble() * 1.0 + 3.5;
                                double upDist = rand.nextDouble() * 2.5;
                                double rightDist = rand.nextDouble() * 4.0 - 2.0;
                                Vec3d sparkPos = new Vec3d(player.posX + forward.xCoord * forwardDist,
                                        player.posY + forward.yCoord * forwardDist,
                                        player.posZ + forward.zCoord * forwardDist);
                                sparkPos = sparkPos.addVector(right.xCoord * rightDist, right.yCoord * rightDist, right.zCoord * rightDist);
                                sparkPos = sparkPos.addVector(up.xCoord * upDist, up.yCoord * upDist, up.zCoord * upDist);

                                double speed = -0.6;
//                                EntityFX particle = new ParticleVRTeleportFX(
//                                        player.world,
//                                        sparkPos.xCoord, sparkPos.yCoord, sparkPos.zCoord,
//                                        motionDir.xCoord * speed, motionDir.yCoord * speed, motionDir.zCoord * speed,
//                                        1.0f);
//                                mc.effectRenderer.addEffect(particle);
                            }
                        }
                    } else
                    {
                        movementTeleportProgress = 0;
                    }

                    if (progress >= 1.0)
                    {
                        doTeleport = true;
                    }
                }
            }
        }
        else //not holding down Ltrigger
        {
            if (vrMovementStyle.teleportOnRelease && movementTeleportProgress>=1.0f)
            {
                dest = movementTeleportDestination;
                doTeleport = true;
            }
            player.movementTeleportTimer = 0;
            movementTeleportProgress = 0;
        }

        if (doTeleport && dest!=null && (dest.xCoord != 0 || dest.yCoord !=0 || dest.zCoord != 0)) //execute teleport
        {
            movementTeleportDistance = (float)MathHelper.sqrt(dest.squareDistanceTo(player.posX, player.posY, player.posZ));
            boolean playTeleportSound = movementTeleportDistance > 0.0f && vrMovementStyle.endTeleportingSound != null;
            Block block = null;

            if (playTeleportSound)
            {
                String sound = vrMovementStyle.endTeleportingSound;
                if (sound != null)
                {
                    player.playSound(SoundEvents.getRegisteredSoundEvent(sound), vrMovementStyle.endTeleportingSoundVolume, 1.0F);
                }
            }
            else
            {
                playFootstepSound(mc, dest.xCoord, dest.yCoord, dest.zCoord);
            }

     	   //execute teleport               
            if(this.noTeleportClient){
            	String tp = "/tp " + mc.player.getName() + " " + dest.xCoord + " " +dest.yCoord + " " + dest.zCoord;      
            	mc.player.sendChatMessage(tp);
            } else {          
            	if(NetworkHelper.serverSupportsDirectTeleport)	player.teleported = true;
                player.setPositionAndUpdate(dest.xCoord, dest.yCoord, dest.zCoord);
            }

            doTeleportCallback();
            
          //  System.out.println("teleport " + dest.toString());

            if (playTeleportSound)
            {
                String sound = vrMovementStyle.endTeleportingSound;
                if (sound != null)
                {
                    player.playSound(SoundEvents.getRegisteredSoundEvent(sound), vrMovementStyle.endTeleportingSoundVolume, 1.0F);
                }
            }
            else
            {
                playFootstepSound(mc, dest.xCoord, dest.yCoord, dest.zCoord);
            }
  
        }
        else //standing still
        {
			doPlayerMoveInRoom(player);
        }



        mc.mcProfiler.endSection();
    }

	private Vec3d getHeadCenter(Minecraft mc) {
		Vec3d hmdpos=mc.roomScale.getHMDPos_Room();
		Vec3d hmddir= mc.roomScale.getHMDDir_World().normalize().scale(mc.vrSettings.headToHmdLength);
		return hmdpos.add(hmddir.scale(-1));
	}


	public void doTeleportCallback(){
        Minecraft mc = Minecraft.getMinecraft();

        this.disableSwing = 3;

        if(mc.vrSettings.vrLimitedSurvivalTeleport){
          mc.player.addExhaustion((float) (movementTeleportDistance / 16 * 1.2f));    
          
          if (!mc.vrPlayer.getFreeMove() && mc.playerController.isNotCreative() && mc.vrPlayer.vrMovementStyle.arcAiming){
          	teleportEnergy -= movementTeleportDistance * 4;	
          }       
        }
        
        mc.player.fallDistance = 0.0F;

        mc.player.movementTeleportTimer = -1;
        
    }
    
    private boolean wasYMoving;
    
    private void doPlayerMoveInRoom(EntityPlayerSP player){

     	if(roomScaleMovementDelay > 0){
    		roomScaleMovementDelay--;
    		return;
    	}
    	Minecraft mc = Minecraft.getMinecraft();

    	if(player.isSneaking()) {return;} //jrbudda : prevent falling off things or walking up blocks while moving in room scale.
    	if(player.isRiding()) return; //dont fall off the tracks man
    	if(player.isDead) return; //
    	if(player.isPlayerSleeping()) return; //
    	if(mc.jumpTracker.isjumping()) return; //
    	if(Math.abs(player.motionX) > 0.01) return;
    	if(Math.abs(player.motionZ) > 0.01) return;
    	
    	float playerHalfWidth = player.width / 2.0F;

    	// move player's X/Z coords as the HMD moves around the room

    	Vec3d eyePos = getHMDPos_World();

    	double x = eyePos.xCoord;
    	double y = player.posY;
    	double z = eyePos.zCoord;

    	// create bounding box at dest position
    	AxisAlignedBB bb = new AxisAlignedBB(
    			x - (double) playerHalfWidth,
    			y,
    			z - (double) playerHalfWidth,
    			x + (double) playerHalfWidth,
    			y + (double) player.height,
    			z + (double) playerHalfWidth);

    	Vec3d torso = null;

    	// valid place to move player to?
    	float var27 = 0.0625F;
    	boolean emptySpot = mc.world.getCollisionBoxes(player, bb).isEmpty();

    	if (emptySpot)
    	{
    		// don't call setPosition style functions to avoid shifting room origin
    		player.posX = x;
    		if (!mc.vrSettings.simulateFalling)	{
    			 player.posY = y;                	
    		}
    		player.posZ = z;
    
    		 if(player.getRidingEntity()!=null){ //you're coming with me, horse! //TODO: use mount's bounding box.
    				player.getRidingEntity().posX = x;
    				if (!mc.vrSettings.simulateFalling)	{
    					player.getRidingEntity().posY = y;                	
    	    		}
    			  	 player.getRidingEntity().posZ = z;
    		 }
    		 
    		player.setEntityBoundingBox(new AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.minY + player.height, bb.maxZ));
    		player.fallDistance = 0.0F;

    		torso = getEstimatedTorsoPosition(x, y, z);


    	}

    	//             test for climbing up a block
    	else if ((mc.vrSettings.walkUpBlocks || (mc.climbTracker.isGrabbingLadder() && mc.vrSettings.realisticClimbEnabled)) && player.fallDistance == 0)
    	{
    		if (torso == null)
    		{
    			torso = getEstimatedTorsoPosition(x, y, z);
    		}

    		// is the player significantly inside a block?
    		float climbShrink = player.width * 0.45f;
    		double shrunkClimbHalfWidth = playerHalfWidth - climbShrink;
    		AxisAlignedBB bbClimb = new AxisAlignedBB(
    				torso.xCoord - shrunkClimbHalfWidth,
    				bb.minY,
    				torso.zCoord - shrunkClimbHalfWidth,
    				torso.xCoord + shrunkClimbHalfWidth,
    				bb.maxY,
    				torso.zCoord + shrunkClimbHalfWidth);

    		boolean iscollided = !mc.world.getCollisionBoxes(player, bbClimb).isEmpty();

    		if(iscollided){
    			double xOffset = torso.xCoord - x;
    			double zOffset = torso.zCoord - z;

    			bb = bb.offset(xOffset, 0, zOffset);
         	 
    			int extra = 0;
    			if(player.isOnLadder() && mc.vrSettings.realisticClimbEnabled)
    				extra = 6;
    			
    			for (int i = 0; i <=10+extra ; i++)
    			{
    				bb = bb.offset(0, .1, 0);

    				emptySpot = mc.world.getCollisionBoxes(player, bb).isEmpty();
    				if (emptySpot)
    				{
    	    			x += xOffset;  	
    	    			z += zOffset;
    					y += 0.1f*i;
    					
    					player.posX = x;
    					player.posY = y;
    					player.posZ = z;
    					
    					player.setEntityBoundingBox(new AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ));

    					roomOrigin = roomOrigin.addVector(xOffset, 0.1f*i, zOffset);

    					Vec3d look = player.getLookVec();
    					Vec3d forward = new Vec3d(look.xCoord,0,look.zCoord).normalize();
    					player.fallDistance = 0.0F;
    					playFootstepSound(mc,
    							player.posX + forward.xCoord * 0.4f,
    							player.posY-player.height,
    							player.posZ + forward.zCoord * 0.4f);
    					break;
    				}
    			}
    		}
    	}
    	if(debugSpawn) System.out.println("dpminR " + player.posX + " " + player.posY + " " + player.posZ);

    }
	   
    public void playFootstepSound( Minecraft mc, double x, double y, double z )
    { //TODO: re-implement
//        Block block = mc.world.getBlockState(MathHelper.floor_double(x),
//                MathHelper.floor_double(y - 0.5f),
//                MathHelper.floor_double(z));
//
//        if (block != null && block.getMaterial() != Material.air)
//        {
//            mc.getSoundHandler().playSound(new PositionedSoundRecord(new ResourceLocation(block.stepSound.getStepSound()),
//                    (block.stepSound.getVolume() + 1.0F) / 8.0F,
//                    block.stepSound.getFrequency() * 0.5F,
//                    (float) x, (float) y, (float) z));
//        }
    }

    // use simple neck modeling to estimate torso location
    public Vec3d getEstimatedTorsoPosition(double x, double y, double z)
    {
        Entity player = Minecraft.getMinecraft().player;
        Vec3d look = player.getLookVec();
        Vec3d forward = new Vec3d(look.xCoord, 0, look.zCoord).normalize();
        float factor = (float)look.yCoord * 0.25f;
        Vec3d torso = new Vec3d(
                x + forward.xCoord * factor,
                y + forward.yCoord * factor,
                z + forward.zCoord * factor);

        return torso;
    }


    public void updateTeleportArc(Minecraft mc, Entity player)
    {
        Vec3d start = this.getControllerPos_World(1);
        Vec3d tiltedAim = mc.roomScale.getControllerDir_World(1);
        Matrix4f handRotation =MCOpenVR.getAimRotation(1);
        
        if(mc.vrSettings.seated){
        	start = mc.entityRenderer.getControllerRenderPos(0);
        	tiltedAim = mc.roomScale.getControllerDir_World(0);
        	handRotation =MCOpenVR.getAimRotation(0);
        }
        
        Matrix4f rot = Matrix4f.rotationY(this.worldRotationRadians);
        handRotation = Matrix4f.multiply(rot, handRotation);
        
        // extract hand roll
        Quatf handQuat = OpenVRUtil.convertMatrix4ftoRotationQuat(handRotation);
        EulerOrient euler = OpenVRUtil.getEulerAnglesDegYXZ(handQuat);
        
        int maxSteps = 50;
        movementTeleportArc[0] = new Vec3d(
        		start.xCoord,
        		start.yCoord,
        		start.zCoord);
        
        movementTeleportArcSteps = 1;

        // calculate gravity vector for arc
        float gravityAcceleration = 0.098f;
        Matrix4f rollCounter = OpenVRUtil.rotationZMatrix((float)Math.toRadians(-euler.roll));
        Matrix4f gravityTilt = OpenVRUtil.rotationXMatrix((float)Math.PI * -.8f);
        Matrix4f gravityRotation = Matrix4f.multiply(handRotation, rollCounter);
        
        Vector3f forward = new Vector3f(0,1,0);
        Vector3f gravityDirection = gravityRotation.transform(forward);
        Vec3d gravity = new Vec3d(-gravityDirection.x, -gravityDirection.y, -gravityDirection.z);
        
        gravity = gravity.scale(gravityAcceleration);

        
     //   gravity.rotateAroundY(this.worldRotationRadians);

        // calculate initial move step	
        float speed = 0.5f;
        Vec3d velocity = new Vec3d(
                tiltedAim.xCoord * speed,
                tiltedAim.yCoord * speed,
                tiltedAim.zCoord * speed);

        Vec3d pos = new Vec3d(start.xCoord, start.yCoord, start.zCoord);
        Vec3d newPos;

        // trace arc
        for (int i=movementTeleportArcSteps;i<maxSteps;i++)
        {
        	if (i*4 > teleportEnergy) {
        		break;
        		}
        	newPos = new Vec3d(
            pos.xCoord + velocity.xCoord,
            pos.yCoord + velocity.yCoord,
            pos.zCoord + velocity.zCoord);

      	
            boolean	water =false;
            if(mc.vrSettings.seated )
            	water = mc.entityRenderer.itemRenderer.inwater;
            else{
                water = mc.entityRenderer.itemRenderer.isInsideOfMaterial(start, Material.WATER);
            }
            
            RayTraceResult collision = tpRaytrace(player.world, pos, newPos, !water, true, false);
			
            if (collision != null && collision.typeOfHit != Type.MISS)
            {
        		
                movementTeleportArc[i] = new Vec3d(
                		collision.hitVec.xCoord,
                		collision.hitVec.yCoord,
                		collision.hitVec.zCoord);

                movementTeleportArcSteps = i + 1;

                Vec3d traceDir = pos.subtract(newPos).normalize();
                Vec3d reverseEpsilon = new Vec3d(-traceDir.xCoord * 0.02, -traceDir.yCoord * 0.02, -traceDir.zCoord * 0.02);

                checkAndSetTeleportDestination(mc, player, start, collision, reverseEpsilon);
                          
                break;
            }

            pos = new Vec3d(newPos.xCoord, newPos.yCoord, newPos.zCoord);


            movementTeleportArc[i] = new Vec3d(
            		newPos.xCoord,
            		newPos.yCoord,
            		newPos.zCoord);

            movementTeleportArcSteps = i + 1;

            velocity = velocity.add(gravity);

        }
    }

    /**
     * Performs a raycast against all blocks in the world. Args : Vec1, Vec2, stopOnLiquid,
     * ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock
     */
    public RayTraceResult tpRaytrace(World w, Vec3d vec31, Vec3d vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock)
    {
        if (!Double.isNaN(vec31.xCoord) && !Double.isNaN(vec31.yCoord) && !Double.isNaN(vec31.zCoord))
        {
            if (!Double.isNaN(vec32.xCoord) && !Double.isNaN(vec32.yCoord) && !Double.isNaN(vec32.zCoord))
            {
                int i = MathHelper.floor(vec32.xCoord);
                int j = MathHelper.floor(vec32.yCoord);
                int k = MathHelper.floor(vec32.zCoord);
                int l = MathHelper.floor(vec31.xCoord);
                int i1 = MathHelper.floor(vec31.yCoord);
                int j1 = MathHelper.floor(vec31.zCoord);
                BlockPos blockpos = new BlockPos(l, i1, j1);
                IBlockState iblockstate = w.getBlockState(blockpos);
                Block block = iblockstate.getBlock();
                if(iblockstate.getBlock() == Blocks.WATER){
                	ignoreBlockWithoutBoundingBox = !stopOnLiquid;
                }
                if (block == Blocks.WATERLILY || (!ignoreBlockWithoutBoundingBox || iblockstate.getCollisionBoundingBox(w, blockpos) != Block.NULL_AABB) && block.canCollideCheck(iblockstate, stopOnLiquid))
                {
                    RayTraceResult raytraceresult = iblockstate.collisionRayTrace(w, blockpos, vec31, vec32);

                    if (raytraceresult != null)
                    {
                        return raytraceresult;
                    }
                }

                RayTraceResult raytraceresult2 = null;
                int k1 = 200;

                while (k1-- >= 0)
                {
                    if (Double.isNaN(vec31.xCoord) || Double.isNaN(vec31.yCoord) || Double.isNaN(vec31.zCoord))
                    {
                        return null;
                    }

                    if (l == i && i1 == j && j1 == k)
                    {
                        return returnLastUncollidableBlock ? raytraceresult2 : null;
                    }

                    boolean flag2 = true;
                    boolean flag = true;
                    boolean flag1 = true;
                    double d0 = 999.0D;
                    double d1 = 999.0D;
                    double d2 = 999.0D;

                    if (i > l)
                    {
                        d0 = (double)l + 1.0D;
                    }
                    else if (i < l)
                    {
                        d0 = (double)l + 0.0D;
                    }
                    else
                    {
                        flag2 = false;
                    }

                    if (j > i1)
                    {
                        d1 = (double)i1 + 1.0D;
                    }
                    else if (j < i1)
                    {
                        d1 = (double)i1 + 0.0D;
                    }
                    else
                    {
                        flag = false;
                    }

                    if (k > j1)
                    {
                        d2 = (double)j1 + 1.0D;
                    }
                    else if (k < j1)
                    {
                        d2 = (double)j1 + 0.0D;
                    }
                    else
                    {
                        flag1 = false;
                    }

                    double d3 = 999.0D;
                    double d4 = 999.0D;
                    double d5 = 999.0D;
                    double d6 = vec32.xCoord - vec31.xCoord;
                    double d7 = vec32.yCoord - vec31.yCoord;
                    double d8 = vec32.zCoord - vec31.zCoord;

                    if (flag2)
                    {
                        d3 = (d0 - vec31.xCoord) / d6;
                    }

                    if (flag)
                    {
                        d4 = (d1 - vec31.yCoord) / d7;
                    }

                    if (flag1)
                    {
                        d5 = (d2 - vec31.zCoord) / d8;
                    }

                    if (d3 == -0.0D)
                    {
                        d3 = -1.0E-4D;
                    }

                    if (d4 == -0.0D)
                    {
                        d4 = -1.0E-4D;
                    }

                    if (d5 == -0.0D)
                    {
                        d5 = -1.0E-4D;
                    }

                    EnumFacing enumfacing;

                    if (d3 < d4 && d3 < d5)
                    {
                        enumfacing = i > l ? EnumFacing.WEST : EnumFacing.EAST;
                        vec31 = new Vec3d(d0, vec31.yCoord + d7 * d3, vec31.zCoord + d8 * d3);
                    }
                    else if (d4 < d5)
                    {
                        enumfacing = j > i1 ? EnumFacing.DOWN : EnumFacing.UP;
                        vec31 = new Vec3d(vec31.xCoord + d6 * d4, d1, vec31.zCoord + d8 * d4);
                    }
                    else
                    {
                        enumfacing = k > j1 ? EnumFacing.NORTH : EnumFacing.SOUTH;
                        vec31 = new Vec3d(vec31.xCoord + d6 * d5, vec31.yCoord + d7 * d5, d2);
                    }

                    l = MathHelper.floor(vec31.xCoord) - (enumfacing == EnumFacing.EAST ? 1 : 0);
                    i1 = MathHelper.floor(vec31.yCoord) - (enumfacing == EnumFacing.UP ? 1 : 0);
                    j1 = MathHelper.floor(vec31.zCoord) - (enumfacing == EnumFacing.SOUTH ? 1 : 0);
                    blockpos = new BlockPos(l, i1, j1);
                    IBlockState iblockstate1 = w.getBlockState(blockpos);
                    Block block1 = iblockstate1.getBlock();

                    if (!ignoreBlockWithoutBoundingBox || iblockstate1.getMaterial() == Material.PORTAL || iblockstate1.getCollisionBoundingBox(w, blockpos) != Block.NULL_AABB)
                    {
                        if (block1.canCollideCheck(iblockstate1, stopOnLiquid))
                        {
                            RayTraceResult raytraceresult1 = iblockstate1.collisionRayTrace(w, blockpos, vec31, vec32);

                            if (raytraceresult1 != null)
                            {
                                return raytraceresult1;
                            }
                        }
                        else
                        {
                            raytraceresult2 = new RayTraceResult(RayTraceResult.Type.MISS, vec31, enumfacing, blockpos);
                        }
                    }
                }

                return returnLastUncollidableBlock ? raytraceresult2 : null;
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }
    
    public void updateTeleportDestinations(EntityRenderer renderer, Minecraft mc, Entity player)
    {
        mc.mcProfiler.startSection("updateTeleportDestinations");

        // no teleporting if on a server that disallows teleporting
        if (getFreeMove())
        {
            movementTeleportDestination=new Vec3d(0,0,0);
            movementTeleportArcSteps = 0;
            return;
        }

        if (vrMovementStyle.arcAiming)
        {
            movementTeleportDestination=new Vec3d(0,0,0);

            if (movementTeleportProgress>0.0f)
            {
                updateTeleportArc(mc, player);
            }
        }
        else //non-arc modes.
        {
            Vec3d start = this.getControllerPos_World(1);
            Vec3d aimDir = mc.roomScale.getControllerDir_World(1);

            // setup teleport forwards to the mouse cursor
            double movementTeleportDistance = 250.0;
            Vec3d movementTeleportPos = start.addVector(
                    aimDir.xCoord * movementTeleportDistance,
                    aimDir.yCoord * movementTeleportDistance,
                    aimDir.zCoord * movementTeleportDistance);
            RayTraceResult collision = mc.world.rayTraceBlocks(start, movementTeleportPos, !mc.player.isInWater(), true, false);
            Vec3d traceDir = start.subtract(movementTeleportPos).normalize();
            Vec3d reverseEpsilon = new Vec3d(-traceDir.xCoord * 0.02, -traceDir.yCoord * 0.02, -traceDir.zCoord * 0.02);

            // don't update while charging up a teleport
            if (movementTeleportProgress != 0)
                return;

            if (collision != null && collision.typeOfHit != Type.MISS)
            {
                checkAndSetTeleportDestination(mc, player, start, collision, reverseEpsilon);
            }
        }
        mc.mcProfiler.endSection();
    }

    // look for a valid place to stand on the block that the trace collided with
    private boolean checkAndSetTeleportDestination(Minecraft mc, Entity player, Vec3d start, RayTraceResult collision, Vec3d reverseEpsilon)
    {

    	BlockPos bp = collision.getBlockPos();
    	IBlockState testClimb = player.world.getBlockState(bp);
    	if (testClimb.getBlock() == Blocks.WATER){
    		Vec3d hitVec = new Vec3d(collision.hitVec.xCoord, bp.getY(), collision.hitVec.zCoord );

    		Vec3d offset = hitVec.subtract(player.posX, player.getEntityBoundingBox().minY, player.posZ);
    		AxisAlignedBB bb = player.getEntityBoundingBox().offset(offset.xCoord, offset.yCoord, offset.zCoord);
    		boolean emptySpotReq = mc.world.getCollisionBoxes(player,bb).isEmpty();

    		if(!emptySpotReq){
    			Vec3d center = new Vec3d(bp).addVector(0.5, 0, 0.5);
    			offset = center.subtract(player.posX, player.getEntityBoundingBox().minY, player.posZ);
    			bb = player.getEntityBoundingBox().offset(offset.xCoord, offset.yCoord, offset.zCoord);
    			emptySpotReq = mc.world.getCollisionBoxes(player,bb).isEmpty(); 	
    		}
    		float ex = 0;
    		if(mc.vrSettings.seated)ex = 0.5f;
    		if(emptySpotReq){
    			movementTeleportDestination = new Vec3d((bb.maxX + bb.minX) /2,bb.minY+ex, (bb.maxZ + bb.minZ) /2);

    			movementTeleportDestinationSideHit = collision.sideHit;
    			return true;
    		}

    	} else if (collision.sideHit != EnumFacing.UP) 
    	{ //sides  		    		
    		//jrbudda require arc hitting top of block.	unless ladder or vine or creative or limits off.

			if(testClimb instanceof BlockLadder|| testClimb instanceof BlockVine){
    			Vec3d dest = new Vec3d(bp.getX()+0.5, bp.getY() + 0.5, bp.getZ()+0.5);

    			Block playerblock = mc.world.getBlockState(bp.down()).getBlock();
    			if(playerblock == testClimb.getBlock()) dest = dest.addVector(0,-1,0);

    			movementTeleportDestination = dest.scale(1);

    			movementTeleportDestinationSideHit = collision.sideHit;
    			return true; //really should check if the block above is passable. Maybe later.
    		} else {
    			if (!mc.player.capabilities.allowFlying && mc.vrSettings.vrLimitedSurvivalTeleport) {return false;} //if creative, check if can hop on top.
    		}
    	}

    	double y = 0;
    	BlockPos hitBlock = collision.getBlockPos().down();

    	for(int k = 0; k<2; k++){

    		testClimb = player.world.getBlockState(hitBlock);
    		Vec3d hitVec = new Vec3d(collision.hitVec.xCoord, hitBlock.getY() + testClimb.getBoundingBox(mc.world, hitBlock).maxY, collision.hitVec.zCoord );
    		Vec3d offset = hitVec.subtract(player.posX, player.getEntityBoundingBox().minY, player.posZ);
    		AxisAlignedBB bb = player.getEntityBoundingBox().offset(offset.xCoord, offset.yCoord, offset.zCoord);
    		double ex = 0;
    		if (testClimb.getBlock() == Blocks.SOUL_SAND) ex = 0.05;

    		boolean emptySpotReq = mc.world.getCollisionBoxes(player,bb).isEmpty() &&
    				!mc.world.getCollisionBoxes(player,bb.expand(0, .125 + ex, 0)).isEmpty();     

    		if(!emptySpotReq){
    			Vec3d center = new Vec3d(hitBlock).addVector(0.5, testClimb.getBoundingBox(mc.world, hitBlock).maxY, 0.5);
    			offset = center.subtract(player.posX, player.getEntityBoundingBox().minY, player.posZ);
    			bb = player.getEntityBoundingBox().offset(offset.xCoord, offset.yCoord, offset.zCoord);
    			emptySpotReq = mc.world.getCollisionBoxes(player,bb).isEmpty() &&
    					!mc.world.getCollisionBoxes(player,bb.expand(0, .125 + ex, 0)).isEmpty();     	
    		}

    		if(emptySpotReq){
    			Vec3d dest = new Vec3d((bb.maxX + bb.minX) /2, hitBlock.getY() + testClimb.getBoundingBox(mc.world, hitBlock).maxY, (bb.maxZ + bb.minZ) /2);
    			float maxTeleportDist = 16.0f;		

    			if (start.distanceTo(dest)  > maxTeleportDist) return false;

    			movementTeleportDestination = dest.scale(1);
    			movementTeleportDistance = start.distanceTo(movementTeleportDestination);
    			return true;
    		}

    		hitBlock = hitBlock.up();
    	}

    	return false;
    }

    private boolean canStand(World w, BlockPos bp){
    	if(w.getBlockState(bp).getBlock().isPassable(w, bp) && !w.getBlockState(bp).isFullBlock()) 
    		bp = bp.down(); //raytrace hit snow or something...
    	return !w.getBlockState(bp).getBlock().isPassable(w, bp) && w.getBlockState(bp.up()).getBlock().isPassable(w, bp.up()) &&  w.getBlockState(bp.up(2)).getBlock().isPassable(w, bp.up(2));
    }
    
    // rough interpolation between arc locations
    public Vec3d getInterpolatedArcPosition(float progress)
    {
        // not enough points to interpolate or before start
        if (movementTeleportArcSteps == 1 || progress <= 0.0f)
        {
            return new Vec3d(
                    movementTeleportArc[0].xCoord,
                    movementTeleportArc[0].yCoord,
                    movementTeleportArc[0].zCoord);
        }

        // past end of arc
        if (progress>=1.0f)
        {
            return new Vec3d(
                    movementTeleportArc[movementTeleportArcSteps-1].xCoord,
                    movementTeleportArc[movementTeleportArcSteps-1].yCoord,
                    movementTeleportArc[movementTeleportArcSteps-1].zCoord);
        }

        // which two points are we between?
        float stepFloat = progress * (float)(movementTeleportArcSteps - 1);
        int step = (int) Math.floor(stepFloat);

        double deltaX = movementTeleportArc[step+1].xCoord - movementTeleportArc[step].xCoord;
        double deltaY = movementTeleportArc[step+1].yCoord - movementTeleportArc[step].yCoord;
        double deltaZ = movementTeleportArc[step+1].zCoord - movementTeleportArc[step].zCoord;

        float stepProgress = stepFloat - step;

        return new Vec3d(
                movementTeleportArc[step].xCoord + deltaX * stepProgress,
                movementTeleportArc[step].yCoord + deltaY * stepProgress,
                movementTeleportArc[step].zCoord + deltaZ * stepProgress);
    }

    //VIVECRAFT SWINGING SUPPORT
    private Vec3d[] lastWeaponEndAir = new Vec3d[]{new Vec3d(0, 0, 0), new Vec3d(0,0,0)};
    private boolean[] lastWeaponSolid = new boolean[2];
	public Vec3d[] weaponEnd= new Vec3d[2];
	public Vec3d[] weaponEndlast= new Vec3d[]{new Vec3d(0, 0, 0), new Vec3d(0,0,0)};
	
    public boolean[] shouldIlookatMyHand= new boolean[2];
    public boolean[] IAmLookingAtMyHand= new boolean[2];
    
    public int disableSwing = 3;
    
    public void updateSwingAttack()
    {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;

        if (!mc.vrSettings.weaponCollision)
            return;

        if (mc.vrSettings.seated)
            return;
        
        if(mc.vrSettings.vrFreeMoveMode == mc.vrSettings.FREEMOVE_RUNINPLACE && player.moveForward > 0){
        	return; //dont hit things while RIPing.
        }
        
        if(player.isActiveItemStackBlocking()){
        	return; //dont hit things while blocking.
        }
        
        if(mc.jumpTracker.isjumping()) return;
        
        mc.mcProfiler.startSection("updateSwingAttack");
        
        Vec3d forward = new Vec3d(0,0,-1);
        
        for(int c =0 ;c<2;c++){

        	Vec3d handPos = this.getControllerPos_World(c);
        	Vec3d handDirection = getCustomHandVector(c, forward);

        	ItemStack is = player.getHeldItem(c==0?EnumHand.MAIN_HAND:EnumHand.OFF_HAND);
        	Item item = null;

        	double speedthresh = 1.8f;
        	float weaponLength;
        	float entityReachAdd;

        	if(is!=null )item = is.getItem();

            boolean tool = false;
            boolean sword = false;

            if(item instanceof ItemSword){
            	sword = true;
            	tool = true;    	
            }
            else if (item instanceof ItemTool ||
            		item instanceof ItemHoe
            		){
            	tool = true;
            }
            else if(item !=null && Reflector.forgeExists()){ //tinkers hack
            	String t = item.getClass().getSuperclass().getName().toLowerCase();
            	//System.out.println(c);
            	if (t.contains("weapon") || t.contains("sword")) {
            		sword = true;
            		tool = true;
            	} else 	if 	(t.contains("tool")){
            		tool = true;
            	}
            }    

            if (sword){
                 	entityReachAdd = 2.5f;
            		weaponLength = 0.3f;
            		tool = true;
            } else if (tool){
            	entityReachAdd = 1.8f;
            	weaponLength = 0.3f;
        		tool = true;
            } else if (item !=null){
            	weaponLength = 0.1f;
            	entityReachAdd = 0.3f;
            } else {
            	weaponLength = 0.0f;
            	entityReachAdd = 0.3f;
            }

        	weaponLength *= this.worldScale;

        	weaponEnd[c] = new Vec3d(
        			handPos.xCoord + handDirection.xCoord * weaponLength,
        			handPos.yCoord + handDirection.yCoord * weaponLength,
        			handPos.zCoord + handDirection.zCoord * weaponLength);     

        	if (disableSwing > 0 ) {
        		disableSwing--;
        		if(disableSwing<0)disableSwing = 0;
        		weaponEndlast[c] = new Vec3d(weaponEnd[c].xCoord,	 weaponEnd[c].yCoord, 	 weaponEnd[c].zCoord);
        		return;
        	}


        	float speed = (float) MCOpenVR.controllerHistory[c].averageSpeed(0.1);

        	weaponEndlast[c] = new Vec3d(weaponEnd[c].xCoord, weaponEnd[c].yCoord, weaponEnd[c].zCoord);

//        	int passes = (int) (tickDist / .1f); //TODO someday....

        	int bx = (int) MathHelper.floor(weaponEnd[c].xCoord);
        	int by = (int) MathHelper.floor(weaponEnd[c].yCoord);
        	int bz = (int) MathHelper.floor(weaponEnd[c].zCoord);

        	boolean inAnEntity = false;
        	boolean insolidBlock = false;
        	boolean canact = speed > speedthresh && !lastWeaponSolid[c];

        	Vec3d extWeapon = new Vec3d(
        			handPos.xCoord + handDirection.xCoord * (weaponLength + entityReachAdd),
        			handPos.yCoord + handDirection.yCoord * (weaponLength + entityReachAdd),
        			handPos.zCoord + handDirection.zCoord * (weaponLength + entityReachAdd));

        	//Check EntityCollisions first
        	//experiment.
        	AxisAlignedBB weaponBB = new AxisAlignedBB(
        			handPos.xCoord < extWeapon.xCoord ? handPos.xCoord : extWeapon.xCoord  ,
        					handPos.yCoord < extWeapon.yCoord ? handPos.yCoord : extWeapon.yCoord  ,
        							handPos.zCoord < extWeapon.zCoord ? handPos.zCoord : extWeapon.zCoord  ,
        									handPos.xCoord > extWeapon.xCoord ? handPos.xCoord : extWeapon.xCoord  ,
        											handPos.yCoord > extWeapon.yCoord ? handPos.yCoord : extWeapon.yCoord  ,
        													handPos.zCoord > extWeapon.zCoord ? handPos.zCoord : extWeapon.zCoord  
        			);

        	List entities = mc.world.getEntitiesWithinAABBExcludingEntity(
        			mc.getRenderViewEntity(), weaponBB);
        	for (int e = 0; e < entities.size(); ++e)
        	{
        		Entity hitEntity = (Entity) entities.get(e);
        		if (hitEntity.canBeCollidedWith() && !(hitEntity == mc.getRenderViewEntity().getRidingEntity()) )			{
        			if(mc.vrSettings.animaltouching && hitEntity instanceof EntityAnimal && !tool && !lastWeaponSolid[c]){
        				mc.playerController.interactWithEntity(player, hitEntity, is,c==0?EnumHand.MAIN_HAND:EnumHand.OFF_HAND);
        				MCOpenVR.triggerHapticPulse(c, 250);
        				lastWeaponSolid[c] = true;
        				inAnEntity = true;
        			} 
        			else 
        			{
        				if(canact){
        					mc.playerController.attackEntity(player, hitEntity);
        					MCOpenVR.triggerHapticPulse(c, 1000);
        					lastWeaponSolid[c] = true;
        				}
        				inAnEntity = true;
        			}
        		}
        	}

        	if(!inAnEntity){
        		if(mc.climbTracker.isClimbeyClimb()){
        			if(c == 0 && mc.gameSettings.keyBindAttack.isKeyDown() || !tool ) continue;
        			if(c == 1 && mc.gameSettings.keyBindForward.isKeyDown() || !tool ) continue;
        		}
        		
        		BlockPos bp =null;
        		bp = new BlockPos(weaponEnd[c]);
        		IBlockState block = mc.world.getBlockState(bp);
        		Material material = block.getMaterial();

        		// every time end of weapon enters a solid for the first time, trace from our previous air position
        		// and damage the block it collides with... 

        		RayTraceResult col = mc.world.rayTraceBlocks(lastWeaponEndAir[c], weaponEnd[c], true, false, true);
        		boolean flag = col!=null && col.getBlockPos().equals(bp); //fix ladder but prolly break everything else.
        		if (flag && (shouldIlookatMyHand[c] || (col != null && col.typeOfHit == Type.BLOCK)))
        		{
        			this.shouldIlookatMyHand[c] = false;
        			if (!(material == material.AIR))
        			{
        				if (block.getMaterial().isLiquid()) {
        					if(item == Items.BUCKET) {       						
        						//mc.playerController.onPlayerRightClick(player, player.world,is, col.blockX, col.blockY, col.blockZ, col.sideHit,col.hitVec);
        						this.shouldIlookatMyHand[c] = true;
        						if (IAmLookingAtMyHand[c]){

        							if(	Minecraft.getMinecraft().playerController.processRightClick(player, player.world,is,c==0?EnumHand.MAIN_HAND:EnumHand.OFF_HAND)==EnumActionResult.SUCCESS){
        								mc.entityRenderer.itemRenderer.resetEquippedProgress(c==0?EnumHand.MAIN_HAND:EnumHand.OFF_HAND);					
        							}
        						}
        					}
        				} else {
        					if(canact && (!mc.vrSettings.realisticClimbEnabled || !(block.getBlock() instanceof BlockLadder))) { 
        						int p = 3;
        						if(item instanceof ItemHoe){
        							Minecraft.getMinecraft().playerController.
        							processRightClickBlock(player, (WorldClient) player.world,is,bp,col.sideHit, col.hitVec, c==0?EnumHand.MAIN_HAND:EnumHand.OFF_HAND);
        						}else{
        							p += (speed - speedthresh) / 2;

        							for (int i = 0; i < p; i++)
        							{
        								//set delay to 0
        								clearBlockHitDelay();			
        								boolean test = mc.climbTracker.isGrabbingLadder();
        								//all this comes from plaeyrControllerMP clickMouse and friends.

        								//all this does is sets the block you're currently hitting, has no effect in survival mode after that.
        								//but if in creaive mode will clickCreative on the block
        								mc.playerController.clickBlock(col.getBlockPos(), col.sideHit);

        								if(!getIsHittingBlock()) //seems to be the only way to tell it broke.
        									break;

        								//apply destruction for survival only
        								mc.playerController.onPlayerDamageBlock(col.getBlockPos(), col.sideHit);

        								if(!getIsHittingBlock()) //seems to be the only way to tell it broke.
        									break;

        								//something effects
        								mc.effectRenderer.addBlockHitEffects(col.getBlockPos(), col.sideHit);

        							}
        						}
        						blockDust(col.hitVec.xCoord, col.hitVec.yCoord, col.hitVec.zCoord, 3*p, block);

        						MCOpenVR.triggerHapticPulse(c, 250*p);
        						//   System.out.println("Hit block speed =" + speed + " mot " + mot + " thresh " + speedthresh) ;            				
        						lastWeaponSolid[c] = true;
        					}
        					insolidBlock = true;
        				}
        			}
        		}
        	}

            if ((!inAnEntity && !insolidBlock ) || lastWeaponEndAir[c].lengthVector() ==0)
        	{
        		this.lastWeaponEndAir[c] = new Vec3d(
        				weaponEnd[c].xCoord,
        				weaponEnd[c].yCoord,
        				weaponEnd[c].zCoord
        				);
        		lastWeaponSolid[c] = false;
        	}


        }
        
        mc.mcProfiler.endSection();
    }
    
	private Random rand = new Random();

    
	public void blockDust(double x, double y, double z, int count, IBlockState bs){
		for (int i = 0; i < count; ++i)
		{
			Minecraft.getMinecraft().world.spawnParticle(EnumParticleTypes.BLOCK_DUST,
					x+ ((double)this.rand.nextFloat() - 0.5D)*.02f,
					y + ((double)this.rand.nextFloat() - 0.5D)*.02f,
					z + ((double)this.rand.nextFloat()- 0.5D)*.02f,
					((double)this.rand.nextFloat()- 0.5D)*.1f,((double)this.rand.nextFloat()- 0.5D)*.05f,((double)this.rand.nextFloat()- 0.5D)*.1f,
					new int[] {Block.getStateId(bs)});      	
		}
	}
    
	private boolean getIsHittingBlock(){
		return (Boolean)MCReflection.getField(MCReflection.PlayerControllerMP_isHittingBlock, Minecraft.getMinecraft().playerController);
	}
	
    // VIVE START - function to allow damaging blocks immediately
	private void clearBlockHitDelay() { 
		MCReflection.setField(MCReflection.PlayerControllerMP_blockHitDelay, Minecraft.getMinecraft().playerController, 0);
	}
        
	public boolean getFreeMove() { return isFreeMoveCurrent; }
	
	public void setFreeMove(boolean free) { 
		boolean was = isFreeMoveCurrent;
		isFreeMoveCurrent = free;

		if(free != was){
			CPacketCustomPayload pack =	NetworkHelper.getVivecraftClientPacket(PacketDiscriminators.MOVEMODE, isFreeMoveCurrent ?  new byte[]{1} : new byte[]{0});
			
			if(Minecraft.getMinecraft().getConnection() !=null)
				Minecraft.getMinecraft().getConnection().sendPacket(pack);
			
			if(Minecraft.getMinecraft().vrSettings.seated){
				Minecraft.getMinecraft().printChatMessage("Movement mode set to: " + (free ? "Free Move: WASD": "Teleport: W"));
				
			} else {
				Minecraft.getMinecraft().printChatMessage("Movement mode set to: " + (free ? Minecraft.getMinecraft().vrSettings.getKeyBinding(VRSettings.VrOptions.FREEMOVE_MODE): "Teleport"));
				
			}
		
			if(noTeleportClient && !free){
				Minecraft.getMinecraft().printChatMessage("Warning: This server may not allow teleporting.");
			}

		}
	}

	public float getTeleportEnergy () {return teleportEnergy;}


	public Vec3d getWalkMultOffset(){
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		if(player==null || !player.initFromServer)
			return Vec3d.ZERO;
		float walkmult=Minecraft.getMinecraft().vrSettings.walkMultiplier;
		Vec3d pos=vecMult(MCOpenVR.getCenterEyePosition(),interpolatedWorldScale);
		return new Vec3d(pos.xCoord*walkmult,pos.yCoord,pos.zCoord*walkmult).subtract(pos);
	}

	//================= IROOMSCALEADAPTER =============================
	
	
	public float worldScale =  Minecraft.getMinecraft().vrSettings.vrWorldScale;
	public float worldRotationRadians;
	public float interpolatedWorldScale =  Minecraft.getMinecraft().vrSettings.vrWorldScale;
	
	@Override
	public boolean isHMDTracking() {
		return MCOpenVR.headIsTracking;
	}

	private Vec3d vecMult(Vec3d in, float factor){
		return new Vec3d(in.xCoord * factor,	in.yCoord * factor, in.zCoord*factor);
	}
	
	@Override
	public Vec3d getHMDPos_World() {
		Vec3d out = vecMult(MCOpenVR.getCenterEyePosition(),interpolatedWorldScale).add(getWalkMultOffset()).rotateYaw(worldRotationRadians);
				
		return out.addVector(roomOrigin.xCoord, roomOrigin.yCoord, roomOrigin.zCoord);
	}

	@Override
	public Vec3d getHMDDir_World() {
		Vector3f v3 = MCOpenVR.headDirection;
		Vec3d out = new Vec3d(v3.x, v3.y, v3.z).rotateYaw(worldRotationRadians);
		return out;
	}

	@Override
	public float getHMDYaw_Room() {
		Vec3d dir = getHMDDir_Room();
		 return (float)Math.toDegrees(Math.atan2(-dir.xCoord, dir.zCoord));      
	}

	@Override
	public float getHMDYaw_World() {
		Vec3d dir = getHMDDir_World();
		 return (float)Math.toDegrees(Math.atan2(-dir.xCoord, dir.zCoord));      
	}

	@Override
	public float getHMDPitch_Room() {
		Vec3d dir = getHMDDir_Room();
		return (float)Math.toDegrees(Math.asin(dir.yCoord/dir.lengthVector())); 
	}

	@Override
	public float getHMDPitch_World() {
		Vec3d dir = getHMDDir_World();
		return (float)Math.toDegrees(Math.asin(dir.yCoord/dir.lengthVector())); 
	}

	@Override
	public Vec3d getRoomOriginPos_World() {
		return roomOrigin;
	}
	
	public Vec3d getInterpolatedRoomOriginPos_World(float nano) {
		Vec3d out = new Vec3d(
		lastroomOrigin.xCoord + (roomOrigin.xCoord - lastroomOrigin.xCoord) * (double)nano,
		lastroomOrigin.yCoord + (roomOrigin.yCoord - lastroomOrigin.yCoord) * (double)nano,
		lastroomOrigin.zCoord + (roomOrigin.zCoord - lastroomOrigin.zCoord) * (double)nano);
		return out;
	}

	@Override
	public Vec3d getRoomOriginUpDir_World() { //ummmm
		return new Vec3d(0, 1, 0);
	}
	
	@Override
	public FloatBuffer getHMDMatrix_World() {
		Matrix4f out = MCOpenVR.hmdRotation;
		Matrix4f rot = Matrix4f.rotationY(worldRotationRadians);
		return Matrix4f.multiply(rot, out).toFloatBuffer();
	}
	
	@Override
	public Vec3d getEyePos_World(renderPass eye) {
		Vec3d out = vecMult(MCOpenVR.getEyePosition(eye),interpolatedWorldScale).add(getWalkMultOffset()).rotateYaw(worldRotationRadians);			
		return out.addVector(roomOrigin.xCoord, roomOrigin.yCoord, roomOrigin.zCoord);
	}
	

	@Override
	public FloatBuffer getControllerMatrix_World(int controller) {
		Matrix4f out = MCOpenVR.getAimRotation(controller);
		Matrix4f rot = Matrix4f.rotationY(worldRotationRadians);
		return Matrix4f.multiply(rot,out).toFloatBuffer();
	}
	
	public FloatBuffer getControllerMatrix_World_Transposed(int controller) {
		Matrix4f out = MCOpenVR.getAimRotation(controller);
		Matrix4f rot = Matrix4f.rotationY(worldRotationRadians);
		return Matrix4f.multiply(rot,out).transposed().toFloatBuffer();
	}

	@Override
	public Vec3d getCustomControllerVector(int controller, Vec3d axis) {
		Vector3f v3 = MCOpenVR.getAimRotation(controller).transform(new Vector3f((float)axis.xCoord, (float)axis.yCoord,(float) axis.zCoord));
		Vec3d out =  new Vec3d(v3.x, v3.y, v3.z).rotateYaw(worldRotationRadians);
		return out;
	}

	@Override
	public Vec3d getCustomHMDVector(Vec3d axis) {
		Vector3f v3 = MCOpenVR.hmdRotation.transform(new Vector3f((float)axis.xCoord, (float)axis.yCoord, (float)axis.zCoord));
		Vec3d out = new Vec3d(v3.x, v3.y, v3.z).rotateYaw(worldRotationRadians);
		return out;
	}
	
	@Override
	public Vec3d getHMDPos_Room() {
		return vecMult(MCOpenVR.getCenterEyePosition(),interpolatedWorldScale).add(getWalkMultOffset());

	}

	@Override
	public Vec3d getControllerPos_Room(int i) {
		return vecMult(MCOpenVR.getAimSource(i),interpolatedWorldScale).add(getWalkMultOffset());
	}
	
	@Override
	public Vec3d getEyePos_Room(renderPass eye) {
		return vecMult(MCOpenVR.getEyePosition(eye),interpolatedWorldScale).add(getWalkMultOffset());
	}

	@Override
	public FloatBuffer getHMDMatrix_Room() {
		return MCOpenVR.hmdRotation.toFloatBuffer();
	}

	public FloatBuffer getHMDMatrix_Roomalt() {
		return MCOpenVR.hmdRotation.inverted().toFloatBuffer();
	}
	
	@Override
	public float getControllerYaw_Room(int controller) {
		if(controller == 0) return MCOpenVR.aimYaw;
		return MCOpenVR.laimYaw;
	}

	@Override
	public float getControllerPitch_Room(int controller) {
		if(controller == 0) return MCOpenVR.aimPitch;
		return MCOpenVR.laimPitch;
	}

	@Override
	public Vec3d getControllerPos_World(int c) {
		Vec3d out = vecMult(MCOpenVR.getAimSource(c),interpolatedWorldScale).add(getWalkMultOffset());
		out =out.rotateYaw(worldRotationRadians);
		return out.addVector(roomOrigin.xCoord, roomOrigin.yCoord, roomOrigin.zCoord);
	}

	@Override
	public Vec3d getControllerDir_Room(int c) {
		Vector3f v3 = c==0?MCOpenVR.controllerDirection : MCOpenVR.lcontrollerDirection;
		return new Vec3d(v3.x, v3.y, v3.z);
	}

	@Override
	public float getControllerYaw_World(int controller) {
		Vec3d dir = getControllerDir_World(controller);
		return (float)Math.toDegrees(Math.atan2(-dir.xCoord, dir.zCoord)); 
	}

	@Override
	public float getControllerPitch_World(int controller) {
		Vec3d dir = getControllerDir_World(controller);
		return (float)Math.toDegrees(Math.asin(dir.yCoord/dir.lengthVector())); 
	}

	@Override
	public float getControllerRoll_World(int controller) {
		org.lwjgl.util.vector.Matrix4f mat = (org.lwjgl.util.vector.Matrix4f)new org.lwjgl.util.vector.Matrix4f().load(getControllerMatrix_World(controller));
		return (float)-Math.toDegrees(Math.atan2(mat.m10, mat.m11));
	}

	@Override
	public Vec3d getControllerDir_World(int c) {
		Vector3f v3 = c==0?MCOpenVR.controllerDirection : MCOpenVR.lcontrollerDirection;
		if(c==2)v3=MCOpenVR.thirdcontrollerDirection;
		Vec3d out = new Vec3d(v3.x, v3.y, v3.z).rotateYaw(worldRotationRadians);
		return out;
	}

	@Override
	public boolean isControllerTracking(int c) {
		return MCOpenVR.controllerTracking[c];
	}
	
	@Override
	public Vec3d getCustomHandVector(int controller, Vec3d axis) {
		Vector3f v3 = MCOpenVR.getHandRotation(controller).transform(new Vector3f((float)axis.xCoord, (float)axis.yCoord,(float) axis.zCoord));
		Vec3d out =  new Vec3d(v3.x, v3.y, v3.z).rotateYaw(worldRotationRadians);
		return out;
	}

	@Override
	public Vec3d getHMDDir_Room() {
		Vector3f v3 = MCOpenVR.headDirection;
		Vec3d out = new Vec3d(v3.x, v3.y, v3.z);
		return out;
	}
	
}

