package com.mtbs3d.minecrift.gameplay;

import javax.swing.LayoutStyle;

import com.mtbs3d.minecrift.api.IRoomscaleAdapter;
import com.mtbs3d.minecrift.provider.MCOpenVR;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ClimbTracker {

	private boolean c0Latched = false;
	private boolean c1Latched = false;

	private boolean gravityOverride=false;


	public Vec3d[] latchStart = new Vec3d[]{new Vec3d(0,0,0), new Vec3d(0,0,0)};
	public double[] latchStartBodyY = new double[2];
	public int latchStartController = -1;

	public boolean isGrabbingLadder(){
		return c0Latched || c1Latched;
	}

	public boolean isActive(EntityPlayerSP p){
		if(Minecraft.getMinecraft().vrSettings.seated)
			return false;
		if(!Minecraft.getMinecraft().vrSettings.vrFreeMove && !Minecraft.getMinecraft().vrSettings.simulateFalling)
			return false;
		if(!Minecraft.getMinecraft().vrSettings.realisticClimbEnabled)
			return false;
		if(p==null || p.isDead)
			return false;
		if(p.isRiding())
			return false;
		if(p.moveForward > 0) 
			return false;
		return true;
	}

	public void doProcess(Minecraft minecraft, EntityPlayerSP player){
		if(!isActive(player)) {
			latchStartController = -1;
			c1Latched = false;
			c0Latched = false;
			player.setNoGravity(false);
			return;
		}

		IRoomscaleAdapter provider = minecraft.roomScale;

		boolean[] ok = new boolean[2];

		for(int c=0;c<2;c++){
			Vec3d controllerPos=minecraft.roomScale.getControllerPos_World(c);
			BlockPos bp = new BlockPos(controllerPos);
			IBlockState bs = minecraft.theWorld.getBlockState(bp);
			Block b = bs.getBlock();
			if(b == Blocks.LADDER || b ==Blocks.VINE){
				int meta = b.getMetaFromState(bs);
				Vec3d cpos = controllerPos.subtract(bp.getX(), bp.getY(), bp.getZ());

				if(meta == 2){
					ok[c] = cpos.zCoord > .9 && (cpos.xCoord > .1 && cpos.xCoord < .9);
				} else if (meta == 3){
					ok[c] = cpos.zCoord < .1 && (cpos.xCoord > .1 && cpos.xCoord < .9);
				} else if (meta == 4){
					ok[c] = cpos.xCoord > .9 && (cpos.zCoord > .1 && cpos.zCoord < .9);
				} else if (meta == 5){
					ok[c] = cpos.xCoord < .1 && (cpos.zCoord > .1 && cpos.zCoord < .9);
				}			
			} else {
				if(latchStart[c].subtract(controllerPos).lengthSquared() > 0.25) 
					ok[c] = false;
				else
					ok[c] = c==0?c0Latched:c1Latched; //dont let go when leaving block, only when not on ladder in ladder block.
			}
		}


		if(!ok[0] && c0Latched){
			minecraft.vrPlayer.triggerHapticPulse(0, 200);
		}

		if(ok[0] && !c0Latched){
			latchStart[0] = minecraft.roomScale.getControllerPos_World(0);
			latchStartBodyY[0] = player.posY;
			latchStartController = 0;
			minecraft.vrPlayer.triggerHapticPulse(0, 1000);
		}

		if(!ok[1] && c1Latched){
			minecraft.vrPlayer.triggerHapticPulse(1, 200);
		}

		if(ok[1] && !c1Latched){
			latchStart[1] = minecraft.roomScale.getControllerPos_World(1);
			latchStartBodyY[1] = player.posY;
			latchStartController = 1;
			minecraft.vrPlayer.triggerHapticPulse(1, 1000);
		}

		c0Latched = ok[0];
		c1Latched = ok[1];

		if(c0Latched || c1Latched && !gravityOverride) {
			player.setNoGravity(true);
			gravityOverride=true;
		}
		if(!c0Latched && !c1Latched && gravityOverride){
			player.setNoGravity(false);
			gravityOverride=false;
		}

		if(!c0Latched && !c1Latched){
			latchStartController = -1;
			return; //fly u fools
		}

		int c =0;

		if(c0Latched && c1Latched){ //y u do dis?
			if(latchStartController >=0)
				c = latchStartController; //use whichever one grabbed most recently.
			//if u manage to get both controllers onto the ladder in one tick.. congratz, use c0.
		} else if (c1Latched){
			c =1;
		}

		double now = minecraft.roomScale.getControllerPos_World(c).yCoord;

		double delta= -(now - latchStart[c].yCoord);

		player.motionY = delta;

	}

}
