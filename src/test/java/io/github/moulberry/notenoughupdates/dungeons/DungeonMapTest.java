package io.github.moulberry.notenoughupdates.dungeons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.properties.PropertyMap.Serializer;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
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

	private static final String minecraftClassFilePath =
		// TODO: Figure out what is up with the Minecraft$xx.class files, change this to the common root
		minecraftRunDir + ".mixin.out/class/net/minecraft/client/Minecraft.class";
	private static final String mineCraftClassOutputPath =
		tweakOutputRoot + "net/minecraft/client/Minecraft.class";

	private static final String statListClassFilePath =
		tweakSourceRoot + "minecraft/net/minecraft/stats/StatList.class";
	private static final String statListClassOutputPath =
		tweakOutputRoot + "net/minecraft/stats/StatList.class";

	private static final String simpleReloadableResourceManagerFilePath =
		tweakSourceRoot + "minecraft/net/minecraft/client/resources/SimpleReloadableResourceManager.class";
	private static final String simpleReloadableResourceManagerOutputPath =
		tweakOutputRoot + "net/minecraft/client/resources/SimpleReloadableResourceManager.class";

	private static final String modelManagerFilePath =
		tweakSourceRoot + "minecraft/net/minecraft/client/resources/model/ModelManager.class";
	private static final String modelManagerOutputPath =
		tweakOutputRoot + "net/minecraft/client/resources/model/ModelManager.class";

	private static final String localeFilePath =
		tweakSourceRoot + "minecraft/net/minecraft/client/resources/Locale.class";
	private static final String localeOutputPath =
		tweakOutputRoot + "net/minecraft/client/resources/Locale.class";

	private static final String stringTranslateFilePath =
		tweakSourceRoot + "minecraft/net/minecraft/util/StringTranslate.class";
	private static final String stringTranslateOutputPath =
		tweakOutputRoot + "net/minecraft/util/StringTranslate.class";


	private static final String textureManagerFilePath =
		tweakSourceRoot + "minecraft/net/minecraft/client/renderer/texture/TextureManager.class";
	private static final String textureManagerOutputPath =
		tweakOutputRoot + "net/minecraft/client/renderer/texture/TextureManager.class";

	private static final String textureMapFilePath =
		tweakSourceRoot + "minecraft/net/minecraft/client/renderer/texture/TextureMap.class";
	private static final String textureMapOutputPath =
		tweakOutputRoot + "net/minecraft/client/renderer/texture/TextureMap.class";

	private static final String progressBarFilePath =
		tweakSourceRoot + "minecraft/net/minecraftforge/fml/common/ProgressManager.class";
	private static final String progressBarOutputPath =
		tweakOutputRoot + "net/minecraftforge/fml/common/ProgressManager.class";

	private static final String fmlLogFilePath =
		tweakSourceRoot + "minecraft/net/minecraftforge/fml/common/FMLLog.class";
	private static final String fmlLogOutputPath =
		tweakOutputRoot + "net/minecraftforge/fml/common/FMLLog.class";

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

	// Method to stub on the stats init method to avoid issues with forge init when loading language files
	// TODO:
	//  Extract the initial class from forgeSrc-1.8.9-11.15.1.2318-1.8.9-PROJECT(NotEnoughUpdates).jar and
	//  write out the patched class prior to running tests if possible and in all tests setup if not
	private void statListTweaks() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(statListClassFilePath);
		tweaker.updateMethodBody("init", "()V", "{}");
		tweaker.writeToFile(statListClassOutputPath);
	}

	private void tweakSimpleReloadableResourceManager() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(simpleReloadableResourceManagerFilePath);

		MethodTweaker reloadResourcesTweaker =
			tweaker.createMethodTweaker("reloadResources", "(Ljava/util/List;)V");
		//  net.minecraftforge.fml.common.ProgressManager.ProgressBar resReload = net.minecraftforge.fml.common.ProgressManager.push("Loading Resources", resourcesPacksList.size()+1, true);
		reloadResourcesTweaker.addNopTweak(4, 15);
		// resReload.step(iresourcepack.getPackName());
		reloadResourcesTweaker.addNopTweak(90, 11);
		// resReload.step("Reloading listeners");
		reloadResourcesTweaker.addNopTweak(110, 6);
		// net.minecraftforge.fml.common.ProgressManager.pop(resReload);
		reloadResourcesTweaker.addNopTweak(120, 4);
		reloadResourcesTweaker.applyTweaks();

		MethodTweaker notifyReloadListenersTweaker =
			tweaker.createMethodTweaker("notifyReloadListeners", "()V");
		//  net.minecraftforge.fml.common.ProgressManager.ProgressBar resReload = net.minecraftforge.fml.common.ProgressManager.push("Loading Resource", 1);
		notifyReloadListenersTweaker.addNopTweak(0, 15);
		// resReload.step(iresourcemanagerreloadlistener.getClass());
		notifyReloadListenersTweaker.addNopTweak(44, 12);
		// net.minecraftforge.fml.common.ProgressManager.pop(resReload);
		notifyReloadListenersTweaker.addNopTweak(66, 4);
		notifyReloadListenersTweaker.applyTweaks();


		MethodTweaker registerReloadListenerTweaker =
			tweaker.createMethodTweaker("registerReloadListener", "(Lnet/minecraft/client/resources/IResourceManagerReloadListener;)V");
		// net.minecraftforge.fml.common.ProgressManager.ProgressBar resReload = net.minecraftforge.fml.common.ProgressManager.push("Loading Resource", 1);
		// resReload.step(reloadListener.getClass());
		registerReloadListenerTweaker.addNopTweak(0, 19);
		// net.minecraftforge.fml.common.ProgressManager.pop(resReload);
		registerReloadListenerTweaker.addNopTweak(37, 4);
		registerReloadListenerTweaker.applyTweaks();


		tweaker.writeToFile(simpleReloadableResourceManagerOutputPath);
	}

	private void tweakModelManager() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(modelManagerFilePath);
		tweaker.newMethodFromFile("/home/dave/git/NotEnoughUpdates/ModelManager-onResourceManagerReload.java");
		tweaker.updateMethodBody("onResourceManagerReload", "(Lnet/minecraft/client/resources/IResourceManager;)V", "{ this.onResourceManagerReloadOrig($1); }");
		tweaker.writeToFile(modelManagerOutputPath);
	}

	private void tweakTextureManager() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(textureManagerFilePath);

		MethodTweaker onResourceManagerReloadTweaker =
			tweaker.createMethodTweaker("onResourceManagerReload", "(Lnet/minecraft/client/resources/IResourceManager;)V");
		// net.minecraftforge.fml.common.ProgressManager.ProgressBar bar = net.minecraftforge.fml.common.ProgressManager.push("Reloading Texture Manager", this.mapTextureObjects.keySet().size(), true);
		onResourceManagerReloadTweaker.addNopTweak(0, 21);
		// bar.step(entry.getKey().toString());
		onResourceManagerReloadTweaker.addNopTweak(56, 17);
		// net.minecraftforge.fml.common.ProgressManager.pop(bar);
		onResourceManagerReloadTweaker.addNopTweak(101, 4);
		onResourceManagerReloadTweaker.applyTweaks();


		tweaker.writeToFile(textureManagerOutputPath);
	}

	private void tweakTextureMap() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(textureMapFilePath);

