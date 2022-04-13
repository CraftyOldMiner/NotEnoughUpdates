package io.github.moulberry.notenoughupdates.dungeons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.properties.PropertyMap.Serializer;
import io.github.moulberry.notenoughupdates.util.NEUDebugLogger;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfiguration;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.PixelFormat;
import tv.twitch.broadcast.FrameBuffer;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.List;

class DungeonMapTest {
	private static final DungeonMap demoMap = new DungeonMap();
	private ClassLoader classLoader = getClass().getClassLoader();
	private Framebuffer framebuffer;
	private final IMetadataSerializer metadataSerializer_ = new IMetadataSerializer();
	private IReloadableResourceManager mcResourceManager;
	/** The RenderEngine instance used by Minecraft */
	public TextureManager renderEngine;

	private static final String minecraftClassFilePath =
		"/home/dave/git/NotEnoughUpdates/run/.mixin.out/class/net/minecraft/client/Minecraft.class";
	private static final String mineCraftClassOutputPath =
		"/home/dave/git/NotEnoughUpdates/build/classes/java/test/net/minecraft/client/Minecraft.class";
	private static final String statListClassFilePath =
		"/home/dave/git/NotEnoughUpdates/.gradle/minecraft/net/minecraft/stats/StatList.class";
	private static final String statListClassOutputPath =
		"/home/dave/git/NotEnoughUpdates/build/classes/java/test/net/minecraft/stats/StatList.class";

	@BeforeAll
	public static void setupForTests() {
		NEUDebugLogger.logMethod = 	DungeonMapTest::neuDebugLog;
		NEUDebugLogger.allFlagsEnabled = true;
		SharedLibraryLoader.load();
	}

	@BeforeEach
	public void setupForTest() throws LWJGLException {
		try {
			createDisplay();
			OpenGlHelper.initializeTextures();
		} catch (LWJGLException lwjglException) {
			Log(String.format("Failed to create display. %s", lwjglException.getStackTrace().toString()));
			throw lwjglException;
		}
	}

	@AfterEach
	public void cleanupFromTest() {
		try {
			Display.releaseContext();
		} catch (LWJGLException e) {
			throw new RuntimeException(e);
		}
		Display.destroy();
	}

	private void addMinecraftDefaultConstructor() throws CannotCompileException, IOException {
		ClassPool pool = ClassPool.getDefault();
		CtClass minecraftClass = pool.makeClass(new FileInputStream(minecraftClassFilePath));
		ClassFile minecraftClassFile = minecraftClass.getClassFile();
		CtConstructor defaultConstructor = CtNewConstructor.make("public " + minecraftClass.getSimpleName() + "() {" +
			"this.theMinecraft = this;" +
			"}", minecraftClass);
		minecraftClass.addConstructor(defaultConstructor);

		// Write the class out
		File outputClassFile = new File(mineCraftClassOutputPath);
		outputClassFile.getParentFile().mkdirs();
		outputClassFile.createNewFile(); // if file already exists will do nothing
		minecraftClassFile.write(new DataOutputStream(new FileOutputStream(outputClassFile, false)));
	}

	private void stubOutStatListInit() throws CannotCompileException, IOException, NotFoundException {
		ClassPool pool = ClassPool.getDefault();
		CtClass statListClass = pool.makeClass(new FileInputStream(statListClassFilePath));
		ClassFile statListClassFile = statListClass.getClassFile();
		CtMethod initMethod = statListClass.getMethod("init", "()V");
		initMethod.setBody("{}");

		// Write the class out
		File outputClassFile = new File(statListClassOutputPath);
		outputClassFile.getParentFile().mkdirs();
		outputClassFile.createNewFile(); // if file already exists will do nothing
		statListClassFile.write(new DataOutputStream(new FileOutputStream(outputClassFile, false)));
	}

