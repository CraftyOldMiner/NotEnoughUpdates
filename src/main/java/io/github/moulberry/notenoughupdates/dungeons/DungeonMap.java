package io.github.moulberry.notenoughupdates.dungeons;

import com.google.common.collect.Iterables;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.BackgroundBlur;
import io.github.moulberry.notenoughupdates.core.config.Position;
import io.github.moulberry.notenoughupdates.core.util.MiscUtils;
import io.github.moulberry.notenoughupdates.core.util.StringUtils;
import io.github.moulberry.notenoughupdates.dungeons.ColorMap.ColoredArea;
import io.github.moulberry.notenoughupdates.options.seperateSections.DungeonMapConfig;
import io.github.moulberry.notenoughupdates.util.NEUResourceManager;
import io.github.moulberry.notenoughupdates.util.SpecialColour;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Matrix4f;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec4b;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.awt.*;
import java.util.List;
import java.util.*;

import static io.github.moulberry.notenoughupdates.dungeons.DungeonMapConstants.*;
import static io.github.moulberry.notenoughupdates.dungeons.DungeonMapConstants.ResourceLocs.*;
import static io.github.moulberry.notenoughupdates.dungeons.DungeonMapData.*;
import static io.github.moulberry.notenoughupdates.dungeons.DungeonMapData.NeuConfigData.*;

public class DungeonMap {
	private static DungeonMap instance = null;
	private static final Minecraft mc = Minecraft.getMinecraft();
	private static final DungeonMapConfig config = NotEnoughUpdates.INSTANCE.config.dungeonMap;

	private final ColorMap currentColorMap = new ColorMap(10, 20, 4, 5);
	private final HashMap<RoomOffset, Room> roomMap = new HashMap<>();
	private ColoredArea startRoom = null;
	private int connectorSize = 5;
	private int roomSize = 0;

	private long lastDecorationsMillis = -1;
	private long lastLastDecorationsMillis = -1;

	private final Map<String, MapPosition> playerEntityMapPositions = new HashMap<>();
	private final Map<String, MapPosition> playerMarkerMapPositions = new HashMap<>();
	private final Set<MapPosition> rawPlayerMarkerMapPositions = new HashSet<>();
	private final Map<String, MapPosition> playerMarkerMapPositionsLast = new HashMap<>();
	private final HashMap<String, Integer> playerIdMap = new HashMap<>();

	private final Map<String, ResourceLocation> playerSkinMap = new HashMap<>();

	public static DungeonMap getInstance() {
		if (instance == null) {
			instance = new DungeonMap();
		}
		return instance;
	}

	private static class RoomOffset {
		int x;
		int y;

		public RoomOffset(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public RoomOffset left() {
			return new RoomOffset(x - 1, y);
		}

		public RoomOffset right() {
			return new RoomOffset(x + 1, y);
		}

		public RoomOffset up() {
			return new RoomOffset(x, y - 1);
		}

		public RoomOffset down() {
			return new RoomOffset(x, y + 1);
		}

		public RoomOffset[] getNeighbors() {
			return new RoomOffset[]{left(), right(), up(), down()};
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			RoomOffset that = (RoomOffset) o;
			return x == that.x && y == that.y;
		}

		@Override
		public int hashCode() {
			return Objects.hash(x, y);
		}
	}

	private enum RoomConnectionType {
		NONE, WALL, CORRIDOR, ROOM_DIVIDER
	}

	private static class RoomConnection {
		RoomConnectionType type;
		Color colour;

		public RoomConnection(RoomConnectionType type, Color colour) {
			this.type = type;
			this.colour = colour;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			RoomConnection that = (RoomConnection) o;
			return type == that.type &&
				Objects.equals(colour, that.colour);
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, colour);
		}
	}

	private static final ResourceLocation mapIcons = new ResourceLocation("textures/map/map_icons.png");

	public static Framebuffer mapFramebuffer1 = null;
	public static Framebuffer mapFramebuffer2 = null;
	public static Matrix4f projectionMatrix = null;
	public static Shader mapShader = null;

	private static Framebuffer checkFramebufferSizes(Framebuffer framebuffer, int width, int height) {
		if (framebuffer == null || framebuffer.framebufferWidth != width || framebuffer.framebufferHeight != height) {
			if (framebuffer == null) {
				framebuffer = new Framebuffer(width, height, true);
			} else {
				framebuffer.createBindFramebuffer(width, height);
			}
			framebuffer.setFramebufferFilter(GL11.GL_NEAREST);
		}
		return framebuffer;
	}

	private static void upload(Shader shader, int width, int height, int scale, float radiusSq) {
		if (shader == null) return;
		shader.getShaderManager().getShaderUniformOrDefault("ProjMat").set(projectionMatrix);
		shader.getShaderManager().getShaderUniformOrDefault("InSize").set(width * scale, height * scale);
		shader.getShaderManager().getShaderUniformOrDefault("OutSize").set(width, height);
		shader.getShaderManager().getShaderUniformOrDefault("ScreenSize").set((float) width, (float) height);
		shader.getShaderManager().getShaderUniformOrDefault("radiusSq").set(radiusSq);
	}

