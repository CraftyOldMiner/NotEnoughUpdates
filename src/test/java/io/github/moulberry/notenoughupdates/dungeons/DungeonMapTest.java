package io.github.moulberry.notenoughupdates.dungeons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.properties.PropertyMap.Serializer;
import io.github.moulberry.notenoughupdates.testutil.ClassTweaker;
import io.github.moulberry.notenoughupdates.testutil.ClassTweaker.MethodTweaker;
import io.github.moulberry.notenoughupdates.util.NEUDebugLogger;
import javassist.CannotCompileException;
import javassist.NotFoundException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfiguration;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.Session;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;

class DungeonMapTest {
	private static final DungeonMap demoMap = new DungeonMap();
	private ClassLoader classLoader = getClass().getClassLoader();
	private Framebuffer framebuffer;
	private final IMetadataSerializer metadataSerializer_ = new IMetadataSerializer();
	private IReloadableResourceManager mcResourceManager;
	/** The RenderEngine instance used by Minecraft */
	public TextureManager renderEngine;
	private Minecraft mc;

	private static final String minecraftRunDir = "/home/dave/git/NotEnoughUpdates/run/";
	private static final String tweakSourceRoot = "/home/dave/git/NotEnoughUpdates/.gradle/";
	private static final String tweakOutputRoot ="/home/dave/git/NotEnoughUpdates/build/classes/java/test/";

	private static final String forgeLoaderClassFilePath =
		tweakSourceRoot + "minecraft/net/minecraftforge/fml/common/Loader.class";
	private static final String forgeLoaderClassOutputPath =
		tweakOutputRoot + "net/minecraftforge/fml/common/Loader.class";

	private static final String forgeModelLoaderClassFilePath =
		tweakSourceRoot + "minecraft/net/minecraftforge/client/model/ModelLoader.class";
	private static final String forgeModelLoaderClassOutputPath =
		tweakOutputRoot + "net/minecraftforge/client/model/ModelLoader.class";

	private static final String forgeProgressBarFilePath =
		tweakSourceRoot + "minecraft/net/minecraftforge/fml/common/ProgressManager.class";
	private static final String forgeProgressBarOutputPath =
		tweakOutputRoot + "net/minecraftforge/fml/common/ProgressManager.class";

	private static final String forgeFmlLogFilePath =
		tweakSourceRoot + "minecraft/net/minecraftforge/fml/common/FMLLog.class";
	private static final String forgeFmlLogOutputPath =
		tweakOutputRoot + "net/minecraftforge/fml/common/FMLLog.class";

	private static final String minecraftClassFilePath =
		// TODO: Figure out what is up with the Minecraft$xx.class files, change this to the common root
		// TODO: also pick up other mixin files from the build (if they can be built)
		minecraftRunDir + ".mixin.out/class/net/minecraft/client/Minecraft.class";
	private static final String mineCraftClassOutputPath =
		tweakOutputRoot + "net/minecraft/client/Minecraft.class";

	private static final String modelManagerFilePath =
		tweakSourceRoot + "minecraft/net/minecraft/client/resources/model/ModelManager.class";
	private static final String modelManagerOutputPath =
		tweakOutputRoot + "net/minecraft/client/resources/model/ModelManager.class";


	@BeforeAll
	public static void setupForTests() {
		NEUDebugLogger.logMethod = 	DungeonMapTest::neuDebugLog;
		NEUDebugLogger.allFlagsEnabled = true;
		SharedLibraryLoader.load();
	}

