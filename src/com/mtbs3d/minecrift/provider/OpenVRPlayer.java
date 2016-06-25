package com.mtbs3d.minecrift.provider;


import de.fruitfly.ovr.enums.EyeType;
import de.fruitfly.ovr.structs.EulerOrient;
import de.fruitfly.ovr.structs.Matrix4f;
import de.fruitfly.ovr.structs.Quatf;
import de.fruitfly.ovr.structs.Vector3f;
import de.fruitfly.ovr.util.BufferUtil;
import io.netty.util.concurrent.GenericFutureListener;
import jopenvr.OpenVRUtil;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.monster.EntityMob;
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
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
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

// VIVE
public class OpenVRPlayer implements IRoomscaleAdapter
{
    public double lastRoomUpdateTime = 0;
    public Vec3d movementTeleportDestination = new Vec3d(0.0,0.0,0.0);
    public EnumFacing movementTeleportDestinationSideHit;
    public double movementTeleportProgress;
    public double movementTeleportDistance;
        
    private Vec3d roomOrigin = new Vec3d(0,0,0);
    private Vec3d lastroomOrigin = new Vec3d(0,0,0);
    private Vec3d interPolatedRoomOrigin = new Vec3d(0,0,0);
    
    public VRMovementStyle vrMovementStyle = new VRMovementStyle();
    public Vec3d[] movementTeleportArc = new Vec3d[50];
    public int movementTeleportArcSteps = 0;
    private boolean freeMoveMode = true;        // true when connected to another server that doesn't have this mod
	public boolean useLControllerForRestricedMovement = true;
    public double lastTeleportArcDisplayOffset = 0;
    public boolean noTeleportClient = true;
    
    private float teleportEnergy;
    
    public static OpenVRPlayer get()
    {
        return Minecraft.getMinecraft().vrPlayer;
    }

    public OpenVRPlayer()
    {
        for (int i=0;i<50;i++)
        {
            movementTeleportArc[i] = new Vec3d(0,0,0);
        }
    }
   
    public void onFrameUpdate(float nano){
     	interPolatedRoomOrigin = getInterpolatedRoomOriginPos_World(nano);
    }
    
    public void setRoomOrigin(double x, double y, double z, boolean reset ) { 
    	if (reset){
    		interPolatedRoomOrigin = new Vec3d(x, y, z);
    		lastroomOrigin = new Vec3d(x, y, z);
    	}
    	this.roomOrigin = new Vec3d(x, y, z);
        lastRoomUpdateTime = Minecraft.getMinecraft().stereoProvider.getCurrentTimeSecs();
    }
    
    //set room 
    public void snapRoomOriginToPlayerEntity(EntityPlayerSP player, boolean reset)
    {
        if (Thread.currentThread().getName().equals("Server thread"))
            return;

        if(player.posX == 0 && player.posY == 0 &&player.posZ == 0) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        
        Vec3d campos = mc.roomScale.getHMDPos_Room().rotateY(worldRotationRadians);

        double x = player.posX - campos.xCoord;
        double y = player.boundingBox.minY;
        double z = player.posZ - campos.zCoord;

        setRoomOrigin(x, y, z, reset);

    }
    
    public  double topofhead = 1.62;
    
    
    private float lastworldRotation= 0f;
	private float lastWorldScale;
    
