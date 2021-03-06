package jrtr.swrenderer;

import jrtr.RenderContext;
import jrtr.RenderItem;
import jrtr.SceneManagerInterface;
import jrtr.SceneManagerIterator;
import jrtr.Shader;
import jrtr.Texture;
import jrtr.VertexData;
import jrtr.glrenderer.GLRenderPanel;

import java.awt.Color;
import java.awt.image.*;
import java.util.ListIterator;

import javax.vecmath.*;


/**
 * A skeleton for a software renderer. It works in combination with
 * {@link SWRenderPanel}, which displays the output image. In project 3 
 * you will implement your own rasterizer in this class.
 * <p>
 * To use the software renderer, you will simply replace {@link GLRenderPanel} 
 * with {@link SWRenderPanel} in the user application.
 */
public class SWRenderContext implements RenderContext {

	private SceneManagerInterface sceneManager;
	private BufferedImage colorBuffer;
	private float[][] zBuffer;
	private Matrix4f C; // World space to Camera space
	private Matrix4f D;	// Viewprot transformation
	private Matrix4f P;
	private boolean nearestNeigbor = false;

	public void setSceneManager(SceneManagerInterface sceneManager)
	{
		this.sceneManager = sceneManager;
	}

	/**
	 * This is called by the SWRenderPanel to render the scene to the 
	 * software frame buffer.
	 */
	public void display()
	{
		if(sceneManager == null) return;

		beginFrame();

		SceneManagerIterator iterator = sceneManager.iterator();	
		while(iterator.hasNext())
		{
			draw(iterator.next());
		}		

		endFrame();
	}

	/**
	 * This is called by the {@link SWJPanel} to obtain the color buffer that
	 * will be displayed.
	 */
	public BufferedImage getColorBuffer()
	{
		return colorBuffer;
	}

	/**
	 * Set a new viewport size. The render context will also need to store
	 * a viewport matrix, which you need to reset here. 
	 */
	public void setViewportSize(int width, int height)
	{	
		colorBuffer = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		D = new Matrix4f(width/2, 	0, 			0, 		width/2,
				0, 			-height/2, 	0, 		height/2,
				0, 			0, 			0.5f, 	0.5f,
				0, 			0, 			0, 		1
				);
	}

