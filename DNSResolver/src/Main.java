import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class Main {
    public static void sendResponseBack(DatagramSocket udpSocket, byte[] dataArr, byte[] ip, int port) throws IOException {
        InetAddress clientAdd = InetAddress.getByAddress(ip);
        DatagramPacket toClient = new DatagramPacket(dataArr, dataArr.length, clientAdd, port);
        udpSocket.send(toClient);
    }

    public static DNSMessage sendRequestToGoogle(byte[] dataArr) {
        DNSMessage dnsMessage = null;
        try {
            DatagramSocket udpSocket = new DatagramSocket(9000);
            InetAddress googleAdd = InetAddress.getByName("8.8.8.8");
            DatagramPacket toGoogle = new DatagramPacket(dataArr, dataArr.length, googleAdd, 53);
            udpSocket.send(toGoogle);

            int length = 512;
            byte[] dataBuffer = new byte[length];
            DatagramPacket fromGoogle = new DatagramPacket(dataBuffer, length);
            udpSocket.receive(fromGoogle);
            dnsMessage = DNSMessage.decodeMessage(fromGoogle.getData());
        }
        catch (Exception ex) {

        }
        return dnsMessage;
    }

    public static void main(String[] args) {
        try {
            DatagramSocket udpSocket = new DatagramSocket(8053);
            int length = 512;
            while (true) {
                byte[] dataBuffer = new byte[length];
                DatagramPacket dataPacket = new DatagramPacket(dataBuffer, length);
                udpSocket.receive(dataPacket);
                //String clientIp = dataPacket.getAddress().toString();
                byte clientIp[] = { 127, 0, 0, 1 };
                int clientPort = dataPacket.getPort();

                DNSMessage dnsMessage = DNSMessage.decodeMessage(dataPacket.getData());
                DNSRecord dnsRecord = DNSCache.checkDnsCache(dnsMessage.dnsQuestionList[0]);
                if (dnsRecord != null) {
//                    dnsMessage.dnsAnswers = new DNSRecord[1];
//                    dnsMessage.dnsAnswers[0] = dnsRecord;
                    DNSMessage response = DNSMessage.buildResponse(dnsMessage, new DNSRecord[] {dnsRecord});
                    sendResponseBack(udpSocket, dnsMessage.toBytes(), clientIp, clientPort);
                }
                else {
                    DNSMessage googleResponse = sendRequestToGoogle(dataPacket.getData());
                    if (googleResponse.dnsHeader.answerCount > 0) {
                        sendResponseBack(udpSocket, googleResponse.toBytes(), clientIp, clientPort);
                        DNSCache.dnsHash(googleResponse.dnsQuestionList[0], googleResponse.dnsAnswers[0]);
                    }
                }
            }
        }
        catch (Exception ex) {

        }
    }
}