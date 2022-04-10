package io.github.moulberry.notenoughupdates.dungeons;

import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.options.seperateSections.DungeonMapConfig;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec4b;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

public class DungeonMapData {
	private static DungeonMapData instance;
	public static DungeonMapData getInstance() {
		if (instance != null) return instance;
		instance = new DungeonMapData();
		return instance;
	}

	private static final DungeonMapConfig dmConfig = NotEnoughUpdates.INSTANCE.config.dungeonMap;

	private final ColorMap colorMap;
	private PlayerData playerData = null;
	private long lastRefreshMillis = 0;
	// TODO: move this stuff and anything else that depends on the Minecraft class into an interface
	private static final Minecraft mc = Minecraft.getMinecraft();
	DoubleSupplier getPlayerX = () -> Minecraft.getMinecraft().thePlayer.posX;
	DoubleSupplier getPlayerY = () -> Minecraft.getMinecraft().thePlayer.posY;
	DoubleSupplier getPlayerZ = () -> Minecraft.getMinecraft().thePlayer.posZ;

	public DungeonMapData() {
		this(new ColorMap());
	}

	public DungeonMapData(ColorMap colorMap) {
		this.colorMap = colorMap;
	}

	public PlayerData getPlayerData() {
		return this.playerData;
	}

	public PlayerData getPlayerDataImmutable() {
		return new PlayerData(this.playerData);
	}

	public Map<String, Vec4b> getMapDecorations() {
		return this.colorMap.getDecorations();
	}

	public void refreshDataIfNeeded() {
		if (hasChanged()) refreshData();
	}

	public void refreshData() {
		lastRefreshMillis = System.currentTimeMillis();
		playerData.setXYZ(getPlayerX.getAsDouble(), getPlayerY.getAsDouble(), getPlayerZ.getAsDouble());
		// TODO: Player list
		// TODO: Config
	}

	public boolean hasChanged() {
		// TODO: Player list
		// TODO: NeuConfigData
		PlayerData freshPlayerData = new PlayerData(getPlayerX.getAsDouble(),
			getPlayerY.getAsDouble(),
			getPlayerZ.getAsDouble());
		if (!this.playerData.equals(freshPlayerData) ||
			(lastRefreshMillis < colorMap.getLastUpdatedMillis()) ||
			(NeuConfigData.hasBeenUpdated(lastRefreshMillis)))
		{
			return true;
		}
		return false;
	}

	public boolean shouldRender() {
		return !dmConfig.dmEnable || mc.thePlayer == null || !colorMap.hasData();
	}

	public void saveMap() {
		colorMap.saveMapToPngFile("DungeonMap.png");
	}

	public String getCoordinateData() {
		PlayerData playerData = DungeonMapData.getInstance().getPlayerData();
		Collection<Vec4b> decorations = DungeonMapData.getInstance().getMapDecorations().values();

		StringBuilder sb = new StringBuilder();
		sb.append(EnumChatFormatting.YELLOW + "Player X Z Coordinates : ");
		sb.append(EnumChatFormatting.WHITE + String.format("%f %f\n", playerData.getX(), playerData.getZ()));

		sb.append(EnumChatFormatting.YELLOW + "Player Map Decoration: ");
		if (decorations != null) {
			for (Vec4b vec4b : decorations) {
				byte id = vec4b.func_176110_a();
				if (id != 1) continue;

				sb.append(EnumChatFormatting.WHITE + String.format("%d %d\n", vec4b.func_176112_b(), vec4b.func_176113_c()));
				break;
			}
		} else {
			sb.append(EnumChatFormatting.WHITE + "<NONE>");
		}
		return sb.toString();
	}

	public boolean isFloorOne() {
		Scoreboard scoreboard = mc.thePlayer.getWorldScoreboard();
		ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1);
		java.util.List<Score> scores = new ArrayList<>(scoreboard.getSortedScores(sidebarObjective));
		for (int i = scores.size() - 1; i >= 0; i--) {
			Score score = scores.get(i);
			ScorePlayerTeam scoreplayerteam1 = scoreboard.getPlayersTeam(score.getPlayerName());
			String line = ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score.getPlayerName());
			line = Utils.cleanColour(line);

