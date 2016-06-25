/**
 * Copyright 2013 Mark Browning, StellaArtois
 * Licensed under the LGPL 3.0 or later (See LICENSE.md for details)
 */
package com.mtbs3d.minecrift.render;

import com.mtbs3d.minecrift.utils.TextureSelector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.*;

public class FBOParams
{
    protected enum FBO_SUPPORT
    {
        USE_EXT_UNKNOWN,
        USE_GL30,
        USE_EXT,
    }

    int _textureType;
    int _originalColorTextureId = -1;
    int _fboWidth;
    int _fboHeight;
    String _fboName;
    Minecraft mc;
    public TextureSelector textureSelector=null;

    protected static FBO_SUPPORT fboSupport = FBO_SUPPORT.USE_EXT_UNKNOWN;

    public FBOParams(String fboName, int textureType, int internalFormat, int baseFormat, int bufferType, int fboWidth, int fboHeight, TextureSelector set) throws Exception {
        _fboName = fboName;
        mc = Minecraft.getMinecraft();
        _textureType = textureType;
        textureSelector = set;
        _fboWidth = fboWidth;
        _fboHeight = fboHeight;

        int _FBOBindPoint = ARBFramebufferObject.GL_FRAMEBUFFER;

        if (fboSupport == FBO_SUPPORT.USE_EXT_UNKNOWN)
        {
            // The framebuffer, which regroups 0, 1, or more textures, and 0 or 1 depth buffer.
            try {
                _frameBufferId = GL30.glGenFramebuffers();
                fboSupport = FBO_SUPPORT.USE_GL30;
            }
            catch (IllegalStateException ex)
            {
                System.out.println("[Minecrift] FBO creation: GL30.glGenFramebuffers not supported. Attempting to use EXTFramebufferObject.glGenFramebuffersEXT");
                fboSupport = FBO_SUPPORT.USE_EXT;

                try {
                    _frameBufferId = EXTFramebufferObject.glGenFramebuffersEXT();
                }
                catch (IllegalStateException ex1)
                {
                    System.out.println("[Minecrift] FBO creation: EXTFramebufferObject.glGenFramebuffersEXT not supported, FBO creation failed.");
                    throw ex1;
                }
            }
        }
        else if (fboSupport == FBO_SUPPORT.USE_GL30)
        {
            _frameBufferId = GL30.glGenFramebuffers();
        }
        else
        {
            _frameBufferId = EXTFramebufferObject.glGenFramebuffersEXT();
        }

        if (fboSupport == FBO_SUPPORT.USE_GL30)
        {
            if (textureSelector != null) {
                _colorTextureId = textureSelector.getFirstTexId();
                _originalColorTextureId = -1;
            }
            else {
                _colorTextureId = GL11.glGenTextures();
                _originalColorTextureId = _colorTextureId;
            }
            _depthRenderBufferId = GL30.glGenRenderbuffers();

            GL30.glBindFramebuffer(_FBOBindPoint, _frameBufferId);
            
            GL11.glBindTexture(textureType, _colorTextureId);
            mc.checkGLError("FBO bind texture");

            GL11.glEnable(textureType);
            GL11.glTexParameterf(textureType, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameterf(textureType, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            if (textureSelector == null)
                GL11.glTexImage2D(textureType, 0, internalFormat, fboWidth, fboHeight, 0, baseFormat, bufferType, (java.nio.ByteBuffer) null);

            //System.out.println("[Minecrift] FBO '" + fboName + "': w: " + fboWidth + ", h: " + fboHeight);

            GL30.glFramebufferTexture2D(_FBOBindPoint, GL30.GL_COLOR_ATTACHMENT0, textureType, _colorTextureId, 0);

            mc.checkGLError("FBO bind texture framebuffer");

            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, _depthRenderBufferId);                // bind the depth renderbuffer
            GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL14.GL_DEPTH_COMPONENT24, fboWidth, fboHeight); // get the data space for it
            GL30.glFramebufferRenderbuffer(_FBOBindPoint, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, _depthRenderBufferId);
            mc.checkGLError("FBO bind depth framebuffer");
        }
        else
        {
            if (textureSelector != null) {
                _colorTextureId = textureSelector.getFirstTexId();
                _originalColorTextureId = -1;
            }
            else {
                _colorTextureId = GL11.glGenTextures();
                _originalColorTextureId = _colorTextureId;
            }
            _depthRenderBufferId = EXTFramebufferObject.glGenRenderbuffersEXT();

            EXTFramebufferObject.glBindFramebufferEXT(_FBOBindPoint, _frameBufferId);
            mc.checkGLError("FBO bind framebuffer");

            GL11.glBindTexture(textureType, _colorTextureId);
            mc.checkGLError("FBO bind texture");

            GL11.glEnable(textureType);
            GL11.glTexParameterf(textureType, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameterf(textureType, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            if (textureSelector == null)
                GL11.glTexImage2D(textureType, 0, internalFormat, fboWidth, fboHeight, 0, baseFormat, bufferType, (java.nio.ByteBuffer) null);
            //System.out.println("[Minecrift] FBO '" + fboName + "': w: " + fboWidth + ", h: " + fboHeight);

            EXTFramebufferObject.glFramebufferTexture2DEXT(_FBOBindPoint, EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, textureType, _colorTextureId, 0);

            mc.checkGLError("FBO bind texture framebuffer");

            EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, _depthRenderBufferId);                // bind the depth renderbuffer
            EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, GL14.GL_DEPTH_COMPONENT24, fboWidth, fboHeight); // get the data space for it
            EXTFramebufferObject.glFramebufferRenderbufferEXT(_FBOBindPoint, EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, _depthRenderBufferId);
            mc.checkGLError("FBO bind depth framebuffer");
            GL11.glBindTexture(textureType, 0);
        }

        if (!checkFramebufferStatus())
        {
            // OK, if we have an error here - then throw an exception
            System.out.println("[Minecrift] FAILED to create framebuffer!!");
            throw new Exception("Failed to create framebuffer");
        }
    }

