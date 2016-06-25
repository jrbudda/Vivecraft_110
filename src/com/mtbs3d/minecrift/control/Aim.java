package com.mtbs3d.minecrift.control;

import com.mtbs3d.minecrift.settings.VRSettings;
import de.fruitfly.ovr.enums.EyeType;
import net.minecraft.client.Minecraft;

/**
 * Created by StellaArtois on 11/18/2014.
 */
public class Aim
{
    static protected float aimPitch      = 0f;
    static protected float aimYaw        = 0f;
    static protected float lastAimPitch  = 0f;
    static protected float lastAimYaw    = 0f;
    static protected float bodyPitch     = 0f;
    static protected float bodyYaw       = 0f;
    static protected float lastBodyPitch = 0f;
    static protected float lastBodyYaw   = 0f;
    static protected float aimYawOffset  = 0f;
    static protected float prevHeadYaw   = 0f;
    static protected float prevHeadPitch = 0f;
    static protected boolean holdCenter = false;
    static protected DiscreteAngle discreteYaw = new DiscreteAngle();
    static protected DiscreteAngle discretePitch = new DiscreteAngle();

    Minecraft mc = Minecraft.getMinecraft();

    public void updateAim(int aimType, float aimPitchAdd, float aimYawAdd, float aimPitchRate, float aimYawRate)
    {
//        if (this.mc.currentScreen != null)
//            return;
//
//        boolean updatedPitch = false;
//        boolean updatedYaw = false;
//        double currentTimeSecs = this.mc.getCurrentTimeSecs();
//
//        configureComfortMode();
//
//
//        // *** Pitch ***
//
//        float headPitch = this.mc.headTracker.getHeadPitchDegrees(EyeType.ovrEye_Center);
//        float headPitchDelta = headPitch - this.prevHeadPitch;
//        this.prevHeadPitch = headPitch;
//
//        if (holdCenter) {
//            aimPitch = headPitch;
//        }
//        else if( this.mc.vrSettings.keyholeHeight > 0 )     // Keyhole
//        {
//            float totalHeadPitch = headPitch + bodyPitch;
//            float keyHoleHeight = this.mc.vrSettings.keyholeHeight /2;
//            float keyholeBot = Math.max(-90,totalHeadPitch - keyHoleHeight);
//            float keyholeTop = Math.min(90,totalHeadPitch + keyHoleHeight);
//
//            if ( aimType == VRSettings.AIM_TYPE_RECENTER )
//            {
//                headPitchDelta = 0;
//                //square the number to give more precision in the low angles
//                aimPitch = headPitch + aimPitchRate * Math.abs(aimPitchRate)* keyHoleHeight;
//                if( aimPitch > 90.0f )
//                    aimPitch = 90.0f;
//                else if( aimPitch < -90.0f )
//                    aimPitch = -90.0f;
//            }
//
//            if (this.mc.vrSettings.crosshairHeadRelative)
//                aimPitch += headPitchDelta;
//
//            if (aimType == VRSettings.AIM_TYPE_TIGHT)
//            {
//                // Keep cursor constrained to keyhole
//                if (aimPitch > keyholeTop)
//                    aimPitch = keyholeTop;
//                else if (aimPitch < keyholeBot)
//                    aimPitch = keyholeBot;
//            }
//
//            // Only accrue controller pitch *delta* while touching the edge of the keyhole...
//            if( aimPitchAdd != 0 )
//            {
//                aimPitch += aimPitchAdd;
//                if( aimPitch > keyholeTop )
//                {
//                    if (aimPitchAdd > 0 || aimType != VRSettings.AIM_TYPE_LOOSE)
//                    {
//                        if (this.mc.vrSettings.allowMousePitchInput)
//                        {
//                            discretePitch.update(aimType == VRSettings.AIM_TYPE_TIGHT ? aimPitch - keyholeTop : aimType == VRSettings.AIM_TYPE_RECENTER ? 0.75f*aimPitchAdd : aimPitchAdd, currentTimeSecs);
//                            bodyPitch = (float) discretePitch.getCurrent(this.mc.PredictedDisplayTimeSeconds);
//                            bodyPitch -= (!this.mc.vrSettings.crosshairHeadRelative ? 0 : headPitchDelta);
//                        }
//                        else
//                        {
//                            bodyPitch = 0;
//                            discretePitch.resetAngle();
//                        }
//
//                        aimPitch = keyholeTop;
//                        updatedPitch = true;
//                    }
//                }
//                else if( aimPitch < keyholeBot )
//                {
//                    if (aimPitchAdd < 0 || aimType != VRSettings.AIM_TYPE_LOOSE)
//                    {
//                        if (this.mc.vrSettings.allowMousePitchInput) {
//                            discretePitch.update(aimType == VRSettings.AIM_TYPE_TIGHT ? aimPitch - keyholeBot : aimType == VRSettings.AIM_TYPE_RECENTER ? 0.75f*aimPitchAdd : aimPitchAdd, currentTimeSecs);
//                            bodyPitch = (float) discretePitch.getCurrent(this.mc.PredictedDisplayTimeSeconds);
//                            bodyPitch -= (!this.mc.vrSettings.crosshairHeadRelative ? 0 : headPitchDelta);
//                        }
//                        else
//                        {
//                            bodyPitch = 0;
//                            discretePitch.resetAngle();
//                        }
//
//                        aimPitch = keyholeBot;
//                        updatedPitch = true;
//                    }
//                }
//
//                if ( aimType == VRSettings.AIM_TYPE_RECENTER && !updatedPitch )
//                    aimPitch -= aimPitchAdd;
//            }
//        }
//        else if( this.mc.vrSettings.allowMousePitchInput )     // No keyhole with allow pitch change
//        {
//            aimPitch = 0;
//            discretePitch.update(aimPitchAdd, currentTimeSecs);
//            bodyPitch = (float)discretePitch.getCurrent(this.mc.PredictedDisplayTimeSeconds);
//            updatedPitch = true;
//        }
//        else   // No keyhole, no pitch change
//        {
//            aimPitch = 0;
//            bodyPitch = 0;
//            discretePitch.resetAngle();
//            updatedPitch = true;
//        }
//
//        // Make sure we *always* update the move state, even with 0 delta. This allows
//        // any view ratcheting to complete correctly
//        if (!updatedPitch)
//        {
//            if (this.mc.vrSettings.allowMousePitchInput)
//            {
//                discretePitch.update(0, currentTimeSecs);
//                bodyPitch = (float) discretePitch.getCurrent(this.mc.PredictedDisplayTimeSeconds);
//                bodyPitch -= (!this.mc.vrSettings.crosshairHeadRelative ? 0 : headPitchDelta);
//            }
//            else
//            {
//                bodyPitch = 0;
//                discretePitch.resetAngle();
//            }
//            updatedPitch = true;
//        }
//        if( bodyPitch > 90 )
//            bodyPitch = 90;
//        if( bodyPitch < -90 )
//            bodyPitch = -90;
//
//        boolean pitchGood = aimPitch != 90 && aimPitch != -90;
//
//
//        // *** Yaw ***
//
//        float headYaw = this.mc.headTracker.getHeadYawDegrees(EyeType.ovrEye_Center);
//        float headYawDelta = headYaw - this.prevHeadYaw;
//        this.prevHeadYaw = headYaw;
//
//        if (holdCenter) {
//            aimYaw = headYaw;
//        }
//        else if( this.mc.vrSettings.keyholeWidth > 0 )    // Keyhole
//        {
//            float keyholeYawWidth = this.mc.vrSettings.keyholeWidth /2;
//            float keyholeYawLeft = headYaw - keyholeYawWidth;
//            float keyholeYawRight = headYaw + keyholeYawWidth;
//
//            if ( aimType == VRSettings.AIM_TYPE_RECENTER )
//            {
//                headYawDelta = 0;
//                if( Math.abs(aimYawRate) < 0.75 ) {
//                    aimYaw = headYaw + (16 * aimYawRate * Math.abs(aimYawRate)* keyholeYawWidth / 9);
//                }
//                else if( aimYawRate > 0) {
//                    aimYaw = aimYawRate > 0 ? keyholeYawRight : keyholeYawLeft;
//                }
//            }
//
//            if (this.mc.vrSettings.crosshairHeadRelative)
//                aimYaw += headYawDelta;
//
//            if (aimType == VRSettings.AIM_TYPE_TIGHT)
//            {
//                // Keep cursor constrained to keyhole
//                if (aimYaw > keyholeYawRight)
//                    aimYaw = keyholeYawRight;
//                else if (aimYaw < keyholeYawLeft)
//                    aimYaw = keyholeYawLeft;
//            }
//
//            // Only accrue controller yaw *delta* while touching the edge of the keyhole...
//            if( pitchGood && aimYawAdd != 0)
//            {
//                aimYaw += aimYawAdd;
//
//                // If we are using cycle left / cycle right key mapping for yaw control,
//                // don't accrue any additional yaw here...
//                if(this.mc.vrSettings.useKeyBindingForComfortYaw && aimYaw > keyholeYawRight) {
//                    aimYaw = keyholeYawRight;
//                }
//                else if(this.mc.vrSettings.useKeyBindingForComfortYaw && aimYaw < keyholeYawLeft) {
//                    aimYaw = keyholeYawLeft;
//                }
//
//                if( aimYaw > keyholeYawRight )
//                {
//                    if (aimYawAdd > 0 || aimType != VRSettings.AIM_TYPE_LOOSE)
//                    {
//                        discreteYaw.update(aimType == VRSettings.AIM_TYPE_TIGHT ? aimYaw - keyholeYawRight : aimType == VRSettings.AIM_TYPE_RECENTER ? 0.75f*aimYawAdd : aimYawAdd, currentTimeSecs);
//                        bodyYaw = (float) discreteYaw.getCurrent(this.mc.PredictedDisplayTimeSeconds);
//                        bodyYaw -= (!this.mc.vrSettings.crosshairHeadRelative ? 0 : headYawDelta);
//                        aimYaw = keyholeYawRight;
//                        updatedYaw = true;
//                    }
//                }
//                else if( aimYaw < keyholeYawLeft )
//                {
//                    if (aimYawAdd < 0 || aimType != VRSettings.AIM_TYPE_LOOSE)
//                    {
//                        discreteYaw.update(aimType == VRSettings.AIM_TYPE_TIGHT ? aimYaw - keyholeYawLeft : aimType == VRSettings.AIM_TYPE_RECENTER ? 0.75f*aimYawAdd : aimYawAdd, currentTimeSecs);
//                        bodyYaw = (float) discreteYaw.getCurrent(this.mc.PredictedDisplayTimeSeconds);
//                        bodyYaw -= (!this.mc.vrSettings.crosshairHeadRelative ? 0 : headYawDelta);
//                        aimYaw = keyholeYawLeft;
//                        updatedYaw = true;
//                    }
//                }
//            }
//
//            if ( aimType == VRSettings.AIM_TYPE_RECENTER && !updatedYaw )
//                aimYaw -= aimYawAdd;
//        }
//        else  // No keyhole
//        {
//            aimYaw = 0;
//            discreteYaw.update(aimYawAdd, currentTimeSecs);
//            bodyYaw = (float)discreteYaw.getCurrent(this.mc.PredictedDisplayTimeSeconds);
//            updatedYaw = true;
//        }
//
//        // Make sure we *always* update the move state, even with 0 delta. This allows
//        // any view ratcheting to complete correctly
//        if(!updatedYaw)
//        {
//            discreteYaw.update(0, currentTimeSecs);
//            bodyYaw = (float)discreteYaw.getCurrent(this.mc.PredictedDisplayTimeSeconds);
//            bodyYaw -= (!this.mc.vrSettings.crosshairHeadRelative ? 0 : headYawDelta);
//        }
//
//        bodyYaw %= 360;
//
//        lastAimPitch = aimPitch;
//        lastAimYaw = aimYaw;
//        lastBodyPitch = bodyPitch;
//        lastBodyYaw = bodyYaw;
    }