	public void render(int centerX, int centerY) {
		boolean useFb = config.dmCompat <= 1 && OpenGlHelper.isFramebufferEnabled();
		boolean useShd = config.dmCompat <= 0 && OpenGlHelper.areShadersSupported();

		ScaledResolution scaledResolution = Utils.pushGuiScale(2);

		int minRoomX = 999;
		int minRoomY = 999;
		int maxRoomX = -999;
		int maxRoomY = -999;
		for (RoomOffset offset : roomMap.keySet()) {
			minRoomX = Math.min(offset.x, minRoomX);
			minRoomY = Math.min(offset.y, minRoomY);
			maxRoomX = Math.max(offset.x, maxRoomX);
			maxRoomY = Math.max(offset.y, maxRoomY);
		}

		int borderSizeOption = Math.round(config.dmBorderSize);

		int renderRoomSize =  getRenderRoomSize();
		int renderConnSize = NeuConfigData.getRenderConnSize();

		MapPosition playerPos = null;

		if (playerEntityMapPositions.containsKey(mc.thePlayer.getName())) {
			playerPos = playerEntityMapPositions.get(mc.thePlayer.getName());
		} else if (playerMarkerMapPositions.containsKey(mc.thePlayer.getName())) {
			playerPos = playerMarkerMapPositions.get(mc.thePlayer.getName());
		}

		int rotation = 180;
		if (playerPos != null && config.dmRotatePlayer) {
			rotation = (int) playerPos.rotation;
		}

		int mapSizeX;
		int mapSizeY;
		if (config.dmBorderStyle <= 1) {
			mapSizeX = 80 + Math.round(40 * config.dmBorderSize);
		} else {
			mapSizeX = borderSizeOption == 0 ? 90 : borderSizeOption == 1 ? 120 : borderSizeOption == 2 ? 160 : 240;
		}
		mapSizeY = mapSizeX;
		int roomsSizeX = (maxRoomX - minRoomX) * (renderRoomSize + renderConnSize) + renderRoomSize;
		int roomsSizeY = (maxRoomY - minRoomY) * (renderRoomSize + renderConnSize) + renderRoomSize;
		int mapCenterX = mapSizeX / 2;
		int mapCenterY = mapSizeY / 2;
		int scaleFactor = 8;

		projectionMatrix = Utils.createProjectionMatrix(mapSizeX * scaleFactor, mapSizeY * scaleFactor);
		mapFramebuffer1 = checkFramebufferSizes(mapFramebuffer1, mapSizeX * scaleFactor, mapSizeY * scaleFactor);
		mapFramebuffer2 = checkFramebufferSizes(mapFramebuffer2, mapSizeX * scaleFactor, mapSizeY * scaleFactor);
		mapFramebuffer1.framebufferColor[1] = 0;
		mapFramebuffer1.framebufferColor[2] = 0;

		try {
			if (mapShader == null) {
				mapShader = new Shader(new NEUResourceManager(mc.getResourceManager()),
					"dungeonmap", mapFramebuffer1, mapFramebuffer2
				);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Utils.pushGuiScale(-1);
			return;
		}

		int backgroundColour =
			SpecialColour.specialToChromaRGB(config.dmBackgroundColour);

		mapFramebuffer1.framebufferColor[0] = ((backgroundColour >> 16) & 0xFF) / 255f;
		mapFramebuffer1.framebufferColor[1] = ((backgroundColour >> 8) & 0xFF) / 255f;
		mapFramebuffer1.framebufferColor[2] = (backgroundColour & 0xFF) / 255f;
		mapFramebuffer2.framebufferColor[0] = ((backgroundColour >> 16) & 0xFF) / 255f;
		mapFramebuffer2.framebufferColor[1] = ((backgroundColour >> 8) & 0xFF) / 255f;
		mapFramebuffer2.framebufferColor[2] = (backgroundColour & 0xFF) / 255f;

		try {
			if (useFb) {
				mapFramebuffer1.framebufferClear();
				mapFramebuffer2.framebufferClear();
			}

			GlStateManager.pushMatrix();
			{
				if (useFb) {
					GlStateManager.matrixMode(5889);
					GlStateManager.loadIdentity();
					GlStateManager.ortho(0.0D, mapSizeX * scaleFactor, mapSizeY * scaleFactor, 0.0D, 1000.0D, 3000.0D);
					GlStateManager.matrixMode(5888);
					GlStateManager.loadIdentity();
					GlStateManager.translate(0.0F, 0.0F, -2000.0F);

					GlStateManager.scale(scaleFactor, scaleFactor, 1);
					mapFramebuffer1.bindFramebuffer(true);

					GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
					GlStateManager.disableBlend();
				} else {
					GL11.glEnable(GL11.GL_SCISSOR_TEST);
					GL11.glScissor(
						(centerX - mapSizeX / 2) * 2,
						mc.displayHeight - (centerY + mapSizeY / 2) * 2,
						mapSizeX * 2,
						mapSizeY * 2
					);

					GlStateManager.translate(centerX - mapSizeX / 2, centerY - mapSizeY / 2, 100);
				}

				if (config.dmBackgroundBlur > 0.1 &&
					config.dmBackgroundBlur < 100 &&
					config.dmEnable) {
					GlStateManager.translate(-centerX + mapSizeX / 2, -centerY + mapSizeY / 2, 0);
					BackgroundBlur.renderBlurredBackground(config.dmBackgroundBlur,
						scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(),
						centerX - mapSizeX / 2, centerY - mapSizeY / 2, mapSizeX, mapSizeY
					);
					BackgroundBlur.markDirty();
					GlStateManager.translate(centerX - mapSizeX / 2, centerY - mapSizeY / 2, 0);
				}

				GlStateManager.translate(mapCenterX, mapCenterY, 10);

				if (!useFb || config.dmBackgroundBlur > 0.1 &&
					config.dmBackgroundBlur < 100) {
					GlStateManager.enableBlend();
					GL14.glBlendFuncSeparate(
						GL11.GL_SRC_ALPHA,
						GL11.GL_ONE_MINUS_SRC_ALPHA,
						GL11.GL_ONE,
						GL11.GL_ONE_MINUS_SRC_ALPHA
					);
				}
				Utils.drawRectNoBlend(-mapCenterX, -mapCenterY, mapCenterX, mapCenterY, backgroundColour);

				GlStateManager.rotate(-rotation + 180, 0, 0, 1);

				if (config.dmCenterPlayer && playerPos != null) {
					float x = playerPos.getRenderX(0);
					float y = playerPos.getRenderY(0);
					x -= minRoomX * (renderRoomSize + renderConnSize);
					y -= minRoomY * (renderRoomSize + renderConnSize);

					GlStateManager.translate(-x, -y, 0);
				} else {
					GlStateManager.translate(-roomsSizeX / 2, -roomsSizeY / 2, 0);
				}

				for (Map.Entry<RoomOffset, Room> entry : roomMap.entrySet()) {
					RoomOffset roomOffset = entry.getKey();
					Room room = entry.getValue();

					int x = (roomOffset.x - minRoomX) * (renderRoomSize + renderConnSize);
					int y = (roomOffset.y - minRoomY) * (renderRoomSize + renderConnSize);

					GlStateManager.pushMatrix();
					GlStateManager.translate(x, y, 0);

					room.render(renderRoomSize, renderConnSize);

					GlStateManager.translate(-x, -y, 0);
					GlStateManager.popMatrix();
				}

				GlStateManager.translate(-mapCenterX + roomsSizeX / 2f, -mapCenterY + roomsSizeY / 2f, 0);

				GlStateManager.translate(mapCenterX, mapCenterY, 0);
				GlStateManager.rotate(rotation - 180, 0, 0, 1);
				GlStateManager.translate(-mapCenterX, -mapCenterY, 0);

				GlStateManager.translate(mapCenterX, mapCenterY, 0);

				for (Map.Entry<RoomOffset, Room> entry : roomMap.entrySet()) {
					RoomOffset roomOffset = entry.getKey();
					Room room = entry.getValue();

					float x =
						(roomOffset.x - minRoomX) * (renderRoomSize + renderConnSize) - roomsSizeX / 2f + renderRoomSize / 2f;
					float y =
						(roomOffset.y - minRoomY) * (renderRoomSize + renderConnSize) - roomsSizeY / 2f + renderRoomSize / 2f;
					float x2 = (float) (-x * Math.cos(Math.toRadians(-rotation)) + y * Math.sin(Math.toRadians(-rotation)));
					float y2 = (float) (-x * Math.sin(Math.toRadians(-rotation)) - y * Math.cos(Math.toRadians(-rotation)));

					GlStateManager.pushMatrix();
					GlStateManager.translate(x2, y2, 0);

					GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
					room.renderNoRotate(renderRoomSize, renderConnSize, rotation);

					GlStateManager.translate(-x2, -y2, 0);
					GlStateManager.popMatrix();
				}

				GlStateManager.translate(-mapCenterX, -mapCenterY, 0);

				GlStateManager.translate(mapCenterX, mapCenterY, 0);
				GlStateManager.rotate(-rotation + 180, 0, 0, 1);
				GlStateManager.translate(-mapCenterX, -mapCenterY, 0);

				GlStateManager.translate(mapCenterX - roomsSizeX / 2f, mapCenterY - roomsSizeY / 2f, 0);

				Tessellator tessellator = Tessellator.getInstance();
				WorldRenderer worldrenderer = tessellator.getWorldRenderer();
				int k = 0;

				for (Map.Entry<String, MapPosition> entry : playerMarkerMapPositions.entrySet()) {
					String name = entry.getKey();
					MapPosition pos = entry.getValue();
					float x = pos.getRenderX(0);
					float y = pos.getRenderY(0);
					float angle = pos.rotation;

					boolean doInterp = config.dmPlayerInterp;
					if (!isFloorOne && playerEntityMapPositions.containsKey(name)) {
						MapPosition entityPos = playerEntityMapPositions.get(name);
						angle = entityPos.rotation;

						float deltaX = entityPos.getRenderX(9) - pos.getRenderX(0);
						float deltaY = entityPos.getRenderY(9) - pos.getRenderY(0);

						x += deltaX;
						y += deltaY;

						doInterp = false;
					}

					float minU = 3 / 4f;
					float minV = 0;

					if (name.equals(mc.thePlayer.getName())) {
						minU = 1 / 4f;
					}

					float maxU = minU + 1 / 4f;
					float maxV = minV + 1 / 4f;

					if (doInterp && playerMarkerMapPositionsLast.containsKey(name)) {
						MapPosition last = playerMarkerMapPositionsLast.get(name);
						float xLast = last.getRenderX(0);
						float yLast = last.getRenderY(0);

						float distSq = (x - xLast) * (x - xLast) + (y - yLast) * (y - yLast);
						if (distSq < renderRoomSize * renderRoomSize / 4f) {
							float angleLast = last.rotation;
							if (angle > 180 && angleLast < 180) angleLast += 360;
							if (angleLast > 180 && angle < 180) angle += 360;

							float interpFactor = Math.round((System.currentTimeMillis() - lastDecorationsMillis) * 100f) / 100f /
								(lastDecorationsMillis - lastLastDecorationsMillis);
							interpFactor = Math.max(0, Math.min(1, interpFactor));

							x = xLast + (x - xLast) * interpFactor;
							y = yLast + (y - yLast) * interpFactor;
							angle = angleLast + (angle - angleLast) * interpFactor;
							angle %= 360;
						}
					}

					boolean blackBorder = false;
					boolean headLayer = false;
					int pixelWidth = 8;
					int pixelHeight = 8;
					if (renderRoomSize >= 24) {
						pixelWidth = pixelHeight = 12;
					}
					GlStateManager.color(1, 1, 1, 1);
					if ((!NotEnoughUpdates.INSTANCE.config.dungeons.showOwnHeadAsMarker ||
						playerMarkerMapPositions.size() <= 1 || minU != 1 / 4f) &&
						config.dmPlayerHeads >= 1 &&
						playerSkinMap.containsKey(entry.getKey())) {
						mc.getTextureManager().bindTexture(playerSkinMap.get(entry.getKey()));

						minU = 8 / 64f;
						minV = 8 / 64f;
						maxU = 16 / 64f;
						maxV = 16 / 64f;

						headLayer = true;
						if (config.dmPlayerHeads >= 2) {
							blackBorder = true;
						}
					} else {
						mc.getTextureManager().bindTexture(mapIcons);
					}

					x -= minRoomX * (renderRoomSize + renderConnSize);
					y -= minRoomY * (renderRoomSize + renderConnSize);

					GlStateManager.pushMatrix();

					GlStateManager.disableDepth();
					GlStateManager.enableBlend();
					GL14.glBlendFuncSeparate(
						GL11.GL_SRC_ALPHA,
						GL11.GL_ONE_MINUS_SRC_ALPHA,
						GL11.GL_ONE,
						GL11.GL_ONE_MINUS_SRC_ALPHA
					);

					GlStateManager.translate(x, y, -0.02F);
					GlStateManager.scale(config.dmIconScale,
						config.dmIconScale, 1
					);
					GlStateManager.rotate(angle, 0.0F, 0.0F, 1.0F);
					GlStateManager.translate(-0.5F, 0.5F, 0.0F);

					if (blackBorder) {
						Gui.drawRect(
							-pixelWidth / 2 - 1,
							-pixelHeight / 2 - 1,
							pixelWidth / 2 + 1,
							pixelHeight / 2 + 1,
							0xff111111
						);
						GlStateManager.color(1, 1, 1, 1);
					}

					worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
					worldrenderer.pos(-pixelWidth / 2f, pixelHeight / 2f, 30 + ((float) k * -0.005F)).tex(minU, minV).endVertex();
					worldrenderer.pos(pixelWidth / 2f, pixelHeight / 2f, 30 + ((float) k * -0.005F)).tex(maxU, minV).endVertex();
					worldrenderer.pos(pixelWidth / 2f, -pixelHeight / 2f, 30 + ((float) k * -0.005F)).tex(maxU, maxV).endVertex();
					worldrenderer
						.pos(-pixelWidth / 2f, -pixelHeight / 2f, 30 + ((float) k * -0.005F))
						.tex(minU, maxV)
						.endVertex();
					tessellator.draw();

					if (headLayer) {
						worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
						worldrenderer.pos(-pixelWidth / 2f, pixelHeight / 2f, 30 + ((float) k * -0.005F) + 0.001f).tex(
							minU + 0.5f,
							minV
						).endVertex();
						worldrenderer.pos(pixelWidth / 2f, pixelHeight / 2f, 30 + ((float) k * -0.005F) + 0.001f).tex(
							maxU + 0.5f,
							minV
						).endVertex();
						worldrenderer.pos(pixelWidth / 2f, -pixelHeight / 2f, 30 + ((float) k * -0.005F) + 0.001f).tex(
							maxU + 0.5f,
							maxV
						).endVertex();
						worldrenderer.pos(-pixelWidth / 2f, -pixelHeight / 2f, 30 + ((float) k * -0.005F) + 0.001f).tex(
							minU + 0.5f,
							maxV
						).endVertex();
						tessellator.draw();
					}
					GlStateManager.popMatrix();
					k--;
				}

				if (useFb) {
					GlStateManager.enableBlend();
					GL14.glBlendFuncSeparate(
						GL11.GL_SRC_ALPHA,
						GL11.GL_ONE_MINUS_SRC_ALPHA,
						GL11.GL_ONE,
						GL11.GL_ONE_MINUS_SRC_ALPHA
					);
				} else {
					GL11.glDisable(GL11.GL_SCISSOR_TEST);
				}
			}
			GlStateManager.popMatrix();

			if (useFb) {
				Framebuffer renderFromBuffer = mapFramebuffer1;
				if (useShd) {
					GlStateManager.pushMatrix();
					{
						try {
							upload(mapShader, mapSizeX, mapSizeY, scaleFactor, NeuConfigData.getBorderRadius());
							mapShader.setProjectionMatrix(projectionMatrix);
							mapShader.loadShader(0);
							renderFromBuffer = mapFramebuffer2;
						} catch (Exception ignored) {
						}
					}
					GlStateManager.popMatrix();
				}

				mc.getFramebuffer().bindFramebuffer(true);

				Utils.pushGuiScale(2);

				GlStateManager.translate(centerX, centerY, 100);

				renderFromBuffer.bindFramebufferTexture();
				Utils.drawTexturedRect(-mapSizeX / 2, -mapSizeY / 2, mapSizeX, mapSizeY,
					0, 1, 1, 0, GL11.GL_NEAREST
				);
				GlStateManager.bindTexture(0);

				GlStateManager.translate(-centerX, -centerY, -100);

				Utils.pushGuiScale(-1);
			}

			GlStateManager.translate(centerX, centerY, 100);

			if (config.dmChromaBorder) {
				int colour = SpecialColour.specialToChromaRGB(config.dmBorderColour);

				Gui.drawRect(-mapCenterX - 2, -mapCenterY - 2, -mapCenterX, -mapCenterY,
					colour
				); //topleft
				Gui.drawRect(-mapCenterX - 2, mapCenterY + 2, -mapCenterX, mapCenterY,
					SpecialColour.rotateHue(colour, -180)
				); //bottomleft
				Gui.drawRect(mapCenterX, -mapCenterY - 2, mapCenterX + 2, mapCenterY,
					SpecialColour.rotateHue(colour, -180)
				); //topright
				Gui.drawRect(mapCenterX, mapCenterY, mapCenterX + 2, mapCenterY + 2,
					colour
				); //bottomright

				for (int i = 0; i < 20; i++) {
					int start1 = SpecialColour.rotateHue(colour, -9 * i);
					int start2 = SpecialColour.rotateHue(colour, -9 * i - 9);
					int end1 = SpecialColour.rotateHue(colour, -180 - 9 * i);
					int end2 = SpecialColour.rotateHue(colour, -180 - 9 * i - 9);

					Utils.drawGradientRect(-mapCenterX - 2, -mapCenterY + (int) (mapSizeY * (i / 20f)), -mapCenterX,
						-mapCenterY + (int) (mapSizeY * ((i + 1) / 20f)), start1, start2
					); //left
					Utils.drawGradientRect(mapCenterX, -mapCenterY + (int) (mapSizeX * (i / 20f)), mapCenterX + 2,
						-mapCenterY + (int) (mapSizeX * ((i + 1) / 20f)),
						end1, end2
					); //right
					Utils.drawGradientRectHorz(-mapCenterX + (int) (mapSizeX * (i / 20f)), -mapCenterY - 2,
						-mapCenterX + (int) (mapSizeX * ((i + 1) / 20f)), -mapCenterY, start1, start2
					); //top
					Utils.drawGradientRectHorz(-mapCenterX + (int) (mapSizeX * (i / 20f)),
						mapCenterY, -mapCenterX + (int) (mapSizeX * ((i + 1) / 20f)), mapCenterY + 2,
						end1, end2
					); //bottom
				}

			} else {
				Gui.drawRect(-mapCenterX - 2, -mapCenterY, -mapCenterX, mapCenterY,
					SpecialColour.specialToChromaRGB(config.dmBorderColour)
				); //left
				Gui.drawRect(mapCenterX, -mapCenterY, mapCenterX + 2, mapCenterY,
					SpecialColour.specialToChromaRGB(config.dmBorderColour)
				); //right
				Gui.drawRect(-mapCenterX - 2, -mapCenterY - 2, mapCenterX + 2, -mapCenterY,
					SpecialColour.specialToChromaRGB(config.dmBorderColour)
				); //top
				Gui.drawRect(-mapCenterX - 2, mapCenterY, mapCenterX + 2, mapCenterY + 2,
					SpecialColour.specialToChromaRGB(config.dmBorderColour)
				); //bottom
			}

			String sizeId = borderSizeOption == 0 ? "small" : borderSizeOption == 2 ? "large" : "medium";

			ResourceLocation rl = new ResourceLocation("notenoughupdates:dungeon_map/borders/" + sizeId + "/" +
				config.dmBorderStyle + ".png");
			if (mc.getTextureManager().getTexture(rl) != TextureUtil.missingTexture) {
				mc.getTextureManager().bindTexture(rl);
				GlStateManager.color(1, 1, 1, 1);

				int size = borderSizeOption == 0 ? 165 : borderSizeOption == 1 ? 220 : borderSizeOption == 2 ? 300 : 440;
				Utils.drawTexturedRect(-size / 2, -size / 2, size, size, GL11.GL_NEAREST);
			}

			GlStateManager.translate(-centerX, -centerY, -100);
		} catch (Exception e) {
			e.printStackTrace();
			mc.getFramebuffer().bindFramebuffer(true);
			mc.entityRenderer.setupOverlayRendering();
		}

		Utils.pushGuiScale(-1);

		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.enableDepth();
		GlStateManager.disableLighting();
	}

	public void updateRoomConnections(RoomOffset roomOffset) {
		if (roomMap.containsKey(roomOffset)) {
			Room room = roomMap.get(roomOffset);
			Color[][] colourMap = currentColorMap.getCurrentColorMap();

			int otherPixelFilled = 0;
			int otherPixelColour = 0;
			for (int xOff = 0; xOff < roomSize; xOff++) {
				for (int yOff = 0; yOff < roomSize; yOff++) {
					int x = startRoom.x + roomOffset.x * (roomSize + connectorSize) + xOff;
					int y = startRoom.y + roomOffset.y * (roomSize + connectorSize) + yOff;

					if (x > 0 && y > 0 && x < colourMap.length && y < colourMap[x].length) {
						Color c = colourMap[x][y];
						if (!c.equals(room.colour)) {
							if (otherPixelColour == c.getRGB()) {
								otherPixelFilled++;
							} else {
								otherPixelFilled--;
								if (otherPixelFilled <= 0) {
									otherPixelFilled = 1;
									otherPixelColour = c.getRGB();
								}
							}
						}
					}
				}
			}

			room.tickColour = 0;
			if ((float) otherPixelFilled / roomSize / connectorSize > 0.05) {
				room.tickColour = otherPixelColour;
			}

			for (int k = 0; k < 4; k++) {
				Color colour = null;
				int totalFilled = 0;

				for (int i = 0; i < roomSize; i++) {
					for (int j = 1; j <= connectorSize; j++) {
						int x = startRoom.x + roomOffset.x * (roomSize + connectorSize);
						int y = startRoom.y + roomOffset.y * (roomSize + connectorSize);

						if (k == 0) {
							x += i;
							y -= j;
						} else if (k == 1) {
							x += roomSize + j - 1;
							y += i;
						} else if (k == 2) {
							x += i;
							y += roomSize + j - 1;
						} else {
							x -= j;
							y += i;
						}

						if (x > 0 && y > 0 && x < colourMap.length && y < colourMap[x].length) {
							Color pixel = colourMap[x][y];
							if (pixel.getAlpha() > 40) {
								if (colour == null) {
									colour = pixel;
									totalFilled = 1;
								} else {
									if (colour.equals(pixel)) {
										totalFilled++;
									} else {
										totalFilled--;
										if (totalFilled <= 0) {
											colour = pixel;
											totalFilled = 1;
										}
									}
								}
							}
						}
					}
				}
				float proportionFilled = (float) totalFilled / roomSize / connectorSize;

				RoomConnectionType type = RoomConnectionType.WALL;
				if (proportionFilled > 0.8) {
					type = RoomConnectionType.ROOM_DIVIDER;
				} else if (proportionFilled > 0.1) {
					type = RoomConnectionType.CORRIDOR;
				}
				if (k == 0) {
					room.up = new RoomConnection(type, colour);
				} else if (k == 1) {
					room.right = new RoomConnection(type, colour);
				} else if (k == 2) {
					room.down = new RoomConnection(type, colour);
				} else {
					room.left = new RoomConnection(type, colour);
				}
			}

			int x = startRoom.x + roomOffset.x * (roomSize + connectorSize) + roomSize + connectorSize / 2;
			int y = startRoom.y + roomOffset.y * (roomSize + connectorSize) + roomSize + connectorSize / 2;

			room.fillCorner = false;
			if (x > 0 && y > 0 && x < colourMap.length && y < colourMap[x].length) {
				Color pixel = colourMap[x][y];
				if (pixel.equals(room.colour)) {
					room.fillCorner = true;
				}
			}
		}
	}

	public void loadNeighbors(RoomOffset room) {
		Color[][] colourMap = currentColorMap.getCurrentColorMap();

		if (!roomMap.containsKey(room)) {
			roomMap.put(room, new Room());
		}
		for (RoomOffset neighbor : room.getNeighbors()) {
			if (!roomMap.containsKey(neighbor)) {
				int x = startRoom.x + neighbor.x * (roomSize + connectorSize);
				int y = startRoom.y + neighbor.y * (roomSize + connectorSize);

				if (x >= 0 && y >= 0 && x + roomSize < colourMap.length && y + roomSize < colourMap[x].length) {
					if (colourMap[x][y].getAlpha() > 100) {
						roomMap.put(neighbor, new Room());
						loadNeighbors(neighbor);
					}
				}
			}
		}
	}

	public void updateRoomColours() {
		Color[][] colourMap = currentColorMap.getCurrentColorMap();
		for (Map.Entry<RoomOffset, Room> entry : roomMap.entrySet()) {
			int x = startRoom.x + entry.getKey().x * (roomSize + connectorSize);
			int y = startRoom.y + entry.getKey().y * (roomSize + connectorSize);

			try {
				entry.getValue().colour = colourMap[x][y];
			} catch (Exception ignored) {
			}
		}
	}

	private class MapPosition {
		public float roomOffsetX;
		public float connOffsetX;

		public float roomOffsetY;
		public float connOffsetY;

		public float rotation;

		public MapPosition(float roomOffsetX, float connOffsetX, float roomOffsetY, float connOffsetY) {
			this.roomOffsetX = roomOffsetX;
			this.connOffsetX = connOffsetX;
			this.roomOffsetY = roomOffsetY;
			this.connOffsetY = connOffsetY;
		}

		public float getRenderX(int blockOffset) {
			return (roomOffsetX + blockOffset) * getRenderRoomSize() + connOffsetX * getRenderConnSize();
		}

		public float getRenderY(int blockOffset) {
			return (roomOffsetY + blockOffset) * getRenderRoomSize() + connOffsetY * getRenderConnSize();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			MapPosition that = (MapPosition) o;
			return Float.compare(that.roomOffsetX, roomOffsetX) == 0 &&
				Float.compare(that.connOffsetX, connOffsetX) == 0 &&
				Float.compare(that.roomOffsetY, roomOffsetY) == 0 &&
				Float.compare(that.connOffsetY, connOffsetY) == 0 &&
				Float.compare(that.rotation, rotation) == 0;
		}

		@Override
		public int hashCode() {
			return Objects.hash(roomOffsetX, connOffsetX, roomOffsetY, connOffsetY, rotation);
		}

		@Override
		public String toString() {
			return String.format("roomOffsetX=%f roomOffsetY=%f connOffsetX=%f connOffsetY=%f rotation=%f",
				roomOffsetX, roomOffsetY, connOffsetX, connOffsetY, rotation);
		}
	}
	private boolean isFloorOne = false;
	private boolean failMap = false;
	private long lastClearCache = 0;

	public void renderMap(
		int centerX, int centerY, ColorMap colorMap, Set<String> playerNames,
		boolean usePlayerPositions, float partialTicks
	) {
		if (!config.dmEnable) return;
		if (colorMap == null) return;
		this.currentColorMap.setColorData(colorMap.getCurrentColorBytes());

		boolean searchForPlayers = false;
		if (System.currentTimeMillis() - lastClearCache > 1000) {
			roomMap.clear();
			searchForPlayers = true;
			connectorSize = -1;
			roomSize = -1;
			failMap = false;

			lastClearCache = System.currentTimeMillis();

			isFloorOne = false;
			Scoreboard scoreboard = mc.thePlayer.getWorldScoreboard();

			ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1);

			List<Score> scores = new ArrayList<>(scoreboard.getSortedScores(sidebarObjective));

			for (int i = scores.size() - 1; i >= 0; i--) {
				Score score = scores.get(i);
				ScorePlayerTeam scoreplayerteam1 = scoreboard.getPlayersTeam(score.getPlayerName());
				String line = ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score.getPlayerName());
				line = Utils.cleanColour(line);

				if (line.contains("(F1)") || line.contains("(E)") || line.contains("(M1)")) {
					isFloorOne = true;
					break;
				}
			}
		}