	private void tweakForgeLoader() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(forgeLoaderClassFilePath);
		tweaker.updateConstructorBody("{}");
		tweaker.writeToFile(forgeLoaderClassOutputPath);
	}

	private void tweakForgeModelLoader() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(forgeModelLoaderClassFilePath);

		MethodTweaker reloadResourcesTweaker =
			tweaker.createMethodTweaker("ModelLoader", "(Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/client/renderer/texture/TextureMap;Lnet/minecraft/client/renderer/BlockModelShapes;)V");
		// Replace enableVerboseMissingInfo init to just return true
		// Original code: enableVerboseMissingInfo = (Boolean)Launch.blackboard.get("fml.deobfuscatedEnvironment") || Boolean.parseBoolean(System.getProperty("forge.verboseMissingModelLogging", "false"))
		reloadResourcesTweaker.addNopTweak(67, 32);
		reloadResourcesTweaker.addNopTweak(100, 4);
		reloadResourcesTweaker.applyTweaks();
		tweaker.writeToFile(forgeModelLoaderClassOutputPath);
	}

	// Method to create patched Minecraft class with a default constructor - probably not needed with other approach below
	private void tweakMinecraft() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(minecraftClassFilePath);
		tweaker.newMethodFromFile("/home/dave/git/NotEnoughUpdates/StartupTrimmed.java");
		tweaker.newMethodFromFile("/home/dave/git/NotEnoughUpdates/Minecraft-getGLMaximumTextureSize.java");
		tweaker.updateMethodBody("startGame", "()V", "{ startGameTest(); }");
		tweaker.updateMethodBody("getGLMaximumTextureSize", "()I", "{ return getGLMaximumTextureSizeOrig(); }");
		tweaker.writeToFile(mineCraftClassOutputPath);
	}

	private void tweakModelManager() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(modelManagerFilePath);
		// TODO: See if we can retain this instead of overwriting it so we can leverage net/minecraftforge/client/model/ModelLoader.java for loading models from mods
		tweaker.newMethodFromFile("/home/dave/git/NotEnoughUpdates/ModelManager-onResourceManagerReload.java");
		tweaker.updateMethodBody("onResourceManagerReload", "(Lnet/minecraft/client/resources/IResourceManager;)V", "{ this.onResourceManagerReloadOrig($1); }");
		tweaker.writeToFile(modelManagerOutputPath);
	}

	private void tweakForgeProgressManager() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(forgeProgressBarFilePath);
		ClassTweaker childClassTweaker = tweaker.getChildClassTweaker("ProgressManager$ProgressBar");

		childClassTweaker.updateMethodBody("step",
			"(Ljava/lang/String;)V",
			"{}");

		tweaker.makeNestedConstuctorsPublic();
		tweaker.updateMethodBody("push",
			"(Ljava/lang/String;IZ)Lnet/minecraftforge/fml/common/ProgressManager$ProgressBar;",
			"{ return new net.minecraftforge.fml.common.ProgressManager$ProgressBar(\"dummy bar\", 999);}");

		tweaker.updateMethodBody("pop",
			"(Lnet/minecraftforge/fml/common/ProgressManager$ProgressBar;)V",
			"{}");

		tweaker.writeToFile(forgeProgressBarOutputPath);
	}

	private void tweakForgeFMLLog() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(forgeFmlLogFilePath);
		tweaker.makeNestedConstuctorsPublic();
		tweaker.updateMethodBody("info",
			"(Ljava/lang/String;[Ljava/lang/Object;)V",
			"{}");
		tweaker.writeToFile(forgeFmlLogOutputPath);
	}

	private void instantiateMinecraftClass() {
		System.setProperty("java.net.preferIPv4Stack", "true");
		// TODO: Build this path properly, consider parameterizing it. Populate automatically for a build?
		File gameDir = new File(minecraftRunDir);
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

		this.mc = new Minecraft(gameconfiguration);
	}

	@Test
	public void someTest1() throws NotFoundException, CannotCompileException, IOException {
		// Generate tweaked class files to allow loading without a Minecraft process
		tweakForgeModelLoader();
		tweakForgeProgressManager();
		tweakForgeFMLLog();
		tweakForgeLoader();
		tweakMinecraft();
		tweakModelManager();

		// Now actually create all the Minecraft classes
		instantiateMinecraftClass();

		//		NotEnoughUpdates neu = new NotEnoughUpdates();
		mc.run();

		GuiDungeonMapEditor mapEditor = new GuiDungeonMapEditor();
		GlStateManager.pushMatrix();
		GlStateManager.popMatrix();
	}

	private static void neuDebugLog(String message) {
		Log(message);
	}

	private static void Log(String message) { System.out.println(message); }
}
