package io.github.moulberry.notenoughupdates.dungeons;

import net.minecraft.block.material.MapColor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

public class ColorMap {
	public static final int MAP_SIZE = 128;
	public static final int TRANSPARENT = MapColor.airColor.colorValue; // 0
	public static final int GREEN_CHECK_COLOR = MapColor.foliageColor.colorValue; // 7
	public static final int START_ROOM = MapColor.foliageColor.colorValue; // 7
	public static final int RED_X_COLOR = MapColor.tntColor.colorValue; // 4
	public static final int WATCHER_ROOM = MapColor.tntColor.colorValue; // 4
	public static final int WHITE_CHECK_COLOR = MapColor.snowColor.colorValue; // 8
	public static final int TRAP_ROOM_COLOR = MapColor.adobeColor.colorValue; // 15
	public static final int PUZZLE_ROOM = MapColor.magentaColor.colorValue; // 16
	public static final int MINI_BOSS_ROOM = MapColor.yellowColor.colorValue; // 18
	public static final int FAIRY_ROOM = MapColor.pinkColor.colorValue; // 20
	public static final int UNKNOWN_ROOM_COLOR = MapColor.grayColor.colorValue; // 21
	public static final int REGULAR_ROOM = MapColor.brownColor.colorValue; // 26
	public static final int QUESTION_MARK = MapColor.blackColor.colorValue; // 29

	public static final int ALPHA_255 = 0x11;
	public static final int ALPHA_170 = 0x10;
	public static final int ALPHA_85 = 0x01;
	public static final int ALPHA_NONE = 0x00;

	public static final Color[] colorMappingTable = createColorMappingTable();

	private static Color[] createColorMappingTable() {
		Color[] mappingTable = new Color[MapColor.mapColorArray.length];
		for (int i = 0; i < MapColor.mapColorArray.length; i++) {
			mappingTable[i] = new Color(MapColor.mapColorArray[i / 4].getMapColor(i & 3), true);
		}
		return mappingTable;
	}

	private byte[] currentColorBytes = null;
	private Color[][] currentColorMap = null;
	private BufferedImage bufferedimage = null;
	private boolean hasData = false;

	private ArrayList<ColorRoom> rooms = new ArrayList<>();

	public static byte[] getBlackMap1D() {
		return new byte[MAP_SIZE * MAP_SIZE];
	}

	/**
	 * Generates a two-dimensional array that represents a black map
	 * <p>
	 * getBlackMap1D is the recommended method to use since two-dimensional
	 * arrays are converted to a single dimension when calling {@link ColorMap#setColorData(byte[][])}
	 *
	 * @return A black map array
	 */
	public static byte[][] getBlackMap2D() {
		return new byte[MAP_SIZE][MAP_SIZE];
	}

	public void clear() {
		hasData = false;
		rooms.clear();
	}

	public int getColorWithAlpha(int x, int y) {
		if (x > MAP_SIZE || x < 0 || y > MAP_SIZE || y < 0) {
			throw new IllegalArgumentException("Invalid map index");
		}
		return currentColorBytes[y * MAP_SIZE + x];
	}

	public int getAlphaAsInt(int x, int y) {
		if (x > MAP_SIZE || x < 0 || y > MAP_SIZE || y < 0) {
			throw new IllegalArgumentException("Invalid map index");
		}

		int alpha = (currentColorBytes[y * MAP_SIZE + x] & 0b11) & 0xff;
		return (alpha * 64)-1;
	}

	public void FindCompoundRooms() {
		// TODO: Check for rooms that should be considered a single room
	}

	// Gets a room given the upper-left coordinates of the room
	private ColorRoom discoverRoomAtCoords(int startX, int startY, int expectedColorWithAlpha) {
		ColorRoom theRoom = new ColorRoom(startX, startY, expectedColorWithAlpha, 0, 0);

		for (int yIndex = startY; yIndex < MAP_SIZE; yIndex++) {
			int maxX;
			if (theRoom.width == 0) {
				maxX = MAP_SIZE-1;
			} else {
				maxX = theRoom.x + theRoom.width - 1;
			}
			int currentRowWidth = 0;
			int xIndex = startX;
			while (xIndex <= maxX) {
				if (getColorWithAlpha(xIndex, yIndex) == expectedColorWithAlpha) {
					currentRowWidth++;
					xIndex++;
				} else {
					break;
				}
			}

			if (theRoom.width == 0) {
				theRoom.width = currentRowWidth;
				continue;
			}

			if (currentRowWidth != theRoom.width) {
				break;
			}

			theRoom.height++;
		}

		return theRoom;
	}