    public void configureComfortMode()
    {
        boolean yawComfortOn = this.mc.vrSettings.useVrComfort == this.mc.vrSettings.VR_COMFORT_YAW || this.mc.vrSettings.useVrComfort == this.mc.vrSettings.VR_COMFORT_PITCHANDYAW;
        boolean pitchComfortOn = this.mc.vrSettings.useVrComfort == this.mc.vrSettings.VR_COMFORT_PITCH || this.mc.vrSettings.useVrComfort == this.mc.vrSettings.VR_COMFORT_PITCHANDYAW;
        discreteYaw.configure(yawComfortOn ? this.mc.vrSettings.vrComfortTransitionAngleDegs : 0,
                this.mc.vrSettings.vrComfortTransitionTimeSecs, this.mc.vrSettings.vrComfortTransitionLinear);
        discretePitch.configure(pitchComfortOn ? this.mc.vrSettings.vrComfortTransitionAngleDegs : 0,
                this.mc.vrSettings.vrComfortTransitionTimeSecs, this.mc.vrSettings.vrComfortTransitionLinear);
    }

    static public float getBodyYaw() {
        return bodyYaw;
    }

    static public void setAimYawOffset(float yawOffset) {
        aimYawOffset = yawOffset;
    }

    static public float getBodyPitch() {
        return bodyPitch;
    }

    static public float getAimYaw() {
        return (bodyYaw + aimYaw + aimYawOffset) % 360;
    }

    static public float getAimPitch() {
        return aimPitch;
    }

    static public void setHoldCenter(boolean state) {
        holdCenter = state;
    }

    static public double getYawTransitionPercent() {
        return discreteYaw._percent;
    }

    static public void triggerYawChange(boolean isPositive) {
        discreteYaw.triggerChange(isPositive);
    }

    static public double getPitchTransitionPercent() {
        return discretePitch._percent;
    }
}
