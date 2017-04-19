package com.mtbs3d.minecrift.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import net.minecraft.util.math.Vec3d;

public class Utils
{
	public static Field getDeclaredField(Class clazz, String unObfuscatedName, String obfuscatedName, String srgName)
	{
		Field field = null;
		String s = clazz.getName();

		try
		{
			field = clazz.getDeclaredField(unObfuscatedName);
		}
		catch (NoSuchFieldException e)
		{
			try
			{
				field = clazz.getDeclaredField(obfuscatedName);
			}
			catch (NoSuchFieldException e1)
			{
				try
				{
					field = clazz.getDeclaredField(srgName);
				}
				catch (NoSuchFieldException e2)
				{
					System.out.println("[Vivecraft] WARNING: could not reflect field :" + unObfuscatedName + "," + srgName + "," + obfuscatedName + " in " + clazz.toString());
				};
			};
		}

		return field;
	}
	
  	public static Vector3f convertVector(de.fruitfly.ovr.structs.Vector3f vector) {
		return new Vector3f(vector.x, vector.y, vector.z);
	}

	public static de.fruitfly.ovr.structs.Vector3f convertToOVRVector(Vector3f vector) {
		return new de.fruitfly.ovr.structs.Vector3f(vector.x, vector.y, vector.z);
	}

	
	public static Vector3f directionFromMatrix(Matrix4f matrix, float x, float y, float z) {
		Vector4f vec = new Vector4f(x, y, z, 0);
		Matrix4f.transform(matrix, vec, vec);
		vec.normalise(vec);
		return new Vector3f(vec.x, vec.y, vec.z);
	}
	
	/* With thanks to http://ramblingsrobert.wordpress.com/2011/04/13/java-word-wrap-algorithm/ */
    public static void wordWrap(String in, int length, ArrayList<String> wrapped)
    {
        String newLine = "\n";
        String wrappedLine;
        boolean quickExit = false;

        // Remove carriage return
        in = in.replace("\r", "");

        if(in.length() < length)
        {
            quickExit = true;
            length = in.length();
        }

        // Split on a newline if present
        if(in.substring(0, length).contains(newLine))
        {
            wrappedLine = in.substring(0, in.indexOf(newLine)).trim();
            wrapped.add(wrappedLine);
            wordWrap(in.substring(in.indexOf(newLine) + 1), length, wrapped);
            return;
        }
        else if (quickExit)
        {
            wrapped.add(in);
            return;
        }

        // Otherwise, split along the nearest previous space / tab / dash
        int spaceIndex = Math.max(Math.max( in.lastIndexOf(" ", length),
                in.lastIndexOf("\t", length)),
                in.lastIndexOf("-", length));

        // If no nearest space, split at length
        if(spaceIndex == -1)
            spaceIndex = length;

        // Split!
        wrappedLine = in.substring(0, spaceIndex).trim();
        wrapped.add(wrappedLine);
        wordWrap(in.substring(spaceIndex), length, wrapped);
    }
    
	public static Vector2f convertVector(Vector2 vector) {
		return new Vector2f(vector.getX(), vector.getY());
	}

	public static Vector2 convertVector(Vector2f vector) {
		return new Vector2(vector.getX(), vector.getY());
	}

	public static Vector3f convertVector(Vector3 vector) {
		return new Vector3f(vector.getX(), vector.getY(), vector.getZ());
	}

	public static Vector3 convertVector(Vector3f vector) {
		return new Vector3(vector.getX(), vector.getY(), vector.getZ());
	}

	public static Vector3 convertVector(Vec3d vector) {
		return new Vector3((float)vector.xCoord, (float)vector.yCoord, (float)vector.zCoord);
	}

	public static Vector3f convertToVector3f(Vec3d vector) {
		return new Vector3f((float)vector.xCoord, (float)vector.yCoord, (float)vector.zCoord);
	}

	public static Quaternion quatLerp(Quaternion start, Quaternion end, float fraction) {
		Quaternion quat = new Quaternion();
		quat.w = start.w + (end.w - start.w) * fraction;
		quat.x = start.x + (end.x - start.x) * fraction;
		quat.y = start.y + (end.y - start.y) * fraction;
		quat.z = start.z + (end.z - start.z) * fraction;
		return quat;
	}

