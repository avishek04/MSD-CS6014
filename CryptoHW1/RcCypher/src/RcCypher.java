import org.junit.Assert;
import java.nio.charset.StandardCharsets;

public class RcCypher {
    private int[] s;
    private int i, j;
    private byte[] roundKey;
    public void GenerateRoundKey(byte[] message) {
        roundKey = new byte[message.length];
        for (int i = 0; i < roundKey.length; i++) {
            roundKey[i] = nextByte();
        }
    }

    public RcCypher(byte[] key) {
        j = 0;
        s = new int[256];

        for (int k = 0; k < 256; k++) {
            s[k] = k;
        }

        for (int k = 0; k < 256; k++) {
            j = (j + s[k] + key[k % key.length]) % 256;
            int temp = s[k];
            s[k] = s[j];
            s[j] = temp;
        }
        i = j = 0;
    }

    public byte nextByte() {
        i = (++i) % 256;
        j = (j + s[i]) % 256;
        int temp = s[i];
        s[i] = s[j];
        s[j] = temp;
        return (byte) s[(s[i] + s[j]) % 256];
    }

    public byte[] encrypt(byte[] message) {
        GenerateRoundKey(message);
        byte[] cypherTxt = new byte[message.length];
        for (int i = 0; i < message.length; i++) {
            cypherTxt[i] = (byte) (message[i] ^ roundKey[i]);
        }
        return cypherTxt;
    }

    public byte[] decrypt(byte[] cypherTxt) {
        byte[] text = new byte[cypherTxt.length];
        for (int i = 0; i < cypherTxt.length; i++) {
            if (roundKey != null) {
                text[i] = (byte) (cypherTxt[i] ^ roundKey[i]);
            }
            else {
                text[i] = (byte) (cypherTxt[i] ^ nextByte());
            }
        }
        return text;
    }

    public static void main(String[] args){

        //RC Cypher - Section 1

        byte[] key1 = "firstKey".getBytes();
        String  message1 = "Hi Alice this is Bob";
        RcCypher rc1 = new RcCypher(key1);
        byte[] cypherText1 = rc1.encrypt(message1.getBytes(StandardCharsets.UTF_8));
        byte[] decryptText1 = rc1.decrypt(cypherText1);

        byte[] key2 = "secondKey".getBytes();
        RcCypher rc2 = new RcCypher(key2);
        byte[] decryptText2 = rc2.decrypt(cypherText1);

        Assert.assertEquals(message1, new String(decryptText1,StandardCharsets.UTF_8));
        Assert.assertNotEquals(message1, new String(decryptText2,StandardCharsets.UTF_8));

        ////RC Cypher - Section 2

        String  message2 = "This is CS6014";
        byte[] cypherTextSameKey1 = rc1.encrypt(message2.getBytes(StandardCharsets.UTF_8));

        byte[] sameKeyRes = new byte[cypherTextSameKey1.length];
        for (int i = 0; i < sameKeyRes.length; i++) {
            sameKeyRes[i] = (byte) (cypherText1[i] ^ cypherTextSameKey1[i]);
        }

        System.out.println("Message: " + new String(decryptText1, StandardCharsets.UTF_8));
        System.out.println("Decrypt XOR: " + new String(sameKeyRes, StandardCharsets.UTF_8));

        //RC Cypher - Section 3

        String orgMsg = "Your salary is $1000";
        byte[] orgMsgStrm = orgMsg.getBytes(StandardCharsets.UTF_8);

        String modMsg = "Your salary is $9999";
        byte[] modMsgStrm = modMsg.getBytes(StandardCharsets.UTF_8);


        byte[] orgCypher = rc1.encrypt(orgMsgStrm);
        byte[] modCypher =  new byte[orgCypher.length];

        for (int i = 0; i < modCypher.length; i++) {
            modCypher[i] = (byte) ((orgMsgStrm[i] ^ rc1.roundKey[i]) ^ (orgMsgStrm[i] ^ modMsgStrm[i]));
        }
        byte[] dcryptModMsg = rc1.decrypt(modCypher);

        System.out.println(new String(dcryptModMsg, StandardCharsets.UTF_8));
        Assert.assertEquals(modMsg, new String(dcryptModMsg, StandardCharsets.UTF_8));
    }
}

