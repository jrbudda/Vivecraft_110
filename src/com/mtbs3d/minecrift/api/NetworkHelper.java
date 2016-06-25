package com.mtbs3d.minecrift.api;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Charsets;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.network.play.server.SPacketCustomPayload;

public class NetworkHelper {

	public enum PacketDiscriminators {
		VERSION,
		POSITIONS,
		DAMAGE,
		MOVEMODE
	}

	private final static String channel = "Vivecraft";
	
	public static CPacketCustomPayload getVivecraftClientPacket(PacketDiscriminators command, byte[] payload)
	{
		PacketBuffer pb = new PacketBuffer(Unpooled.buffer());
		pb.writeByte(command.ordinal());
		pb.writeBytes(payload);
        return  (new CPacketCustomPayload(channel, pb));
	}
	
	public static SPacketCustomPayload getVivecraftServerPacket(PacketDiscriminators command, byte[] payload)
	{//TODO: Packetbuffer?
		PacketBuffer pb = new PacketBuffer(Unpooled.buffer());
		pb.writeByte(command.ordinal());
		pb.writeBytes(payload);
        return (new SPacketCustomPayload(channel, pb));
	}
}