		if (failMap) {
			return;
		}

		int alphaPixels = 0;
		for (int x = 0; x < ColorMap.MAP_SIZE; x++) {
			for (int y = 0; y < ColorMap.MAP_SIZE; y++) {
				if (colorMap.getAlphaAsInt(x, y) < 50) {
					alphaPixels++;
				}
			}
		}
		// if less than 10 percent of pixels were alpha then it's not the dungeon map
		if (alphaPixels < ColorMap.MAP_SIZE * ColorMap.MAP_SIZE / 10) {
			failMap = true;
			return;
		}

		if (startRoom == null || roomSize <= 0) {
			// TODO: fix this
			if (!colorMap.hasRoomWithColor(ColorMap.DungeonMapColor.START_ROOM.colorIndex() << 2)) {
				failMap = true;
				return;
			}
		}

		if (connectorSize <= 0) {
			for (int i = 0; i < roomSize; i++) {
				for (int k = 0; k < 4; k++) {
					for (int j = 1; j < 8; j++) {
						int x;
						int y;

						if (k == 0) {
							x = startRoom.x + i;
							y = startRoom.y - j;
						} else if (k == 1) {
							x = startRoom.x + roomSize + j - 1;
							y = startRoom.y + i;
						} else if (k == 2) {
							x = startRoom.x + i;
							y = startRoom.y + roomSize + j - 1;
						} else {
							x = startRoom.x - j;
							y = startRoom.y + i;
						}

						if (x > 0 && y > 0 && x < ColorMap.MAP_SIZE && y < ColorMap.MAP_SIZE) {
							if (colorMap.getAlphaAsInt(x, y) > 80) {
								if (j == 1) {
									break;
								}
								connectorSize = Math.min(connectorSize, j - 1);
							}
						}
					}
				}
			}

			if (connectorSize <= 0) {
				connectorSize = 4;
			}
		}

