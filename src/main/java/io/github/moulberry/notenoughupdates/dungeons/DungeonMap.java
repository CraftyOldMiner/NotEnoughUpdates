package io.github.moulberry.notenoughupdates.dungeons;

import com.google.common.collect.Iterables;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.core.config.Position;
import io.github.moulberry.notenoughupdates.dungeons.ColorMap.ColoredArea;
import io.github.moulberry.notenoughupdates.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.Matrix4f;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;
import java.util.*;

import static io.github.moulberry.notenoughupdates.dungeons.DungeonMapConstants.*;

public class DungeonMap {
	private static DungeonMap instance = null;
	private static final Minecraft mc = Minecraft.getMinecraft();

	private ColorMap currentColorMap = new ColorMap();
	private ColoredArea startRoom = null;
	private int connectorSize = 5;
	private int roomSize = 0;

	private long lastDecorationsMillis = -1;
	private long lastLastDecorationsMillis = -1;

//	private final Map<String, MapPosition> playerEntityMapPositions = new HashMap<>();
//	private final Map<String, MapPosition> playerMarkerMapPositions = new HashMap<>();
//	private final Set<MapPosition> rawPlayerMarkerMapPositions = new HashSet<>();
//	private final Map<String, MapPosition> playerMarkerMapPositionsLast = new HashMap<>();
//	private final HashMap<String, Integer> playerIdMap = new HashMap<>();

//	private final Map<String, ResourceLocation> playerSkinMap = new HashMap<>();

	public static DungeonMap getInstance() {
		if (instance == null) {
			instance = new DungeonMap();
		}
		return instance;
	}
	public static Matrix4f projectionMatrix = null;


	@SubscribeEvent
	public void onWorldChange(WorldEvent.Load event) {
		// TODO: clear the map data when the location changes
	}

	@SubscribeEvent
	public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
		if (!NotEnoughUpdates.INSTANCE.hasSkyblockScoreboard()) return;
		if (event.type != RenderGameOverlayEvent.ElementType.ALL || !DungeonMapData.getInstance().shouldRender()) {
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

		Set<String> playerNames = new HashSet<>();
		int players = 0;
		for (ScorePlayerTeam team : mc.thePlayer.getWorldScoreboard().getTeams()) {
			if (team.getTeamName().startsWith("a") && team.getMembershipCollection().size() == 1) {
				String teamPlayerName = Iterables.get(team.getMembershipCollection(), 0);
				playerNames.add(teamPlayerName);
				if (++players >= 6) break;
			}
		}

		Position mapPosFromConfig = DungeonMapData.NeuConfigData.getMapPosition();

		int borderSize = 80 + Math.round(40 * DungeonMapData.NeuConfigData.getBorderSizeValue());
		ScaledResolution scaledResolution = Utils.pushGuiScale(2);
		ColorMapRenderer.getInstance().renderMap(
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
