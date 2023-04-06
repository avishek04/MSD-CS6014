import java.io.*;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.Certificate;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec; //for loading secret keys
import java.lang.Math;
import java.util.Arrays;
import java.util.Objects;

public class Main {
    public static String getMessage(byte[] decryptMsg) {
        byte[] message = Arrays.copyOf(decryptMsg, decryptMsg.length - 32);
        return Arrays.toString(message);
    }

    public static boolean isAuthentic(byte[] orgMac, byte[] decryptMsg) {
        byte[] mac = Arrays.copyOfRange(decryptMsg, decryptMsg.length - 31, decryptMsg.length);
        return Arrays.equals(orgMac, mac);
    }

    public static byte[] encrypt(byte[] key, byte[] data, byte[] IV) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(IV);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] key, byte[] data, byte[] IV) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        SecretKey secretKeySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(IV);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);
        return cipher.doFinal(data);
    }

    public static byte[] HMAC(byte[] key, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);
        return mac.doFinal(data);
    }

    public static byte[] hdkfExpand(byte[] key, byte[] tag) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        out.write(0x1);
        byte[] data = new byte[16];
        data = out.toByteArray();
        return HMAC(key, data);
    }
}