		playerNames.add(mc.thePlayer.getName());
		if (searchForPlayers) {
			for (EntityPlayer player : mc.theWorld.playerEntities) {
				if (player instanceof AbstractClientPlayer && playerNames.contains(player.getName())) {
					AbstractClientPlayer aplayer = (AbstractClientPlayer) player;
					ResourceLocation skin = aplayer.getLocationSkin();
					if (skin != DefaultPlayerSkin.getDefaultSkin(aplayer.getUniqueID())) {
						playerSkinMap.put(player.getName(), skin);
						playerIdMap.put(player.getName(), player.getEntityId());
					}
				}
			}
		}

		playerEntityMapPositions.clear();
		if (usePlayerPositions) {
			for (String playerName : playerNames) {
				if (playerIdMap.containsKey(playerName)) {
					Entity entity = mc.theWorld.getEntityByID(playerIdMap.get(playerName));
					if (entity instanceof EntityPlayer) {
						EntityPlayer player = (EntityPlayer) entity;

						float roomX = (float) player.posX / (roomSizeBlocks + 1);
						float roomY = (float) player.posZ / (roomSizeBlocks + 1);

						float playerRoomOffsetX = (float) Math.floor(roomX);
						float playerConnOffsetX = (float) Math.floor(roomX);
						float playerRoomOffsetY = (float) Math.floor(roomY);
						float playerConnOffsetY = (float) Math.floor(roomY);

						float roomXInBlocks = (float) player.posX % (roomSizeBlocks + 1);
						if (roomXInBlocks < 2) { //0,1
							playerConnOffsetX -= 2 / 5f - roomXInBlocks / 5f;
						} else if (roomXInBlocks > roomSizeBlocks - 2) { //31,30,29
							playerRoomOffsetX++;
							playerConnOffsetX += (roomXInBlocks - (roomSizeBlocks - 2)) / 5f;
						} else {
							playerRoomOffsetX += (roomXInBlocks - 2) / (roomSizeBlocks - 4);
						}

						float roomYInBlocks = (float) player.posZ % (roomSizeBlocks + 1);
						if (roomYInBlocks < 2) { //0,1
							playerConnOffsetY -= 2 / 5f - roomYInBlocks / 5f;
						} else if (roomYInBlocks > roomSizeBlocks - 2) { //31,30,29
							playerRoomOffsetY++;
							playerConnOffsetY += (roomYInBlocks - (roomSizeBlocks - 2)) / 5f;
						} else {
							playerRoomOffsetY += (roomYInBlocks - 2) / (roomSizeBlocks - 4);
						}

						playerRoomOffsetX -= startRoom.x / (roomSize + connectorSize);
						playerRoomOffsetY -= startRoom.y / (roomSize + connectorSize);
						playerConnOffsetX -= startRoom.x / (roomSize + connectorSize);
						playerConnOffsetY -= startRoom.y / (roomSize + connectorSize);

						MapPosition pos = new MapPosition(
							playerRoomOffsetX,
							playerConnOffsetX,
							playerRoomOffsetY,
							playerConnOffsetY
						);
						pos.rotation =
							(player.prevRotationYawHead + (player.rotationYawHead - player.prevRotationYawHead) * partialTicks) % 360;
						if (pos.rotation < 0) pos.rotation += 360;
						playerEntityMapPositions.put(player.getName(), pos);
					}
				}
			}
		}

