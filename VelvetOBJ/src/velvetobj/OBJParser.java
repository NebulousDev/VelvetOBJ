package velvetobj;

import java.awt.List;
import java.util.ArrayList;
import java.util.HashMap;

public class OBJParser
{
	private static final String OBJ_VERTEX 		= "v";
	private static final String OBJ_TEXTURE 	= "vt";
	private static final String OBJ_NORMAL		= "vn";
	private static final String OBJ_FACE		= "f";
	private static final String OBJ_OBJECT		= "o";
	private static final String OBJ_GROUP		= "g";
	
	private static final int 	NO_INDEX		= -1;
	
	private static String 		sFilepath 		= null;
	private static int 			sLinenumber 	= 0;
	
	public static OBJModel parseOBJ(String filepath)
	{
		// Read File //
		String filedata = OBJFileUtils.readFileAsString(filepath);
		
		if(filedata == null)
			System.out.println("[VELVET OBJ] Failed to parse OBJ. File returned null.");
		
		sFilepath 	= null;
		sLinenumber = 0;
		
		// Create Model //
		OBJModel model 	= new OBJModel();
		model.name 		= OBJFileUtils.getFilename(filepath);
		
		// Temporary Lists //
		ArrayList<OBJVertex> 	vertices 	= new ArrayList<>();
		ArrayList<Integer>		indices 	= new ArrayList<>();
		ArrayList<float[]>		positions 	= new ArrayList<>();
		ArrayList<float[]>		textures 	= new ArrayList<>();
		ArrayList<float[]>		normals 	= new ArrayList<>();
		
		HashMap<String, Integer> indexMap	= new HashMap<>();
		
		ArrayList<OBJBundle>	objectBundles 		= new ArrayList<>();
		ArrayList<OBJBundle>	materialBundles 	= new ArrayList<>();
		
		// Split File //
		String[] lines = filedata.split("\r\n|\r|\n");
		
		// Parse Lines //
		for(String line : lines)
		{
			String[] tokens = tokenize(line, "\\s+");
			
			boolean success = true;
			
			switch(tokens[0])
			{
				case OBJ_VERTEX: 	success = parseVertex(tokens, positions); 						break;
				case OBJ_TEXTURE: 	success = parseTexture(tokens, textures);						break;
				case OBJ_NORMAL: 	success = parseNormal(tokens, normals);							break;
				case OBJ_FACE:	 	success = parseFace(tokens, positions, textures, normals,
															vertices, indices, indexMap);			break;
				case OBJ_OBJECT:
				case OBJ_GROUP:
				{
					if(!objectBundles.isEmpty())
					{
						OBJBundle lastBundle = objectBundles.get(objectBundles.size() - 1);
						lastBundle.count = indices.size() - lastBundle.index;
					}
					
					OBJBundle bundle = new OBJBundle();
					bundle.name = tokens[1];
					bundle.index = indices.size();
					objectBundles.add(bundle);
				}
				
				default: break;
			}
			
			if(!success) return null;
			
			++sLinenumber;
		}
		
		// Generate Final Lists //
		OBJVertex[] resVertices 	= new OBJVertex[vertices.size()];
		int[] 		resIndices 		= new int[indices.size()];
		
		for(int i = 0; i < vertices.size(); i++)
			resVertices[i] = vertices.get(i);
		
		for(int i = 0; i < indices.size(); i++)
			resIndices[i] = indices.get(i);
		
		model.vertices 	= resVertices;
		model.indices 	= resIndices;
		
		if(objectBundles.isEmpty())
		{
			OBJBundle bundle = new OBJBundle();
			bundle.name = model.name;
			bundle.index = 0;
			bundle.count = indices.size();
			objectBundles.add(bundle);
		}
		
		if(materialBundles.isEmpty())
		{
			OBJBundle bundle = new OBJBundle();
			bundle.name = model.name;
			bundle.index = 0;
			bundle.count = indices.size();
			objectBundles.add(bundle);
		}
		
		OBJBundle[] resObjectBundle 	= new OBJBundle[objectBundles.size()];
		OBJBundle[] resMatrialBundle 	= new OBJBundle[materialBundles.size()];
		
		for(int i = 0; i < objectBundles.size(); i++)
			resObjectBundle[i] = objectBundles.get(i);
		
		for(int i = 0; i < materialBundles.size(); i++)
			resMatrialBundle[i] = materialBundles.get(i);
		
		model.objectBundles 	= resObjectBundle;
		model.materialBundles 	= resMatrialBundle;
		
		sFilepath 	= null;
		sLinenumber = 0;
		
		return model;
	}

	private static boolean parseVertex(String[] tokens, ArrayList<float[]> positionsList)
	{
		try
		{
			float[] positions = new float[]
			{
				Float.parseFloat(tokens[1]),
				Float.parseFloat(tokens[2]),
				Float.parseFloat(tokens[3]),
			};
			
			positionsList.add(positions);
			
			return true;
		}

		catch(NumberFormatException e)
		{
			objError("Failed to parse float value.");
			return false;
		}
		
		catch(IndexOutOfBoundsException e)
		{
			objError("Too few vertex arguments.");
			return false;
		}
	}
	
