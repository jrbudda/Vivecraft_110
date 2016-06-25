package com.mtbs3d.minecrift.api;

import java.nio.FloatBuffer;

import org.lwjgl.util.vector.Quaternion;

import com.mtbs3d.minecrift.render.QuaternionHelper;

import de.fruitfly.ovr.enums.EyeType;
import de.fruitfly.ovr.util.BufferUtil;
import net.minecraft.util.math.Vec3d;

/**
 * This interface defines convenience methods for getting 'world coordinate' vectors from room-scale VR systems.
 *
 * @author jrbudda
 *
 */
public interface IRoomscaleAdapter  {

    public boolean isHMDTracking();
	public Vec3d getHMDPos_World();
	public Vec3d getHMDPos_Room(); 
	public Vec3d getHMDDir_World(); 
	public float getHMDYaw_World();  //degrees
	public float getHMDPitch_World(); //degrees
	
	public FloatBuffer getHMDMatrix_World();
	public FloatBuffer getHMDMatrix_Room();	
	public FloatBuffer getControllerMatrix_World(int controller);
	
	public Vec3d getEyePos_World(EyeType eye);
	public Vec3d getEyePos_Room(EyeType eye);
	
    public boolean isControllerMainTracking();
	public Vec3d getControllerMainPos_World(); 
	public Vec3d getControllerMainDir_World(); 
	public float getControllerMainYaw_World(); //degrees
	public float getControllerMainPitch_World(); //degrees
	
	public float getControllerYaw_Room(int controller); //degrees
	public float getControllerPitch_Room(int controller); //degrees
	
    public boolean isControllerOffhandTracking();
	public Vec3d getControllerOffhandPos_World(); 
	public Vec3d getControllerOffhandDir_World(); 
	public float getControllerOffhandYaw_World(); //degrees
	public float getControllerOffhandPitch_World(); //degrees
	
	public Vec3d getCustomControllerVector(int controller, Vec3d axis);
	
	public Vec3d getRoomOriginPos_World(); //degrees
	public Vec3d getRoomOriginUpDir_World(); //what do you do
	
	public void triggerHapticPulse(int controller, int duration);
	public Vec3d getControllerPos_Room(int i);
	public Vec3d getControllerPos_World(int c);


	
}

