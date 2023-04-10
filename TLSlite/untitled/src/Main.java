import java.io.*;
import java.lang.reflect.Array;
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
        return new String(message);
    }

    public static boolean isAuthentic(byte[] orgMac, byte[] decryptMsg) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] message = Arrays.copyOf(decryptMsg, decryptMsg.length - 32);
        byte[] msgMac = HMAC32Bit(orgMac, message);
        byte[] last32Byte = Arrays.copyOfRange(decryptMsg, decryptMsg.length - 32, decryptMsg.length);
        return Arrays.equals(msgMac, last32Byte);
    }

    public static byte[] encrypt(byte[] msgMAC, byte[] key, byte[] IV) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(IV);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);
        return cipher.doFinal(msgMAC);
    }

    public static byte[] decrypt(byte[] key, byte[] data, byte[] IV) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKey secretKeySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(IV);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);
        return cipher.doFinal(data);
    }

    public static byte[] HMAC(byte[] key, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);
        return Arrays.copyOfRange(mac.doFinal(data), 0, 16);
    }

    public static byte[] concatenate(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static byte[] HMAC32Bit(byte[] key, byte[] msg) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);
        return mac.doFinal(msg);
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