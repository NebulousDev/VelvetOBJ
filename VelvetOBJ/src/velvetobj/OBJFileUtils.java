package velvetobj;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class OBJFileUtils
{
	public static byte[] readFileAsBytes(String filepath)
	{
		byte[] buffer = new byte[1024];
		InputStream fileStream = OBJFileUtils.class.getResourceAsStream(filepath);
		ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
		
		if(fileStream == null)
		{
			System.out.println("[VELVET OBJ] Error reading file: Unable to locate file: " + filepath);
			return null;
		}
		
		try
		{
			int bytes = 0;
			while((bytes = fileStream.read(buffer)) > 0)
				byteBuffer.write(buffer, 0, bytes);
			fileStream.close();
		}
		
		catch(Exception e)
		{
			System.out.println("[VELVET OBJ] Error reading file: Unable to read bytes from: " + filepath);
			e.printStackTrace();
			return null;
		}
		
		return byteBuffer.toByteArray();
	}
	
	public static String readFileAsString(String filepath)
	{
		return new String(readFileAsBytes(filepath));
	}
	
	public static String getContainingFolder(String filepath)
	{
		int last = filepath.lastIndexOf("/");	//TODO: Allow other methods
		return filepath.substring(0, last);
	}
	
	public static String stripExtention(String filename)
	{
		int last = filename.lastIndexOf(".");
		return filename.substring(0, last);
	}
	
	public static String getFilename(String filepath)
	{
		String[] tokens = filepath.split("/|\\\\");
		String filename = tokens[tokens.length - 1];
		return stripExtention(filename);
	}
	
	public static String getFileExtention(String filepath)
	{
		int last = filepath.lastIndexOf(".");
		return filepath.substring(last);
	}
}