    public void selectTexture()
    {
        if(textureSelector!=null)
        {
            this._colorTextureId = textureSelector.getCurrentTexId();
            if (fboSupport == FBO_SUPPORT.USE_GL30)
            {
                GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, _textureType, _colorTextureId, 0);
            }
            else
            {
                EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, _textureType, _colorTextureId, 0);
            }
            mc.clearGLError(); //ugh, forge why.
            mc.checkGLError("FBO bind texture framebuffer");
         }
    }

    public void bindRenderTarget()
    {
        if (fboSupport == FBO_SUPPORT.USE_GL30)
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, _frameBufferId );
        else
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, _frameBufferId);
    }

    public void bindTexture()
    {
        OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(_textureType, _colorTextureId);
    }

    public void bindTexture_Unit1()
    {
        OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(_textureType, _colorTextureId);
    }

    public int getColorTextureId()
    {
        return _colorTextureId;
    }

    public int getFramebufferId()
    {
        return _frameBufferId;
    }

    public int getFramebufferWidth()
    {
        return _fboWidth;
    }

    public int getFramebufferHeight()
    {
        return _fboHeight;
    }

    public void delete()
    {
        if (_depthRenderBufferId != -1)
        {
            if (fboSupport == FBO_SUPPORT.USE_GL30)
                GL30.glDeleteRenderbuffers(_depthRenderBufferId);
            else
                EXTFramebufferObject.glDeleteRenderbuffersEXT(_depthRenderBufferId);

            _depthRenderBufferId = -1;
        }

        if (_originalColorTextureId != -1)
        {
            GL11.glDeleteTextures(_originalColorTextureId);
            _originalColorTextureId = -1;
            _colorTextureId = -1;
        }

        if (_frameBufferId != -1)
        {
            if (fboSupport == FBO_SUPPORT.USE_GL30)
                GL30.glDeleteFramebuffers(_frameBufferId);
            else
                EXTFramebufferObject.glDeleteFramebuffersEXT(_frameBufferId);

            _frameBufferId = -1;
        }
    }

    public boolean checkFramebufferStatus()
    {
        return checkFramebufferStatus(ARBFramebufferObject.GL_FRAMEBUFFER, _frameBufferId);
    }

    public static boolean checkReadFramebufferStatus(int fboId)
    {
         return checkFramebufferStatus(ARBFramebufferObject.GL_READ_FRAMEBUFFER, fboId);
    }

    public static boolean checkDrawFramebufferStatus(int fboId)
    {
        return checkFramebufferStatus(ARBFramebufferObject.GL_DRAW_FRAMEBUFFER, fboId);
    }

    // check FBO completeness
    public static boolean checkFramebufferStatus(int fboBindPoint, int fboId)
    {
        int FBOBindPoint = ARBFramebufferObject.GL_FRAMEBUFFER;
        if (fboBindPoint == ARBFramebufferObject.GL_READ_FRAMEBUFFER)
            FBOBindPoint = ARBFramebufferObject.GL_READ_FRAMEBUFFER;
        else if (fboBindPoint == ARBFramebufferObject.GL_DRAW_FRAMEBUFFER)
            FBOBindPoint = ARBFramebufferObject.GL_DRAW_FRAMEBUFFER;

        int FBOTypeBinding = ARBFramebufferObject.GL_FRAMEBUFFER_BINDING;
        if (FBOBindPoint == ARBFramebufferObject.GL_READ_FRAMEBUFFER)
            FBOTypeBinding = ARBFramebufferObject.GL_READ_FRAMEBUFFER_BINDING;
        else if (FBOBindPoint == ARBFramebufferObject.GL_DRAW_FRAMEBUFFER)
            FBOTypeBinding = ARBFramebufferObject.GL_DRAW_FRAMEBUFFER_BINDING;

        int origFboId = GL11.glGetInteger(FBOTypeBinding);
        ARBFramebufferObject.glBindFramebuffer(FBOBindPoint, fboId);

        boolean success = true;
        String error = "Unknown";

        try {

            // check FBO status
            int status = ARBFramebufferObject.glCheckFramebufferStatus(ARBFramebufferObject.GL_FRAMEBUFFER);
            switch(status)
            {
                case ARBFramebufferObject.GL_FRAMEBUFFER_COMPLETE:
                    error = ("[Minecrift] Framebuffer complete.");
                    success = true;
                    break;

                case ARBFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                    error = ("[ERROR] Framebuffer incomplete: Attachment is NOT complete.");
                    success = false;
                    break;

                case ARBFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                    error = ("[ERROR] Framebuffer incomplete: No image is attached to FBO.");
                    success = false;
                    break;

    //            case GL30.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
    //                error = ("[ERROR] Framebuffer incomplete: Attached images have different dimensions.");
    //                success = false;
    //                break;

    //            case ARBFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:
    //                error = ("[ERROR] Framebuffer incomplete: Color attached images have different internal formats.");
    //                success = false;
    //                break;

                case ARBFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                    error = ("[ERROR] Framebuffer incomplete: Draw buffer.");
                    success = false;
                    break;

                case ARBFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                    error = ("[ERROR] Framebuffer incomplete: Read buffer.");
                    success = false;
                    break;

                case ARBFramebufferObject.GL_FRAMEBUFFER_UNSUPPORTED:
                    error = ("[ERROR] Framebuffer incomplete: Unsupported by FBO implementation.");
                    success = false;
                    break;

                default:
                    error = ("[ERROR] Framebuffer incomplete: Unknown error.");
                    success = false;
                    break;
            }

            if (!success)
                throw new Exception("Framebuffer is NOT complete! " + error);

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        ARBFramebufferObject.glBindFramebuffer(FBOBindPoint, origFboId);

        return success;
    }

    public static String dumpBoundBuffers()
    {
        StringBuilder sb = new StringBuilder();
        int allFboId = GL11.glGetInteger(ARBFramebufferObject.GL_FRAMEBUFFER_BINDING);
        int readFboId = GL11.glGetInteger(ARBFramebufferObject.GL_READ_FRAMEBUFFER_BINDING);
        int drawFboId = GL11.glGetInteger(ARBFramebufferObject.GL_DRAW_FRAMEBUFFER_BINDING);
        int boundTextureId = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        sb.append("Bound GL_FRAMEBUFFER: " + allFboId).append("\n");
        sb.append("Bound GL_READ_FRAMEBUFFER: " + readFboId).append("\n");
        sb.append("Bound GL_DRAW_FRAMEBUFFER: " + drawFboId).append("\n");
        sb.append("Bound Read Texture:        " + boundTextureId);

        return sb.toString();
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("FBO:    " + _fboName).append("\n");
        sb.append("Size:   " + _fboWidth + " x " + _fboHeight).append("\n");
        sb.append("FB ID:  " + _frameBufferId).append("\n");
        sb.append("Tex ID: " + _colorTextureId).append("\n");
        if (textureSelector != null)
            sb.append("TextureSelector:\n" + textureSelector.toString());
        else
            sb.append("TextureSelector: null");
        return sb.toString();
    }

    int _frameBufferId = -1;
    int _colorTextureId = -1;
    int _depthRenderBufferId = -1;
}

