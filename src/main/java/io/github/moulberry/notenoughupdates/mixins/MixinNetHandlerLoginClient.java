package io.github.moulberry.notenoughupdates.mixins;

import io.github.moulberry.notenoughupdates.NotEnoughUpdates;
import net.minecraft.client.network.NetHandlerLoginClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Mixin(NetHandlerLoginClient.class)
public class MixinNetHandlerLoginClient {
	private SecretKey generateRandomKey() {
		try
		{
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(128);
			return keyGen.generateKey();
		}
		catch (NoSuchAlgorithmException ex)
		{
			throw new Error(ex);
		}
	}

	@Redirect(method = "handleEncryptionRequest", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/CryptManager;createNewSharedKey()Ljavax/crypto/SecretKey;"))
	private SecretKey createNewSharedKey() {
		if (NotEnoughUpdates.INSTANCE.config.hidden.dev &&
			!NotEnoughUpdates.INSTANCE.config.hidden.devStaticKey.isEmpty()
		) {
			if (NotEnoughUpdates.INSTANCE.config.hidden.devStaticKey.equals("generate")) {
				SecretKey secretKey = generateRandomKey();
				NotEnoughUpdates.INSTANCE.config.hidden.devStaticKey =
					Base64.getEncoder().encodeToString(secretKey.getEncoded());
				NotEnoughUpdates.INSTANCE.saveConfig();
			}

			try {
				byte[] staticKey = Base64.getDecoder().decode(NotEnoughUpdates.INSTANCE.config.hidden.devStaticKey);
				Base64.getEncoder().encodeToString(staticKey);
				System.out.println("Using static encryption key from config file");
				return new SecretKeySpec(staticKey, 0, staticKey.length, "AES");
			} catch (IllegalArgumentException ex) {
				System.out.println("Error encountered using static encryption key, falling back to random key");
				ex.printStackTrace();
			}
		}

		return generateRandomKey();
	}
}
