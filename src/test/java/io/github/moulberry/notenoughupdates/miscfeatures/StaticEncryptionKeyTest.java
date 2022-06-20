package io.github.moulberry.notenoughupdates.miscfeatures;

import io.github.moulberry.notenoughupdates.core.util.Vec3Comparable;
import io.github.moulberry.notenoughupdates.miscfeatures.CrystalMetalDetectorSolver.SolutionState;
import io.github.moulberry.notenoughupdates.util.NEUDebugLogger;
import net.minecraft.util.BlockPos;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Base64;

class StaticEncryptionKeyTest {
	@BeforeEach
	void setUp() {
		NEUDebugLogger.logMethod = 	StaticEncryptionKeyTest::neuDebugLog;
		NEUDebugLogger.allFlagsEnabled = true;
	}

	private static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
				+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}

	@Test
	void test_varint_stuff() {
		byte[] bytes = new byte[] { -128, 2, 54, 0 };
		int i = 0;
		int j = 0;

		int index = 0;
		while (true)
		{
			byte b0 = bytes[index];
			index++;
			i |= (b0 & 127) << j++ * 7;

			if (j > 5)
			{
				throw new RuntimeException("VarInt too big");
			}

			if ((b0 & 128) != 128)
			{
				break;
			}
		}

		System.out.println(i);
	}

	@Test
	void test_decryption_stuff() {
		String encrypted_bytes_string = "b1d2c65f39347233a91f2537d48c7665cf5450cc82223666e8d979e1b9fb3db46fd807dfe17c1892a277b4ce3449b466426b853afd09f6c36fbd924234c0dce90649217592ce3c";
		byte[] encrypted_bytes = hexStringToByteArray(encrypted_bytes_string);
		byte[] decrypted_bytes = new byte [encrypted_bytes.length];

		String session_key_base64 = "SJk4sCEUMGeeEqgedxKDyw==";
		// DONE: Base64 decoding is the same
		byte[] session_key_bytes = Base64.getDecoder().decode(session_key_base64);
		SecretKey secretKey = new SecretKeySpec(session_key_bytes, 0, session_key_bytes.length, "AES");

//	Below is equivalent to: Cipher cipher = CryptManager.createNetCipherInstance(2, secretKey);
		Cipher cipher;
		try
		{
			cipher = Cipher.getInstance("AES/CFB8/NoPadding");
			// secretKey.getEncoded just clones the array, IvParameterSpec copies it & truncates it to 16 bytes
			cipher.init(2, (Key)secretKey, (AlgorithmParameterSpec)(new IvParameterSpec(secretKey.getEncoded())));


			// TODO: Check the old protocol version, the last byte of the first part might actually be the first byte of the compressed packet
			// TODO: Finish reading 1.8.9 - https://wiki.vg/index.php?title=Protocol&oldid=6742#Handshaking
			// Java decrypted:
			// [3, 3, -128, 2, 54, 0, 2, 36, 54, 57, 56, 101, 49, 57, 57, 100, 45, 54, 98, 100, 49, 45, 52, 98, 49, 48, 45, 97, 98, 48, 99, 45, 53, 50, 102, 101, 100, 100, 49, 52, 54, 48, 100, 99, 14, 67, 114, 97, 102, 116, 121, 79, 108, 100, 77, 105, 110, 101, 114, 11, 0, 63, 6, 70, 77, 76, 124, 72, 83, 0, 1]
			// Wireshark decrypted:
			//   03 03 80 02   36 00 																		 │ ····6·           │
			//   Login Packet:
			//   02 24   36 39 38 65   31 39 39 64                       │       ·$698e199d │
			//   2d 36 62 64   31 2d 34 62   31 30 2d 61   62 30 63 2d   │ -6bd1-4b10-ab0c- │
			//   35 32 66 65   64 64 31 34   36 30 64 63   0e 43 72 61   │ 52fedd1460dc·Cra │
			//   66 74 79 4f   6c 64 4d 69   6e 65 72 0b   00 3f 06 46   │ ftyOldMiner··?·F │
			//   4d 4c 7c 48   53 00 01
			// TODO: If it's different, figure out how to make the GCrypt implementation have the NoPadding option
			// TODO: put a breakpoint in NettyEncryptionTranslator->decipher to see which packets make it there
			cipher.update(encrypted_bytes, 0, encrypted_bytes.length, decrypted_bytes, 0);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	private static void neuDebugLog(String message) {
		System.out.println(message);
	}
}
