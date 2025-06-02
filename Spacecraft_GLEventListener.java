import gmaths.*;
import java.nio.*;
import com.jogamp.common.nio.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.awt.*;
import com.jogamp.opengl.util.glsl.*;
import com.jogamp.opengl.util.texture.*;
import com.jogamp.opengl.util.texture.awt.*;
import com.jogamp.opengl.util.texture.spi.JPEGImage;

/**
 * Class for handling rendering the elements of the spacecraft, user interactions and general 
 * setup for the scene.
 * 
 * I declare that this code is my own work.
 * Author: Ellis Barker
 * Email address: ebarker5@sheffield.ac.uk
 * 
 * Changes made:
 * - changeGlobalLightIntensity(): process UI interaction for the global light's intensity
 * - changeSpotlightIntensity(): process UI interaction for the spotlight's intensity
 * - changeDistanceThreshold(): process UI interaction for robot 1's dancing proximity distance
 * - startStopRobot1Movement(): process UI interaction for manually setting robot 1's dancing state
 * - startStopRobot2Movement(): process UI interaction for manually setting robot 2's movement
 */
public class Spacecraft_GLEventListener implements GLEventListener {
	private static final boolean DISPLAY_SHADERS = false;
	private Camera camera;
	
	/**
	 * Constructor. Set the camera for the scene and its position/target.
	 * 
	 * @param camera The camera used in rendering.
	 */
	public Spacecraft_GLEventListener(Camera camera) {
		this.camera = camera;
		this.camera.setPosition(new Vec3(0f,7.5f,0.01f));
		this.camera.setTarget(new Vec3(0f,-4f,0f));
	}
	
	// ***************************************************
	/* METHODS DEFINED BY GLEventListener */

