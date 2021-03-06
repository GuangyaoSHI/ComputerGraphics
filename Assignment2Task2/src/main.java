
import jrtr.*;
import jrtr.glrenderer.*;
import jrtr.gldeferredrenderer.*;

import javax.swing.*;

import java.awt.Point;
import java.awt.event.*;
import java.io.IOException;

import javax.vecmath.*;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Implements a simple application that opens a 3D rendering window and 
 * shows a rotating cube.
 */
public class main
{	
	static RenderPanel renderPanel;
	static RenderContext renderContext;
	static Shader normalShader;
	static Shader diffuseShader;
	static Material material;
	static SimpleSceneManager sceneManager;
	static Shape shape;
	static float currentstep, basicstep;
	static boolean mousePressed;
	static ObjReader objReader;

	/**
	 * An extension of {@link GLRenderPanel} or {@link SWRenderPanel} to 
	 * provide a call-back function for initialization. Here we construct
	 * a simple 3D scene and start a timer task to generate an animation.
	 */ 
	public final static class SimpleRenderPanel extends GLRenderPanel
	{
		/**
		 * Initialization call-back. We initialize our renderer here.
		 * 
		 * @param r	the render context that is associated with this render panel
		 */
		public void init(RenderContext r)
		{
			renderContext = r;

			VertexData vertexData;
			objReader = new ObjReader();
			try {
				vertexData = objReader.read("/Users/Severin/_Uni Bern/3. Semester/Computer Graphics/Eclipse Workspace/obj/Teapot.obj", 2f, renderContext);
			} catch (IOException e1) {
				vertexData = createCube();
				e1.printStackTrace();
			}


			// Make a scene manager and add the object
			sceneManager = new SimpleSceneManager();
			shape = new Shape(vertexData);
			sceneManager.addShape(shape);

			// Add the scene to the renderer
			renderContext.setSceneManager(sceneManager);

			// Load some more shaders
			normalShader = renderContext.makeShader();
			try {
				normalShader.load("../jrtr/shaders/normal.vert", "../jrtr/shaders/normal.frag");
			} catch(Exception e) {
				System.out.print("Problem with shader:\n");
				System.out.print(e.getMessage());
			}

			diffuseShader = renderContext.makeShader();
			try {
				diffuseShader.load("../jrtr/shaders/diffuse.vert", "../jrtr/shaders/diffuse.frag");
			} catch(Exception e) {
				System.out.print("Problem with shader:\n");
				System.out.print(e.getMessage());
			}

			// Make a material that can be used for shading
			material = new Material();
			material.shader = diffuseShader;
			material.diffuseMap = renderContext.makeTexture();
			try {
				material.diffuseMap.load("../textures/plant.jpg");
			} catch(Exception e) {				
				System.out.print("Could not load texture.\n");
				System.out.print(e.getMessage());
			}

			// Register a timer task
			Timer timer = new Timer();
			basicstep = 0.01f;
			currentstep = basicstep;
			timer.scheduleAtFixedRate(new AnimationTask(), 0, 10);
		}

		private VertexData createCube(){
			// Make a simple geometric object: a cube

			// The vertex positions of the cube
			float v[] = {-1,-1,1, 1,-1,1, 1,1,1, -1,1,1,		// front face
					-1,-1,-1, -1,-1,1, -1,1,1, -1,1,-1,	// left face
					1,-1,-1,-1,-1,-1, -1,1,-1, 1,1,-1,		// back face
					1,-1,1, 1,-1,-1, 1,1,-1, 1,1,1,		// right face
					1,1,1, 1,1,-1, -1,1,-1, -1,1,1,		// top face
					-1,-1,1, -1,-1,-1, 1,-1,-1, 1,-1,1};	// bottom face

			// The vertex normals 
			float n[] = {0,0,1, 0,0,1, 0,0,1, 0,0,1,			// front face
					-1,0,0, -1,0,0, -1,0,0, -1,0,0,		// left face
					0,0,-1, 0,0,-1, 0,0,-1, 0,0,-1,		// back face
					1,0,0, 1,0,0, 1,0,0, 1,0,0,			// right face
					0,1,0, 0,1,0, 0,1,0, 0,1,0,			// top face
					0,-1,0, 0,-1,0, 0,-1,0, 0,-1,0};		// bottom face

			// The vertex colors
			float c[] = {1,0,0, 1,0,0, 1,0,0, 1,0,0,
					0,1,0, 0,1,0, 0,1,0, 0,1,0,
					1,0,0, 1,0,0, 1,0,0, 1,0,0,
					0,1,0, 0,1,0, 0,1,0, 0,1,0,
					0,0,1, 0,0,1, 0,0,1, 0,0,1,
					0,0,1, 0,0,1, 0,0,1, 0,0,1};

			// Texture coordinates 
			float uv[] = {0,0, 1,0, 1,1, 0,1,
					0,0, 1,0, 1,1, 0,1,
					0,0, 1,0, 1,1, 0,1,
					0,0, 1,0, 1,1, 0,1,
					0,0, 1,0, 1,1, 0,1,
					0,0, 1,0, 1,1, 0,1};

			// Construct a data structure that stores the vertices, their
			// attributes, and the triangle mesh connectivity
			VertexData vertexData = renderContext.makeVertexData(24);
			vertexData.addElement(c, VertexData.Semantic.COLOR, 3);
			vertexData.addElement(v, VertexData.Semantic.POSITION, 3);
			vertexData.addElement(n, VertexData.Semantic.NORMAL, 3);
			vertexData.addElement(uv, VertexData.Semantic.TEXCOORD, 2);

			// The triangles (three vertex indices for each triangle)
			int indices[] = {0,2,3, 0,1,2,			// front face
					4,6,7, 4,5,6,			// left face
					8,10,11, 8,9,10,		// back face
					12,14,15, 12,13,14,	// right face
					16,18,19, 16,17,18,	// top face
					20,22,23, 20,21,22};	// bottom face

			vertexData.addIndices(indices);
			return vertexData;
		}
	}


