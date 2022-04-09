package io.github.moulberry.notenoughupdates.commands.dungeon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import io.github.moulberry.notenoughupdates.commands.ClientCommandBase;
import io.github.moulberry.notenoughupdates.dungeons.ColorMap;
import io.github.moulberry.notenoughupdates.dungeons.GuiDungeonMapEditor;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.MapData;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class MapCommand extends ClientCommandBase {

	public MapCommand() {
		super("neumap");
	}

	private void populateNeuColorMapFromJson(JsonObject json) {
		try {
			ColorMap colorMap = new ColorMap();
			byte[][] colorData = ColorMap.getBlackMap2D();

			for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
				int x = Integer.parseInt(entry.getKey().split(":")[0]);
				int y = Integer.parseInt(entry.getKey().split(":")[1]);

				// TODO: change the file to be map-style colors
				int color = entry.getValue().getAsInt();
				switch (color) {
					case 0x10000000: // 268435456 = white w/alpha = 16
						color = ColorMap.TRANSPARENT & ColorMap.ALPHA_170; // TODO: figure out the alpha
						break;
					case 0x18000000: // 402653184 = white w/alpha = 24
						color = ColorMap.TRANSPARENT & ColorMap.ALPHA_85; // TODO: figure out the alpha
						break;
					case 0xffe5e533: // -1710797
						color = ColorMap.MINI_BOSS_ROOM & ColorMap.ALPHA_255;
						break;
					case 0xff72431b: // -9288933
						color = ColorMap.REGULAR_ROOM & ColorMap.ALPHA_255;
						break;
					case 0xffb24cd8: // -5092136
						color = ColorMap.PUZZLE_ROOM & ColorMap.ALPHA_255;
						break;
					case 0xff007c00: // -16745472
						color = ColorMap.START_ROOM & ColorMap.ALPHA_255;
						break;
					case 0xffffffff: // -1
						color = ColorMap.QUESTION_MARK & ColorMap.ALPHA_255; // TODO: double check these alpha values
						break;
					case 0xfff27fa5: // -884827
						color = ColorMap.FAIRY_ROOM & ColorMap.ALPHA_255;
						break;
					case 0xffff0000: // -65536
						color = ColorMap.WATCHER_ROOM & ColorMap.ALPHA_255;
						break;
					default:
						throw new IllegalArgumentException(String.format("Invalid value in F1Full.json %d", color));
				}
				colorData[x][y] = (byte)(color & 0xff);
			}

			colorMap.setColorData(colorData);
			NotEnoughUpdates.INSTANCE.colorMap = colorMap;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Override
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		if (NotEnoughUpdates.INSTANCE.colorMap == null || !NotEnoughUpdates.INSTANCE.colorMap.hasData()) {
			try (
				BufferedReader reader = new BufferedReader(new InputStreamReader(Minecraft
					.getMinecraft()
					.getResourceManager()
					.getResource(
						new ResourceLocation("notenoughupdates:maps/F1Full.json"))
					.getInputStream(), StandardCharsets.UTF_8))
			) {
				JsonObject json = NotEnoughUpdates.INSTANCE.manager.gson.fromJson(reader, JsonObject.class);
				populateNeuColorMapFromJson(json);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (!NotEnoughUpdates.INSTANCE.config.hidden.dev) {
			NotEnoughUpdates.INSTANCE.openGui = new GuiDungeonMapEditor();
			return;
		}

		if (args.length == 1 && args[0].equals("reset")) {
			if (NotEnoughUpdates.INSTANCE.colorMap != null) {
				NotEnoughUpdates.INSTANCE.colorMap.clear();
			};
			return;
		}

		if (args.length != 2) {
			NotEnoughUpdates.INSTANCE.openGui = new GuiDungeonMapEditor();
			return;
		}

		if (args[0].equals("save")) {
			ItemStack stack = Minecraft.getMinecraft().thePlayer.getHeldItem();
			if (stack != null && stack.getItem() instanceof ItemMap) {
				ItemMap map = (ItemMap) stack.getItem();
				MapData mapData = map.getMapData(stack, Minecraft.getMinecraft().theWorld);

				if (mapData == null) return;

				JsonObject json = new JsonObject();
				for (int i = 0; i < 16384; ++i) {
					int x = i % 128;
					int y = i / 128;

					int j = mapData.colors[i] & 255;

					Color c;
					if (j / 4 == 0) {
						c = new Color((i + i / 128 & 1) * 8 + 16 << 24, true);
					} else {
						c = new Color(MapColor.mapColorArray[j / 4].getMapColor(j & 3), true);
					}

					json.addProperty(x + ":" + y, c.getRGB());
				}

				try {
					new File(NotEnoughUpdates.INSTANCE.manager.configLocation, "maps").mkdirs();
					NotEnoughUpdates.INSTANCE.manager.writeJson(
						json,
						new File(NotEnoughUpdates.INSTANCE.manager.configLocation, "maps/" + args[1] + ".json")
					);
				} catch (Exception e) {
					e.printStackTrace();
				}

				Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN +
					"Saved to file."));
			}

			return;
		}

		if (args[0].equals("load")) {
			JsonObject json = NotEnoughUpdates.INSTANCE.manager.getJsonFromFile(new File(
				NotEnoughUpdates.INSTANCE.manager.configLocation,
				"maps/" + args[1] + ".json"
			));

			populateNeuColorMapFromJson(json);
			return;
		}

		NotEnoughUpdates.INSTANCE.openGui = new GuiDungeonMapEditor();
	}
}
