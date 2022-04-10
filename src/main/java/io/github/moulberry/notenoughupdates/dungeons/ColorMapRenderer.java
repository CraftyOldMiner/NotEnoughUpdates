package io.github.moulberry.notenoughupdates.dungeons;

import io.github.moulberry.notenoughupdates.core.BackgroundBlur;
import io.github.moulberry.notenoughupdates.dungeons.ColorMap.ColoredAreaStatus;
import io.github.moulberry.notenoughupdates.util.NEUResourceManager;
import io.github.moulberry.notenoughupdates.util.SpecialColour;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.util.Matrix4f;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.HashMap;
import java.util.Set;

import static io.github.moulberry.notenoughupdates.dungeons.ColorMap.*;
import static io.github.moulberry.notenoughupdates.dungeons.DungeonMapConstants.ResourceLocs.*;
import static io.github.moulberry.notenoughupdates.dungeons.DungeonMapData.NeuConfigData.*;

public class ColorMapRenderer {
	Minecraft mc = Minecraft.getMinecraft();
	private DungeonMapData dungeonMapData;
	public static Framebuffer mapFramebuffer1 = null;
	public static Framebuffer mapFramebuffer2 = null;
	public static Matrix4f projectionMatrix = null;
	public static Shader mapShader = null;

	public ColorMapRenderer() {
		this(DungeonMapData.getInstance());
	}

	public ColorMapRenderer(DungeonMapData dungeonMapData) {
		this.dungeonMapData = dungeonMapData;
	}