	private static boolean parseTexture(String[] tokens, ArrayList<float[]> texturesList)
	{
		try
		{
			float[] textures = new float[]
			{
				Float.parseFloat(tokens[1]),
				Float.parseFloat(tokens[2]),
			};
			
			texturesList.add(textures);
			
			return true;
		}

		catch(NumberFormatException e)
		{
			objError("Failed to parse float value.");
			return false;
		}
		
		catch(IndexOutOfBoundsException e)
		{
			objError("Too few vertex texture arguments.");
			return false;
		}
	}
	
	private static boolean parseNormal(String[] tokens, ArrayList<float[]> normalsList)
	{
		try
		{
			float[] normals = new float[]
			{
				Float.parseFloat(tokens[1]),
				Float.parseFloat(tokens[2]),
				Float.parseFloat(tokens[3]),
			};
			
			normalsList.add(normals);
			
			return true;
		}

		catch(NumberFormatException e)
		{
			objError("Failed to parse float value.");
			return false;
		}
		
		catch(IndexOutOfBoundsException e)
		{
			objError("Too few vertex normal arguments.");
			return false;
		}
	}
	
	private static boolean parseFace(String[] tokens, ArrayList<float[]> positionsList, ArrayList<float[]> texturesList, ArrayList<float[]> normalsList, 
			ArrayList<OBJVertex> verticesList, ArrayList<Integer> indicesList, HashMap<String, Integer> indexMap)
	{
		// Build first three vertices //
		if(!buildVertex(tokens[1], positionsList, texturesList, normalsList, verticesList, indicesList, indexMap)) return false;
		if(!buildVertex(tokens[2], positionsList, texturesList, normalsList, verticesList, indicesList, indexMap)) return false;
		if(!buildVertex(tokens[3], positionsList, texturesList, normalsList, verticesList, indicesList, indexMap)) return false;

		if(tokens.length > 4)
		{
			for(int i = 3; i < tokens.length; i++)
			{
				if(!buildVertex(tokens[1], positionsList, texturesList, normalsList, verticesList, indicesList, indexMap)) return false;
				if(!buildVertex(tokens[i - 1], positionsList, texturesList, normalsList, verticesList, indicesList, indexMap)) return false;
				if(!buildVertex(tokens[i], positionsList, texturesList, normalsList, verticesList, indicesList, indexMap)) return false;
			}
		}

		return true;
	}

	private static boolean buildVertex(String token, ArrayList<float[]> positionsList, ArrayList<float[]> texturesList, ArrayList<float[]> normalsList, 
			ArrayList<OBJVertex> verticesList, ArrayList<Integer> indicesList, HashMap<String, Integer> indexMap)
	{
		try
		{
			if(!indexMap.containsKey(token))
			{
				String[] elements = tokenize(token, "/");
				
				int positionIdx 	= NO_INDEX;
				int textureIdx 		= NO_INDEX;
				int normalIdx 		= NO_INDEX;
				
				switch (elements.length)
				{
					case 1:
					{
						positionIdx = Integer.parseInt(elements[0]) - 1;
						break;
					}
					
					case 2:
					{
						positionIdx = Integer.parseInt(elements[0]) - 1;
						textureIdx 	= Integer.parseInt(elements[1]) - 1;
						break;
					}
					
					case 3:
					{
						if(elements[1].equals(""))
						{
							positionIdx = Integer.parseInt(elements[0]) - 1;
							normalIdx 	= Integer.parseInt(elements[2]) - 1;
						}
						
						else
						{
							positionIdx = Integer.parseInt(elements[0]) - 1;
							textureIdx 	= Integer.parseInt(elements[1]) - 1;
							normalIdx 	= Integer.parseInt(elements[2]) - 1;
						}
						
						break;
					}
		
					default:
					{
						objError("Invalid element size.");
						return false;
					}
				}
				
				OBJVertex vertex = new OBJVertex();
				
				if(positionIdx != NO_INDEX) 	vertex.position = positionsList.get(positionIdx);
				if(textureIdx != NO_INDEX) 		vertex.texture 	= texturesList.get(textureIdx);
				if(normalIdx != NO_INDEX) 		vertex.normal 	= normalsList.get(normalIdx);
				
				verticesList.add(vertex);
				
				int index = indexMap.size();
				indicesList.add(index);
				
				indexMap.put(token, index);
			}
			
			else
			{
				indicesList.add(indexMap.get(token));
			}
		}

		catch(NumberFormatException e)
		{
			objError("Failed to parse int value.");
			return false;
		}
		
		catch(IndexOutOfBoundsException e)
		{
			objError("Too few vertex normal arguments.");
			return false;
		}
		
		return true;
	}
	
	private static void objError(String error)
	{
		System.out.println("[VELVET OBJ] Error parsing OBJ file:\n\tFILE: " 
				+ sFilepath + "\n\tLINE: " + sLinenumber + "\nREASON: " + error);
	}

	private static String indexHash(int pos, int tex, int norm)
	{
		return "" + pos + tex + norm;
	}
	
	private static String[] tokenize(String line, String regex)
	{
		return line.trim().toLowerCase().split(regex);
	}
}