	@Test
	public void someTest0() throws NotFoundException, CannotCompileException, IOException {
		stubOutStatListInit();
		System.setProperty("java.net.preferIPv4Stack", "true");
		File gameDir = new File("/home/dave/git/NotEnoughUpdates/run/");

		File assetsDir = new File(gameDir, "assets/");
		File resourcePacksDir = new File(gameDir, "resourcepacks/");
		int width = 854;
		int height = 480;
		boolean fullScreen = false;
		boolean checkGlErrors = true;
		boolean demo = false;
		String version = "1.8.9";
		String serverName = null;
		int serverPort = 25565;
		String userName = "Player1234";
		String uuid = "Player" + Minecraft.getSystemTime() % 1000L;
		String accessToken = "dummytoken";
		String sessionType = "mojang";

		Gson gson = (new GsonBuilder()).registerTypeAdapter(PropertyMap.class, new Serializer()).create();
		PropertyMap userProperties = (PropertyMap)gson.fromJson("{}", PropertyMap.class);
		PropertyMap profileProperties = (PropertyMap)gson.fromJson("{}", PropertyMap.class);
		Session session = new Session(userName, uuid, (String)accessToken, (String)sessionType);
		GameConfiguration gameconfiguration = new GameConfiguration(
			new GameConfiguration.UserInformation(
				session,
				userProperties,
				profileProperties,
				Proxy.NO_PROXY
			),
			new GameConfiguration.DisplayInformation(width, height, fullScreen, checkGlErrors),
			new GameConfiguration.FolderInformation(gameDir, resourcePacksDir, assetsDir, null),
			new GameConfiguration.GameInformation(demo, version),
			new GameConfiguration.ServerInformation(serverName, serverPort)
		);
		Runtime.getRuntime().addShutdownHook(new Thread("Client Shutdown Thread") {
			public void run() {
				Minecraft.stopIntegratedServer();
			}
		});
		Thread.currentThread().setName("Client thread");
		Minecraft minecraft = new Minecraft(gameconfiguration);
		// TODO: One of the following:
		//   1. See if an uninitialized object can be created
		//   2. Shim out stuff in the Minecraft constructor
		//   3. See if field retrieval can be intercepted
	}

	@Test
	public void someTest1() throws NotFoundException, CannotCompileException, IOException {
		ClassPool pool = ClassPool.getDefault();
		CtClass minecraftClass = pool.makeClass(new FileInputStream(minecraftClassFilePath));
		ClassFile minecraftClassFile = minecraftClass.getClassFile();
		CtConstructor defaultConstructor = CtNewConstructor.make("public " + minecraftClass.getSimpleName() + "() {" +
			"this.theMinecraft = this;" +
			"}", minecraftClass);
		minecraftClass.addConstructor(defaultConstructor);

		// Write the class out
		File outputClassFile = new File("/home/dave/git/NotEnoughUpdates/build/classes/java/test/net/minecraft/client/Minecraft.class");
		outputClassFile.getParentFile().mkdirs();
		outputClassFile.createNewFile(); // if file already exists will do nothing
		minecraftClassFile.write(new DataOutputStream(new FileOutputStream(outputClassFile, false)));

		Bootstrap.register();
		GuiDungeonMapEditor mapEditor = new GuiDungeonMapEditor();
		GlStateManager.pushMatrix();
		GlStateManager.popMatrix();
	}

	@Test
	public void someTest2() {
		GlStateManager.pushMatrix();
		GlStateManager.popMatrix();
	}

	private void createDisplay() throws LWJGLException
	{
		Display.setResizable(true);
		Display.setTitle(DungeonMapTest.class.getName());
		try
		{
			Display.create((new PixelFormat()).withDepthBits(24));
		}
		catch (LWJGLException lwjglexception)
		{
			Log(String.format("Couldn't set pixel format. %s", lwjglexception.getStackTrace().toString()));
			try
			{
				Thread.sleep(1000L);
			}
			catch (InterruptedException var3)
			{
				// ignored
			}
			Display.create();
		}
	}

	private static void neuDebugLog(String message) {
		Log(message);
	}

	private static void Log(String message) { System.out.println(message); }
}