	public void renderMap(
		int centerX, int centerY, ColorMap colorMap, Set<String> playerNames,
		boolean usePlayerPositions, float partialTicks
	) {
		if (!dungeonMapData.shouldRender()) return;
		dungeonMapData.refreshDataIfNeeded();

		float borderSize = DungeonMapData.NeuConfigData.getBorderSizeValue();
		String borderColor = DungeonMapData.NeuConfigData.getBorderColor()
		float backgroundBlur = DungeonMapData.NeuConfigData.getBackgroundBlur();
		boolean useFb = DungeonMapData.NeuConfigData.getRenderCompat() <= 1 && OpenGlHelper.isFramebufferEnabled();
		boolean useShd = DungeonMapData.NeuConfigData.getRenderCompat() <= 0 && OpenGlHelper.areShadersSupported();

		boolean searchForPlayers = false;

		// TODO: grab the connector size from the the connector in the ColorMap
		int connectorSize = 4;

		// TODO: get the playerNames from DungeonMapData, have it refresh them as-needed

		// TODO: handle player positions
//		playerEntityMapPositions.clear();
//		if (usePlayerPositions) {
//			for (String playerName : playerNames) {
//				if (playerIdMap.containsKey(playerName)) {
//					Entity entity = mc.theWorld.getEntityByID(playerIdMap.get(playerName));
//					if (entity instanceof EntityPlayer) {
//						EntityPlayer player = (EntityPlayer) entity;
//
//						float roomX = (float) player.posX / (roomSizeBlocks + 1);
//						float roomY = (float) player.posZ / (roomSizeBlocks + 1);
//
//						float playerRoomOffsetX = (float) Math.floor(roomX);
//						float playerConnOffsetX = (float) Math.floor(roomX);
//						float playerRoomOffsetY = (float) Math.floor(roomY);
//						float playerConnOffsetY = (float) Math.floor(roomY);
//
//						float roomXInBlocks = (float) player.posX % (roomSizeBlocks + 1);
//						if (roomXInBlocks < 2) { //0,1
//							playerConnOffsetX -= 2 / 5f - roomXInBlocks / 5f;
//						} else if (roomXInBlocks > roomSizeBlocks - 2) { //31,30,29
//							playerRoomOffsetX++;
//							playerConnOffsetX += (roomXInBlocks - (roomSizeBlocks - 2)) / 5f;
//						} else {
//							playerRoomOffsetX += (roomXInBlocks - 2) / (roomSizeBlocks - 4);
//						}
//
//						float roomYInBlocks = (float) player.posZ % (roomSizeBlocks + 1);
//						if (roomYInBlocks < 2) { //0,1
//							playerConnOffsetY -= 2 / 5f - roomYInBlocks / 5f;
//						} else if (roomYInBlocks > roomSizeBlocks - 2) { //31,30,29
//							playerRoomOffsetY++;
//							playerConnOffsetY += (roomYInBlocks - (roomSizeBlocks - 2)) / 5f;
//						} else {
//							playerRoomOffsetY += (roomYInBlocks - 2) / (roomSizeBlocks - 4);
//						}
//
//						playerRoomOffsetX -= startRoom.x / (roomSize + connectorSize);
//						playerRoomOffsetY -= startRoom.y / (roomSize + connectorSize);
//						playerConnOffsetX -= startRoom.x / (roomSize + connectorSize);
//						playerConnOffsetY -= startRoom.y / (roomSize + connectorSize);
//
//						DungeonMap.MapPosition pos = new DungeonMap.MapPosition(
//							playerRoomOffsetX,
//							playerConnOffsetX,
//							playerRoomOffsetY,
//							playerConnOffsetY
//						);
//						pos.rotation =
//							(player.prevRotationYawHead + (player.rotationYawHead - player.prevRotationYawHead) * partialTicks) % 360;
//						if (pos.rotation < 0) pos.rotation += 360;
//						playerEntityMapPositions.put(player.getName(), pos);
//					}
//				}
//			}
//		}

		// TODO: handle map decorations
//		Map<String, Vec4b> mapDecorations = DungeonMapData.getInstance().getMapDecorations();
//		if (mapDecorations != null && mapDecorations.size() > 0) {
//			java.util.List<DungeonMap.MapPosition> positions = new ArrayList<>();
//			int decorations = 0;
//			for (Vec4b vec4b : mapDecorations.values()) {
//				byte id = vec4b.func_176110_a();
//				if (id != 1 && id != 3) continue;
//
//				float x = (float) vec4b.func_176112_b() / 2.0F + 64.0F;
//				float y = (float) vec4b.func_176113_c() / 2.0F + 64.0F;
//
//				if (x < 0 || y < 0 || x > 128 || y > 128) {
//					continue;
//				}
//
//				float deltaX = x - startRoom.x;
//				float deltaY = y - startRoom.y;
//
//				float roomsOffsetX = (int) Math.floor(deltaX / (roomSize + connectorSize));
//				float connOffsetX = (int) Math.floor(deltaX / (roomSize + connectorSize));
//				float xRemainder = deltaX % (roomSize + connectorSize);
//				if (Math.abs(xRemainder) > roomSize) {
//					roomsOffsetX += Math.copySign(1, xRemainder);
//					connOffsetX += Math.copySign(1, xRemainder) * (Math.abs(xRemainder) - roomSize) / connectorSize;
//				} else {
//					roomsOffsetX += xRemainder / roomSize;
//				}
//				if (deltaX < 0 && xRemainder != 0) {
//					roomsOffsetX++;
//					connOffsetX++;
//				}
//				float roomsOffsetY = (int) Math.floor(deltaY / (roomSize + connectorSize));
//				float connOffsetY = (int) Math.floor(deltaY / (roomSize + connectorSize));
//				float yRemainder = deltaY % (roomSize + connectorSize);
//				if (Math.abs(yRemainder) > roomSize) {
//					roomsOffsetY += Math.copySign(1, yRemainder);
//					connOffsetY += Math.copySign(1, yRemainder) * (Math.abs(yRemainder) - roomSize) / connectorSize;
//				} else {
//					roomsOffsetY += yRemainder / roomSize;
//				}
//				if (deltaY < 0 && yRemainder != 0) {
//					roomsOffsetY++;
//					connOffsetY++;
//				}
//
//				float angle = (float) (vec4b.func_176111_d() * 360) / 16.0F;
//
//				DungeonMap.MapPosition pos = new DungeonMap.MapPosition(roomsOffsetX, connOffsetX, roomsOffsetY, connOffsetY);
//				pos.rotation = angle % 360;
//				if (pos.rotation < 0) pos.rotation += 360;
//
//				if (decorations++ <= 6) {
//					positions.add(pos);
//				}
//				rawPlayerMarkerMapPositions.add(pos);
//			}
//
//			boolean different = playerMarkerMapPositions.size() != positions.size();
//
//			if (!different) {
//				for (DungeonMap.MapPosition pos : playerMarkerMapPositions.values()) {
//					if (!positions.contains(pos)) {
//						different = true;
//						break;
//					}
//				}
//			}
//
//			if (different && positions.size() > 0) {
//				lastLastDecorationsMillis = lastDecorationsMillis;
//				lastDecorationsMillis = System.currentTimeMillis();
//
//				playerMarkerMapPositionsLast.clear();
//				for (Map.Entry<String, DungeonMap.MapPosition> entry : playerMarkerMapPositions.entrySet()) {
//					playerMarkerMapPositionsLast.put(entry.getKey(), entry.getValue());
//				}
//				playerMarkerMapPositions.clear();
//
//				Set<String> foundPlayers = new HashSet<>();
//				for (Map.Entry<String, DungeonMap.MapPosition> entry : playerEntityMapPositions.entrySet()) {
//					playerMarkerMapPositions.put(entry.getKey(), entry.getValue());
//					playerMarkerMapPositionsLast.put(entry.getKey(), entry.getValue());
//					foundPlayers.add(entry.getKey());
//				}
//
//				HashMap<String, HashMap<Integer, Float>> distanceMap = new HashMap<>();
//				for (Map.Entry<String, DungeonMap.MapPosition> entry : playerMarkerMapPositionsLast.entrySet()) {
//					HashMap<Integer, Float> deltaDists = new HashMap<>();
//					for (int i = 0; i < positions.size(); i++) {
//						float dx = entry.getValue().getRenderX(0) - positions.get(i).getRenderX(0);
//						float dy = entry.getValue().getRenderY(0) - positions.get(i).getRenderY(0);
//						deltaDists.put(i, dx * dx + dy * dy);
//					}
//					distanceMap.put(entry.getKey(), deltaDists);
//				}
//
//				java.util.List<String> playerList = new ArrayList<>(playerMarkerMapPositionsLast.keySet());
//				java.util.List<java.util.List<String>> playerPermutations = permutations(playerList);
//
//				java.util.List<Integer> finalUsedIndexes = new ArrayList<>();
//				if (playerPermutations.size() > 0) {
//					HashMap<String, Integer> smallestPermutation = null;
//					float smallestTotalDistance = 0;
//
//					for (java.util.List<String> permutation : playerPermutations) {
//						HashMap<String, Integer> usedIndexes = new HashMap<>();
//
//						float totalDistance = 0;
//						for (String player : permutation) {
//							int smallestIndex = -1;
//							float smallestDist = 0;
//							for (Map.Entry<Integer, Float> entry : distanceMap.get(player).entrySet()) {
//								if (!usedIndexes.containsValue(entry.getKey())) {
//									if (smallestIndex == -1 || entry.getValue() < smallestDist) {
//										smallestIndex = entry.getKey();
//										smallestDist = entry.getValue();
//									}
//								}
//							}
//							if (smallestIndex != -1) {
//								usedIndexes.put(player, smallestIndex);
//								totalDistance += smallestDist;
//							}
//						}
//
//						if (smallestPermutation == null || totalDistance < smallestTotalDistance) {
//							smallestPermutation = usedIndexes;
//							smallestTotalDistance = totalDistance;
//						}
//					}
//
//					//System.out.println("--- PERM START ---");
//					for (Map.Entry<String, Integer> entry : smallestPermutation.entrySet()) {
//						//System.out.println(entry.getKey() + ":" + entry.getValue() + " : Total dist: " + smallestTotalDistance);
//						finalUsedIndexes.add(entry.getValue());
//						playerMarkerMapPositions.put(entry.getKey(), positions.get(entry.getValue()));
//					}
//				}
//
//				List<Integer> nonUsedIndexes = new ArrayList<>();
//				for (int i = 0; i < positions.size(); i++) {
//					if (!finalUsedIndexes.contains(i)) {
//						nonUsedIndexes.add(i);
//					}
//				}
//
//				for (String missingPlayer : playerNames) {
//					if (!playerList.contains(missingPlayer)) {
//						if (nonUsedIndexes.isEmpty()) break;
//						playerMarkerMapPositions.put(missingPlayer, positions.get(nonUsedIndexes.get(0)));
//						nonUsedIndexes.remove(0);
//					}
//				}
//			}
//		}
//		else if (mapDecorations == null) {
//			playerMarkerMapPositions.clear();
//			playerMarkerMapPositionsLast.clear();
//
//			for (Map.Entry<String, DungeonMap.MapPosition> entry : playerEntityMapPositions.entrySet()) {
//				playerMarkerMapPositions.put(entry.getKey(), entry.getValue());
//			}
//		}

		// TODO: determine if the stuff after this point should be in a separate method
		//render(centerX, centerY);

		ScaledResolution scaledResolution = Utils.pushGuiScale(2);

		int minRoomX = 999;
		int minRoomY = 999;
		int maxRoomX = -999;
		int maxRoomY = -999;
		for (DungeonMap.RoomOffset offset : roomMap.keySet()) {
			minRoomX = Math.min(offset.x, minRoomX);
			minRoomY = Math.min(offset.y, minRoomY);
			maxRoomX = Math.max(offset.x, maxRoomX);
			maxRoomY = Math.max(offset.y, maxRoomY);
		}

		int borderSizeOption = Math.round(borderSize);

		int renderRoomSize = getRenderRoomSize();
		int renderConnSize = DungeonMapData.NeuConfigData.getRenderConnSize();

		// handle playerPos
//		DungeonMap.MapPosition playerPos = null;
//		if (playerEntityMapPositions.containsKey(mc.thePlayer.getName())) {
//			playerPos = playerEntityMapPositions.get(mc.thePlayer.getName());
//		} else if (playerMarkerMapPositions.containsKey(mc.thePlayer.getName())) {
//			playerPos = playerMarkerMapPositions.get(mc.thePlayer.getName());
//		}
//
		int rotation = 180;
//		if (playerPos != null && DungeonMapData.NeuConfigData.getRotatePlayer()) {
//			rotation = (int) playerPos.rotation;
//		}

		int mapSizeX;
		int mapSizeY;
		if (DungeonMapData.NeuConfigData.getBorderStyle() <= 1) {
			mapSizeX = 80 + Math.round(40 * borderSize);
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
			SpecialColour.specialToChromaRGB(DungeonMapData.NeuConfigData.getBackgroundColor());

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

				if (backgroundBlur > 0.1 &&	backgroundBlur < 100) {
					GlStateManager.translate(-centerX + mapSizeX / 2, -centerY + mapSizeY / 2, 0);
					BackgroundBlur.renderBlurredBackground(backgroundBlur,
						scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(),
						centerX - mapSizeX / 2, centerY - mapSizeY / 2, mapSizeX, mapSizeY
					);
					BackgroundBlur.markDirty();
					GlStateManager.translate(centerX - mapSizeX / 2, centerY - mapSizeY / 2, 0);
				}

				GlStateManager.translate(mapCenterX, mapCenterY, 10);

				if (!useFb || backgroundBlur > 0.1 &&	backgroundBlur < 100) {
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

				// TODO: implement this
//				if (getCenterPlayer() && playerPos != null) {
//					float x = playerPos.getRenderX(0);
//					float y = playerPos.getRenderY(0);
//					x -= minRoomX * (renderRoomSize + renderConnSize);
//					y -= minRoomY * (renderRoomSize + renderConnSize);
//
//					GlStateManager.translate(-x, -y, 0);
//				} else {
					GlStateManager.translate(-roomsSizeX / 2, -roomsSizeY / 2, 0);
//				}

				for (ColoredArea area : colorMap.getRooms()) {
					DungeonMap.RoomOffset roomOffset = entry.getKey();
					int x = (roomOffset.x - minRoomX) * (renderRoomSize + renderConnSize);
					int y = (roomOffset.y - minRoomY) * (renderRoomSize + renderConnSize);
					GlStateManager.pushMatrix();
					GlStateManager.translate(x, y, 0);
					// TODO: Fix the fillcorner param if needed
					AreaRenderer.render(area, roomsSizeX, connectorSize, false);
					GlStateManager.translate(-x, -y, 0);
					GlStateManager.popMatrix();
				}

				GlStateManager.translate(-mapCenterX + roomsSizeX / 2f, -mapCenterY + roomsSizeY / 2f, 0);

				GlStateManager.translate(mapCenterX, mapCenterY, 0);
				GlStateManager.rotate(rotation - 180, 0, 0, 1);
				GlStateManager.translate(-mapCenterX, -mapCenterY, 0);

				GlStateManager.translate(mapCenterX, mapCenterY, 0);

				for (ColoredArea area : colorMap.getRooms()) {
					DungeonMap.RoomOffset roomOffset = entry.getKey();
					float x =
						(roomOffset.x - minRoomX) * (renderRoomSize + renderConnSize) - roomsSizeX / 2f + renderRoomSize / 2f;
					float y =
						(roomOffset.y - minRoomY) * (renderRoomSize + renderConnSize) - roomsSizeY / 2f + renderRoomSize / 2f;
					float x2 = (float) (-x * Math.cos(Math.toRadians(-rotation)) + y * Math.sin(Math.toRadians(-rotation)));
					float y2 = (float) (-x * Math.sin(Math.toRadians(-rotation)) - y * Math.cos(Math.toRadians(-rotation)));
					GlStateManager.pushMatrix();
					GlStateManager.translate(x2, y2, 0);
					GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
					// TODO: Fix the fillcorner param if needed
					AreaRenderer.renderIndicator(area, rotation);
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

				// TODO: Implement this
//				for (Map.Entry<String, DungeonMap.MapPosition> entry : playerMarkerMapPositions.entrySet()) {
//					String name = entry.getKey();
//					DungeonMap.MapPosition pos = entry.getValue();
//					float x = pos.getRenderX(0);
//					float y = pos.getRenderY(0);
//					float angle = pos.rotation;
//
//					boolean doInterp = getPlayerInterpolate();
//					if (!isFloorOne && playerEntityMapPositions.containsKey(name)) {
//						DungeonMap.MapPosition entityPos = playerEntityMapPositions.get(name);
//						angle = entityPos.rotation;
//
//						float deltaX = entityPos.getRenderX(9) - pos.getRenderX(0);
//						float deltaY = entityPos.getRenderY(9) - pos.getRenderY(0);
//
//						x += deltaX;
//						y += deltaY;
//
//						doInterp = false;
//					}
//
//					float minU = 3 / 4f;
//					float minV = 0;
//
//					if (name.equals(mc.thePlayer.getName())) {
//						minU = 1 / 4f;
//					}
//
//					float maxU = minU + 1 / 4f;
//					float maxV = minV + 1 / 4f;
//
//					if (doInterp && playerMarkerMapPositionsLast.containsKey(name)) {
//						DungeonMap.MapPosition last = playerMarkerMapPositionsLast.get(name);
//						float xLast = last.getRenderX(0);
//						float yLast = last.getRenderY(0);
//
//						float distSq = (x - xLast) * (x - xLast) + (y - yLast) * (y - yLast);
//						if (distSq < renderRoomSize * renderRoomSize / 4f) {
//							float angleLast = last.rotation;
//							if (angle > 180 && angleLast < 180) angleLast += 360;
//							if (angleLast > 180 && angle < 180) angle += 360;
//
//							float interpFactor = Math.round((System.currentTimeMillis() - lastDecorationsMillis) * 100f) / 100f /
//								(lastDecorationsMillis - lastLastDecorationsMillis);
//							interpFactor = Math.max(0, Math.min(1, interpFactor));
//
//							x = xLast + (x - xLast) * interpFactor;
//							y = yLast + (y - yLast) * interpFactor;
//							angle = angleLast + (angle - angleLast) * interpFactor;
//							angle %= 360;
//						}
//					}
//
//					boolean blackBorder = false;
//					boolean headLayer = false;
//					int pixelWidth = 8;
//					int pixelHeight = 8;
//					if (renderRoomSize >= 24) {
//						pixelWidth = pixelHeight = 12;
//					}
//					GlStateManager.color(1, 1, 1, 1);
//					if ((!NotEnoughUpdates.INSTANCE.config.dungeons.showOwnHeadAsMarker ||
//						playerMarkerMapPositions.size() <= 1 || minU != 1 / 4f) &&
//						getPlayerHeads() >= 1 &&
//						playerSkinMap.containsKey(entry.getKey())) {
//						mc.getTextureManager().bindTexture(playerSkinMap.get(entry.getKey()));
//
//						minU = 8 / 64f;
//						minV = 8 / 64f;
//						maxU = 16 / 64f;
//						maxV = 16 / 64f;
//
//						headLayer = true;
//						if (getPlayerHeads() >= 2) {
//							blackBorder = true;
//						}
//					} else {
//						mc.getTextureManager().bindTexture(mapIcons);
//					}
//
//					x -= minRoomX * (renderRoomSize + renderConnSize);
//					y -= minRoomY * (renderRoomSize + renderConnSize);
//
//					GlStateManager.pushMatrix();
//
//					GlStateManager.disableDepth();
//					GlStateManager.enableBlend();
//					GL14.glBlendFuncSeparate(
//						GL11.GL_SRC_ALPHA,
//						GL11.GL_ONE_MINUS_SRC_ALPHA,
//						GL11.GL_ONE,
//						GL11.GL_ONE_MINUS_SRC_ALPHA
//					);
//
//					GlStateManager.translate(x, y, -0.02F);
//					GlStateManager.scale(getIconScale(), getIconScale(), 1);
//					GlStateManager.rotate(angle, 0.0F, 0.0F, 1.0F);
//					GlStateManager.translate(-0.5F, 0.5F, 0.0F);
//
//					if (blackBorder) {
//						Gui.drawRect(
//							-pixelWidth / 2 - 1,
//							-pixelHeight / 2 - 1,
//							pixelWidth / 2 + 1,
//							pixelHeight / 2 + 1,
//							0xff111111
//						);
//						GlStateManager.color(1, 1, 1, 1);
//					}
//
//					worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
//					worldrenderer.pos(-pixelWidth / 2f, pixelHeight / 2f, 30 + ((float) k * -0.005F)).tex(minU, minV).endVertex();
//					worldrenderer.pos(pixelWidth / 2f, pixelHeight / 2f, 30 + ((float) k * -0.005F)).tex(maxU, minV).endVertex();
//					worldrenderer.pos(pixelWidth / 2f, -pixelHeight / 2f, 30 + ((float) k * -0.005F)).tex(maxU, maxV).endVertex();
//					worldrenderer
//						.pos(-pixelWidth / 2f, -pixelHeight / 2f, 30 + ((float) k * -0.005F))
//						.tex(minU, maxV)
//						.endVertex();
//					tessellator.draw();
//
//					if (headLayer) {
//						worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
//						worldrenderer.pos(-pixelWidth / 2f, pixelHeight / 2f, 30 + ((float) k * -0.005F) + 0.001f).tex(
//							minU + 0.5f,
//							minV
//						).endVertex();
//						worldrenderer.pos(pixelWidth / 2f, pixelHeight / 2f, 30 + ((float) k * -0.005F) + 0.001f).tex(
//							maxU + 0.5f,
//							minV
//						).endVertex();
//						worldrenderer.pos(pixelWidth / 2f, -pixelHeight / 2f, 30 + ((float) k * -0.005F) + 0.001f).tex(
//							maxU + 0.5f,
//							maxV
//						).endVertex();
//						worldrenderer.pos(-pixelWidth / 2f, -pixelHeight / 2f, 30 + ((float) k * -0.005F) + 0.001f).tex(
//							minU + 0.5f,
//							maxV
//						).endVertex();
//						tessellator.draw();
//					}
//					GlStateManager.popMatrix();
//					k--;
//				}

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
							uploadShader(mapShader, mapSizeX, mapSizeY, scaleFactor, DungeonMapData.NeuConfigData.getBorderRadius());
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

			if (DungeonMapData.NeuConfigData.getBorderChroma()) {
				int colour = SpecialColour.specialToChromaRGB(borderColor);

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
					SpecialColour.specialToChromaRGB(borderColor)
				); //left
				Gui.drawRect(mapCenterX, -mapCenterY, mapCenterX + 2, mapCenterY,
					SpecialColour.specialToChromaRGB(borderColor)
				); //right
				Gui.drawRect(-mapCenterX - 2, -mapCenterY - 2, mapCenterX + 2, -mapCenterY,
					SpecialColour.specialToChromaRGB(borderColor)
				); //top
				Gui.drawRect(-mapCenterX - 2, mapCenterY, mapCenterX + 2, mapCenterY + 2,
					SpecialColour.specialToChromaRGB(borderColor)
				); //bottom
			}

			String sizeId = borderSizeOption == 0 ? "small" : borderSizeOption == 2 ? "large" : "medium";

			ResourceLocation rl = new ResourceLocation("notenoughupdates:dungeon_map/borders/" + sizeId + "/" +
				getBorderStyle() + ".png");
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

	private static void uploadShader(Shader shader, int width, int height, int scale, float radiusSq) {
		if (shader == null) return;
		shader.getShaderManager().getShaderUniformOrDefault("ProjMat").set(projectionMatrix);
		shader.getShaderManager().getShaderUniformOrDefault("InSize").set(width * scale, height * scale);
		shader.getShaderManager().getShaderUniformOrDefault("OutSize").set(width, height);
		shader.getShaderManager().getShaderUniformOrDefault("ScreenSize").set((float) width, (float) height);
		shader.getShaderManager().getShaderUniformOrDefault("radiusSq").set(radiusSq);
	}

	public static final HashMap<ColoredAreaStatus, ResourceLocation> AREA_STATUS_TEXTURES =
		new HashMap<ColoredAreaStatus, ResourceLocation>() {{
			put(ColoredAreaStatus.FAILED, CROSS);
			put(ColoredAreaStatus.PARTIALLY_COMPLETE, WHITE_CHECK_INDICATOR);
			put(ColoredAreaStatus.FULLY_COMPLETED, GREEN_CHECK);
			put(ColoredAreaStatus.QUESTION, QUESTION);
		}};

	public static final HashMap<DungeonMapColor, ResourceLocation> ROOM_TEXTURES =
		new HashMap<DungeonMapColor, ResourceLocation>() {{
			put(DungeonMapColor.REGULAR_ROOM, ROOM_BROWN);
			put(DungeonMapColor.UNKNOWN_ROOM, ROOM_GRAY);
			put(DungeonMapColor.START_ROOM, ROOM_GREEN);
			put(DungeonMapColor.FAIRY_ROOM, ROOM_PINK);
			put(DungeonMapColor.PUZZLE_ROOM, ROOM_PURPLE);
			put(DungeonMapColor.WATCHER_ROOM, ROOM_RED);
			put(DungeonMapColor.MINI_BOSS_ROOM, ROOM_YELLOW);
			put(DungeonMapColor.TRAP_ROOM, ROOM_ORANGE);
		}};

	private static class AreaRenderer {
		private Minecraft mc = Minecraft.getMinecraft();
		ColoredArea area;

		public AreaRenderer(ColoredArea area) {
			this.area = area;
		}

		public static void renderIndicator(ColoredArea area, int rotation) {
			float scale = DungeonMapData.NeuConfigData.getIconScale();
			boolean orientCheck = DungeonMapData.NeuConfigData.getOrientCheck();
			ResourceLocation indicatorTex = AREA_STATUS_TEXTURES.get(area.status);

			Minecraft.getMinecraft().getTextureManager().bindTexture(indicatorTex);

			float x = 0;
			float y = 0;

			if (DungeonMapData.NeuConfigData.getCenterCheck()) {
				// TODO: understand and do these adjustments
//				if (fillCorner) {
//					x += -(roomSize + connectorSize) / 2f * Math.cos(Math.toRadians(rotation - 45)) * 1.414f;
//					y += (roomSize + connectorSize) / 2f * Math.sin(Math.toRadians(rotation - 45)) * 1.414;
//				}
//				if (down.type == DungeonMap.RoomConnectionType.ROOM_DIVIDER && right.type != DungeonMap.RoomConnectionType.ROOM_DIVIDER) {
//					x += -(roomSize + connectorSize) / 2f * Math.sin(Math.toRadians(rotation));
//					y += -(roomSize + connectorSize) / 2f * Math.cos(Math.toRadians(rotation));
//				} else if (down.type != DungeonMap.RoomConnectionType.ROOM_DIVIDER && right.type == DungeonMap.RoomConnectionType.ROOM_DIVIDER) {
//					x += -(roomSize + connectorSize) / 2f * Math.cos(Math.toRadians(rotation));
//					y += (roomSize + connectorSize) / 2f * Math.sin(Math.toRadians(rotation));
//				}
			}
			GlStateManager.translate(x, y, 0);
			if (!orientCheck) {
				GlStateManager.rotate(-rotation + 180, 0, 0, 1);
			}

			GlStateManager.pushMatrix();
			GlStateManager.scale(scale, scale, 1);
			Utils.drawTexturedRect(-5, -5, 10, 10, GL11.GL_NEAREST);
			GlStateManager.popMatrix();

			if (orientCheck) {
				GlStateManager.rotate(rotation - 180, 0, 0, 1);
			}
			GlStateManager.translate(-x, -y, 0);
		}

		public static void render(ColoredArea area, int roomSize, int connectorSize, boolean fillCorner) {
			ResourceLocation roomTex = ROOM_TEXTURES.get(area.colorIndex);
			if (roomTex != null) {
				Minecraft.getMinecraft().getTextureManager().bindTexture(roomTex);
				GlStateManager.color(1, 1, 1, 1);
				Utils.drawTexturedRect(0, 0, roomSize, roomSize, GL11.GL_LINEAR);
			} else {
				// NOTE: TODO: Check if MapColor.blackColor not being  pure black that was previously used is a problem
				Gui.drawRect(0, 0, roomSize, roomSize, MapColor.blackColor.colorValue);
			}

			if (fillCorner) {
				GlStateManager.color(1, 1, 1, 1);
				Minecraft.getMinecraft().getTextureManager().bindTexture(CORNER_BROWN);
				Utils.drawTexturedRect(roomSize, roomSize, connectorSize, connectorSize, GL11.GL_NEAREST);
			}

			// TODO: draw connectors
//			for (int k = 0; k < 2; k++) {
//				DungeonMap.RoomConnection connection = down;
//				if (k == 1) connection = right;
//
//				if (connection.type == DungeonMap.RoomConnectionType.NONE || connection.type == DungeonMap.RoomConnectionType.WALL) continue;
//
//				ResourceLocation corridorTex = null;
//				if (connection.colour.getRed() == 114 && connection.colour.getGreen() == 67 &&
//					connection.colour.getBlue() == 27) {
//					corridorTex = connection.type == DungeonMap.RoomConnectionType.CORRIDOR ? CORRIDOR_BROWN : DIVIDER_BROWN;
//				} else if (connection.colour.getRed() == 65 && connection.colour.getGreen() == 65 &&
//					connection.colour.getBlue() == 65) {
//					corridorTex = CORRIDOR_GRAY;
//				} else if (connection.colour.getRed() == 0 && connection.colour.getGreen() == 124 &&
//					connection.colour.getBlue() == 0) {
//					corridorTex = CORRIDOR_GREEN;
//				} else if (connection.colour.getRed() == 242 && connection.colour.getGreen() == 127 &&
//					connection.colour.getBlue() == 165) {
//					corridorTex = CORRIDOR_PINK;
//				} else if (connection.colour.getRed() == 178 && connection.colour.getGreen() == 76 &&
//					connection.colour.getBlue() == 216) {
//					corridorTex = CORRIDOR_PURPLE;
//				} else if (connection.colour.getRed() == 255 && connection.colour.getGreen() == 0 &&
//					connection.colour.getBlue() == 0) {
//					corridorTex = CORRIDOR_RED;
//				} else if (connection.colour.getRed() == 229 && connection.colour.getGreen() == 229 &&
//					connection.colour.getBlue() == 51) {
//					corridorTex = CORRIDOR_YELLOW;
//				} else if (connection.colour.getRed() == 216 && connection.colour.getGreen() == 127 &&
//					connection.colour.getBlue() == 51) {
//					corridorTex = CORRIDOR_ORANGE;
//				}
//
//				if (corridorTex == null) {
//					int xOffset = 0;
//					int yOffset = 0;
//					int width = 0;
//					int height = 0;
//
//					if (connection == right) {
//						xOffset = roomSize;
//						width = connectorSize;
//						height = roomSize;
//
//						if (connection.type == DungeonMap.RoomConnectionType.CORRIDOR) {
//							height = 8;
//							yOffset += 4;
//						}
//					} else if (connection == down) {
//						yOffset = roomSize;
//						width = roomSize;
//						height = connectorSize;
//
//						if (connection.type == DungeonMap.RoomConnectionType.CORRIDOR) {
//							width = 8;
//							xOffset += 4;
//						}
//					}
//
//					Gui.drawRect(xOffset, yOffset, xOffset + width, yOffset + height, connection.colour.getRGB());
//				} else {
//					GlStateManager.color(1, 1, 1, 1);
//					mc.getTextureManager().bindTexture(corridorTex);
//					GlStateManager.pushMatrix();
//					if (connection == right) {
//						GlStateManager.translate(roomSize / 2f, roomSize / 2f, 0);
//						GlStateManager.rotate(-90, 0, 0, 1);
//						GlStateManager.translate(-roomSize / 2f, -roomSize / 2f, 0);
//					}
//					Utils.drawTexturedRect(0, roomSize, roomSize, connectorSize, GL11.GL_NEAREST);
//					GlStateManager.popMatrix();
//				}
//			}
		}
	}
}
