import java.io.*;

public class DNSHeader {
    private byte[] byteArray;
    public int transactId;
    public boolean isQuery;
    public int opcode;
    public int authoritativeAns;
    public boolean truncated;
    public boolean recursionDesired;
    public boolean recursionAvailable;
    public int questions;
    public int answerCount;
    public int nameServerCount;
    public int additionalCount;

    public static DNSHeader decodeHeader(ByteArrayInputStream byteInputStream) {
        DNSHeader dnsHeader = new DNSHeader();
        try {
            dnsHeader.byteArray = byteInputStream.readNBytes(12);
            dnsHeader.transactId = ((dnsHeader.byteArray[0] & 0x00FF) << 8) | (dnsHeader.byteArray[1] & 0x00FF);
            dnsHeader.isQuery = ((dnsHeader.byteArray[2] >> 7) & 0x01) == 0;
            dnsHeader.opcode = (dnsHeader.byteArray[2] >> 3) & 0x0F;
            dnsHeader.authoritativeAns = (dnsHeader.byteArray[2] >> 2) & 0x01;
            dnsHeader.truncated = ((dnsHeader.byteArray[2] >> 1) & 0x01) == 1;
            dnsHeader.recursionDesired = (dnsHeader.byteArray[2] & 0x01) == 1;
            dnsHeader.recursionAvailable = ((dnsHeader.byteArray[3] >> 7) & 0x01) == 1;
            dnsHeader.questions = ((dnsHeader.byteArray[4] & 0x00FF) << 8) | (0x00FF & dnsHeader.byteArray[5]);
            dnsHeader.answerCount = ((dnsHeader.byteArray[6] & 0x00FF) << 8) | (0x00FF & dnsHeader.byteArray[7]);
            dnsHeader.nameServerCount = ((dnsHeader.byteArray[8] & 0x00FF) << 8) | (0x00FF & dnsHeader.byteArray[9]);
            dnsHeader.additionalCount = ((dnsHeader.byteArray[10] & 0x00FF) << 8) | (0x00FF & dnsHeader.byteArray[11]);
        }
        catch (Exception ex) {
        }
        return dnsHeader;
    }

    public static DNSHeader buildHeaderForResponse(DNSMessage request, DNSMessage response) {
        DNSHeader responseHeader;
        responseHeader = request.dnsHeader;
        responseHeader.isQuery = false; //1
        responseHeader.authoritativeAns = 0;
        responseHeader.answerCount = 1;
        responseHeader.byteArray[2] = (byte) 0x81;
        responseHeader.byteArray[3] = (byte) 0x80;
        responseHeader.byteArray[5] = (byte) 0x01;
        responseHeader.byteArray[7] = (byte) 0x01;
        return responseHeader;
    }

    public void writeBytes(ByteArrayOutputStream outputStream) {
        try {
//            byte[] outByte = byteArray;
//            outByte[2] = (byte) (byteArray[3] | 0x80);
//            outByte[7] = 0x00;
//            outByte[8] = 0x01;
//            outputStream.write(outByte);
            outputStream.write(byteArray);
        }
        catch (Exception ex) {

        }
    }

//    public String toString() {
//
//    }
}