//		MethodTweaker onResourceManagerReloadTweaker =
//			tweaker.createMethodTweaker("onResourceManagerReload", "(Lnet/minecraft/client/resources/IResourceManager;)V");
//		// net.minecraftforge.fml.common.ProgressManager.ProgressBar bar = net.minecraftforge.fml.common.ProgressManager.push("Reloading Texture Manager", this.mapTextureObjects.keySet().size(), true);
//		onResourceManagerReloadTweaker.addNopTweak(0, 21);
//		// bar.step(entry.getKey().toString());
//		onResourceManagerReloadTweaker.addNopTweak(56, 17);
//		// net.minecraftforge.fml.common.ProgressManager.pop(bar);
//		onResourceManagerReloadTweaker.addNopTweak(101, 4);
//		onResourceManagerReloadTweaker.applyTweaks();


		tweaker.writeToFile(textureMapOutputPath);
	}

	private void tweakLocale() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(localeFilePath);

		MethodTweaker onResourceManagerReloadTweaker =
			tweaker.createMethodTweaker("loadLocaleData", "(Ljava/io/InputStream;)V");

		// inputStreamIn = net.minecraftforge.fml.common.FMLCommonHandler.instance().loadLanguage(properties, inputStreamIn);
		// if (inputStreamIn == null) return;
		onResourceManagerReloadTweaker.addNopTweak(0, 16);
		onResourceManagerReloadTweaker.applyTweaks();


		tweaker.writeToFile(localeOutputPath);
	}

	private void tweakStringTranslate() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(stringTranslateFilePath);

		MethodTweaker parseLangFileTweaker =
			tweaker.createMethodTweaker("parseLangFile", "(Ljava/io/InputStream;)Ljava/util/HashMap;");
		// inputstream = net.minecraftforge.fml.common.FMLCommonHandler.instance().loadLanguage(table, inputstream);
		// if (inputstream == null) return table;
		parseLangFileTweaker.addNopTweak(4, 15);
		parseLangFileTweaker.applyTweaks();

		tweaker.writeToFile(stringTranslateOutputPath);
	}

	private void tweakForgeProgressManager() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(progressBarFilePath);
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

		tweaker.writeToFile(progressBarOutputPath);
	}

	private void tweakForgeFMLLog() throws IOException {
		ClassTweaker tweaker = new ClassTweaker(fmlLogFilePath);
		tweaker.makeNestedConstuctorsPublic();
		tweaker.updateMethodBody("info",
			"(Ljava/lang/String;[Ljava/lang/Object;)V",
			"{}");
		tweaker.writeToFile(fmlLogOutputPath);
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
//		tweakForgeModelLoader();
		tweakForgeProgressManager();
		tweakForgeFMLLog();
		tweakForgeLoader();
		tweakMinecraft();
		tweakModelManager();
//		tweakSimpleReloadableResourceManager();
//		tweakStringTranslate();
//		tweakTextureManager();
//		tweakLocale();

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