			if (line.contains("(F1)") || line.contains("(E)") || line.contains("(M1)")) {
				return true;
			}
		}

		return false;
	}



	/**
	 * This class is for getting any data that is based on NEU configuration options.
	 */
	public static class NeuConfigData {
		private static final HashMap<Integer, Float> borderRadiusCache = new HashMap<>();
		private static long lastUpdateMillis = 0;

		public static int getRenderCompat() {
			return dmConfig.dmCompat;
		}

		// TODO: include this in update detection
		public static int getRenderRoomSize() {
			double roomSizeOption = dmConfig.dmRoomSize;
			if (roomSizeOption <= 0) return 12;
			return 12 + (int) Math.round(roomSizeOption * 4);
		}

		public static int getRenderConnSize() {
			int roomSizeOption = Math.round(dmConfig.dmRoomSize);
			if (roomSizeOption <= 0) return 3;
			return 3 + roomSizeOption;
		}

		public static boolean getCenterCheck() {
			return dmConfig.dmCenterCheck;
		}

		public static boolean getCenterPlayer() {
			return dmConfig.dmCenterPlayer;
		}

		public static int getPlayerHeads() {
			return dmConfig.dmPlayerHeads;
		}

		public static boolean getOrientCheck() {
			return dmConfig.dmCenterCheck;
		}

		public static boolean getRotatePlayer() {
			return dmConfig.dmRotatePlayer;
		}

		public static boolean getPlayerInterpolate() {
			return dmConfig.dmPlayerInterp;
		}

		public static String getBackgroundColor() {
			return dmConfig.dmBackgroundColour;
		}

		public static float getBackgroundBlur() {
			return dmConfig.dmBackgroundBlur;
		}

		public static float getIconScale() {
			return dmConfig.dmIconScale;
		}

		public static String getBorderSizeName() {
			int borderSizeConfig = Math.round(dmConfig.dmBorderSize);
			return borderSizeConfig == 0 ? "small" : borderSizeConfig == 2 ? "large" : "medium";
		}

		public static float getBorderSizeValue() {
			return dmConfig.dmBorderSize;
		}

		public static int getBorderStyle() {
			return dmConfig.dmBorderStyle;
		}

		public static String getBorderColor() {
			return dmConfig.dmBorderColour;
		}

		public static boolean getBorderChroma() {
			return dmConfig.dmChromaBorder;
		}

		// TODO: Actually review this, introduce a reresh method for this class that clears the cache
		public static float getBorderRadius() {
			String borderSizeName = NeuConfigData.getBorderSizeName();
			int borderStyle = NeuConfigData.getBorderStyle();

			if (borderRadiusCache.containsKey(borderStyle)) {
				return borderRadiusCache.get(borderStyle);
			}

			try (
				BufferedReader reader = new BufferedReader(new InputStreamReader(Minecraft
					.getMinecraft()
					.getResourceManager()
					.getResource(
						new ResourceLocation("notenoughupdates:dungeon_map/borders/" + borderSizeName + "/" + borderStyle + ".json"))
					.getInputStream(), StandardCharsets.UTF_8))
			) {
				JsonObject json = NotEnoughUpdates.INSTANCE.manager.gson.fromJson(reader, JsonObject.class);
				float radiusSq = json.get("radiusSq").getAsFloat();

				borderRadiusCache.put(borderStyle, radiusSq);
				return radiusSq;
			} catch (Exception ignored) {
			}

			borderRadiusCache.put(borderStyle, 1f);
			return 1f;
		}

		public static boolean hasBeenUpdated(long sinceMillis) {
			// TODO : code this based on any of the config changing
			long currentTimeMillis = System.currentTimeMillis();
			if  (currentTimeMillis > lastUpdateMillis + 5000) {
				lastUpdateMillis = currentTimeMillis;
				return true;
			}

			return false;
		}
	}

	public static class PlayerData {
		private double x;
		private double y;
		private double z;

		public PlayerData(PlayerData other) {
			other.x = this.x;
			other.y = this.y;
			other.z = this.z;
		}

		public PlayerData(double x, double y, double z) {
			this.setXYZ(x, y, z);
		}

		public double getX() {
			return x;
		}

		public double getY() {
			return y;
		}

		public double getZ() {
			return z;
		}

		public void setX(double x) {
			this.x = x;
		}

		public void setY(double y) {
			this.y = y;
		}

		public void setZ(double z) {
			this.z = z;
		}

		public void setXYZ(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public BlockPos getAsBlockPos() {
			return new BlockPos(x, y, z);
		}

		public boolean equals(PlayerData other) {
			return
				this.x == other.x &&
				this.y == other.y &&
				this.z == other.z;
		}

		public int hashCode() {
			return 31 * Double.hashCode(x) + Double.hashCode(y) + Double.hashCode(z);
		}
	}
}
