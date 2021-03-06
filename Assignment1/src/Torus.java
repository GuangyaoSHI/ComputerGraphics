/*
 * Assignment 1, Computer Graphics
 * Severin Tobler, 28.09.2015
 * 
 */

import jrtr.*;

public class Torus extends Shape{
	
	public Torus(RenderContext renderContext, int mesh, float R, float r) {
		super(calculateVertexData(renderContext, mesh, R, r));
	}
	
	
	private static VertexData calculateVertexData(RenderContext renderContext, int m, float R, float r){
		
		double dt = 2*Math.PI/m;
		double dp = 2*Math.PI/m;

		float[] v = new float[m*m*3];
		float[] n = new float[v.length];
		float[] c = new float[v.length];

		for(int p=0; p<m; p++){
			for(int t=0; t<m; t++){
				// normals
				n[p*3*m+t*3] = (float) (Math.cos(dp*p)*Math.cos(dt*t));
				n[p*3*m+t*3+1] = (float) (Math.cos(dp*p)*Math.sin(dt*t));
				n[p*3*m+t*3+2] = (float) Math.sin(dp*p);
				// vertexes
				v[p*3*m+t*3] = (float) (R*Math.cos(dt*t)+r*n[p*3*m+t*3]);
				v[p*3*m+t*3+1] = (float) (R*Math.sin(dt*t)+r*n[p*3*m+t*3+1]);
				v[p*3*m+t*3+2] = r*n[p*3*m+t*3+2];
				// colors
				if((p*t)%2 == 0)
					c[p*3*m+t*3+1] = 1;
			}
		}
	
		
		// indices
		int indices[] = new int[6*m*m];
		
		// within the plane
		for(int p=0; p<m-1; p++){
			for(int t=0; t<m-1; t++){
				indices[p*6*(m-1)+t*6] = p*m+t;
				indices[p*6*(m-1)+t*6+1] = p*m+(t+1);
				indices[p*6*(m-1)+t*6+2] = (p+1)*m+t;

				indices[p*6*(m-1)+t*6+3] = (p+1)*m+t;
				indices[p*6*(m-1)+t*6+4] = p*m+(t+1);
				indices[p*6*(m-1)+t*6+5] = (p+1)*m+(t+1);
			}
		}
				
		// from last to first
		int idx = 6*(m-1)*(m-1);
		for(int p=0; p<m-1; p++){
			indices[idx+p*6] = p*m+m-1;
			indices[idx+p*6+1] = p*m;
			indices[idx+p*6+2] = (p+1)*m+m-1;
			
			indices[idx+p*6+3] = p*m;
			indices[idx+p*6+4] = (p+1)*m;
			indices[idx+p*6+5] = (p+1)*m+m-1;
		}

		idx = idx+(m-1)*6;
		for(int t=0; t<m-1; t++){
			indices[idx+t*6] = (m-1)*m+t;
			indices[idx+t*6+1] = (m-1)*m+t+1;
			indices[idx+t*6+2] = t;
			
			indices[idx+t*6+3] = (m-1)*m+t+1;
			indices[idx+t*6+4] = t+1;
			indices[idx+t*6+5] = t;
		}
		
		// corner
		indices[6*m*m-6] = m-1;
		indices[6*m*m-5] = 0;
		indices[6*m*m-4] = (m-1)*(m+1);

		indices[6*m*m-3] = 0;
		indices[6*m*m-2] = (m-1)*m;
		indices[6*m*m-1] = (m-1)*(m+1);
		
		
		VertexData vertexData = renderContext.makeVertexData(v.length/3);
		vertexData.addElement(c, VertexData.Semantic.COLOR, 3);
		vertexData.addElement(v, VertexData.Semantic.POSITION, 3);
		vertexData.addElement(n, VertexData.Semantic.NORMAL, 3);
		//vertexData.addElement(uv, VertexData.Semantic.TEXCOORD, 2);

		
		vertexData.addIndices(indices);
		
		return vertexData;
	}
	
	
}
