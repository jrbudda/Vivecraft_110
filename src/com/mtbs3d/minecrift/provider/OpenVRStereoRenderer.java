package com.mtbs3d.minecrift.provider;

import com.mtbs3d.minecrift.api.IStereoProvider;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import de.fruitfly.ovr.enums.EyeType;
import de.fruitfly.ovr.structs.*;
import jopenvr.HiddenAreaMesh_t;
import jopenvr.HmdMatrix44_t;
import jopenvr.JOpenVRLibrary;
import jopenvr.OpenVRUtil;
import jopenvr.Texture_t;
import jopenvr.VRTextureBounds_t;
import net.minecraft.client.gui.GuiScreen;

import java.nio.IntBuffer;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
/**
 * Created by jrbudda
 */
public class OpenVRStereoRenderer implements IStereoProvider
{
	// TextureIDs of framebuffers for each eye
	private int LeftEyeTextureId;

	private HiddenAreaMesh_t[] hiddenMeshes = new HiddenAreaMesh_t[2];
	private float[][] hiddenMesheVertecies = new float[2][];

	@Override
	public RenderTextureInfo getRenderTextureSizes(float renderScaleFactor)
	{
		IntBuffer rtx = IntBuffer.allocate(1);
		IntBuffer rty = IntBuffer.allocate(1);
		MCOpenVR.vrsystem.GetRecommendedRenderTargetSize.apply(rtx, rty);

		RenderTextureInfo info = new RenderTextureInfo();
		info.HmdNativeResolution.w = rtx.get(0);
		info.HmdNativeResolution.h = rty.get(0);
		info.LeftFovTextureResolution.w = (int) (rtx.get(0) );
		info.LeftFovTextureResolution.h = (int) (rty.get(0) );
		info.RightFovTextureResolution.w = (int) (rtx.get(0) );
		info.RightFovTextureResolution.h = (int) (rty.get(0) );

		if ( info.LeftFovTextureResolution.w % 2 != 0) info.LeftFovTextureResolution.w++;
		if ( info.LeftFovTextureResolution.h % 2 != 0) info.LeftFovTextureResolution.w++;
		if ( info.RightFovTextureResolution.w % 2 != 0) info.LeftFovTextureResolution.w++;
		if ( info.RightFovTextureResolution.h % 2 != 0) info.LeftFovTextureResolution.w++;

		info.CombinedTextureResolution.w = info.LeftFovTextureResolution.w + info.RightFovTextureResolution.w;
		info.CombinedTextureResolution.h = info.LeftFovTextureResolution.h;


		for (int i = 0; i < 2; i++) {
			hiddenMeshes[i] = MCOpenVR.vrsystem.GetHiddenAreaMesh.apply(i);
			hiddenMeshes[i].read();

			hiddenMesheVertecies[i] = new float[hiddenMeshes[i].unTriangleCount * 3 * 2];
			Pointer arrptr = new Memory(hiddenMeshes[i].unTriangleCount * 3 * 2);
			hiddenMeshes[i].pVertexData.getPointer().read(0, hiddenMesheVertecies[i], 0, hiddenMesheVertecies[i].length);

			for (int ix = 0;ix < hiddenMesheVertecies[i].length;ix+=2) {
				hiddenMesheVertecies[i][ix] = hiddenMesheVertecies[i][ix] * info.LeftFovTextureResolution.w * renderScaleFactor;
				hiddenMesheVertecies[i][ix + 1] = hiddenMesheVertecies[i][ix +1] * info.LeftFovTextureResolution.h * renderScaleFactor;
			}
		}

		//		Pointer pointers = new Memory(k_pch_SteamVR_Section.length()+1);
		//		pointers.setString(0, k_pch_SteamVR_Section);
		//		Pointer pointerk = new Memory(k_pch_SteamVR_RenderTargetMultiplier_Float.length()+1);
		//		pointerk.setString(0, k_pch_SteamVR_RenderTargetMultiplier_Float);
		//		IntByReference err = new IntByReference();
		//		float test = vrSettings.GetFloat.apply(pointers, pointerk, 99.9f, err);
		//		vrSettings.SetFloat.apply(pointers, pointerk, renderScaleFactor, err);
		//		float test2 = vrSettings.GetFloat.apply(pointers, pointerk, 99.9f, err);

		return info;
	}

