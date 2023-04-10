import javax.crypto.Mac;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.naming.ldap.StartTlsRequest;
import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class Client {
    private static final BigInteger N = new BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AACAA68FFFFFFFFFFFFFFFF", 16);

    private static final BigInteger g = BigInteger.valueOf((long) 2);

    private static byte[] nonce = new byte[32];

    private static byte[] messages;

    private static BigInteger dhPrivateKey;
    private static BigInteger dhPublicKey;
    private static PrivateKey privateKey;
    private static BigInteger signedDHPublicKey;
    private static Certificate serverCertificate;
    private static Certificate clientCertificate;
    private static BigInteger serverDHPublicKey;
    private static BigInteger signedServerDHPublicKey;
    private static BigInteger sharedSecretKey;
    private static byte[] serverEncrypt;
    private static byte[] clientEncrypt;
    private static byte[] serverMac;
    private static byte[] clientMac;
    private static byte[] serverIV;
    private static byte[] clientIV;

    public static void main(String[] args) throws Exception {
        Socket clientSocket = new Socket("localhost", 8080);

        ObjectOutputStream clientOut = new ObjectOutputStream(clientSocket.getOutputStream());
        ObjectInputStream clientIn = new ObjectInputStream(clientSocket.getInputStream());

        if (clientSocket.isConnected()) {
            //step-1 - send
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(nonce);
            clientOut.writeObject(nonce);
//            messages = nonce;

            //step-2 - receive
            serverCertificate = (Certificate) clientIn.readObject();
            serverDHPublicKey = (BigInteger) clientIn.readObject();
            signedServerDHPublicKey = (BigInteger) clientIn.readObject();

            //step-3 - send
            CertificateFactory clientCF = CertificateFactory.getInstance("X.509");
            clientCertificate = clientCF.generateCertificate(new FileInputStream("/Users/avishekchoudhury/MSD-CS6014/MSD-CS6014/TLSlite/CASignedClientCertificate.pem")); //data1
            clientOut.writeObject(clientCertificate);

//            secureRandom.nextBytes(dhPrivateKey.toByteArray());
            dhPrivateKey = new BigInteger(secureRandom.generateSeed(128));
            dhPublicKey = g.modPow(dhPrivateKey, N); //DATA 2
            clientOut.writeObject(dhPublicKey);

            String clientPvtKeyFile = "/Users/avishekchoudhury/MSD-CS6014/MSD-CS6014/TLSlite/clientPrivateKey.der";
            Path path = Paths.get(clientPvtKeyFile);
            byte[] clientPvtKeyArr = Files.readAllBytes(path);
            PKCS8EncodedKeySpec encodedServerPvtKey = new PKCS8EncodedKeySpec(clientPvtKeyArr);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(encodedServerPvtKey);

            Signature clientSignature = Signature.getInstance("SHA256WithRSA");
            clientSignature.initSign(privateKey);
            clientSignature.update(dhPublicKey.toByteArray());
            signedDHPublicKey = new BigInteger(clientSignature.sign());
            clientOut.writeObject(signedDHPublicKey);

            //step-4 form shared secret key
            sharedSecretKey = serverDHPublicKey.modPow(dhPrivateKey, N);

            //step-5 - derive 6 session keys
            byte[] prk = Main.HMAC(nonce, sharedSecretKey.toByteArray());
            serverEncrypt = Main.hdkfExpand(prk, "server encrypt".getBytes());
            clientEncrypt = Main.hdkfExpand(serverEncrypt, "client encrypt".getBytes());
            serverMac = Main.hdkfExpand(clientEncrypt, "server MAC".getBytes());
            clientMac = Main.hdkfExpand(serverMac, "client MAC".getBytes());
            serverIV = Main.hdkfExpand(clientMac, "server IV".getBytes());
            clientIV = Main.hdkfExpand(serverIV, "client IV".getBytes());

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            outStream.write(nonce);
            outStream.write(serverCertificate.getEncoded());
            outStream.write(serverDHPublicKey.toByteArray());
            outStream.write(signedServerDHPublicKey.toByteArray());
            outStream.write(clientCertificate.getEncoded());
            outStream.write(dhPublicKey.toByteArray());
            outStream.write(signedDHPublicKey.toByteArray());

            byte[] serverMAC = (byte[]) clientIn.readObject();
            outStream.write(serverMAC);

            byte[] handshakeMsg = outStream.toByteArray();
            byte[] finalMAC = Main.HMAC(clientMac, handshakeMsg);
            clientOut.writeObject(finalMAC);

            String message1 = "From Client: Hello World";
            byte[] msgMAC = Main.concatenate(message1.getBytes(), Main.HMAC32Bit(clientMac, message1.getBytes()));
            byte[] cipherTxt = Main.encrypt(msgMAC, clientEncrypt, clientIV);
            clientOut.writeObject(cipherTxt);

            byte[] serverMsg = (byte[]) clientIn.readObject();
            byte[] decrypt = Main.decrypt(serverEncrypt, serverMsg, serverIV);

            if (Main.isAuthentic(serverMac, decrypt)) {
                String message2 = Main.getMessage(decrypt);
                System.out.println(message2);
            }
        }
    }
}
