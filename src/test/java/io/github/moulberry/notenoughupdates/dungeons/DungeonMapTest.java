package io.github.moulberry.notenoughupdates.dungeons;

import io.github.moulberry.notenoughupdates.util.NEUDebugLogger;
import net.minecraft.client.renderer.GlStateManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.OpenGLException;
import org.lwjgl.opengl.PixelFormat;

class DungeonMapTest {
	private static final DungeonMap demoMap = new DungeonMap();
	private static Display;

	@BeforeAll
	void setupForTests() {
		NEUDebugLogger.logMethod = 	DungeonMapTest::neuDebugLog;
		NEUDebugLogger.allFlagsEnabled = true;
		SharedLibraryLoader.load();
	}

	@BeforeEach
	void setupForTest() throws LWJGLException {
		try {
			createDisplay();
		} catch (LWJGLException lwjglException) {
			Log(String.format("Failed to create display. %s", lwjglException.getStackTrace().toString()));
			throw lwjglException;
		}
	}

	@AfterEach
	void cleanupFromTest() {
		Display.destroy();
	}

	@Test
	public void someTest() {
		try {
			createDisplay();
		} catch (LWJGLException lwjglException) {
			Assertions.assertTrue(false, lwjglException.getStackTrace().toString());
		}
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
			Log(String.format("Couldn\'t set pixel format. %s", lwjglexception.getStackTrace().toString());
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