		loadNeighbors(new RoomOffset(0, 0));
		updateRoomColours();
		for (RoomOffset offset : roomMap.keySet()) {
			updateRoomConnections(offset);
		}

		if (roomMap.isEmpty()) {
			failMap = true;
			return;
		}

		Map<String, Vec4b> mapDecorations = DungeonMapData.getInstance().getMapDecorations();
		if (mapDecorations != null && mapDecorations.size() > 0) {
			List<MapPosition> positions = new ArrayList<>();
			int decorations = 0;
			for (Vec4b vec4b : mapDecorations.values()) {
				byte id = vec4b.func_176110_a();
				if (id != 1 && id != 3) continue;

				float x = (float) vec4b.func_176112_b() / 2.0F + 64.0F;
				float y = (float) vec4b.func_176113_c() / 2.0F + 64.0F;

				if (x < 0 || y < 0 || x > 128 || y > 128) {
					continue;
				}

				float deltaX = x - startRoom.x;
				float deltaY = y - startRoom.y;

				float roomsOffsetX = (int) Math.floor(deltaX / (roomSize + connectorSize));
				float connOffsetX = (int) Math.floor(deltaX / (roomSize + connectorSize));
				float xRemainder = deltaX % (roomSize + connectorSize);
				if (Math.abs(xRemainder) > roomSize) {
					roomsOffsetX += Math.copySign(1, xRemainder);
					connOffsetX += Math.copySign(1, xRemainder) * (Math.abs(xRemainder) - roomSize) / connectorSize;
				} else {
					roomsOffsetX += xRemainder / roomSize;
				}
				if (deltaX < 0 && xRemainder != 0) {
					roomsOffsetX++;
					connOffsetX++;
				}
				float roomsOffsetY = (int) Math.floor(deltaY / (roomSize + connectorSize));
				float connOffsetY = (int) Math.floor(deltaY / (roomSize + connectorSize));
				float yRemainder = deltaY % (roomSize + connectorSize);
				if (Math.abs(yRemainder) > roomSize) {
					roomsOffsetY += Math.copySign(1, yRemainder);
					connOffsetY += Math.copySign(1, yRemainder) * (Math.abs(yRemainder) - roomSize) / connectorSize;
				} else {
					roomsOffsetY += yRemainder / roomSize;
				}
				if (deltaY < 0 && yRemainder != 0) {
					roomsOffsetY++;
					connOffsetY++;
				}

				float angle = (float) (vec4b.func_176111_d() * 360) / 16.0F;

				MapPosition pos = new MapPosition(roomsOffsetX, connOffsetX, roomsOffsetY, connOffsetY);
				pos.rotation = angle % 360;
				if (pos.rotation < 0) pos.rotation += 360;

				if (decorations++ <= 6) {
					positions.add(pos);
				}
				rawPlayerMarkerMapPositions.add(pos);
			}

			boolean different = playerMarkerMapPositions.size() != positions.size();

			if (!different) {
				for (MapPosition pos : playerMarkerMapPositions.values()) {
					if (!positions.contains(pos)) {
						different = true;
						break;
					}
				}
			}

			if (different && positions.size() > 0) {
				lastLastDecorationsMillis = lastDecorationsMillis;
				lastDecorationsMillis = System.currentTimeMillis();

				playerMarkerMapPositionsLast.clear();
				for (Map.Entry<String, MapPosition> entry : playerMarkerMapPositions.entrySet()) {
					playerMarkerMapPositionsLast.put(entry.getKey(), entry.getValue());
				}
				playerMarkerMapPositions.clear();

				Set<String> foundPlayers = new HashSet<>();
				for (Map.Entry<String, MapPosition> entry : playerEntityMapPositions.entrySet()) {
					playerMarkerMapPositions.put(entry.getKey(), entry.getValue());
					playerMarkerMapPositionsLast.put(entry.getKey(), entry.getValue());
					foundPlayers.add(entry.getKey());
				}

				HashMap<String, HashMap<Integer, Float>> distanceMap = new HashMap<>();
				for (Map.Entry<String, MapPosition> entry : playerMarkerMapPositionsLast.entrySet()) {
					HashMap<Integer, Float> deltaDists = new HashMap<>();
					for (int i = 0; i < positions.size(); i++) {
						float dx = entry.getValue().getRenderX(0) - positions.get(i).getRenderX(0);
						float dy = entry.getValue().getRenderY(0) - positions.get(i).getRenderY(0);
						deltaDists.put(i, dx * dx + dy * dy);
					}
					distanceMap.put(entry.getKey(), deltaDists);
				}

				List<String> playerList = new ArrayList<>(playerMarkerMapPositionsLast.keySet());
				List<List<String>> playerPermutations = permutations(playerList);

				List<Integer> finalUsedIndexes = new ArrayList<>();
				if (playerPermutations.size() > 0) {
					HashMap<String, Integer> smallestPermutation = null;
					float smallestTotalDistance = 0;

					for (List<String> permutation : playerPermutations) {
						HashMap<String, Integer> usedIndexes = new HashMap<>();

						float totalDistance = 0;
						for (String player : permutation) {
							int smallestIndex = -1;
							float smallestDist = 0;
							for (Map.Entry<Integer, Float> entry : distanceMap.get(player).entrySet()) {
								if (!usedIndexes.containsValue(entry.getKey())) {
									if (smallestIndex == -1 || entry.getValue() < smallestDist) {
										smallestIndex = entry.getKey();
										smallestDist = entry.getValue();
									}
								}
							}
							if (smallestIndex != -1) {
								usedIndexes.put(player, smallestIndex);
								totalDistance += smallestDist;
							}
						}

						if (smallestPermutation == null || totalDistance < smallestTotalDistance) {
							smallestPermutation = usedIndexes;
							smallestTotalDistance = totalDistance;
						}
					}

					//System.out.println("--- PERM START ---");
					for (Map.Entry<String, Integer> entry : smallestPermutation.entrySet()) {
						//System.out.println(entry.getKey() + ":" + entry.getValue() + " : Total dist: " + smallestTotalDistance);
						finalUsedIndexes.add(entry.getValue());
						playerMarkerMapPositions.put(entry.getKey(), positions.get(entry.getValue()));
					}
				}

				List<Integer> nonUsedIndexes = new ArrayList<>();
				for (int i = 0; i < positions.size(); i++) {
					if (!finalUsedIndexes.contains(i)) {
						nonUsedIndexes.add(i);
					}
				}

				for (String missingPlayer : playerNames) {
					if (!playerList.contains(missingPlayer)) {
						if (nonUsedIndexes.isEmpty()) break;
						playerMarkerMapPositions.put(missingPlayer, positions.get(nonUsedIndexes.get(0)));
						nonUsedIndexes.remove(0);
					}
				}
			}
		} else if (mapDecorations == null) {
			playerMarkerMapPositions.clear();
			playerMarkerMapPositionsLast.clear();

			for (Map.Entry<String, MapPosition> entry : playerEntityMapPositions.entrySet()) {
				playerMarkerMapPositions.put(entry.getKey(), entry.getValue());
			}
		}

