package velvetobj;

import java.util.ArrayList;
import java.util.HashMap;

public class OBJParser
{
	private static String 		sFilepath 		= null;
	private static String		sFolderpath		= null;
	private static int 			sLinenumber 	= 0;
	
	private static final String OBJ_VERTEX 		= "v";
	private static final String OBJ_TEXTURE 	= "vt";
	private static final String OBJ_NORMAL		= "vn";
	private static final String OBJ_FACE		= "f";
	private static final String OBJ_OBJECT		= "o";
	private static final String OBJ_GROUP		= "g";
	
	private static final String OBJ_MTLLIB		= "mtllib";
	private static final String OBJ_USEMTL		= "usemtl";
	
	public static OBJModel parseOBJ(String filepath)
	{
		return parseOBJ(filepath, false);
	}
	
	public static OBJModel parseOBJ(String filepath, boolean flipZ)
	{
		// Read File //
		String filedata = OBJFileUtils.readFileAsString(filepath);
		
		if(filedata == null)
			System.out.println("[VELVET OBJ] Failed to parse OBJ. File returned null.");
		
		sFilepath 		= filepath;
		sFolderpath 	= OBJFileUtils.getContainingFolder(filepath);
		sLinenumber 	= 0;
		
		// Create Model //
		OBJModel model 	= new OBJModel();
		model.name 		= OBJFileUtils.getFilename(filepath);
		
		// Temporary Lists //
		ArrayList<OBJVertex> 			vertices 			= new ArrayList<>();
		ArrayList<Integer>				indices 			= new ArrayList<>();
		ArrayList<float[]>				positions 			= new ArrayList<>();
		ArrayList<float[]>				textures 			= new ArrayList<>();
		ArrayList<float[]>				normals 			= new ArrayList<>();
		
		HashMap<String, Integer> 		indexMap			= new HashMap<>();
		
		ArrayList<OBJBundle>			objectBundles 		= new ArrayList<>();
		ArrayList<OBJBundle>			materialBundles 	= new ArrayList<>();
		
		HashMap<String, OBJMaterial> 	materialMap			= new HashMap<>();
		
		// Split File //
		String[] lines = filedata.split("\r\n|\r|\n");
		
		// Parse Lines //
		for(String line : lines)
		{
			String[] tokens = tokenize(line, "\\s+");
			
			boolean success = true;
			
			switch(tokens[0])
			{
				case OBJ_VERTEX: 	success = parseVertex(tokens, positions, flipZ); 							break;
				case OBJ_TEXTURE: 	success = parseTexture(tokens, textures);									break;
				case OBJ_NORMAL: 	success = parseNormal(tokens, normals, flipZ);								break;
				case OBJ_FACE:	 	success = parseFace(tokens, positions, textures, normals,
															vertices, indices, indexMap);						break;
				case OBJ_OBJECT:
				case OBJ_GROUP:		success = parseObject(tokens, objectBundles, materialBundles, indices); 	break;
				case OBJ_MTLLIB:	success = parseMaterialLib(tokens, sFolderpath, materialMap);				break;
				case OBJ_USEMTL:	success = parseUseMaterial(tokens, materialBundles, materialMap, indices); 	break;
				
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
		else
		{
			OBJBundle lastBundle = objectBundles.get(objectBundles.size() - 1);
			lastBundle.count = indices.size() - lastBundle.index;
		}
		
		if(materialBundles.isEmpty())
		{
			OBJBundle bundle = new OBJBundle();
			bundle.name = model.name;
			bundle.index = 0;
			bundle.count = indices.size();
			objectBundles.add(bundle);
		}
		
		else
		{
			OBJBundle lastBundle = materialBundles.get(materialBundles.size() - 1);
			lastBundle.count = indices.size() - lastBundle.index;
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

	private static boolean parseVertex(String[] tokens, ArrayList<float[]> positionsList, boolean flipZ)
	{
		try
		{
			float[] positions = new float[]
			{
				Float.parseFloat(tokens[1]),
				Float.parseFloat(tokens[2]),
				Float.parseFloat(tokens[3]),
			};
			
			if(flipZ) positions[2] = -positions[2];
			
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
	
	private static boolean parseNormal(String[] tokens, ArrayList<float[]> normalsList, boolean flipZ)
	{
		try
		{
			float[] normals = new float[]
			{
				Float.parseFloat(tokens[1]),
				Float.parseFloat(tokens[2]),
				Float.parseFloat(tokens[3]),
			};
			
			if(flipZ) normals[2] = -normals[2];
			
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

	private static final int NO_INDEX = -1;
	
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
	
	private static boolean parseObject(String[] tokens, ArrayList<OBJBundle> objectBundles, ArrayList<OBJBundle> materialBundles, ArrayList<Integer> indices)
	{
		if(!objectBundles.isEmpty())
		{
			OBJBundle lastBundle = objectBundles.get(objectBundles.size() - 1);
			lastBundle.count = indices.size() - lastBundle.index;
		}
		
		OBJBundle bundle = new OBJBundle();
		bundle.name = tokens[1];
		if(!materialBundles.isEmpty()) bundle.material = materialBundles.get(materialBundles.size() - 1).material;	//TODO: improve?
		bundle.index = indices.size();
		objectBundles.add(bundle);
		
		return true;
	}
	
	private static final String MTL_NEWMTL				= "newmtl";
	private static final String MTL_AMBIENT				= "ka";
	private static final String MTL_DIFFUSE				= "kd";
	private static final String MTL_SPECULAR			= "ks";
	private static final String MTL_EXPONENT			= "ns";
	private static final String MTL_ALPHA_D				= "d";
	private static final String MTL_ALPHA_TR			= "tr";
	private static final String MTL_ILLUMINATION		= "illum";
	private static final String MTL_AMBIENT_MAP			= "map_ka";
	private static final String MTL_DIFFUSE_MAP			= "map_kd";
	private static final String MTL_SPECULAR_MAP		= "map_ks";
	private static final String MTL_EXPONENT_MAP		= "map_ns";
	private static final String MTL_ALPHA_D_MAP			= "map_d";
	private static final String MTL_ALPHA_TR_MAP		= "map_tr";
	private static final String MTL_BUMP				= "bump";
	private static final String MTL_BUMP_MAP			= "map_bump";
	private static final String MTL_DISPLACEMENT		= "disp";
	private static final String MTL_DISPLACEMENT_MAP	= "disp_map";
	
	//TODO: Possibly segment this function into smaller functions at some point.
	private static boolean parseMaterialLib(String[] tokens, String folderpath, HashMap<String, OBJMaterial> materialMap)
	{
		String filedata = OBJFileUtils.readFileAsString(folderpath + "/" + tokens[1]);
		
		String[] lines = filedata.split("\r\n|\r|\n");
		
		OBJMaterial material = null;
		
		try
		{
			for(String line : lines)
			{
				String mtlTokens[] = tokenize(line, "\\s+");
				switch(mtlTokens[0])
				{
					case MTL_NEWMTL:
					{
						if(material != null)
							materialMap.put(material.name, material);
						material = new OBJMaterial();
						material.name = mtlTokens[1];
						break;
					}
					
					case MTL_AMBIENT:
					{
						float r = Float.parseFloat(mtlTokens[1]);
						float g = Float.parseFloat(mtlTokens[2]);
						float b = Float.parseFloat(mtlTokens[3]);
						material.ambient = new float[] {r, g, b};
						break;
					}
					
					case MTL_DIFFUSE:
					{
						float r = Float.parseFloat(mtlTokens[1]);
						float g = Float.parseFloat(mtlTokens[2]);
						float b = Float.parseFloat(mtlTokens[3]);
						material.diffuse = new float[] {r, g, b};
						break;
					}
					
					case MTL_SPECULAR:
					{
						float r = Float.parseFloat(mtlTokens[1]);
						float g = Float.parseFloat(mtlTokens[2]);
						float b = Float.parseFloat(mtlTokens[3]);
						material.specular = new float[] {r, g, b};
						break;
					}
					
					case MTL_EXPONENT:
					{
						material.exponent = Float.parseFloat(mtlTokens[1]);
						break;
					}
					
					case MTL_ALPHA_D:
					{
						material.alpha = Float.parseFloat(mtlTokens[1]);
						break;
					}
					
					case MTL_ALPHA_TR:
					{
						material.alpha = Float.parseFloat(mtlTokens[1]);
						material.reverseAlpha = true;
						break;
					}
					
					case MTL_ILLUMINATION:
					{
						material.illum = Integer.parseInt(mtlTokens[1]);
						break;
					}
					
					case MTL_AMBIENT_MAP:
					{
						material.ambientTexture = parseOBJTexture(mtlTokens);
						break;
					}
					
					case MTL_DIFFUSE_MAP:
					{
						material.diffuseTexture = parseOBJTexture(mtlTokens);
						break;
					}
					
					case MTL_SPECULAR_MAP:
					{
						material.specularColorTexture = parseOBJTexture(mtlTokens);
						break;
					}
					
					case MTL_EXPONENT_MAP:
					{
						material.specularHighlightTexture = parseOBJTexture(mtlTokens);
						break;
					}
					
					case MTL_ALPHA_D_MAP:
					{
						material.alphaTexture = parseOBJTexture(mtlTokens);
						break;
					}
					
					case MTL_ALPHA_TR_MAP:
					{
						material.alphaTexture = parseOBJTexture(mtlTokens);
						material.reverseAlpha = true;
						break;
					}
					
					case MTL_BUMP_MAP:
					{
						material.bumpTexture = parseOBJTexture(mtlTokens);
						break;
					}
					
					case MTL_BUMP:
					{
						material.bumpTexture = parseOBJTexture(mtlTokens);
						break;
					}
					
					case MTL_DISPLACEMENT_MAP:
					{
						material.displacementTexture = parseOBJTexture(mtlTokens);
						break;
					}
					
					case MTL_DISPLACEMENT:
					{
						material.displacementTexture = parseOBJTexture(mtlTokens);
						break;
					}
					
					default:
					{
						continue;
					}
				}
			}
			
			if(material != null)
				materialMap.put(material.name, material);
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
	
	public static final String TEX_BLEND_HORIZONTAL = "-blendu";
	public static final String TEX_BLEND_VERTICAL 	= "-blendv";
	public static final String TEX_OFFSET 			= "-o";
	public static final String TEX_SCALE 			= "-s";
	public static final String TEX_CLAMP			= "-clamp";
	
	private static OBJTexture parseOBJTexture(String[] tokens)
	{
		OBJTexture texture = new OBJTexture();
		
		int i = 1;
		boolean name = false;
		while(i != tokens.length && !name)
		{
			switch(tokens[i])
			{
				case TEX_BLEND_HORIZONTAL:
				{
					texture.horizonalBlend = !tokens[++i].toLowerCase().equals("off");
					break;
				}
				
				case TEX_BLEND_VERTICAL:
				{
					texture.verticalBlend = !tokens[++i].toLowerCase().equals("off");
					break;
				}
				
				case TEX_OFFSET:
				{
					float u = Float.parseFloat(tokens[++i]);
					float v = Float.parseFloat(tokens[++i]);
					texture.offset = new float[] {u, v};
					break;
				}
				
				case TEX_SCALE:
				{
					float u = Float.parseFloat(tokens[++i]);
					float v = Float.parseFloat(tokens[++i]);
					texture.scale = new float[] {u, v};
					break;
				}
				
				case TEX_CLAMP:
				{
					texture.clamp = tokens[++i].toLowerCase().equals("on");
					break;
				}
				
				default:
				{
					texture.filename = tokens[i];
					name = true;
					break;
				}
			}
		}
		
		return texture;
	}
	
	private static boolean parseUseMaterial(String[] tokens, ArrayList<OBJBundle> materialBundles, HashMap<String, OBJMaterial> materialMap, ArrayList<Integer> indices)
	{
		if(materialMap.containsKey(tokens[1]))
		{
			if(!materialBundles.isEmpty())
			{
				OBJBundle lastBundle = materialBundles.get(materialBundles.size() - 1);
				lastBundle.count = indices.size() - lastBundle.index;
			}
			
			OBJBundle bundle = new OBJBundle();
			bundle.name = tokens[1];
			bundle.material = materialMap.get(tokens[1]);
			bundle.index = indices.size();
			materialBundles.add(bundle);
		}
		
		return true;
	}
	
	private static void objError(String error)
	{
		System.out.println("[VELVET OBJ] Error parsing OBJ file:\n\tFILE: " 
				+ sFilepath + "\n\tLINE: " + sLinenumber + "\nREASON: " + error);
	}
	
	private static String[] tokenize(String line, String regex)
	{
		return line.trim().toLowerCase().split(regex);
	}
	
}
