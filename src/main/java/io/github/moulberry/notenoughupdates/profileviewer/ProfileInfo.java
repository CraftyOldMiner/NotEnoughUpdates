package io.github.moulberry.notenoughupdates.profileviewer;

public class ProfileInfo {
	public enum GameMode {
		IRONMAN,
		BINGO,
		STRANDED,
		CLASSIC,
		UNKNOWN
	}

	public GameMode gameMode = GameMode.UNKNOWN;
	public Float bankBalance = null;
	public Float purseCoins = null;
	public Integer mythosKills = null;
	public Float fairyExchanges = null;
	// TODO: get the found crystals, parts delivered, using the profile data
	// TODO: get the forge data from the profile - forge_processes (also use hotm perk data to determine if done)
	// TODO: get backpack & ec contents for /ec
	// TODO: get sack information

	// TODO: use harp_quest data for intelligence instead of objectives.talk_to_melody.status
	//  Data looks like this:
//    "selected_song": "fire_and_flames",
//			"selected_song_epoch": 1648198171460,
//			"song_hymn_joy_perfect_completions": 1,
//			"song_frere_jacques_perfect_completions": 1,
//			"song_amazing_grace_perfect_completions": 1,
//			"song_brahms_perfect_completions": 1,
//			"song_happy_birthday_perfect_completions": 1,
//			"song_greensleeves_perfect_completions": 1,
//			"song_jeopardy_perfect_completions": 1,
//			"song_minuet_perfect_completions": 1,
//			"song_joy_world_perfect_completions": 1,
//			"song_pure_imagination_perfect_completions": 1,
//			"song_vie_en_rose_perfect_completions": 1,
//			"song_fire_and_flames_perfect_completions": 1,
//			"claimed_talisman": true

}
