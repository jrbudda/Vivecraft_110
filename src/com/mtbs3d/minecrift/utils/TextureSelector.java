package com.mtbs3d.minecrift.utils;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;

public class TextureSelector {

    public long mFrameIndex;
	private boolean createdTextures = false;
	private ArrayList<Integer> mTextureIds = new ArrayList<Integer>();
	private long mNumTextures=1;

	public void setTextureIds(ArrayList<Integer> textureIds)
    {
		deleteTextures();

        if (textureIds != null)
        {
            mTextureIds  = textureIds;
            mNumTextures = textureIds.size();
            createdTextures = false;
        }
	}

    public void setTextureId(int textureId)
    {
        deleteTextures();

        if (textureId != -1)
        {
            mTextureIds.add(textureId);
            mNumTextures = 1;
            createdTextures = false;
        }
    }

	public void genTextureIds(int internalFormat, int baseFormat, int bufferType, int Width, int Height, int count)
    {
		deleteTextures();

        for (int i = 0; i < count; i++) {
		    int textureId = GL11.glGenTextures();

            int boundTextureId = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, Width, Height, 0, baseFormat, bufferType, (java.nio.ByteBuffer) null);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, boundTextureId);

            mTextureIds.add(textureId);
        }
        mNumTextures = mTextureIds.size();
        createdTextures = true;
	}

    public void setFrameIndex(long frameIndex) {
        mFrameIndex = frameIndex;
    }

	/**
	 * Texture ID for the current frame to bind to the framebuffer.
	 */
	public int getCurrentTexId() {
		return mTextureIds.get(getCurrentSwapIdx());
	}

    public int getFirstTexId() {
        if (mTextureIds.size() < 1)
            return -1;
        return mTextureIds.get(0);
    }

	/**
	 * To be passed to oculus SDK
	 */
	public int getCurrentSwapIdx()
    {
        if (mNumTextures == 0)
            return -1;

		return (int)(mFrameIndex % mNumTextures);
	}

	private void deleteTextures()
	{
		if (createdTextures) {
			for (Integer i : mTextureIds) {
				GL11.glDeleteTextures(i);
			}
			createdTextures = false;
		}
		mTextureIds.clear();
		mNumTextures = 0;
	}

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("i:              " + mFrameIndex).append("\n");
        sb.append("Num Textures:   " + mNumTextures).append("\n");
        sb.append("Cur Texture ID: " + getCurrentSwapIdx()).append("\n");
        sb.append("Texture IDs:").append("\n");
        for (int i = 0; i < mTextureIds.size(); i++)
            sb.append(" " + i + ": " + mTextureIds.get(i)).append("\n");
        return sb.toString();
    }
}

