import velvetobj.OBJModel;
import velvetobj.OBJParser;

public class OBJTestMain
{
	public static final String filepath = "/standard.obj";
	
	public static void main(String[] args)
	{
		System.out.println("[VELVET OBJ - TEST] Attempting to parse '" + filepath + "'.");
		OBJModel model = OBJParser.parseOBJ(filepath);
		System.out.println("[VELVET OBJ - TEST] Successfully parsed '" + model.name + "'.");
	}
}
