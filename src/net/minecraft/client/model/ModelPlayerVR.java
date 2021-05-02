package net.minecraft.client.model;

import java.util.Random;
import java.util.UUID;

import javax.swing.plaf.RootPaneUI;

import com.mtbs3d.minecrift.render.PlayerModelController;
import com.mtbs3d.minecrift.render.PlayerModelController.RotInfo;
import com.mtbs3d.minecrift.render.StaticTexture;
import com.mtbs3d.minecrift.render.VRShaders;
import com.mtbs3d.minecrift.utils.Vector3;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmorVR;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class ModelPlayerVR extends ModelBiped
{
    public ModelRenderer bipedLeftArmwear;
    public ModelRenderer leftshoulder;
    public ModelRenderer rightShoulder;
    public ModelRenderer bipedRightArmwear;
    public ModelRenderer bipedLeftLegwear;
    public ModelRenderer bipedRightLegwear;
    public ModelRenderer bipedBodyWear;
    private final ModelRenderer bipedCape;
    private final ModelRenderer bipedDeadmau5Head;
    private final boolean smallArms;
    public ModelRenderer vrHMD;

    //VIVE START
    public Vec3d renderPos;
   ResourceLocation DIAMOND_HMD = new ResourceLocation("/assets/vivecraft/textures/diamond_hmd.png");
   ResourceLocation GOLD_HMD = new ResourceLocation("/assets/vivecraft/textures/gold_hmd.png");
   ResourceLocation BLACK_HMD = new ResourceLocation("/assets/vivecraft/textures/black_hmd.png");

    //VIVE END
    
   public  LayerBipedArmorVR armor = null;
    
    public ModelPlayerVR(float modelSize, boolean smallArmsIn)
    {
    	super(modelSize, 0.0F, 64, 64);

    	this.bipedCape = new ModelRenderer(this, 0, 0);
    	this.bipedCape.setTextureSize(64, 32);
    	this.bipedCape.addBox(-5.0F, 0.0F, -1.0F, 10, 16, 1, modelSize); 
    	this.smallArms = smallArmsIn;
    	this.bipedDeadmau5Head = new ModelRenderer(this, 24, 0);
    	this.bipedDeadmau5Head.addBox(-3.0F, -6.0F, -1.0F, 6, 6, 1, modelSize);        	
		this.bipedLeftArmwear = new ModelRenderer(this, 48, 48);
		this.bipedRightArmwear = new ModelRenderer(this, 40, 32);
        this.rightShoulder = new ModelRenderer(this, 24, 18);
        this.rightShoulder.addBox(-3.0F, -2.0F, -2.0F, 4, 8, 4, modelSize);
        this.rightShoulder.setRotationPoint(-5.0F, 2.0F , 0.0F);
        this.leftshoulder = new ModelRenderer(this, 24, 18);
        this.leftshoulder.mirror = true;
        this.leftshoulder.addBox(-1.0F, -2.0F, -2.0F, 4, 8, 4, modelSize);
        this.leftshoulder.setRotationPoint(5.0F, 2.0F, 0.0F);
		this.bipedLeftLegwear = new ModelRenderer(this, 0, 48);	
		this.bipedRightLegwear = new ModelRenderer(this, 0, 32);
		this.bipedBodyWear = new ModelRenderer(this, 16, 32);
        this.vrHMD = new ModelRenderer(this, 0, 0);
        this.vrHMD.setTextureSize(16, 16);
        this.vrHMD.addBox(-3.5F, -6.0F, -7.5F, 7, 4, 5, modelSize);
        try {
            Minecraft.getMinecraft().getTextureManager().loadTexture(this.BLACK_HMD, new StaticTexture(this.BLACK_HMD));
            Minecraft.getMinecraft().getTextureManager().loadTexture(this.GOLD_HMD, new StaticTexture(this.GOLD_HMD));
            Minecraft.getMinecraft().getTextureManager().loadTexture(this.DIAMOND_HMD, new StaticTexture(this.DIAMOND_HMD));
		} catch (Exception e) {
		}

    		if (smallArmsIn)
    		{
    			this.bipedLeftArm = new ModelRenderer(this, 32, 48);
    			this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 3, 12, 4, modelSize);
    			this.bipedLeftArm.setRotationPoint(5.0F, 2.5F, 0.0F);
    			this.bipedRightArm = new ModelRenderer(this, 40, 16);
    			this.bipedRightArm.addBox(-2.0F, -2.0F, -2.0F, 3, 12, 4, modelSize);
    			this.bipedRightArm.setRotationPoint(-5.0F, 2.5F, 0.0F);

    			this.bipedLeftArmwear.addBox(-1.0F, -2.0F, -2.0F, 3, 12, 4, modelSize + 0.25F);
    			this.bipedLeftArmwear.setRotationPoint(5.0F, 2.5F, 0.0F);
    			this.bipedRightArmwear = new ModelRenderer(this, 40, 32);
    			this.bipedRightArmwear.addBox(-2.0F, -2.0F, -2.0F, 3, 12, 4, modelSize + 0.25F);
    			this.bipedRightArmwear.setRotationPoint(-5.0F, 2.5F, 10.0F);
    		}
    		else
    		{
    			this.bipedLeftArm = new ModelRenderer(this, 32, 48);
    			this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, modelSize);
    			this.bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
    			this.bipedLeftArmwear = new ModelRenderer(this, 48, 48);
    			this.bipedLeftArmwear.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
    			this.bipedLeftArmwear.setRotationPoint(5.0F, 2.0F, 0.0F);

    			this.bipedRightArmwear.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
    			this.bipedRightArmwear.setRotationPoint(-5.0F, 2.0F, 10.0F);
    		}

    		this.bipedLeftLeg = new ModelRenderer(this, 16, 48);
    		this.bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize);
    		this.bipedLeftLeg.setRotationPoint(1.9F, 12.0F, 0.0F);

    		this.bipedLeftLegwear.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
    		this.bipedLeftLegwear.setRotationPoint(1.9F, 12.0F, 0.0F);
    		this.bipedRightLegwear.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
    		this.bipedRightLegwear.setRotationPoint(-1.9F, 12.0F, 0.0F);
    		this.bipedBodyWear.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, modelSize + 0.25F);
    		this.bipedBodyWear.setRotationPoint(0.0F, 0.0F, 0.0F);
    	}

    /**
     * Sets the models various rotation angles then renders the model.
     */
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale)
    {
        super.render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        GlStateManager.pushMatrix();

        if (this.isChild)
        {
            float f = 2.0F;
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
            GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
            this.bipedLeftLegwear.render(scale);
            this.bipedRightLegwear.render(scale);
            this.bipedLeftArmwear.render(scale);
            this.bipedRightArmwear.render(scale);
            this.bipedBodyWear.render(scale);
        }
        else
        {
            if (entityIn.isSneaking())
            {
                GlStateManager.translate(0.0F, 0.2F, 0.0F);
            }

            this.bipedLeftLegwear.render(scale);
            this.bipedRightLegwear.render(scale);
            this.bipedLeftArmwear.render(scale);
            this.bipedRightArmwear.render(scale);
            this.bipedBodyWear.render(scale);
        }

        this.leftshoulder.render(scale);
        this.rightShoulder.render(scale);
        
        this.vrHMD.render(scale);
        
        GlStateManager.popMatrix();
    }

    public void renderDeadmau5Head(float scale)
    {
        copyModelAngles(this.bipedHead, this.bipedDeadmau5Head);
        this.bipedDeadmau5Head.rotationPointX = 0.0F;
        this.bipedDeadmau5Head.rotationPointY = 0.0F;
        this.bipedDeadmau5Head.render(scale);
    }

    public void renderCape(float scale)
    {
        this.bipedCape.render(scale);
    }

    /**
     * Sets the model's various rotation angles. For bipeds, par1 and par2 are used for animating the movement of arms
     * and legs, where par1 represents the time(so that arms and legs swing back and forth) and par2 represents how
     * "far" arms and legs can swing at most.
     */
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn)
    {
    	super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
    	//Lasciate ogne speranza, voi ch'intrate
    	PlayerModelController.RotInfo rotInfo=PlayerModelController.getInstance().getRotationsForPlayer(((EntityPlayer)entityIn).getGameProfile().getId());
    	if(rotInfo == null) {

    	} else {

    		double minecraftBullshit = -1.501F;

    		Vec3d pos = this.renderPos;
    		float eyaw = (float) Math.toRadians(entityIn.rotationYaw);
    		float yaw1 = (float) Math.atan2(-rotInfo.headRot.xCoord, -rotInfo.headRot.zCoord); 
    		float pitch1 = (float) Math.asin(rotInfo.headRot.yCoord/rotInfo.headRot.lengthVector()); 

    		float yaw3 = (float) Math.atan2(-rotInfo.leftArmRot.xCoord, -rotInfo.leftArmRot.zCoord); 
    		float pitch3 = (float) Math.asin(rotInfo.leftArmRot.yCoord/rotInfo.leftArmRot.lengthVector());           	

    		this.bipedHead.rotateAngleX = (float) -pitch1;

    		Vec3d diff = (rotInfo.leftArmPos.subtract(rotInfo.rightArmPos)).rotateYaw((float)-Math.PI/2);
    		if(rotInfo.reverse) diff = diff.scale(-1);

    		if(rotInfo.seated) 
    			diff = rotInfo.rightArmRot;
    		double ltor =  Math.atan2(-diff.xCoord, diff.zCoord);     		

    		this.bipedHead.rotateAngleY = (float) (Math.PI -yaw1 - ltor);

    		ModelArmorVR armor = this.armor.getModelFromSlot(EntityEquipmentSlot.HEAD);

    		armor.bipedHead.rotateAngleY = (float) (this.bipedHead.rotateAngleY);  				
    		armor.bipedHead.rotateAngleX = this.bipedHead.rotateAngleX;
      		
    		rightShoulder.isHidden = rotInfo.seated;
    		leftshoulder.isHidden = rotInfo.seated;

    		if(!rotInfo.seated){

    			float yaw2 = (float) Math.atan2(-rotInfo.rightArmRot.xCoord, -rotInfo.rightArmRot.zCoord); 
    			float pitch2 = (float) Math.asin(rotInfo.rightArmRot.yCoord/rotInfo.rightArmRot.lengthVector()); 
    			if(pos !=null){
    				Vec3d larm = rotInfo.leftArmPos.subtract(pos).addVector(0,minecraftBullshit,0);
    				larm = larm.rotateYaw((float)(-Math.PI + ltor)).add(rotInfo.leftArmRot.scale(-0.2)).scale(-1/scaleFactor);      		      		        		
    				this.bipedLeftArm.setRotationPoint((float)larm.xCoord, (float)larm.yCoord, -(float)larm.zCoord);          
    				this.bipedLeftArm.rotateAngleX=(float) (-pitch3+ 3*Math.PI/2);
    				this.bipedLeftArm.rotateAngleY=(float) (Math.PI - yaw3 - ltor);

    				Vec3d lsh = new Vec3d(leftshoulder.rotationPointX - larm.xCoord, 
    						leftshoulder.rotationPointY - larm.yCoord,
    						leftshoulder.rotationPointZ - larm.zCoord);

    				float yawls = (float) Math.atan2(-lsh.xCoord, -lsh.zCoord); 
    				float pitchls = (float) Math.asin(lsh.yCoord/lsh.lengthVector()); 		
    				leftshoulder.rotateAngleY = (float) (-yawls);
    				leftshoulder.rotateAngleX = (float) (-pitchls+ 3*Math.PI/2);

    				Vec3d rarm = rotInfo.rightArmPos.subtract(pos).addVector(0,minecraftBullshit,0);
    				rarm = rarm.rotateYaw((float)(-Math.PI + ltor)).add(rotInfo.rightArmRot.scale(-0.2)).scale(-1/scaleFactor);           
    				this.bipedRightArm.setRotationPoint((float)rarm.xCoord, (float)rarm.yCoord, -(float)rarm.zCoord);   
    				this.bipedRightArm.rotateAngleX=(float) (-pitch2+ 3*Math.PI/2);
    				this.bipedRightArm.rotateAngleY=(float) (Math.PI-yaw2 - ltor);


    				Vec3d rsh = new Vec3d(rightShoulder.rotationPointX - rarm.xCoord, 
    						rightShoulder.rotationPointY - rarm.yCoord,
    						rightShoulder.rotationPointZ - rarm.zCoord);

    				float yawrs = (float) Math.atan2(-rsh.xCoord, -rsh.zCoord); 
    				float pitchrs = (float) Math.asin(rsh.yCoord/rsh.lengthVector()); 		
    				rightShoulder.rotateAngleY = (float) (-yawrs);
    				rightShoulder.rotateAngleX = (float) (-pitchrs+ 3*Math.PI/2);


    				armor.bipedLeftArm.rotateAngleY = this.leftshoulder.rotateAngleY;  				
    				armor.bipedLeftArm.rotateAngleX = this.leftshoulder.rotateAngleX;
    				armor.bipedRightArm.rotateAngleY = this.rightShoulder.rotateAngleY;  				
    				armor.bipedRightArm.rotateAngleX = this.rightShoulder.rotateAngleX;
    				this.bipedLeftArm.scaleY = 0.5f;
    				this.bipedLeftArmwear.scaleY = 0.5f;
    				this.bipedRightArm.scaleY = 0.5f;
    				this.bipedRightArmwear.scaleY = 0.5f;	

    				if(rotInfo.reverse){
    					this.rightShoulder.setRotationPoint(5.0F, 2.0F , 0.0F);
    					this.leftshoulder.setRotationPoint(-5.0F, 2.0F , 0.0F);
    				} else {
    					this.rightShoulder.setRotationPoint(-5.0F, 2.0F , 0.0F);
    					this.leftshoulder.setRotationPoint(5.0F, 2.0F , 0.0F);
    				}

    			} 
    		}else {
    			this.bipedLeftArm.scaleY = 1;
    			this.bipedLeftArmwear.scaleY = 1;
    			this.bipedRightArm.scaleY =1;
    			this.bipedRightArmwear.scaleY =1;
    			this.bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);
    			this.bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);

    		}
    	}
    	
    	this.vrHMD.isHidden = false;
    	switch(rotInfo.hmd){
    	case 0:
    		this.vrHMD.isHidden = true;
    		break;
    	case 1:
    		this.vrHMD.setTextureLocation(this.BLACK_HMD);
    		break;
    	case 2:
    		this.vrHMD.setTextureLocation(this.GOLD_HMD);
    		break;
    	case 3:
    		this.vrHMD.setTextureLocation(this.DIAMOND_HMD);	
    		break;
    	case 4:
    		this.vrHMD.setTextureLocation(this.DIAMOND_HMD);	
    		break;
    	}

    	copyModelAngles(this.bipedHead, this.bipedHeadwear);        
        copyModelAngles(this.bipedHead, this.vrHMD);        
    	copyModelAngles(this.bipedLeftLeg, this.bipedLeftLegwear);
    	copyModelAngles(this.bipedRightLeg, this.bipedRightLegwear);
    	copyModelAngles(this.bipedLeftArm, this.bipedLeftArmwear);
    	copyModelAngles(this.bipedRightArm, this.bipedRightArmwear);
    	copyModelAngles(this.bipedBody, this.bipedBodyWear);
    }

    private Random rand = new Random();
    
    public void setInvisible(boolean invisible)
    {
        super.setInvisible(invisible);
        this.bipedLeftArmwear.showModel = invisible;
        this.bipedRightArmwear.showModel = invisible;
        this.bipedLeftLegwear.showModel = invisible;
        this.bipedRightLegwear.showModel = invisible;
        this.bipedBodyWear.showModel = invisible;
        this.bipedCape.showModel = invisible;
        this.bipedDeadmau5Head.showModel = invisible;
        this.vrHMD.showModel = invisible;

    }

    public void postRenderArm(float scale, EnumHandSide side)
    {
        ModelRenderer modelrenderer = this.getArmForSide(side);

        if (this.smallArms)
        {
            float f = 0.5F * (float)(side == EnumHandSide.RIGHT ? 1 : -1);
            modelrenderer.rotationPointX += f;
            modelrenderer.postRender(scale);
            modelrenderer.rotationPointX -= f;
        }
        else
        {
            modelrenderer.postRender(scale);
        }
    }
}