	public void checkandUpdateRotateScale(){
		Minecraft mc = Minecraft.getMinecraft();
	    this.worldScale =  mc.vrSettings.vrWorldScale;
	    this.worldRotationRadians = (float) Math.toRadians(mc.vrSettings.vrWorldRotation);
	    
	    if (worldRotationRadians!= lastworldRotation || worldScale != lastWorldScale) {
	    	if(mc.thePlayer!=null)snapRoomOriginToPlayerEntity(mc.thePlayer, true);
	    	MCOpenVR.onGuiScreenChanged(mc.currentScreen, mc.currentScreen);
	    }
	    lastworldRotation = worldRotationRadians;
	    lastWorldScale = worldScale;		
	}

	
	
	
    public void onLivingUpdate(EntityPlayerSP player, Minecraft mc, Random rand)
    {
    	if(!player.initFromServer) return;
    	
    	this.lastroomOrigin = new Vec3d(
    			roomOrigin.xCoord,
    			roomOrigin.yCoord,
    			roomOrigin.zCoord
    			);
    	

        updateSwingAttack();
        
        this.checkandUpdateRotateScale();

        
       if(mc.vrSettings.vrAllowCrawling){         //experimental
           topofhead = (double) (mc.roomScale.getHMDPos_Room().yCoord + .05);
           
           if(topofhead < .5) {topofhead = 0.5f;}
           if(topofhead > 1.8) {topofhead = 1.8f;}
           
           player.height = (float) topofhead - 0.05f;
           player.spEyeHeight = player.height - 1.62f;
           player.boundingBox.setMaxY( player.boundingBox.minY +  topofhead);  	   
       } else {
    	   player.height = 1.8f;
    	   player.spEyeHeight = 0.12f;
       }
      
        // don't do teleport movement if on a server that doesn't have this mod installed
        if (getFreeMoveMode()) {
        	
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
                        player.playSound(SoundEvents.getSoundEvent(sound), vrMovementStyle.startTeleportingSoundVolume,
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
//                                        player.worldObj,
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
                            player.playSound(SoundEvents.getSoundEvent(sound), vrMovementStyle.startTeleportingSoundVolume,
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
                          //  player.worldObj.spawnParticle("instantSpell", dest.xCoord, dest.yCoord, dest.zCoord, 0, 1.0, 0);
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
//                                        player.worldObj,
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
            movementTeleportDistance = (float)MathHelper.sqrt_double(dest.squareDistanceTo(player.posX, player.posY, player.posZ));
            boolean playTeleportSound = movementTeleportDistance > 0.0f && vrMovementStyle.endTeleportingSound != null;
            Block block = null;

            if (playTeleportSound)
            {
                String sound = vrMovementStyle.endTeleportingSound;
                if (sound != null)
                {
                    player.playSound(SoundEvents.getSoundEvent(sound), vrMovementStyle.endTeleportingSoundVolume, 1.0F);
                }
            }
            else
            {
                playFootstepSound(mc, dest.xCoord, dest.yCoord, dest.zCoord);
            }

            this.disableSwing = 3;
            
            //execute teleport      
            player.setPositionAndUpdate(dest.xCoord, dest.yCoord, dest.zCoord);         
           
            if(mc.vrSettings.vrLimitedSurvivalTeleport){
              player.addExhaustion((float) (movementTeleportDistance / 16 * 1.2f));    
              
              if (!mc.vrPlayer.getFreeMoveMode() && mc.playerController.isNotCreative() && mc.vrPlayer.vrMovementStyle.arcAiming){
              	teleportEnergy -= movementTeleportDistance * 4;	
              }              
            }
               
            
          //  System.out.println("teleport " + dest.toString());
            player.fallDistance = 0.0F;

            if (playTeleportSound)
            {
                String sound = vrMovementStyle.endTeleportingSound;
                if (sound != null)
                {
                    player.playSound(SoundEvents.getSoundEvent(sound), vrMovementStyle.endTeleportingSoundVolume, 1.0F);
                }
            }
            else
            {
                playFootstepSound(mc, dest.xCoord, dest.yCoord, dest.zCoord);
            }

            player.movementTeleportTimer = -1;
            
        }
        else //standing still
        {
			doPlayerMoveInRoom(player);
        }
        mc.mcProfiler.endSection();
    }

    private boolean wasYMoving;
    
    private void doPlayerMoveInRoom(EntityPlayerSP player){
    	// this needs... work...
    	if(player.isSneaking()) {return;} //jrbudda : prevent falling off things or walking up blocks while moving in room scale.
    	if(player.isRiding()) return; //dont fall off the tracks man
    	if(player.isDead) return; //
    	if(player.isPlayerSleeping()) return; //
    	if(this.interPolatedRoomOrigin.xCoord == 0 && this.interPolatedRoomOrigin.yCoord ==0 && this.interPolatedRoomOrigin.zCoord == 0) return;
    	
    	if(Math.abs(player.motionX) > 0.01) return;
    	if(Math.abs(player.motionZ) > 0.01) return;
    	
    	Minecraft mc = Minecraft.getMinecraft();
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
    			y - (double) player.height,
    			z + (double) playerHalfWidth);

    	Vec3d torso = null;

    	// valid place to move player to?
    	float var27 = 0.0625F;
    	boolean emptySpot = mc.theWorld.getCollisionBoxes(player, bb).isEmpty();

    	if (emptySpot)
    	{
    		// don't call setPosition style functions to avoid shifting room origin
    		player.lastTickPosX = player.prevPosX = player.posX = x;
    		if (!mc.vrSettings.simulateFalling)	{
    			player.lastTickPosY = player.prevPosY = player.posY = y;                	
    		}
    		player.lastTickPosZ = player.prevPosZ = player.posZ = z;
    
    		 if(player.getRidingEntity()!=null){ //you're coming with me, horse! //TODO: use mount's bounding box.
    				player.getRidingEntity().lastTickPosX = player.getRidingEntity().prevPosX =  player.getRidingEntity().posX = x;
    				if (!mc.vrSettings.simulateFalling)	{
    					player.getRidingEntity().lastTickPosY = player.getRidingEntity().prevPosY =  	 player.getRidingEntity().posY = y;                	
    	    		}
    				player.getRidingEntity().lastTickPosZ = player.getRidingEntity().prevPosZ =  	 player.getRidingEntity().posZ = z;
    		 }
    		 
    		player.setBoundingBox(new AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.minY + player.height, bb.maxZ));
    		player.fallDistance = 0.0F;

    		torso = getEstimatedTorsoPosition(x, y, z);


    	}

    	//             test for climbing up a block
    	else if (mc.vrSettings.walkUpBlocks && player.fallDistance == 0)
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

    		boolean notyet = mc.theWorld.getCollisionBoxes(player, bbClimb).isEmpty();

    		if(!notyet){
    			double xOffset = torso.xCoord - x;
    			double zOffset = torso.zCoord - z;
    			bb = bb.offset(xOffset, 0, zOffset);
         	 
    			for (int i = 0; i <=10 ; i++)
    			{
    				bb = bb.offset(0, .1, 0);

    				emptySpot = mc.theWorld.getCollisionBoxes(player, bb).isEmpty();
    				if (emptySpot)
    				{
    	    			x += xOffset;  	
    	    			z += zOffset;
    					y += 0.1f*i;
    					
    					player.lastTickPosX = player.prevPosX = player.posX = x;
    					player.lastTickPosY = player.prevPosY = player.posY = y;
    					player.lastTickPosZ = player.prevPosZ = player.posZ = z;
    					
    					player.setBoundingBox(new AxisAlignedBB(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ));

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
    }
	
    public void playFootstepSound( Minecraft mc, double x, double y, double z )
    { //TODO: re-implement
//        Block block = mc.theWorld.getBlockState(MathHelper.floor_double(x),
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
        Entity player = Minecraft.getMinecraft().thePlayer;
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
        Vec3d start = this.getControllerOffhandPos_World();
        Vec3d tiltedAim = mc.roomScale.getControllerOffhandDir_World();
        Matrix4f handRotation =MCOpenVR.getAimRotation(1);
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
        
        gravity = gravity.multiply(gravityAcceleration);

        
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

            RayTraceResult collision = mc.theWorld.rayTraceBlocks(pos, newPos, !mc.thePlayer.isInWater(), true, false);
			
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

            velocity = velocity.addVector(gravity);

        }
    }

    public void updateTeleportDestinations(EntityRenderer renderer, Minecraft mc, Entity player)
    {
        mc.mcProfiler.startSection("updateTeleportDestinations");

        // no teleporting if on a server that disallows teleporting
        if (getFreeMoveMode())
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
            Vec3d start = this.getControllerOffhandPos_World();
            Vec3d aimDir = mc.roomScale.getControllerOffhandDir_World();

            // setup teleport forwards to the mouse cursor
            double movementTeleportDistance = 250.0;
            Vec3d movementTeleportPos = start.addVector(
                    aimDir.xCoord * movementTeleportDistance,
                    aimDir.yCoord * movementTeleportDistance,
                    aimDir.zCoord * movementTeleportDistance);
            RayTraceResult collision = mc.theWorld.rayTraceBlocks(start, movementTeleportPos, !mc.thePlayer.isInWater(), true, false);
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
        boolean bFoundValidSpot = false;

        
		if (collision.sideHit != EnumFacing.DOWN) 
		{ //sides
		//jrbudda require arc hitting top of block.	unless ladder or vine.
			BlockPos bp = collision.getBlock();
			Block testClimb = player.worldObj.getBlockState(collision.getBlock()).getBlock();
		//	System.out.println(testClimb.getUnlocalizedName() + " " + collision.typeOfHit + " " + collision.sideHit);

			if ( testClimb == Blocks.ladder || testClimb == Blocks.vine) {
			            Vec3d dest = new Vec3d(bp.getX()+0.5, bp.getY() + 0.5, bp.getZ()+0.5);
			            
	            		Block playerblock = mc.theWorld.getBlockState(bp.getBlockPosBelow()).getBlock();
	            		if(playerblock == testClimb) dest = dest.addVector(0,-1,0);
	            		
                        movementTeleportDestination = dest.multiply(1);

                        movementTeleportDestinationSideHit = collision.sideHit;
						return true; //really should check if the block above is passable. Maybe later.
			} else {
					if (!mc.thePlayer.capabilities.allowFlying && mc.vrSettings.vrLimitedSurvivalTeleport) {return false;} //if creative, check if can hop on top.
			}
		}
		
        for ( int k = 0; k < 1 && !bFoundValidSpot; k++ )
        {
            Vec3d hitVec = collision.hitVec;// ( k == 1 ) ? collision.hitVec.addVector(-reverseEpsilon.xCoord, -reverseEpsilon.yCoord, -reverseEpsilon.zCoord)
                    						//: collision.hitVec.addVector(reverseEpsilon.xCoord, reverseEpsilon.yCoord, reverseEpsilon.zCoord);

            Vec3d debugPos = new Vec3d(
                    MathHelper.floor_double(hitVec.xCoord) + 0.5,
                    MathHelper.floor_double(hitVec.yCoord),
                    MathHelper.floor_double(hitVec.zCoord) + 0.5);

            BlockPos bp = collision.getBlock();
            

            // search for a solid block with two empty blocks above it
            int startBlockY = bp.getY() -1 ; 
            startBlockY = Math.max(startBlockY, 0);
            for (int by = startBlockY; by < startBlockY + 2; by++)
            {
            	if (canStand(player.worldObj,bp))
            	{
            		float maxTeleportDist = 16.0f;

            		float var27 = 0.0625F; //uhhhh?

            		double ox = hitVec.xCoord - player.posX;
            		double oy = by + 1 - player.posY;
            		double oz = hitVec.zCoord - player.posZ;
            		AxisAlignedBB bb = player.boundingBox.contract((double)var27).offset(ox, oy, oz); 
            		bb=new AxisAlignedBB(bb.minX,by+1f , bb.minZ, bb.maxX, by+2.8f, bb.maxZ);
            		boolean emptySpotReq = mc.theWorld.getCollisionBoxes(player,bb).isEmpty();

            		double ox2 = bp.getX() + 0.5f - player.posX;
            		double oy2 = by + 1.0f - player.posY;
            		double oz2 = bp.getZ() + 0.5f - player.posZ;
            		AxisAlignedBB bb2 = player.boundingBox.contract(var27).offset(ox2, oy2, oz2);
            		bb2=new AxisAlignedBB(bb2.minX,by+1f , bb2.minZ, bb2.maxX, by+2.8f, bb2.maxZ);

            		boolean emptySpotCenter = mc.theWorld.getCollisionBoxes(player,bb2).isEmpty();

            		List l = mc.theWorld.getCollisionBoxes(player,bb2);

            		Vec3d dest;

            		//teleport to exact spot unless collision, then teleport to center.

            		if (emptySpotReq) {           	
            			dest = new Vec3d(hitVec.xCoord, by+1,hitVec.zCoord);
            		}
            		else {
            			dest = new Vec3d(bp.getX() + 0.5f, by + 1f, bp.getZ() + 0.5f);
            		}



            		if (start.distanceTo(dest) <= maxTeleportDist && (emptySpotReq || emptySpotCenter))
            		{

            			IBlockState testClimb = player.worldObj.getBlockState(new BlockPos(bp.getX(), by, bp.getY()));
            		           			
            			double y = 1; //TODO: Re-implement testClimb.getBlockBoundsMaxY();
            			if (testClimb == Blocks.farmland) y = 1f; //cheeky bastard
            			
            			movementTeleportDestination = dest.multiply(1);


            			debugPos = new Vec3d(bp.getX() + 0.5,by+1,bp.getZ() + 0.5);
            					
        

            			bFoundValidSpot = true;

            			break;

            		}
            	}

            }
        }
        
        if(bFoundValidSpot) { movementTeleportDistance = start.distanceTo(movementTeleportDestination);}
        
        return bFoundValidSpot;
    }

    private boolean canStand(World w, BlockPos bp){
    	return false; //TODO reimplement
   // 	return w.getBlockState(bp).isCollidable() && w.getBlockState(bp.getBlockPosAbove()).isPassable() &&  w.getBlockState(bp.getBlockPosAbove(2)).isPassable();
 
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

    private Vec3d lastWeaponEndAir = new Vec3d(0,0,0);
    private boolean lastWeaponSolid = false;

    public float weapongSwingLen;
	public Vec3d weaponEnd;
	public Vec3d weaponEndlast;
	public float tickDist;
    public float lastmot;
	
    public int disableSwing = 3;
    
    public void updateSwingAttack()
    {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;

        if (!mc.vrSettings.weaponCollision)
            return;

        mc.mcProfiler.startSection("updateSwingAttack");

        Vec3d handPos = this.getControllerMainPos_World();
        Vec3d handDirection = this.getControllerMainDir_World();
        
        float thismot =(float) (40* Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ));
        if(!player.onGround) //higher threshold when jumping and falling.
        	thismot =(float) (40* Math.sqrt(player.motionX * player.motionX + player.motionY * player.motionY + player.motionZ * player.motionZ));	
        
        float mot = Math.max(thismot, lastmot);
       
        lastmot = thismot;
        
        ItemStack is = player.inventory.getCurrentItem();
        Item item = null;

        double speedthresh = (float) (is==null ? 3.5f + mot: 4.2f + mot); //account for lower apparent speed due to shorter fulcrum.         
        float weaponLength = is == null ?  0 : 0.3f; //no reach for hand
        float entityReachAdd =0f;
      
        if(is!=null )item = is.getItem();
        
        if (item instanceof ItemSword){
        		entityReachAdd = 2.5f;
        		weaponLength = 0.3f;
               	speedthresh = 3f + mot;
        } else if (item instanceof ItemTool ||
        		item instanceof ItemHoe
        		){
        	entityReachAdd = 1.8f;
        	weaponLength = 0.3f;
        	speedthresh = 3f + mot;
        } else {
        	weaponLength = 0.0f;
        	entityReachAdd = 0.3f;
        	speedthresh = 1.5f + mot;
        }

        weaponLength *= this.worldScale;
        
        weapongSwingLen = weaponLength;
        weaponEnd = new Vec3d(
                handPos.xCoord + handDirection.xCoord * weaponLength,
                handPos.yCoord + handDirection.yCoord * weaponLength,
                handPos.zCoord + handDirection.zCoord * weaponLength);     
        
        if (disableSwing > 0 ) {
        	disableSwing--;
        	if(disableSwing<0)disableSwing = 0;
        	weaponEndlast = new Vec3d(weaponEnd.xCoord,	 weaponEnd.yCoord, 	 weaponEnd.zCoord);
        	lastmot = mot;
        	return;
        }
        
        tickDist = (float) (weaponEndlast.subtract(weaponEnd).lengthVector());
        
        float speed = (float) (tickDist * 20);
        
     	weaponEndlast = new Vec3d(weaponEnd.xCoord,	 weaponEnd.yCoord, 	 weaponEnd.zCoord);
        
        int passes = (int) (tickDist / .1f);
                 
        BlockPos bp = new BlockPos(
         MathHelper.floor_double(weaponEnd.xCoord),
         MathHelper.floor_double(weaponEnd.yCoord),
         MathHelper.floor_double(weaponEnd.zCoord));

        boolean inAnEntity = false;
        boolean insolidBlock = false;
        boolean canact = speed > speedthresh && !lastWeaponSolid;
               
        Vec3d extWeapon = new Vec3d(
                handPos.xCoord + handDirection.xCoord * (weaponLength + entityReachAdd),
                handPos.yCoord + handDirection.yCoord * (weaponLength + entityReachAdd),
                handPos.zCoord + handDirection.zCoord * (weaponLength + entityReachAdd));
        
        	//Check EntityCollisions first
        	{

        		//experiment.
        		AxisAlignedBB weaponBB = new AxisAlignedBB(
        				handPos.xCoord < extWeapon.xCoord ? handPos.xCoord : extWeapon.xCoord  ,
        						handPos.yCoord < extWeapon.yCoord ? handPos.yCoord : extWeapon.yCoord  ,
        								handPos.zCoord < extWeapon.zCoord ? handPos.zCoord : extWeapon.zCoord  ,
        										handPos.xCoord > extWeapon.xCoord ? handPos.xCoord : extWeapon.xCoord  ,
        												handPos.yCoord > extWeapon.yCoord ? handPos.yCoord : extWeapon.yCoord  ,
        														handPos.zCoord > extWeapon.zCoord ? handPos.zCoord : extWeapon.zCoord  
        				);

        		List entities = mc.theWorld.getEntitiesWithinAABBExcludingEntity(
        				mc.getRenderViewEntity(), weaponBB);
        		for (int e = 0; e < entities.size(); ++e)
        		{
        			Entity hitEntity = (Entity) entities.get(e);
        			if (hitEntity.canBeCollidedWith())
        			{
        				if(canact){
        					mc.playerController.attackEntity(player, hitEntity);
        					this.triggerHapticPulse(0, 1000);
           					lastWeaponSolid = true;
        				}
         				inAnEntity = true;
         			}
        		}

        	if(!inAnEntity){
        		IBlockState block = mc.theWorld.getBlockState(bp);
        		Material material = block.getMaterial();

        		// every time end of weapon enters a solid for the first time, trace from our previous air position
        		// and damage the block it collides with... 

        		RayTraceResult col = mc.theWorld.rayTraceBlocks(lastWeaponEndAir, weaponEnd, false, false, true);
        		BlockPos hitblock=col.getBlock();
        		if (col != null && col.typeOfHit == Type.BLOCK)
        		{
        			if (!(block.getMaterial() == material.air) && !block.getMaterial().isLiquid())
        			{
        				if(canact){
        					float hardness = 0;// block.getPlayerRelativeBlockHardness(mc.thePlayer, mc.thePlayer.worldObj, hitblock);
        				//	System.out.println("hardness=" + hardness);
        					if (hardness * 4.0f > 1.0f)
        					{
        						mc.playerController.onPlayerDestroyBlock(hitblock);
        					} else
        					{
        						//is this viable?
        						//mc.playerController.clearBlockHitDelay();
        						for (int i = 0; i < 4; i++)
        						{
        							mc.playerController.onPlayerDamageBlock(hitblock, col.sideHit);
        						}
        					}
             				this.triggerHapticPulse(0, 1000);
            			//   System.out.println("Hit block speed =" + speed + " mot " + mot + " thresh " + speedthresh) ;            				
            				lastWeaponSolid = true;
        				}
           				insolidBlock = true;
        			}
        		}
        	}
        }
        
        	
        	
        if (!inAnEntity && !insolidBlock)
        {
        	this.lastWeaponEndAir = new Vec3d(
        			weaponEnd.xCoord,
        			weaponEnd.yCoord,
        			weaponEnd.zCoord
        			);
            lastWeaponSolid = false;
        }
        mc.mcProfiler.endSection();
    }
	
	public boolean getFreeMoveMode() { return freeMoveMode; }
	
	public void setFreeMoveMode(boolean free) { 
		boolean was = freeMoveMode;
		if(noTeleportClient){
			freeMoveMode = true;
		} else {
			freeMoveMode = free;
		}

		if(free != was){
			CPacketCustomPayload pack =	NetworkHelper.getVivecraftClientPacket(PacketDiscriminators.MOVEMODE, freeMoveMode ?  new byte[]{1} : new byte[]{0});
			Minecraft.getMinecraft().getNetHandler().addToSendQueue(pack);
		}
	}

	public float getTeleportEnergy () {return teleportEnergy;}

	//================= IROOMSCALEADAPTER =============================
	
	
	float worldScale =  Minecraft.getMinecraft().vrSettings.vrWorldScale;
	float worldRotationRadians;
	
	@Override
	public boolean isHMDTracking() {
		return MCOpenVR.headIsTracking;
	}

	private Vec3d vecMult(Vec3d in, float factor){
		return new Vec3d(in.xCoord * factor,	in.yCoord * factor, in.zCoord*factor);
	}
	
	@Override
	public Vec3d getHMDPos_World() {	
		Vec3d out = vecMult(MCOpenVR.getCenterEyePosition(),worldScale).rotateY(worldRotationRadians);
		return out.addVector(interPolatedRoomOrigin.xCoord, interPolatedRoomOrigin.yCoord, interPolatedRoomOrigin.zCoord);
	}

	@Override
	public Vec3d getHMDDir_World() {
		Vector3f v3 = MCOpenVR.headDirection;
		Vec3d out = new Vec3d(v3.x, v3.y, v3.z).rotateY(worldRotationRadians);
		return out;
	}

	@Override
	public float getHMDYaw_World() {
		Vec3d dir = getHMDDir_World();
		 return (float)Math.toDegrees(Math.atan2(dir.xCoord, dir.zCoord));    
	}

	@Override
	public float getHMDPitch_World() {
		Vec3d dir = getHMDDir_World();
		return (float)Math.toDegrees(Math.asin(dir.yCoord/dir.lengthVector())); 
	}

	@Override
	public boolean isControllerMainTracking() {
		return MCOpenVR.controllerTracking[0];
	}

	@Override
	public Vec3d getControllerMainPos_World() {
		Vec3d out = vecMult(MCOpenVR.getAimSource(0),worldScale).rotateY(worldRotationRadians);
		return out.addVector(interPolatedRoomOrigin.xCoord, interPolatedRoomOrigin.yCoord, interPolatedRoomOrigin.zCoord);
		}

	@Override
	public Vec3d getControllerMainDir_World() {
		Vector3f v3 = MCOpenVR.controllerDirection;
		Vec3d out = new Vec3d(v3.x, v3.y, v3.z).rotateY(worldRotationRadians);
		return out;
	}

	@Override
	public float getControllerMainYaw_World() {
		return (float) (MCOpenVR.aimYaw + Minecraft.getMinecraft().vrSettings.vrWorldRotation);
	}

	@Override
	public float getControllerMainPitch_World() {
		return MCOpenVR.aimPitch;
	}

	@Override
	public boolean isControllerOffhandTracking() {
		return MCOpenVR.controllerTracking[1];
	}

	@Override
	public Vec3d getControllerOffhandPos_World() {
		Vec3d out = vecMult(MCOpenVR.getAimSource(1),worldScale).rotateY(worldRotationRadians);
		return out.addVector(interPolatedRoomOrigin.xCoord, interPolatedRoomOrigin.yCoord, interPolatedRoomOrigin.zCoord);	}

	@Override
	public Vec3d getControllerOffhandDir_World() {
		Vector3f v3 = MCOpenVR.lcontrollerDirection;
		Vec3d out = new Vec3d(v3.x, v3.y, v3.z).rotateY(worldRotationRadians);
		return out;
	}

	@Override
	public float getControllerOffhandYaw_World() {
		return  (float) (MCOpenVR.laimYaw + Minecraft.getMinecraft().vrSettings.vrWorldRotation);
	}

	@Override
	public float getControllerOffhandPitch_World() {
		return MCOpenVR.laimPitch;
	}

	@Override
	public Vec3d getRoomOriginPos_World() {
		return roomOrigin;
	}
	
	private Vec3d getInterpolatedRoomOriginPos_World(float nano) {
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
	
	public EulerOrient getHMDEuler_World(){ //TOTO: important place to add user rotation.
		EulerOrient out = MCOpenVR.getOrientationEuler(EyeType.ovrEye_Center);
		out.yaw += Math.toDegrees(this.worldRotationRadians);
		return out;
	}
	

	@Override
	public void triggerHapticPulse(int controller, int strength) {
		MCOpenVR.triggerHapticPulse(controller, strength);
	}

	@Override
	public FloatBuffer getHMDMatrix_World() {
		Matrix4f out = MCOpenVR.hmdRotation;
		Matrix4f rot = Matrix4f.rotationY(worldRotationRadians);
		return Matrix4f.multiply(rot, out).toFloatBuffer();
	}
	
	@Override
	public Vec3d getEyePos_World(EyeType eye) {
		Vec3d out = vecMult(MCOpenVR.getEyePosition(eye),worldScale).rotateY(worldRotationRadians);
		return out.addVector(interPolatedRoomOrigin.xCoord, interPolatedRoomOrigin.yCoord, interPolatedRoomOrigin.zCoord);
	}
	

	@Override
	public FloatBuffer getControllerMatrix_World(int controller) {
		Matrix4f out = MCOpenVR.getAimRotation(controller);
		Matrix4f rot = Matrix4f.rotationY(worldRotationRadians);
		return Matrix4f.multiply(rot,out).transposed().toFloatBuffer();
	}

	@Override
	public Vec3d getCustomControllerVector(int controller, Vec3d axis) {
		Vector3f v3 = MCOpenVR.getAimRotation(controller).transform(new Vector3f((float)axis.xCoord, (float)axis.yCoord,(float) axis.zCoord));
		Vec3d out =  new Vec3d(v3.x, v3.y, v3.z).rotateY(worldRotationRadians);
		return out;
	}

	@Override
	public Vec3d getHMDPos_Room() {
		return vecMult(MCOpenVR.getCenterEyePosition(),worldScale);
	}

	@Override
	public Vec3d getControllerPos_Room(int i) {
		return vecMult(MCOpenVR.getAimSource(i),worldScale);
	}
	
	@Override
	public Vec3d getEyePos_Room(EyeType eye) {
		return vecMult(MCOpenVR.getEyePosition(eye),worldScale);
	}

	@Override
	public FloatBuffer getHMDMatrix_Room() {
		return MCOpenVR.hmdRotation.toFloatBuffer();
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
		return c == 0 ? this.getControllerMainPos_World() : this.getControllerOffhandPos_World();
	}
	

	
}

