import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;

public class DNSRecord {
    public static DNSRecord decodeRecord(ByteArrayInputStream inStream, DNSMessage dnsMessage) {
        DNSRecord dnsRecord = new DNSRecord();
        try {
            dnsRecord.domainNameArr = inStream.readNBytes(2);
            dnsRecord.domainName = dnsMessage.dnsQuestionList[0].toString();
            dnsRecord.type = inStream.readNBytes(2);
            dnsRecord.recordClass = inStream.readNBytes(2);
            dnsRecord.ttlByte = inStream.readNBytes(4);
            dnsRecord.ttl = ((dnsRecord.ttlByte[0] & 0x000000FF) << 24) | ((dnsRecord.ttlByte[1] & 0x000000FF) << 16) |
                    ((dnsRecord.ttlByte[2] & 0x000000FF) << 8) | ((dnsRecord.ttlByte[3] & 0x000000FF));
            dnsRecord.rOctetLenByte = inStream.readNBytes(2);
            dnsRecord.rOctetLength = (dnsRecord.rOctetLenByte[0] & 0x00FF << 8) | dnsRecord.rOctetLenByte[1];
            dnsRecord.rData = inStream.readNBytes(dnsRecord.rOctetLength);

            for (int i = 0; i < dnsRecord.rOctetLength; i++) {
                dnsRecord.ipAddr += Integer.toString(dnsRecord.rData[i] & 0xFF);
                if (i < dnsRecord.rOctetLength - 1) {
                    dnsRecord.ipAddr += ".";
                }
            }
            dnsRecord.dateCreated = System.currentTimeMillis() / 1000;
        }
        catch (Exception ex) {

        }
        return dnsRecord;
    }

    public void writeBytes(ByteArrayOutputStream outStream, HashMap<String, Integer> hashMap) {
        try {
            short offset = hashMap.get(domainName).shortValue();
            byte[] compress = new byte[2];
            compress[0] = (byte)((byte)(offset >> 8) & 0xC0);
            compress[1] = (byte)((offset << 8) >> 8);
            outStream.write(compress);
            outStream.write(type);
            outStream.write(recordClass);
            outStream.write(ttlByte);
            outStream.write(rData);
        }
        catch (Exception ex) {

        }
    }

//    public String toString() {
//
//    }

    public boolean isExpired() {
        return (dateCreated + ttl) < (System.currentTimeMillis() / 1000);
    }

    public long dateCreated;
    public String domainName;
    public byte[] domainNameArr;
    public byte[] type;
    public byte[] recordClass;
    public byte[] ttlByte;
    public Integer ttl;
    public byte[] rOctetLenByte;
    public Integer rOctetLength;
    public byte[] rData;
    public String ipAddr = "";
}
