package net.minecraft.item;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

public class ItemFoodWonder extends ItemFood
{

	public ItemFoodWonder(int amount, float saturation, boolean isWolfFood) {
		super(amount, saturation, isWolfFood);
		if (amount == 1) this.isDrink =true;
	}

	private boolean isDrink =false; 
	
    /**
     * returns the action that specifies what animation to play when the items is being used
     */
    public EnumAction getItemUseAction(ItemStack stack)
    {
    	if(this.isDrink) return EnumAction.DRINK;
        return EnumAction.EAT;
    }
	
    protected void onFoodEaten(ItemStack stack, World worldIn, EntityPlayer player)
    {
    	if(player instanceof EntityPlayerSP){
    		Minecraft.getMinecraft().vrPlayer.wfMode = this.isDrink?-0.1:1;
    		Minecraft.getMinecraft().vrPlayer.wfCount = 400;
    	}
    }
	
    public ActionResult<ItemStack> onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn, EnumHand hand)
    {
            playerIn.setActiveHand(hand);
            return new ActionResult(EnumActionResult.SUCCESS, itemStackIn);
    }
    
    public String getItemStackDisplayName(ItemStack stack)
    {
    	if(isDrink) return "\"DRINK ME\"";
    	return "\"EAT ME\"";
    }
    
}
