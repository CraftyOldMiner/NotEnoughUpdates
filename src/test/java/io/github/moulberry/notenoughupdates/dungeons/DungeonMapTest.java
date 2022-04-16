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
import net.minecraft.client.resources.IReloadableResourceManager;
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

	// TODO: Build these path properly
	private static final String inputClassRoot = "/home/dave/git/NotEnoughUpdates/.gradle/minecraft/";
	private static final String outputClassRoot ="/home/dave/git/NotEnoughUpdates/build/classes/java/test/";

	// TODO:
	//  - Check whether we need to account for mix-ins, if so, can we get them from the run dir's .mixin.out/class?
	//  - Check when the run dir is created normally
	private static final String minecraftRunDir = "/home/dave/git/NotEnoughUpdates/run/";

	// TODO: build these from class names
	private static final String forgeLoaderPath = "net/minecraftforge/fml/common/Loader.class";
	private static final String forgeModelLoaderPath = "net/minecraftforge/client/model/ModelLoader.class";
	private static final String forgeProgressBarPath = "net/minecraftforge/fml/common/ProgressManager.class";
	private static final String forgeFMLLogPath = "net/minecraftforge/fml/common/FMLLog.class";
	private static final String forgeSplashProgressPath = "net/minecraftforge/fml/client/SplashProgress.class";
	private static final String minecraftPath = "net/minecraft/client/Minecraft.class";
	private static final String modelManagerPath = "net/minecraft/client/resources/model/ModelManager.class";

	@BeforeAll
	public static void setupForTests() {
		NEUDebugLogger.logMethod = 	DungeonMapTest::neuDebugLog;
		NEUDebugLogger.allFlagsEnabled = true;
		SharedLibraryLoader.load();
	}

	private void tweakForgeLoader() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(inputClassRoot + forgeLoaderPath);
		tweaker.updateConstructorBody("{}");
		tweaker.writeToFile(outputClassRoot + forgeLoaderPath);
	}

	// TODO: switch to using ExprEditor instead of direct byteCode editing
	private void tweakForgeModelLoader() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(inputClassRoot + forgeModelLoaderPath);
		MethodTweaker reloadResourcesTweaker =
			tweaker.createMethodTweaker("ModelLoader", "(Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/client/renderer/texture/TextureMap;Lnet/minecraft/client/renderer/BlockModelShapes;)V");
		// Replace enableVerboseMissingInfo init to just return true to avoid trying to access an uninitialized Launch.blackboard
		reloadResourcesTweaker.addNopTweak(67, 32);
		reloadResourcesTweaker.addNopTweak(100, 4);
		reloadResourcesTweaker.applyTweaks();
		tweaker.writeToFile(inputClassRoot + forgeModelLoaderPath);
	}

	private void tweakForgeSplashProgress() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(inputClassRoot + forgeSplashProgressPath);
		MethodTweaker splashProgressTweaker = tweaker.createMethodTweaker("<clinit>", "()V");
		// Remove initialization of fmlPack
		splashProgressTweaker.addNopTweak(32, 9);
		splashProgressTweaker.applyTweaks();
		tweaker.writeToFile(outputClassRoot + forgeSplashProgressPath);
	}

	private void tweakForgeProgressManager() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(inputClassRoot + forgeProgressBarPath);

		ClassTweaker childClassTweaker = tweaker.getChildClassTweaker("ProgressManager$ProgressBar");
		childClassTweaker.updateMethodBody("step",
			"(Ljava/lang/String;)V",
			"{}");

		childClassTweaker.makeConstuctorsPublic();
		tweaker.updateMethodBody("push",
			"(Ljava/lang/String;IZ)Lnet/minecraftforge/fml/common/ProgressManager$ProgressBar;",
			"{ return new net.minecraftforge.fml.common.ProgressManager$ProgressBar(\"dummy bar\", 999);}");
		tweaker.updateMethodBody("pop",
			"(Lnet/minecraftforge/fml/common/ProgressManager$ProgressBar;)V",
			"{}");

		tweaker.writeToFile(outputClassRoot + forgeProgressBarPath);
	}

	private void tweakForgeFMLLog() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(inputClassRoot + forgeFMLLogPath);
		tweaker.updateMethodBody("info",
			"(Ljava/lang/String;[Ljava/lang/Object;)V",
			"{}");
		tweaker.writeToFile(outputClassRoot + forgeFMLLogPath);
	}

	// Method to create patched Minecraft class with a default constructor - probably not needed with other approach below
	private void tweakMinecraft() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(inputClassRoot + minecraftPath);
		// TODO:
		//  - Figure out how to build this from the de-obfuscated JAR file instead
		//  - Parameterize the resource pack location and GuiScreen to show
		tweaker.newMethodFromFile("/home/dave/git/NotEnoughUpdates/StartupTrimmed.java");
		tweaker.updateMethodBody("startGame", "()V", "{ startGameTest(); }");
		tweaker.writeToFile(outputClassRoot + minecraftPath);
	}

	private void tweakModelManager() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(inputClassRoot + modelManagerPath);
		// TODO: Figure out how to grab this source code from the de-obfuscated jar files
		tweaker.newMethodFromFile("/home/dave/git/NotEnoughUpdates/ModelManager-onResourceManagerReload.java");
		tweaker.updateMethodBody("onResourceManagerReload", "(Lnet/minecraft/client/resources/IResourceManager;)V", "{ this.onResourceManagerReloadOrig($1); }");
		tweaker.writeToFile(outputClassRoot + modelManagerPath);
	}

	private void instantiateMinecraftClass() {
		System.setProperty("java.net.preferIPv4Stack", "true");
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
		tweakForgeSplashProgress();
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