	@Override
	public Matrix4f getProjectionMatrix(FovPort fov,
			EyeType eyeType,
			float nearClip,
			float farClip)
	{
		if ( eyeType == EyeType.ovrEye_Left )
		{
			HmdMatrix44_t mat = MCOpenVR.vrsystem.GetProjectionMatrix.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left, nearClip, farClip, JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL);
			MCOpenVR.hmdProjectionLeftEye = new Matrix4f();
			return OpenVRUtil.convertSteamVRMatrix4ToMatrix4f(mat, MCOpenVR.hmdProjectionLeftEye);
		}else{
			HmdMatrix44_t mat = MCOpenVR.vrsystem.GetProjectionMatrix.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right, nearClip, farClip, JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL);
			MCOpenVR.hmdProjectionRightEye = new Matrix4f();
			return OpenVRUtil.convertSteamVRMatrix4ToMatrix4f(mat, MCOpenVR.hmdProjectionRightEye);
		}
	}


	@Override
	public EyeType eyeRenderOrder(int index)
	{
		return ( index == 1 ) ? EyeType.ovrEye_Right : EyeType.ovrEye_Left;
	}

	@Override
	public boolean usesDistortion()
	{
		return true;
	}

	@Override
	public boolean isStereo()
	{
		return true;
	}

	@Override
	public double getFrameTiming() {
		return getCurrentTimeSecs();
	}

	@Override
	public void deleteRenderTextures() {
		if (LeftEyeTextureId > 0)	GL11.glDeleteTextures(LeftEyeTextureId);
	}

	@Override
	public String getLastError() { return ""; }

	@Override
	public boolean setCurrentRenderTextureInfo(int index, int textureIdx, int depthId, int depthWidth, int depthHeight)
	{
		return true;
	}
	@Override
	public double getCurrentTimeSecs()
	{
		return System.nanoTime() / 1000000000d;
	}


	@Override
	public boolean providesMirrorTexture() { return false; }

	@Override
	public int createMirrorTexture(int width, int height) { return -1; }

	@Override
	public void deleteMirrorTexture() { }

	@Override
	public boolean providesRenderTextures() { return true; }

	@Override
	public RenderTextureSet createRenderTexture(int lwidth, int lheight)
	{	
		// generate left eye texture
		LeftEyeTextureId = GL11.glGenTextures();
		int boundTextureId = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, LeftEyeTextureId);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, lwidth, lheight, 0, GL11.GL_RGBA, GL11.GL_INT, (java.nio.ByteBuffer) null);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, boundTextureId);

		MCOpenVR.texType.handle = LeftEyeTextureId;
		MCOpenVR.texType.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
		MCOpenVR.texType.eType = JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL;
		MCOpenVR.texType.write();

		RenderTextureSet textureSet = new RenderTextureSet();
		textureSet.leftEyeTextureIds.add(LeftEyeTextureId);
		return textureSet;
	}

	@Override
	public void configureRenderer(GLConfig cfg) {

	}

	public void onGuiScreenChanged(GuiScreen previousScreen, GuiScreen newScreen) {
		MCOpenVR.onGuiScreenChanged(previousScreen, newScreen);
	}

	@Override
	public boolean endFrame(EyeType eye)
	{

		GL11.glFinish();
		int ret = 0;
		if(eye == EyeType.ovrEye_Left){
			ret = MCOpenVR.vrCompositor.Submit.apply(
				JOpenVRLibrary.EVREye.EVREye_Eye_Left,
				MCOpenVR.texType, MCOpenVR.texBounds,
				JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);
		}else{
			ret = MCOpenVR.vrCompositor.Submit.apply(
				JOpenVRLibrary.EVREye.EVREye_Eye_Right,
				MCOpenVR.texType, MCOpenVR.texBounds,
				JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);
		}
		//System.out.println("vsync="+JOpenVRLibrary.VR_IVRCompositor_GetVSync(vrCompositor));
		return true;
	}

	
	public void endFrame() {
		if(MCOpenVR.vrCompositor !=null) MCOpenVR.vrCompositor.PostPresentHandoff.apply();
	}

	
	@Override
	public boolean providesStencilMask() {
		return true;
	}

	@Override
	public float[] getStencilMask(EyeType eye) {
		return eye == EyeType.ovrEye_Left ? hiddenMesheVertecies[0] : hiddenMesheVertecies[1];
	}

	@Override
	public String getName() {
		return "OpenVR";
	}

	@Override
	public boolean isInitialized() {
		return MCOpenVR.initSuccess;
	}

	@Override
	public String getinitError() {
		return MCOpenVR.initStatus;
	}

}
