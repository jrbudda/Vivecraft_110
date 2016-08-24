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
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Quat4d;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;

/**
 * Created by Hendrik on 07-Aug-16.
 */
public class PlayerModelController {
	
	public Map<UUID, RotInfo> vivePlayers = new HashMap<UUID, RotInfo>();

	
	static PlayerModelController instance;
	public static PlayerModelController getInstance(){
		if(instance==null)
			instance=new PlayerModelController();
		return instance;
	}

	public class RotInfo{ public Vec2f leftArm, rightArm, head; }

	public void Update(UUID uuid, byte[] hmddata, byte[] c0data, byte[] c1data){
	
		Vec3d hmdpos = null, c0pos = null, c1pos = null;
		Quaternion hmdq = null, c0q = null, c1q = null;
	
		for (int i = 0; i <= 2; i++) {
			try {
				byte[]arr = null;
				switch(i){
				case 0:	arr = hmddata;
				break;
				case 1: arr = c0data;
				break;
				case 2: arr = c1data;
				break;
				}

				ByteArrayInputStream by = new ByteArrayInputStream(arr);
				DataInputStream da = new DataInputStream(by);

				boolean bool = false;
				if(arr.length >=29)
					bool = da.readBoolean();		

				float posx = da.readFloat();
				float posy = da.readFloat();
				float posz = da.readFloat();
				float rotw = da.readFloat();
				float rotx = da.readFloat();
				float roty = da.readFloat();
				float rotz = da.readFloat();

				da.close();
				
				switch(i){
				case 0:	
					if(bool){ //seated
						vivePlayers.remove(uuid);
						return;
					}
					hmdpos = new Vec3d(posx, posy, posz);
					hmdq = new Quaternion(rotw, rotx, roty, rotz);
					break;
				case 1: 
					c0pos = new Vec3d(posx, posy, posz);
					c0q = new Quaternion(rotw, rotx, roty, rotz);
					break;
				case 2: 
					c1pos = new Vec3d(posx, posy, posz);
					c1q = new Quaternion(rotw, rotx, roty, rotz);
					break;
				}
				
			} catch (IOException e) {

			}
		}

		
		Vector3 shoulderR=new Vector3(0,-0.0f,0);

		//Vector3f sLV3f=MCOpenVR.hmdRotation.transform(new Vector3f((float) shoulderL.xCoord,(float) shoulderL.yCoord,(float) shoulderL.zCoord));
		//Vector3f sRV3f=MCOpenVR.hmdRotation.transform(new Vector3f((float) shoulderR.xCoord,(float) shoulderR.yCoord,(float) shoulderR.zCoord));

		 Angle eu = hmdq.toEuler();
		 float yaw1 = eu.getYaw();
		 float pitch1 = eu.getPitch();
				 
		//Quaternion qua=new Quaternion(Vector3.up(),yaw1);

		//shoulderL=shoulderL.multiply(qua.getMatrix());
		//shoulderR=shoulderR.multiply(qua.getMatrix());

		Vec2f[] vecs=new Vec2f[2];

		for (int i = 0; i <= 1; i++) {
			Vec3d ctr= i == 0 ? c0pos : c1pos;

			Vec3d offset= i==0 ? shoulderR.toVec3d() : shoulderR.toVec3d();
			Vec3d vecCtr = ctr.subtract(hmdpos.add(offset)).normalize();
			Vec3d def = new Vec3d(0,0,-1);
			
			Angle euler=Quaternion.createFromToVector(Utils.convertVector(def),Utils.convertVector(vecCtr)).toEuler();

			double pitch = -euler.getPitch();
			double yaw = euler.getYaw();
			pitch-=90;
			yaw=-yaw;

			vecs[i] = new Vec2f((float)Math.toRadians(pitch), (float)Math.toRadians(yaw));
		}
		
		RotInfo out = vivePlayers.get(uuid);
		if(out == null) out = new RotInfo();
		out.leftArm=vecs[1];
		out.rightArm=vecs[0];
		out.head = new Vec2f((float)Math.toRadians(pitch1), (float)Math.toRadians(yaw1));
		vivePlayers.put(uuid, out);
	}
	

	public RotInfo getRotationsForPlayer(UUID uuid){
		if(debug) 
			return vivePlayers.get(Minecraft.getMinecraft().thePlayer.getUniqueID());
		return vivePlayers.get(uuid);
//		//give your own for testing
//		RotInfo out=new RotInfo();
//		out.leftArm=left;
//		out.rightArm=right;
//		return out;
	}

	public boolean debug = false;

	public boolean isTracked(UUID uuid){
		this.debug = false;
		if(debug) return true;
		return vivePlayers.containsKey(uuid);
	}
}

