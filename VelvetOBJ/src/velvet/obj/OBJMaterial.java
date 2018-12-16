package velvet.obj;

public class OBJMaterial
{
	public String		name;
	public float[] 		ambient;
	public float[] 		diffuse;
	public float[] 		specular;
	public float		exponent;
	public float		alpha;
	public boolean		reverseAlpha;
	public int			illum;
	public OBJTexture	ambientTexture;
	public OBJTexture	diffuseTexture;
	public OBJTexture	specularColorTexture;
	public OBJTexture	specularHighlightTexture;
	public OBJTexture	alphaTexture;
	public OBJTexture	bumpTexture;
	public OBJTexture	displacementTexture;
}