	public ColorRoom getRoomByCoords(int x, int y) {
		for (ColorRoom room : rooms) {
			if (x >= room.x && x <= room.x + room.width &&
					y >= room.y && y <= room.y + room.height) {
				return room;
			}
		}

		return null;
	}

	public boolean hasRoomWithColor(int color) {
		for (ColorRoom room : rooms) {
			if (room.color == color) return true;
		}

		return false;
	}

	private void rediscoverRooms() {
		rooms.clear();
		ColorRoom theRoom = null;

		// Iterate rows, top to bottom - looking for a non-background color
		for (int y = 0; y < ColorMap.MAP_SIZE; y++) {
			for (int x = 0; x < ColorMap.MAP_SIZE; x++) {
				int colorWithAlpha = getColorWithAlpha(x, y);
				// TODO: Check whether this works
				theRoom = getRoomByCoords(x, y);
				if (theRoom == null && colorWithAlpha != TRANSPARENT) {
					theRoom = discoverRoomAtCoords(x, y, colorWithAlpha);
				}

				if (theRoom != null) {
					x += theRoom.width - 1;

					// TODO: do this filtering somewhere else - min is 10, max is 20
//					if (theRoom.width < minWidth || theRoom.width > maxWidth ||
//						theRoom.height < minHeight || theRoom.height > maxHeight) {
//						theRoom = null;
//					}

					rooms.add(theRoom);
				}

			}
		}
	}

	/**
	 * Updates current colors used by other methods in this class, but only if new colors are different.
	 * <p>
	 * Also performs in-place update of the {@link Color} array returned by {@link ColorMap#getCurrentColorMap()}</code>.
	 * <p>
	 *
	 * @param newColors An array of 16384 <code>MapColor</code> bytes. The lowest two bits of each byte
	 *                  are alpha, the upper six are an index into {@link MapColor#mapColorArray}.
	 * @return false if the new colors match the previous colors, true otherwise
	 */
	public boolean setColorData(byte[][] newColors)
	{
		byte[] bytes1D = new byte[16384];
		for (int i1 = 0; i1 < 128; i1++) {
			for (int i2 = 0; i2 < 128; i2++) {
				bytes1D[i1 * 128 + i2] = newColors[i1][i2];
			}
		}

		return this.setColorData(bytes1D);
	}

	public boolean setColorData(byte[] newColors) {
		if (newColors.length != 16384) {
			throw new IllegalArgumentException("unexpected color array length");
		}

		if (Arrays.equals(currentColorBytes, newColors)) {
			return false;
		}

		currentColorBytes = newColors.clone();
		if (currentColorMap == null) {
			currentColorMap = new Color[MAP_SIZE][MAP_SIZE];
			bufferedimage = new BufferedImage(MAP_SIZE, MAP_SIZE, BufferedImage.TYPE_INT_ARGB);
		}

		for (int i = 0; i < MAP_SIZE * MAP_SIZE; ++i) {
			int x = i % MAP_SIZE;
			int y = i / MAP_SIZE;

			int j = currentColorBytes[i] & 255;

			currentColorMap[x][y] = colorMappingTable[j];
			bufferedimage.setRGB(x, y, currentColorMap[x][y].getRGB());
		}

		hasData = true;

		rediscoverRooms();

		return true;
	}

	public Color[][] getCurrentColorMap() {
		if (currentColorBytes == null) {
			throw new IllegalStateException("setCurrentColors must be called before getColorMap");
		}

		return currentColorMap;
	}

	public byte[] getCurrentColorBytes() {
		return currentColorBytes.clone();
	}

	public void saveMapToPngFile(String fileName) {
		if (bufferedimage != null) {
			File dungeonMapFile = new File(fileName);
			try {
				ImageIO.write(bufferedimage, "png", (File)dungeonMapFile);
			} catch (IOException e) {
				// Do nothing
			}
		}
	}

	public boolean hasData() {
		return this.hasData;
	}

	public class ColorRoom {
		public int x;
		public int y;
		public int color;
		public int height;
		public int width;

		ColorRoom(int x, int y, int color, int height, int width) {
			this.x = x;
			this.y = y;
			this.color = color;
			this.height = height;
			this.width = width;
		}

		public int maxX() {
			return this.x + this.width -1;
		}
	}
}