	/**
	 * A timer task that generates an animation. This task triggers
	 * the redrawing of the 3D scene every time it is executed.
	 */
	public static class AnimationTask extends TimerTask
	{

		public void run()
		{
			Vector3f v0 = null;
			Vector3f v1 = null;
			
			// read mouse positions and transform them
			if(mousePressed)
				v0 = project(renderPanel.getCanvas().getMousePosition());
			try {
				Thread.sleep(10);
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
				v0 = null;
			}
			if(mousePressed)
				v1 = project(renderPanel.getCanvas().getMousePosition());
			
			// calculate and apply rotation
			if(v0 != null && v1 != null && !v0.equals(v1)){	// maybe calculate dot product between v0 and v1, if <0 don't calculate
				Vector3f axis = new Vector3f();
				axis.cross(v0, v1);
				float theta = v0.angle(v1);
				Quat4f delta = new Quat4f();
				delta.set(new AxisAngle4f(axis.x, axis.y, axis.z, 1.5f*theta));

				Matrix4f t = new Matrix4f(shape.getTransformation());
				Vector3f rt = new Vector3f();
				t.get(rt);
				Matrix3f r = new Matrix3f();
				t.get(r);
				Quat4f q = new Quat4f();
				q.set(r);

				delta.mul(q);
				t.set(delta,rt,1);
				shape.setTransformation(t);
			}
			
			// Trigger redrawing of the render window
			renderPanel.getCanvas().repaint(); 
		}


		private Vector3f project(Point p){
			if(p == null)
				return null;

			int height = renderPanel.getCanvas().getHeight();
			int width = renderPanel.getCanvas().getWidth();
			double r = (double) Math.min(height, width);

			double x = p.getX()- width/2;
			double y = height/2 - p.getY();

			x = x*2/r;
			y = y*2/r;

			if(Math.abs(x) > 1 || Math.abs(y)>1)
				return null;

			double det = 1 - x*x - y*y;

			if(det <= 0)
				return null;

			double z = Math.sqrt(det);

			Vector3f v = new Vector3f((float) x, (float) y, (float) z);
			v.normalize();
			return v;
		}
	}


	/**
	 * A mouse listener for the main window of this application. This can be
	 * used to process mouse events.
	 */
	public static class SimpleMouseListener implements MouseListener
	{
		public void mousePressed(MouseEvent e) {
			mousePressed = true;
		}
		public void mouseReleased(MouseEvent e) {
			mousePressed = false;
		}
		public void mouseEntered(MouseEvent e) {
		}
		public void mouseExited(MouseEvent e) {
		}
		public void mouseClicked(MouseEvent e) {}
	}

	/**
	 * A key listener for the main window. Use this to process key events.
	 * Currently this provides the following controls:
	 * 's': stop animation
	 * 'p': play animation
	 * '+': accelerate rotation
	 * '-': slow down rotation
	 * 'd': default shader
	 * 'n': shader using surface normals
	 * 'm': use a material for shading
	 */
	public static class SimpleKeyListener implements KeyListener
	{
		public void keyPressed(KeyEvent e)
		{
			switch(e.getKeyChar())
			{
			case 's': {
				// Stop animation
				currentstep = 0;
				break;
			}
			case 'p': {
				// Resume animation
				currentstep = basicstep;
				break;
			}
			case '+': {
				// Accelerate roation
				currentstep += basicstep;
				break;
			}
			case '-': {
				// Slow down rotation
				currentstep -= basicstep;
				break;
			}
			case 'n': {
				// Remove material from shape, and set "normal" shader
				shape.setMaterial(null);
				renderContext.useShader(normalShader);
				break;
			}
			case 'd': {
				// Remove material from shape, and set "default" shader
				shape.setMaterial(null);
				renderContext.useDefaultShader();
				break;
			}
			case 'm': {
				// Set a material for more complex shading of the shape
				if(shape.getMaterial() == null) {
					shape.setMaterial(material);
				} else
				{
					shape.setMaterial(null);
					renderContext.useDefaultShader();
				}
				break;
			}
			}

			// Trigger redrawing
			renderPanel.getCanvas().repaint();
		}

		public void keyReleased(KeyEvent e)
		{
		}

		public void keyTyped(KeyEvent e)
		{
		}

	}

	/**
	 * The main function opens a 3D rendering window, implemented by the class
	 * {@link SimpleRenderPanel}. {@link SimpleRenderPanel} is then called backed 
	 * for initialization automatically. It then constructs a simple 3D scene, 
	 * and starts a timer task to generate an animation.
	 */
	public static void main(String[] args)
	{		
		// Make a render panel. The init function of the renderPanel
		// (see above) will be called back for initialization.
		renderPanel = new SimpleRenderPanel();

		// Make the main window of this application and add the renderer to it
		JFrame jframe = new JFrame("simple");
		jframe.setSize(500, 500);
		jframe.setLocationRelativeTo(null); // center of screen
		jframe.getContentPane().add(renderPanel.getCanvas());// put the canvas into a JFrame window

		// Add a mouse and key listener
		renderPanel.getCanvas().addMouseListener(new SimpleMouseListener());
		renderPanel.getCanvas().addKeyListener(new SimpleKeyListener());
		renderPanel.getCanvas().setFocusable(true);   	    	    

		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jframe.setVisible(true); // show window
	}
}