	public static Matrix4f matrix3to4(Matrix3f matrix) {
		Matrix4f mat = new Matrix4f();
		mat.m00 = matrix.m00;
		mat.m01 = matrix.m01;
		mat.m02 = matrix.m02;
		mat.m10 = matrix.m10;
		mat.m11 = matrix.m11;
		mat.m12 = matrix.m12;
		mat.m20 = matrix.m20;
		mat.m21 = matrix.m21;
		mat.m22 = matrix.m22;
		return mat;
	}

	public static String httpReadLine(String url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setReadTimeout(3000);
		conn.setUseCaches(false);
		conn.setDoInput(true);
		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line = br.readLine();
		br.close();
		conn.disconnect();
		return line;
	}

	public static byte[] httpReadAll(String url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setReadTimeout(3000);
		conn.setUseCaches(false);
		conn.setDoInput(true);
		InputStream is = conn.getInputStream();
		ByteArrayOutputStream bout = new ByteArrayOutputStream(conn.getContentLength());
		byte[] bytes = new byte[4096];
		int count;
		while ((count = is.read(bytes, 0, bytes.length)) != -1) {
			bout.write(bytes, 0, count);
		}
		is.close();
		conn.disconnect();
		return bout.toByteArray();
	}

	public static String httpReadAllString(String url) throws IOException {
		return new String(httpReadAll(url), StandardCharsets.UTF_8);
	}

	public static void httpReadToFile(String url, File file, boolean writeWhenComplete) throws MalformedURLException, IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setReadTimeout(3000);
		conn.setUseCaches(false);
		conn.setDoInput(true);
		InputStream is = conn.getInputStream();
		if (writeWhenComplete) {
			ByteArrayOutputStream bout = new ByteArrayOutputStream(conn.getContentLength());
			byte[] bytes = new byte[4096];
			int count;
			while ((count = is.read(bytes, 0, bytes.length)) != -1) {
				bout.write(bytes, 0, count);
			}
			OutputStream out = new FileOutputStream(file);
			out.write(bout.toByteArray());
			out.flush();
			out.close();
		} else {
			OutputStream out = new FileOutputStream(file);
			byte[] bytes = new byte[4096];
			int count;
			while ((count = is.read(bytes, 0, bytes.length)) != -1) {
				out.write(bytes, 0, count);
			}
			out.flush();
			out.close();
		}
		is.close();
		conn.disconnect();
	}
	
    public static void httpReadToFile(String url, File file) throws IOException {
        httpReadToFile(url, file, false);
    }

	public static List<String> httpReadList(String url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setReadTimeout(3000);
		conn.setUseCaches(false);
		conn.setDoInput(true);
		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		List<String> list = new ArrayList<String>();
		String line;
		while ((line = br.readLine()) != null) {
			list.add(line);
		}
		br.close();
		conn.disconnect();
		return list;
	}

	public static String getFileChecksum(File file, String algorithm) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
		InputStream is = new FileInputStream(file);
		byte[] bytes = new byte[(int)file.length()];
		is.read(bytes);
		is.close();
		MessageDigest md = MessageDigest.getInstance(algorithm);
		md.update(bytes);
		Formatter fmt = new Formatter();
		for (byte b : md.digest()) {
			fmt.format("%02x", b);
		}
		String str = fmt.toString();
		fmt.close();
		return str;
	}

	public static byte[] readFile(File file) throws FileNotFoundException, IOException {
		FileInputStream is = new FileInputStream(file);
		return readFully(is);
	}

	public static String readFileString(File file) throws FileNotFoundException, IOException {
		return new String(readFile(file), "UTF-8");
	}
	
	public static byte[] readFully(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] bytes = new byte[4096];
		int count;
		while ((count = in.read(bytes, 0, bytes.length)) != -1) {
			out.write(bytes, 0, count);
		}
		in.close();
		return out.toByteArray();
	}
	
	public static Vec3d convertToVec3d(Vector3 vector) {
		return new Vec3d(vector.getX(), vector.getY(), vector.getZ());
	}
	
	public static Vec3d vecLerp(Vec3d start, Vec3d end, double fraction) {
		double x = start.xCoord + (end.xCoord - start.xCoord) * fraction;
		double y = start.yCoord + (end.yCoord - start.yCoord) * fraction;
		double z = start.zCoord + (end.zCoord - start.zCoord) * fraction;
		return new Vec3d(x, y, z);
	}

}
