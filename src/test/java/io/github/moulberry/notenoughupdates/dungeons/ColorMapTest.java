package io.github.moulberry.notenoughupdates.dungeons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.github.moulberry.notenoughupdates.dungeons.ColorMap.ColoredArea;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

class ColorMapTest {
	ClassLoader classLoader = getClass().getClassLoader();

	private JsonObject loadMapAsJson(String mapPath) {
		BufferedReader reader = new BufferedReader(
			new InputStreamReader(classLoader.getResourceAsStream(mapPath),
				StandardCharsets.UTF_8));
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.fromJson(reader, JsonObject.class);
	}

	// TODO: Mark this as disabled before creating a PR
	// TODO: Make sure the F1Full map is up-to-date
	@Test
	void generate_output_text_files() throws IOException {
		JsonObject json = loadMapAsJson("assets/notenoughupdates/maps/F1Full.json");
		BufferedWriter writer1 = new BufferedWriter(new FileWriter("testoutput1.txt"));
		BufferedWriter writer2 = new BufferedWriter(new FileWriter("testoutput2.txt"));
		ColorMap.debugJsonParserWriter = writer1;
		ColorMap.debugRoomParsingWriter = writer2;
		ColorMap colorMap = ColorMap.getColorMapFromJson(json);
		writer1.close();
		writer2.close();
		Assertions.assertNotNull(colorMap);
		System.out.printf("\n%d rooms:\n", colorMap.getRoomCount());
		for (ColoredArea room : colorMap.getRooms()) {
			System.out.println(room);
		}
		System.out.printf("\n%d connectors:\n", colorMap.getConnectorCount());
		for (ColoredArea connector : colorMap.getConnectors()) {
			System.out.println(connector);
		}
	}

	@Test
	void room_count_locations_and_sizes_are_correct() throws IOException {
		JsonObject json = loadMapAsJson("assets/notenoughupdates/maps/F1Full.json");
		ArrayList<ColoredArea> expectedRooms = new ArrayList<ColoredArea>();
		// TODO: add equals to ColorRoom & populate the expected rooms
		ColorMap colorMap = ColorMap.getColorMapFromJson(json);
		Assertions.assertEquals(13, colorMap.getRoomCount(), "Room count is incorrect");
		Assertions.assertEquals(11, colorMap.getConnectorCount(), "Connector count is incorrect");
	}

	@Test
	void input_map_matches_room_parsing_map() {
		JsonObject json = loadMapAsJson("assets/notenoughupdates/maps/F1Full.json");
		StringWriter jsonParserWriter = new StringWriter();
		StringWriter roomParsingWriter = new StringWriter();
		ColorMap.debugJsonParserWriter = jsonParserWriter;
		ColorMap.debugRoomParsingWriter = roomParsingWriter;
		ColorMap colorMap = ColorMap.getColorMapFromJson(json);
		Assertions.assertNotNull(colorMap);
		Assertions.assertEquals(jsonParserWriter.toString(), roomParsingWriter.toString());
	}

	@Test
	void get_debug_strings_match() {
		int[] colors = {0x10000000,
			0x18000000, 0xffe5e533, 0xff72431b,
			0xffb24cd8, 0xff007c00, 0xffffffff,
			0xfff27fa5, 0xffff0000};
		for (int color : colors) {
			int colorIndex = ColorMap.getColorIndexFromARGB(color);
			Assertions.assertEquals(ColorMap.getArgbDebugString(color), ColorMap.getColorIndexDebugString(colorIndex));
		}
	}

	@Test
	void room_is_detected() {

	}

	@Test
	void room_below_minimum_size_is_ignored() {

	}

	@Test
	void room_above_maximum_size_is_ignored() {

	}

	@Test
	void connector_below_minimum_size_is_ignored() {

	}

	@Test
	void connector_above_maximum_size_is_ignored() {

	}

	@Test
	void two_rooms_attached_horizontally_by_connector_are_detected() {

	}

	@Test
	void two_rooms_attached_vertically_by_connector_are_detected() {

	}

	@Test
	void one_L_shaped_room_is_detected_as_two_rooms() {

	}

	@Test
	void overlapping_min_and_max_connector_and_room_values_fail() {

	}

	@Test
	void check_mark_in_area_is_ignored() {

	}

	@Test
	void red_x_in_area_is_ignored() {

	}
}