		if (!roomMap.isEmpty() && startRoom.x >= 0 && startRoom.y >= 0) {
			render(centerX, centerY);
		}
	}

	@SubscribeEvent
	public void onWorldChange(WorldEvent.Load event) {
		// TODO: clear the map data when the location changes
	}

	@SubscribeEvent
	public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
		if (!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) return;
		if (event.type != RenderGameOverlayEvent.ElementType.ALL || !config.dmEnable) {
			return;
		}

		if (mc.gameSettings.showDebugInfo ||
			(mc.gameSettings.keyBindPlayerList.isKeyDown() &&
				(!mc.isIntegratedServerRunning() ||	mc.thePlayer.sendQueue.getPlayerInfoMap().size() > 1))) {
			return;
		}

		ItemStack mapSlotStack = mc.thePlayer.inventory.mainInventory[8];
		if (mapSlotStack == null) {
			return;
		}

		if (Item.getIdFromItem(mapSlotStack.getItem()) == NETHER_STAR_ITEM_ID) {
			//This should clear the map if you're in the dungeon boss room
			//so when you're holding a bow it doesn't show the map anymore
			this.currentColorMap.clear();
		}

		boolean holdingBow = mapSlotStack.getItem() == Items.arrow && this.currentColorMap.hasData();
		if (!holdingBow & !(mapSlotStack.getItem() instanceof ItemMap)) {
			return;
		}

		if (holdingBow) {
			// Set color map to all black if we have no data before the bow is equipped
			// TODO: try this out by holding a bow to make sure it's black
			if (!currentColorMap.hasData()) {
				currentColorMap.setColorData(new byte[128*128]); // defaults to 0
			}
		} else {
			ItemMap map = (ItemMap) mapSlotStack.getItem();
			MapData mapData = map.getMapData(mapSlotStack, mc.theWorld);
			if (mapData == null) return;
			currentColorMap.setColorData(mapData.colors);
		}

		// TODO: Figure out what this value is supposed to represent - it was 0 when called for the demo map
