package com.mtbs3d.minecrift.render;

import com.mtbs3d.minecrift.api.IRoomscaleAdapter;
import com.mtbs3d.minecrift.provider.MCOpenVR;
import com.mtbs3d.minecrift.provider.OpenVRPlayer;
import com.mtbs3d.minecrift.utils.Angle;
import com.mtbs3d.minecrift.utils.Quaternion;
import com.mtbs3d.minecrift.utils.Utils;
import com.mtbs3d.minecrift.utils.Vector3;
import de.fruitfly.ovr.structs.EulerOrient;
import de.fruitfly.ovr.structs.Quatf;
import de.fruitfly.ovr.structs.Vector3f;
import jopenvr.OpenVRUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Quat4d;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;

/**
 * Created by Hendrik on 07-Aug-16.
 */
public class PlayerModelController {
	static PlayerModelController instance;
	public static PlayerModelController getInstance(){
		if(instance==null)
			instance=new PlayerModelController();
		return instance;
	}

	public void sendArms(){
		IRoomscaleAdapter room=Minecraft.getMinecraft().roomScale;
		Vec3d hmdPos=room.getHeadPos_Room();

		Vec3d hmddir=room.getHMDDir_World();

		Vector3 shoulderL=new Vector3(0.2f,-0.3f,0);
		Vector3 shoulderR=new Vector3(-0.2f,-0.3f,0);

		//Vector3f sLV3f=MCOpenVR.hmdRotation.transform(new Vector3f((float) shoulderL.xCoord,(float) shoulderL.yCoord,(float) shoulderL.zCoord));
		//Vector3f sRV3f=MCOpenVR.hmdRotation.transform(new Vector3f((float) shoulderR.xCoord,(float) shoulderR.yCoord,(float) shoulderR.zCoord));

		Quaternion qua=new Quaternion(Vector3.up(),room.getHMDYaw_World());

		shoulderL=shoulderL.multiply(qua.getMatrix());
		shoulderR=shoulderR.multiply(qua.getMatrix());

		Vec3d[] vecs=new Vec3d[2];

		for (int i = 0; i <= 1; i++) {
			Vec3d ctr=room.getControllerPos_Room(i);

			Vec3d offset= i==0 ? shoulderR.toVec3d() : shoulderL.toVec3d();
			Vec3d vecCtr = ctr.subtract(hmdPos.add(offset)).normalize();

			Angle euler=Quaternion.createFromToVector(Utils.convertVector(hmddir),Utils.convertVector(vecCtr)).toEuler();


			double pitch = euler.getPitch();
			double yaw = euler.getYaw();
			pitch-=90;
			yaw=-yaw;

			vecs[i] = new Vec3d(Math.toRadians(pitch), Math.toRadians(yaw), 0);

		}

		sendMyAngles(vecs[1],vecs[0]);
	}

	public class RotInfo{ public Vec3d leftArm, rightArm; }

	public RotInfo getArmsForPlayer(String uuid){
		//give your own for testing
		RotInfo out=new RotInfo();
		out.leftArm=left;
		out.rightArm=right;
		return out;
	}

	Vec3d left,right;

	public void sendMyAngles(Vec3d leftArm, Vec3d rightArm){
		//for testing
		left=leftArm;
		right=rightArm;
	}

	public boolean isTracked(String uuid){
		return false;//true;//testing
	}
}