	/**
	 * Initialises the OpenGL context/rendering options and the scene in general.
	 * 
	 * @param drawable The OpenGL drawable object.
	 */
	public void init(GLAutoDrawable drawable) {   
		GL3 gl = drawable.getGL().getGL3();
		System.err.println("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities());
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f); 
		gl.glClearDepth(1.0f);
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LESS);
		gl.glFrontFace(GL.GL_CCW);
		initialise(gl);
		startTime = getSeconds();
	}
	
	/**
	 * Update the camera if the window (drawing area) is resized.
	 * 
	 * @param drawable The OpenGL drawable object.
	 * @param x The x-coordinate of the viewport's origin.
	 * @param y The y-coordinate of the viewport's origin.
	 * @param width The width of the viewport to be set.
	 * @param height The height of the viewport to be set.
	 */
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		GL3 gl = drawable.getGL().getGL3();
		gl.glViewport(x, y, width, height);
		float aspect = (float)width/(float)height;
		camera.setPerspectiveMatrix(Mat4Transform.perspective(45, aspect));
	}

	/**
	 * Render the whole scene (draw every object).
	 * 
	 * @param drawable The OpenGL drawable object.
	 */
	public void display(GLAutoDrawable drawable) {
		GL3 gl = drawable.getGL().getGL3();
		render(gl);
	}

	/**
	 * Clean up resources by disposing of each object (e.g. when closing the window).
	 * 
	 * @param drawable The OpenGL drawable object.
	 */
	public void dispose(GLAutoDrawable drawable) {
		GL3 gl = drawable.getGL().getGL3();
		room.dispose(gl);
		lights[0].dispose(gl);
		lights[1].dispose(gl);
		textures.destroy(gl);
	}

	// ***************************************************
	/* ACTIONS PERFORMED ACHIEVED BY USER INTERACTION */

	/**
	 * Edit the light intensity of the general light by altering its material.
	 * 
	 * @param intensity The level of light intensity desired by user interaction.
	 */
	public void changeGlobalLightIntensity(float intensity) {
		Material material = new Material();
		Vec3 ambient = defaultGlobalLightIntensity.getAmbient();
		Vec3 diffuse = defaultGlobalLightIntensity.getDiffuse();
		Vec3 specular = defaultGlobalLightIntensity.getSpecular();
		material.setAmbient(ambient.x*intensity, ambient.y*intensity, ambient.z*intensity);
		material.setDiffuse(diffuse.x*intensity, diffuse.y*intensity, diffuse.z*intensity);
		material.setSpecular(specular.x*intensity, specular.y*intensity, specular.z*intensity);
		lights[0].setMaterial(material);
	}

	public void changeGlobalLightPosition(String position) {
		switch (position) {
			case "top":
				lights[0].setPosition(new Vec3(0,1,-3));
				break;
			case "bottom":
				lights[0].setPosition(new Vec3(0,1,3));
				break;
			case "left":
				lights[0].setPosition(new Vec3(-3,1,0));
				break;
			case "right":
				lights[0].setPosition(new Vec3(3,1,0));
				break;
			case "default":
				lights[0].setPosition(new Vec3(0,1,0));
				break;
			default:
				lights[0].setPosition(new Vec3(0,1,0));
		}
	}
	
	/**
	 * Edit the light intensity of the spotlight by altering its material.
	 * 
	 * @param intensity The level of light intensity desired by user interaction.
	 */
	public void changeSpotlightIntensity(float intensity) {
		Material material = new Material();
		Vec3 ambient = defaultSpotLightIntensity.getAmbient();
		Vec3 diffuse = defaultSpotLightIntensity.getDiffuse();
		Vec3 specular = defaultSpotLightIntensity.getSpecular();
		material.setAmbient(ambient.x*intensity, ambient.y*intensity, ambient.z*intensity);
		material.setDiffuse(diffuse.x*intensity, diffuse.y*intensity, diffuse.z*intensity);
		material.setSpecular(specular.x*intensity, specular.y*intensity, specular.z*intensity);
		lights[1].setMaterial(material);
	}

	// ***************************************************
	/* THE SCENE */

	private TextureLibrary textures;

	// The environment
	private Room room;

	// Light-related variables
	private Light[] lights = new Light[2];
	private Material defaultGlobalLightIntensity;
	private Material defaultSpotLightIntensity;

	/**
	 * Load textures given their file paths and associate them with certain identifying names.
	 * 
	 * @param gl The OpenGL context used for rendering.
	 */
	private void loadTextures(GL3 gl) {
		textures = new TextureLibrary();
		/* textures.add(gl, "example_diffuse", "assets/textures/stand_diffuse.jpg");
		textures.add(gl, "example_specular", "assets/textures/stand_specular.jpg");
		textures.add(gl, "example_albedo", "assets/textures/rustediron2_basecolor.png");
		textures.add(gl, "example_normal", "assets/textures/rustediron2_normal.png");
		textures.add(gl, "example_metallic", "assets/textures/rustediron2_metallic.png");
		textures.add(gl, "example_roughness", "assets/textures/rustediron2_roughness.png");
		textures.add(gl, "example_ao", "assets/textures/rustediron2_ao.png"); */

		textures.add(gl, "phong_diffuse", "assets/textures/phong1_diffuse.png");
		textures.add(gl, "phong_specular", "assets/textures/phong1_specular.png");
		textures.add(gl, "pbr_albedo", "assets/textures/pbr1_albedo.png");
		textures.add(gl, "pbr_normal", "assets/textures/pbr1_normal.png");
		textures.add(gl, "pbr_metallic", "assets/textures/pbr1_metallic.png");
		textures.add(gl, "pbr_roughness", "assets/textures/pbr1_roughness.png");
		textures.add(gl, "pbr_ao", "assets/textures/pbr1_ao.png");

		/* textures.add(gl, "phong_diffuse", "assets/textures/phong2_diffuse.png");
		textures.add(gl, "phong_specular", "assets/textures/phong2_specular.png");
		textures.add(gl, "pbr_albedo", "assets/textures/pbr2_albedo.png");
		textures.add(gl, "pbr_normal", "assets/textures/pbr2_normal.png");
		textures.add(gl, "pbr_metallic", "assets/textures/pbr2_metallic.png");
		textures.add(gl, "pbr_roughness", "assets/textures/pbr2_roughness.png");
		textures.add(gl, "pbr_ao", "assets/textures/pbr2_ao.png"); */

		/* textures.add(gl, "phong_diffuse", "assets/textures/phong3_diffuse.png");
		textures.add(gl, "phong_specular", "assets/textures/phong3_specular.png");
		textures.add(gl, "pbr_albedo", "assets/textures/pbr3_albedo.png");
		textures.add(gl, "pbr_normal", "assets/textures/pbr3_normal.png");
		textures.add(gl, "pbr_metallic", "assets/textures/pbr3_metallic.png");
		textures.add(gl, "pbr_roughness", "assets/textures/pbr3_roughness.png");
		textures.add(gl, "pbr_ao", "assets/textures/pbr3_ao.png"); */
	}

	/**
	 * Set up each element of the scene in terms of model making, setting of positions/directions,
	 * etc.
	 * 
	 * @param gl The OpenGL context used for rendering.
	 */
	public void initialise(GL3 gl) {
		loadTextures(gl);

		lights[0] = new Light(gl, "cube");
		lights[0].setCamera(camera);
		lights[0].setPosition(new Vec3(0,1,0));
		defaultGlobalLightIntensity = lights[0].getMaterial();
		lights[1] = new Light(gl, "sphere");
		lights[1].setCamera(camera);
		lights[1].setPosition(new Vec3(0,20,0));
		lights[1].setDirection(new Vec3(0,-4,0));
		defaultSpotLightIntensity = lights[1].getMaterial();

		/*Texture[] roomTextures = {textures.get("example_diffuse"),
								  textures.get("example_specular"),
								  textures.get("example_albedo"),
								  textures.get("example_normal"),
								  textures.get("example_metallic"),
								  textures.get("example_roughness"),
								  textures.get("example_ao")};*/
		Texture[] roomTextures = {textures.get("phong_diffuse"),
								  textures.get("phong_specular"),
								  textures.get("pbr_albedo"),
								  textures.get("pbr_normal"),
								  textures.get("pbr_metallic"),
								  textures.get("pbr_roughness"),
								  textures.get("pbr_ao")};
		room = new Room(gl, camera, lights, roomTextures);
	}
	
	/**
	 * Draw each object in the scene.
	 * 
	 * @param gl The OpenGL context used for rendering.
	 */
	public void render(GL3 gl) {
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

		lights[0].render(gl);
		lights[1].render(gl);
		room.render(gl);
	}

	// ***************************************************
	/* TIME */ 
	
	private double startTime;
	
	/**
	 * Get the current time in seconds.
	 * 
	 * @return The current time in seconds in a double format.
	 */
	private double getSeconds() {
		return System.currentTimeMillis()/1000.0;
	}
}