//		int roomSizeBlocks = 31;
		Set<String> playerNames = new HashSet<>();
		int players = 0;
		for (ScorePlayerTeam team : mc.thePlayer.getWorldScoreboard().getTeams()) {
			if (team.getTeamName().startsWith("a") && team.getMembershipCollection().size() == 1) {
				String teamPlayerName = Iterables.get(team.getMembershipCollection(), 0);
				playerNames.add(teamPlayerName);
				if (++players >= 6) break;
			}
		}

		Position mapPosFromConfig = config.dmPosition;

		int borderSize = 80 + Math.round(40 * config.dmBorderSize);
		ScaledResolution scaledResolution = Utils.pushGuiScale(2);
		renderMap(
			mapPosFromConfig.getAbsX(scaledResolution, borderSize / 2) + borderSize / 2,
			mapPosFromConfig.getAbsY(scaledResolution, borderSize / 2) + borderSize / 2,
			currentColorMap,
			playerNames,
			true,
			event.partialTicks
		);
		Utils.pushGuiScale(-1);
	}

	public List<List<String>> permutations(List<String> values) {
		List<List<String>> permutations = new ArrayList<>();

		if (values.size() == 1) {
			permutations.add(values);
			return permutations;
		}

		for (String first : values) {
			List<String> newList = new ArrayList<>();
			for (String val : values) {
				if (!val.equals(first)) {
					newList.add(val);
				}
			}

			for (List<String> list2 : permutations(newList)) {
				List<String> perm = new ArrayList<>();
				perm.add(first);
				perm.addAll(list2);
				permutations.add(perm);
			}
		}

		return permutations;
	}

	Shader blurShaderHorz = null;
	Framebuffer blurOutputHorz = null;
	Shader blurShaderVert = null;
	Framebuffer blurOutputVert = null;
}
