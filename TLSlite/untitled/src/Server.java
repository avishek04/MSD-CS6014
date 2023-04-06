import javax.crypto.interfaces.DHPublicKey;
import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

public class Server {
    private static ServerSocket server;
    private static final BigInteger N = new BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AACAA68FFFFFFFFFFFFFFFF", 16);
    private static final BigInteger g = BigInteger.valueOf((long) 2);
    private static ByteArrayOutputStream messages;
    private static byte[] clientNonce;
    private static BigInteger dhPrivateKey;
    private static BigInteger dhPublicKey;
    private static PrivateKey privateKey;
    private static BigInteger signedDHPublicKey;
    private static Certificate serverCertificate;
    private static Certificate clientCertificate;
    private static BigInteger clientDHPublicKey;
    private static BigInteger signedClientDHPublicKey;
    private static BigInteger sharedSecretKey;
    private static byte[] serverEncrypt;
    private static byte[] clientEncrypt;
    private static byte[] serverMac;
    private static byte[] clientMac;
    private static byte[] serverIV;
    private static byte[] clientIV;

    public static void main(String[] args) throws Exception {
        try {
            server = new ServerSocket(8080);
            Socket socket = server.accept();
            ObjectOutputStream serverOut = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream serverIn = new ObjectInputStream(socket.getInputStream());

            //step-1 - receive
            clientNonce = (byte[]) serverIn.readObject();
//            messages.write(clientNonce);

            //step-2 - send
            CertificateFactory serverCF = CertificateFactory.getInstance("X.509");
            serverCertificate = serverCF.generateCertificate(new FileInputStream("CASignedServerCertificate.pem")); //DATA 1
            serverOut.writeObject(serverCertificate);

            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(dhPrivateKey.toByteArray());
            dhPublicKey =  g.modPow(dhPrivateKey, N); //DATA 2
            serverOut.writeObject(dhPublicKey);

            String serverPvtKeyFile = "serverPrivateKey.der";
            Path path = Paths.get(serverPvtKeyFile);
            byte[] serverPvtKeyArr = Files.readAllBytes(path);
            PKCS8EncodedKeySpec encodedServerPvtKey = new PKCS8EncodedKeySpec(serverPvtKeyArr);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(encodedServerPvtKey);

            Signature serverSignature = Signature.getInstance("SHA256WithRSA");
            serverSignature.initSign(privateKey);
            serverSignature.update(dhPublicKey.toByteArray());
            signedDHPublicKey = new BigInteger(serverSignature.sign());
            serverOut.writeObject(signedDHPublicKey);

            //step-3 receive
            clientCertificate = (Certificate) serverIn.readObject();
            clientDHPublicKey = (BigInteger) serverIn.readObject();
            signedClientDHPublicKey = (BigInteger) serverIn.readObject();

            //step-4 form shared secret key
            sharedSecretKey = clientDHPublicKey.modPow(dhPrivateKey, N);

            //step-5 derive 6 session keys
            byte[] prk = Main.HMAC(clientNonce, sharedSecretKey.toByteArray());
            serverEncrypt = Main.hdkfExpand(prk, "server encrypt".getBytes());
            clientEncrypt = Main.hdkfExpand(serverEncrypt, "client encrypt".getBytes());
            serverMac = Main.hdkfExpand(clientEncrypt, "server MAC".getBytes());
            clientMac = Main.hdkfExpand(serverMac, "client MAC".getBytes());
            serverIV = Main.hdkfExpand(clientMac, "server IV".getBytes());
            clientIV = Main.hdkfExpand(serverIV, "client IV".getBytes());

            //step-6 All handshake message MAC
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            outStream.write(clientNonce);
            outStream.write(serverCertificate.getEncoded());
            outStream.write(dhPublicKey.toByteArray());
            outStream.write(signedDHPublicKey.toByteArray());
            outStream.write(clientCertificate.getEncoded());
            outStream.write(clientDHPublicKey.toByteArray());
            outStream.write(signedClientDHPublicKey.toByteArray());

            byte[] handshakeMsg = outStream.toByteArray();
            byte[] finalMAC = Main.HMAC(serverMac, handshakeMsg);
            serverOut.writeObject(finalMAC);

            byte[] finalClientMAC = (byte[]) serverIn.readObject();
            byte[] clientMsg = (byte[]) serverIn.readObject();
            byte[] decrypt = Main.decrypt(clientEncrypt, clientMsg, clientIV);
            if (Main.isAuthentic(clientMac, decrypt)) {
                String message = Main.getMessage(decrypt);
                System.out.println(message);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
