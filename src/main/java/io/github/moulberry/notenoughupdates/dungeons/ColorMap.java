package io.github.moulberry.notenoughupdates.dungeons;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.block.material.MapColor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ColorMap {
	public static final int MAP_SIZE = 128;
	private static final int ROOM_MIN_SIZE_DEFAULT = 10;
	private static final int ROOM_MAX_SIZE_DEFAULT = 62;
	private static final int CONNECTOR_MIN_SIZE_DEFAULT = 4;
	private static final int CONNECTOR_MAX_SIZE_DEFAULT = 7;
	private static final int ALLOWED_MISMATCHES_WITHIN_ROOM = 3;
	public static final MapColor TRANSPARENT = MapColor.airColor;
	public static final MapColor RED_X = MapColor.tntColor;
	public static final MapColor WATCHER_ROOM = MapColor.tntColor;
	public static final MapColor GREEN_CHECK = MapColor.foliageColor;
	public static final MapColor START_ROOM = MapColor.foliageColor;
	public static final MapColor WHITE_CHECK = MapColor.snowColor;
	public static final MapColor TRAP_ROOM = MapColor.adobeColor;
	public static final MapColor PUZZLE_ROOM = MapColor.magentaColor;
	public static final MapColor MINI_BOSS_ROOM = MapColor.yellowColor;
	public static final MapColor FAIRY_ROOM = MapColor.pinkColor;
	public static final MapColor UNKNOWN_ROOM = MapColor.grayColor;
	public static final MapColor REGULAR_ROOM = MapColor.brownColor;
	public static final MapColor QUESTION_MARK = MapColor.blackColor;

	private static final HashMap<Integer, MapColor> COLOR_INDEX_MAP = new HashMap<Integer, MapColor>() {{
		put(0, TRANSPARENT);
		put(4, WATCHER_ROOM); // RED_X
		put(7, START_ROOM); // GREEN_CHECK
		put(8, WHITE_CHECK);
		put(15, TRAP_ROOM);
		put(16, PUZZLE_ROOM);
		put(18, MINI_BOSS_ROOM);
		put(20, FAIRY_ROOM);
		put(21, UNKNOWN_ROOM);
		put(26, REGULAR_ROOM);
		put(29, QUESTION_MARK);
	}};
	private static final HashMap<Integer, MapColor> ARGB_MAP = new HashMap<Integer, MapColor>() {{
		put(0x10000000, TRANSPARENT);
		put(0x18000000, TRANSPARENT);
		put(0xffe5e533, MINI_BOSS_ROOM);
		put(0xff72431b, REGULAR_ROOM);
		put(0xffb24cd8, PUZZLE_ROOM);
		put(0xff007c00, START_ROOM);
		put(0xffffffff, WHITE_CHECK);
		put(0xfff27fa5, FAIRY_ROOM);
		put(0xffff0000, WATCHER_ROOM);
		put(0xff000000, QUESTION_MARK);
	}};
	private static final HashMap<Integer, String> ARGB_DEBUG_MAP = new HashMap<Integer, String>() {{
		put(0x10000000, "_");
		put(0x18000000, "_");
		put(0xffe5e533, "B");
		put(0xff72431b, "R");
		put(0xffb24cd8, "P");
		put(0xff007c00, "S");
		put(0xffffffff, " ");
		put(0xfff27fa5, "F");
		put(0xffff0000, "W");
		put(0xff000000, "?");
		// TODO: Add trap room and anything else missing
	}};
	private static final HashMap<Integer, String> COLOR_INDEX_DEBUG_MAP = new HashMap<Integer, String>() {{
		put(TRANSPARENT.colorIndex, "_");
		put(MINI_BOSS_ROOM.colorIndex, "B");
		put(REGULAR_ROOM.colorIndex, "R");
		put(PUZZLE_ROOM.colorIndex, "P");
		put(START_ROOM.colorIndex, "S");
		put(WHITE_CHECK.colorIndex, " ");
		put(FAIRY_ROOM.colorIndex, "F");
		put(WATCHER_ROOM.colorIndex, "W");
		put(TRAP_ROOM.colorIndex, "T");
		put(QUESTION_MARK.colorIndex, "?");
	}};

	public final Color[] colorIndexToColorMap = createColorIndexToColorMap();
	public Writer debugRoomParsingWriter = null;

	private byte[] currentColorBytes = null;
	private Color[][] currentColorMap = null;
	private BufferedImage bufferedimage = null;
	private boolean hasData = false;
	private final ArrayList<ColoredArea> rooms = new ArrayList<>();
	private final ArrayList<ColoredArea> connectors = new ArrayList<>();
	private final int minRoomSize;
	private final int maxRoomSize;
	private final int minConnectorSize;
	private final int maxConnectorSize;

	public ColorMap() {
		this(ROOM_MIN_SIZE_DEFAULT, ROOM_MAX_SIZE_DEFAULT, CONNECTOR_MIN_SIZE_DEFAULT, CONNECTOR_MAX_SIZE_DEFAULT);
	}

	public ColorMap(int minRoomSize, int maxRoomSize, int minConnectorSize, int maxConnectorSize) {
		this.minRoomSize = minRoomSize;
		this.maxRoomSize = maxRoomSize;
		this.minConnectorSize = minConnectorSize;
		this.maxConnectorSize = maxConnectorSize;
	}

	private static Color[] createColorIndexToColorMap() {
		Color[] mappingTable = new Color[MapColor.mapColorArray.length];
		for (int i = 0; i < MapColor.mapColorArray.length; i++) {
			mappingTable[i] = new Color(MapColor.mapColorArray[i / 4].getMapColor(i & 3), true);
		}
		return mappingTable;
	}

	public static int getColorIndexFromARGB(int color) {
		 MapColor mapColor = ARGB_MAP.get(color);
		 if (mapColor != null) return mapColor.colorIndex;
		 throw new IllegalArgumentException(String.format("Invalid color value: %d", color));
	}

	public static ColorMap getColorMapFromJson(JsonObject json) {
		return getColorMapFromJson(json, null, null);
	}

	public static ColorMap getColorMapFromJson(JsonObject json, Writer debugJsonParserWriter, Writer roomParsingWriter) {
		try {
			ColorMap colorMap = new ColorMap();
			colorMap.debugRoomParsingWriter = roomParsingWriter;
			byte[][] colorData = ColorMap.getBlackMap2D();

			StringBuilder sb = null;
			if (debugJsonParserWriter != null) {
				sb = new StringBuilder();
			}
			int count = 0;
			for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
				count++;
				int x = Integer.parseInt(entry.getKey().split(":")[0]);
				int y = Integer.parseInt(entry.getKey().split(":")[1]);

				// TODO: change the file to be map-style byte colors
				int color = entry.getValue().getAsInt();
				if (sb != null) {
					sb.append(getArgbDebugString(color));
					if (count % 128 == 0){
						sb.append("\n");
					}
				}
				color = getColorIndexFromARGB(color);
				colorData[x][y] = (byte)(color & 0xff);
			}

			if (sb != null) {
				debugJsonParserWriter.write(sb.toString());
			}
			colorMap.setColorData(colorData);
			return colorMap;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
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

	public static byte[] getBlackMap1D() {
		return new byte[MAP_SIZE * MAP_SIZE];
	}

	public void clear() {
		hasData = false;
		rooms.clear();
		connectors.clear();
	}

	public int getRoomCount() {
		return rooms.size();
	}

	public int getConnectorCount() {
		return connectors.size();
	}

	public int getColorIndexByCoords(int x, int y) {
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

	public void computeAdjacencies() {
		// TODO: implement this
	}

	// Gets a colored area given the upper-left coordinates of the area
	private ColoredArea discoverAreaAtCoords(int startX, int startY, int expectedColorIndex) {
		ColoredArea theArea = new ColoredArea(
			ColoredAreaType.UNKNOWN,
			startX, startY,
			expectedColorIndex,
			0, 0);

		for (int yIndex = startY; yIndex < MAP_SIZE; yIndex++) {
			int maxX;
			if (theArea.width == 0) {
				maxX = MAP_SIZE-1;
			} else {
				maxX = theArea.x + theArea.width - 1;
			}
			int currentRowWidth = 0;
			int xIndex = startX;
			int ignoredCount = 0;
			while (xIndex <= maxX) {
				int actualColorIndex = getColorIndexByCoords(xIndex, yIndex);
				if (actualColorIndex == expectedColorIndex) {
					currentRowWidth++;
					xIndex++;
					ignoredCount = 0;
					continue;
				} else if (ignoredCount < ALLOWED_MISMATCHES_WITHIN_ROOM &&
					(actualColorIndex == WHITE_CHECK.colorIndex  ||
						actualColorIndex == GREEN_CHECK.colorIndex ||
						actualColorIndex == RED_X.colorIndex)) {
					ignoredCount++;
					currentRowWidth++;
					xIndex++;
					continue;
				}
				currentRowWidth -= ignoredCount;
				break;
			}

			// Set the width on the first row
			if (theArea.width == 0) {
				theArea.width = currentRowWidth;
			} else if (currentRowWidth != theArea.width) {
				break;
			}

			theArea.height++;
		}

		// Work from larger to smaller since an over-sized connector looks like a room
		if (theArea.width >= minRoomSize && theArea.width <= maxRoomSize &&
				theArea.height >= minRoomSize && theArea.height <= maxRoomSize) {
			theArea.type = ColoredAreaType.ROOM;
			return theArea;
		}

		// Bail if it's not a connector, check both dimensions since one of them
		// will be invalid since it extends into one of the connected rooms
		if (theArea.width < minConnectorSize && theArea.width > maxConnectorSize &&
				theArea.height < minConnectorSize && theArea.height > maxConnectorSize) {
			return null;
		}

		theArea.type = ColoredAreaType.CONNECTOR;

		// Trim the connector to ensure they don't extend into adjacent rooms
		// NOTE:
		// 	Trimming assumes that the map always has at least a one pixel border
		// 	and doesn't check for going off the map.
		boolean trimmedWidth = false;
		for (int x = theArea.x; x < MAP_SIZE; x++) {
			if (getColorIndexByCoords(x, theArea.y - 1) != TRANSPARENT.colorIndex ||
					getColorIndexByCoords(x, theArea.y + theArea.height) != TRANSPARENT.colorIndex) {
				if (x != theArea.x && x != theArea.x + theArea.width) {
					theArea.width = x - theArea.x;
					trimmedWidth = true;
				}
				break;
			}
		}

		// Trim height
		for (int y = theArea.y; y < MAP_SIZE; y++) {
			if (getColorIndexByCoords(theArea.x - 1, y) != TRANSPARENT.colorIndex ||
					getColorIndexByCoords(theArea.x + theArea.width, y) != TRANSPARENT.colorIndex) {
				if (y != theArea.y && y != theArea.y + theArea.height) {
					theArea.height = y - theArea.y;
					if (trimmedWidth) {
						throw new IllegalStateException(
							String.format("A connector should not require trimming both dimensions. x=%d y=%d w=%d h=%d",
								theArea.x, theArea.y, theArea.width, theArea.height
							));
					}
				}
				break;
			}
		}

		return theArea;
	}

	private ColoredArea getMatchingAreaByCoords(ArrayList<ColoredArea> areas, int x, int y) {
		for (ColoredArea area : areas) {
			if (x >= area.x && x < area.x + area.width &&
				y >= area.y && y < area.y + area.height) {
				return area;
			}
		}

		return null;
	}

	public ColoredArea getRoomByCoords(int x, int y) {
		return getMatchingAreaByCoords(rooms, x, y);
	}

	public ColoredArea getConnectorByCoords(int x, int y) {
		return getMatchingAreaByCoords(connectors, x, y);
	}

	public ColoredArea getAnyAreaByCoords(int x, int y) {
		ColoredArea theArea = getRoomByCoords(x, y);
		return theArea != null ? theArea : getConnectorByCoords(x,y);
	}

	public ArrayList<ColoredArea> getRooms() {
		return rooms;
	}

	public ArrayList<ColoredArea> getConnectors() {
		return connectors;
	}

	public boolean hasRoomWithColor(int color) {
		for (ColoredArea room : rooms) {
			if (room.colorIndex == color) return true;
		}

		return false;
	}

	private void writeMap(Writer writer) {
		if (writer == null) return;
		StringBuilder sbValues;
		sbValues = new StringBuilder();
		for (int y = 0; y < ColorMap.MAP_SIZE; y++) {
			for (int x = 0; x < ColorMap.MAP_SIZE; x++) {
				sbValues.append(getColorIndexDebugString(getColorIndexByCoords(x, y)));
			}
			sbValues.append("\n");
		}
		try {
			writer.write(sbValues.toString());
		} catch (IOException e) {
			// ignored
		}
	}

	private void rediscoverRooms() {
		rooms.clear();
		writeMap(debugRoomParsingWriter);

		// Iterate rows, top to bottom - looking for a non-background color
		for (int y = 0; y < ColorMap.MAP_SIZE; y++) {
			for (int x = 0; x < ColorMap.MAP_SIZE; x++) {
				int colorIndex = getColorIndexByCoords(x, y);
				if (colorIndex != TRANSPARENT.colorIndex) {
					ColoredArea intersectingArea = getAnyAreaByCoords(x, y);
					if (intersectingArea != null) {
						x += intersectingArea.width - 1;
						continue;
					}

					ColoredArea newArea = discoverAreaAtCoords(x, y, colorIndex);
					if (newArea != null) {
						x += newArea.width - 1;
						if (newArea.type == ColoredAreaType.CONNECTOR) {
							connectors.add(newArea);
						} else if (newArea.type == ColoredAreaType.ROOM) {
							rooms.add(newArea);
						} else {
							throw new IllegalStateException("ColoredAreaType != UNKNOWN assertion failed");
						}
					}
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
		for (int y = 0; y < 128; y++) {
			for (int x = 0; x < 128; x++) {
				bytes1D[y * 128 + x] = newColors[x][y];
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

			currentColorMap[x][y] = colorIndexToColorMap[j];
			bufferedimage.setRGB(x, y, currentColorMap[x][y].getRGB());
		}

		rediscoverRooms();
		hasData = true;
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
				ImageIO.write(bufferedimage, "png", dungeonMapFile);
			} catch (IOException e) {
				// Do nothing
			}
		}
	}

	public boolean hasData() {
		return this.hasData;
	}

	public static String getColorIndexDebugString(int color) {
		String debugString = COLOR_INDEX_DEBUG_MAP.get(color);
		if (debugString != null) return debugString;
		throw new IllegalArgumentException(String.format("Invalid color value: %d", color));
	}

	public static String getArgbDebugString(int color) {
		String debugString = ARGB_DEBUG_MAP.get(color);
		if (debugString != null) return debugString;
		throw new IllegalArgumentException(String.format("Invalid color value: %d", color));
	}

	public enum ColoredAreaType {
		CONNECTOR,
		ROOM,
		UNKNOWN
	}

	public static class ColoredArea {
		public ColoredAreaType type;
		public int x;
		public int y;
		public int colorIndex;
		public int height;
		public int width;


		ColoredArea(ColoredAreaType type, int x, int y, int colorIndex, int height, int width) {
			this.type = type;
			this.x = x;
			this.y = y;
			this.colorIndex = colorIndex;
			this.height = height;
			this.width = width;
		}

		public String toString() {
			return String.format("x=%d y=%d colorIndex=%d height=%d width=%d",
				x, y, colorIndex, height, width);
		}
	}
}
