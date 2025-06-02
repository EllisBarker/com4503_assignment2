import gmaths.*;
import java.nio.*;
import com.jogamp.common.nio.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.awt.*;
import com.jogamp.opengl.util.glsl.*;
import com.jogamp.opengl.util.texture.*;

/**
 * Class for setting up the objects needed to draw the room containing all elements of the 
 * spacecraft.
 * 
 * I declare that this code is my own work.
 * Author: Ellis Barker
 * Email address: ebarker5@sheffield.ac.uk
 * 
 * Changes made:
 * - A new class for creating the room and its individual components
 */
public class Room {
	private ModelMultipleLights[] wall;
	private Camera camera;
	private Light[] lights;
	private Texture diffuse, specular,
					albedo, normal, metallic, roughness, ao;
	private float size = 6f;
	private int noObjects = 1;

	/**
	 * Constructor. Initialise the models comprising the room.
	 * 
	 * @param gl The OpenGL context used for rendering.
	 * @param c The camera object in the world.
	 * @param l The light sources in the world (as an array).
	 * @param t The array of all textures to be used for the room.
	 */
	public Room(GL3 gl, Camera c, Light[] l, Texture[] t) {
		camera = c;
		lights = l;
		this.diffuse = t[0];
		this.specular = t[1];
		this.albedo = t[2];
		this.normal = t[3];
		this.metallic = t[4];
		this.roughness = t[5];
		this.ao = t[6];
		wall = new ModelMultipleLights[noObjects];
		Shader shaderPhong = new Shader(
			gl, "assets/shaders/vs_standard.txt", "assets/shaders/fs_standard_m_phong.txt");
		Shader shaderPBR = new Shader(
			gl, "assets/shaders/vs_standard.txt", "assets/shaders/fs_standard_m_pbr.txt");
		Material material = new Material(
			new Vec3(0.5f, 0.5f, 0.5f), 
			new Vec3(0.5f, 0.5f, 0.5f), 
			new Vec3(0.3f, 0.3f, 0.3f), 
			4.0f);

		// Floor
		Mat4 modelMatrix = prepareModelMatrix(size, 1f, size, 0, 0, 0, 0, 0, 0);
		wall[0] = ModelMaker.makePart(gl,
						   			  "floor", 
						   			  material,
						   			  modelMatrix,
						   			  //shaderPhong,
						   			  //new Texture[] {diffuse, specular},
									  shaderPBR,
									  new Texture[] {albedo, normal, metallic, roughness, ao},
						   			  lights,
						   			  camera,
						   			  "two triangles");
	}

	/**
	 * Specialised model matrix preparation function to scale, rotate and translate specific
	 * parts of the room.
	 * 
	 * @param scaleX The scale factor for the model (x-direction)
	 * @param scaleY The scale factor for the model (y-direction)
	 * @param scaleZ The scale factor for the model (z-direction)
	 * @param rotateX The angle at which to rotate the model (around the x-axis)
	 * @param rotateY The angle at which to rotate the model (around the y-axis)
	 * @param rotateZ The angle at which to rotate the model (around the z-axis)
	 * @param translateX The distance with which to translate the model (x-direction)
	 * @param translateY The distance with which to translate the model (y-direction)
	 * @param translateZ The distance with which to translate the model (z-direction)
	 * @return The transformation matrix of one model.
	 */
	private Mat4 prepareModelMatrix(float scaleX, float scaleY, float scaleZ,
									int rotateX, int rotateY, int rotateZ,
									float translateX, float translateY, float translateZ) {
		Mat4 modelMatrix = new Mat4(1);
		modelMatrix = Mat4.multiply(Mat4Transform.scale(scaleX, scaleY, scaleZ), modelMatrix);
		modelMatrix = Mat4.multiply(Mat4Transform.rotateAroundX(rotateX), modelMatrix);
		modelMatrix = Mat4.multiply(Mat4Transform.rotateAroundY(rotateY), modelMatrix);
		modelMatrix = Mat4.multiply(Mat4Transform.rotateAroundZ(rotateZ), modelMatrix);
		modelMatrix = Mat4.multiply(
			Mat4Transform.translate(translateX, translateY, translateZ), 
			modelMatrix);
		return modelMatrix;
	}

	/**
	 * Draw all of the parts of the room.
	 * 
	 * @param gl The OpenGL context used for rendering.
	 */
	public void render(GL3 gl) {
		for (int i=0; i<noObjects; i++) {
			wall[i].render(gl);
		}
	}

	/**
	 * Dispose of each individual part of the room and the resources they use.
	 * 
	 * @param gl The OpenGL context used for rendering.
	 */
	public void dispose(GL3 gl) {
		for (int i=0; i<noObjects; i++) {
			wall[i].dispose(gl);
		}
	}
}