	/**
	 * Clear the framebuffer here.
	 */
	private void beginFrame()
	{
		C = new Matrix4f(sceneManager.getCamera().getCameraMatrix());
		//C.invert();
		P = sceneManager.getFrustum().getProjectionMatrix();
		zBuffer = new float[colorBuffer.getWidth()][colorBuffer.getHeight()];
		colorBuffer = new BufferedImage(colorBuffer.getWidth(), colorBuffer.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
	}

	private void endFrame()
	{		
	}

	/**
	 * The main rendering method. You will need to implement this to draw
	 * 3D objects.
	 */
	private void draw(RenderItem renderItem)
	{
		task1(renderItem);
		//task2(renderItem);

	}


	private void task2(RenderItem renderItem){
		// Variable declarations
		Matrix4f M = renderItem.getShape().getTransformation();	// Object space to World space
		Matrix4f t = new Matrix4f(D);
		t.mul(P);
		t.mul(C);
		t.mul(M);
		VertexData vertexData = renderItem.getShape().getVertexData();
		int indices[] = vertexData.getIndices();
		float[][] colors = new float[3][3];
		float[][] positions = new float[3][4];
		float[][] normals = new float[3][3];
		float[][] texcoords = new float[3][2];


		int k = 0; // index of triangle vertex, k is 0,1, or 2
		// Loop over all vertex indices
		for(int j=0; j<indices.length; j++)
		{
			int i = indices[j];
			// Loop over all attributes of current vertex
			ListIterator<VertexData.VertexElement> itr = vertexData.getElements().listIterator(0);
			while(itr.hasNext())
			{
				VertexData.VertexElement e = itr.next();
				switch (e.getSemantic()) {
				case POSITION:
					Vector4f p = new Vector4f(e.getData()[i*3],e.getData()[i*3+1],e.getData()[i*3+2],1);
					t.transform(p);
					positions[k][0] = p.x;
					positions[k][1] = p.y;
					positions[k][2] = p.z;
					positions[k][3] = p.w;
					k++;
					break;
				case COLOR:
					colors[k][0] = e.getData()[i*3];
					colors[k][1] = e.getData()[i*3+1];
					colors[k][2] = e.getData()[i*3+2];
					break;
				case NORMAL:
					normals[k][0] = e.getData()[i*3];
					normals[k][1] = e.getData()[i*3+1];
					normals[k][2] = e.getData()[i*3+2];
					break;
				case TEXCOORD:
					texcoords[k][0] = e.getData()[i*2];
					texcoords[k][1] = e.getData()[i*2+1];
					break;
				}

				// Draw triangle as soon as we collected the data for 3 vertices
				if(k == 3)
				{
					// Draw the triangle with the collected three vertex positions, etc.
					if(!(positions[0][3]<0 && positions[1][3]<0 && positions[2][3]<0))
						rasterizeTriangle(positions, colors, normals, texcoords, renderItem);
					k = 0;
				}
			}
		}
	}

	private void rasterizeTriangle(float[][] positions,float[][] colors,float[][] normals,float[][] texcoords,RenderItem renderItem){	
		Matrix3f edgF = new Matrix3f(	positions[0][0], positions[0][1], positions[0][3],
				positions[1][0], positions[1][1], positions[1][3],
				positions[2][0], positions[2][1], positions[2][3]
				);

		try{
			edgF.invert();
		} catch(Exception e){
			return;
		}
		Vector3f ax = new Vector3f(texcoords[0][0],texcoords[1][0],texcoords[2][0]);
		edgF.transform(ax);
		Vector3f ay = new Vector3f(texcoords[0][1],texcoords[1][1],texcoords[2][1]);
		edgF.transform(ay);
		Vector3f a1 = new Vector3f(1,1,1);
		edgF.transform(a1);
		edgF.transpose();

		int[] boundry;
		if(positions[0][3]<0 && positions[1][3]<0 && positions[2][3]<0)
			boundry = rect(positions);
		else
			boundry = new int[] {0, colorBuffer.getWidth(), 0, colorBuffer.getHeight()};

		for(int x = boundry[0]; x<boundry[1]; x++){
			for(int y = boundry[2]; y<boundry[3]; y++){
				Vector3f p = new Vector3f(x,y,1);
				float w = a1.dot(p);	// actually 1/w
				edgF.transform(p);
				if(p.x<0 || p.y<0 || p.z<0)
					continue;
				if(w>zBuffer[x][y]) {
					zBuffer[x][y] = w;
					if(renderItem.getShape().getMaterial() == null)
						useColors(x,y,w,p,colors);
					else
						useTexture(x,y,w,ax,ay, (SWTexture) renderItem.getShape().getMaterial().diffuseMap);
				}
			}
		}
	}

	private void useTexture(int x, int y, float w, Vector3f ax, Vector3f ay, SWTexture texture){
		float u = (ax.x*x+ax.y*y+ax.z)/w;
		float v = (ay.x*x+ay.y*y+ay.z)/w;
		u = (float) (Math.round(u*1000)/1000.);
		v = (float) (Math.round(v*1000)/1000.);
		if(nearestNeigbor)
			colorBuffer.setRGB(x, y, texture.getColorNN(u, v));
		else
			colorBuffer.setRGB(x, y, texture.getColorBL(u, v));	
	}


	private void useColors(int x, int y, float w, Vector3f p, float[][] colors){
		float r = (colors[0][0]*p.x+colors[1][0]*p.y+colors[2][0]*p.z)/w;
		if(r>1)
			r = 1;
		float g = (colors[0][1]*p.x+colors[1][1]*p.y+colors[2][1]*p.z)/w;
		if(g>1)
			g = 1;
		float b = (colors[0][2]*p.x+colors[1][2]*p.y+colors[2][2]*p.z)/w;
		if(b>1)
			b = 1;
		Color c = new Color(r,g,b);
		colorBuffer.setRGB(x, y, c.getRGB());
	}



	private int[] rect(float[][] positions){
		float[][] pos = {{positions[0][0]/positions[0][3], positions[1][0]/positions[1][3], positions[2][0]/positions[2][3]},
				{positions[0][1]/positions[0][3], positions[1][1]/positions[1][3], positions[2][1]/positions[2][3]},
		};
		int xmin = Math.round(Math.min(Math.min(pos[0][0], pos[0][1]), pos[0][2]));
		int xmax = Math.round(Math.max(Math.max(pos[0][0], pos[0][1]), pos[0][2]));
		int ymin = Math.round(Math.min(Math.min(pos[1][0], pos[1][1]), pos[1][2]));
		int ymax = Math.round(Math.max(Math.max(pos[1][0], pos[1][1]), pos[1][2]));

		if(xmin < 0)
			xmin = 0;
		if(ymin < 0)
			ymin = 0;
		if(xmin > colorBuffer.getWidth())
			xmin = colorBuffer.getWidth();
		if(ymin > colorBuffer.getHeight())
			ymin = colorBuffer.getHeight();

		return new int[] {xmin, xmax, ymin, ymax};
	}


	private void task1(RenderItem renderItem){
		Matrix4f M = renderItem.getShape().getTransformation();	// Object space to World space
		Matrix4f t = new Matrix4f(D);
		t.mul(P);
		t.mul(C);
		t.mul(M);
		VertexData vertexData = renderItem.getShape().getVertexData();

		ListIterator<VertexData.VertexElement> itr = vertexData.getElements().listIterator(0);
		while(itr.hasNext()){
			VertexData.VertexElement e = itr.next();
			if(e.getSemantic() != VertexData.Semantic.POSITION)
				continue;

			int n = e.getNumberOfComponents();
			float raw[] = e.getData();
			for(int i=0; i<raw.length/n; i++){
				Vector4f p = new Vector4f(raw[i*n],raw[i*n+1],raw[i*n+2],1);

				t.transform(p);

				int x = (int) (p.x/p.w);
				int y = (int) (p.y/p.w);
				if(x<0 || x>=colorBuffer.getWidth() || y<0 || y>=colorBuffer.getHeight())
					continue;
				int c = 0xFFFFFF;
				colorBuffer.setRGB(x, y, c);
			}

		}
	}


	/**
	 * Does nothing. We will not implement shaders for the software renderer.
	 */
	public Shader makeShader()	
	{
		return new SWShader();
	}

	/**
	 * Does nothing. We will not implement shaders for the software renderer.
	 */
	public void useShader(Shader s)
	{
	}

	/**
	 * Does nothing. We will not implement shaders for the software renderer.
	 */
	public void useDefaultShader()
	{
	}

	/**
	 * Does nothing. We will not implement textures for the software renderer.
	 */
	public Texture makeTexture()
	{
		return new SWTexture();
	}

	public VertexData makeVertexData(int n)
	{
		return new SWVertexData(n);		
	}
}
