import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

public class DNSMessage {
    public byte[] byteArray;
    public DNSHeader dnsHeader;
    public DNSQuestion[] dnsQuestionList;
    public int dnsQuesCounter = -1;
    public DNSRecord[] dnsAnswers;
    public byte[] dnsAdditionalRecords; // we need to send it back
    public HashMap<String, Integer> domainLocations = new HashMap<>();
    public static DNSMessage decodeMessage(byte[] dataArr) {
        DNSMessage dnsMessage = new DNSMessage();
        dnsMessage.byteArray = dataArr;
        ByteArrayInputStream messageStream = new ByteArrayInputStream(dataArr);

        try {
            //Header Decoding
            dnsMessage.dnsHeader = DNSHeader.decodeHeader(messageStream);

            //Questions Decoding
            dnsMessage.dnsQuestionList = new DNSQuestion[dnsMessage.dnsHeader.questions];

            for (int i = 0; i < dnsMessage.dnsQuestionList.length; i++) {
                dnsMessage.dnsQuestionList[i] = new DNSQuestion();
                dnsMessage.dnsQuesCounter++;
                dnsMessage.dnsQuestionList[i] = DNSQuestion.decodeQuestion(messageStream, dnsMessage);
            }

            //Record Decoding
            if (!dnsMessage.dnsHeader.isQuery && dnsMessage.dnsHeader.answerCount > 0) {
                dnsMessage.dnsAnswers = new DNSRecord[dnsMessage.dnsHeader.answerCount];

                for (int i = 0; i < dnsMessage.dnsHeader.answerCount; i++) {
                    dnsMessage.dnsAnswers[i] = DNSRecord.decodeRecord(messageStream, dnsMessage);
                }
            }

            //Additional Records
            dnsMessage.dnsAdditionalRecords = messageStream.readAllBytes();
        }
        catch (Exception ex) {

        }
        return dnsMessage;
    }

    public String readDomainName(ByteArrayInputStream inputStream) {
        try {
            int labelLen = inputStream.readNBytes(1)[0];
            while (true) {
                byte[] label = inputStream.readNBytes(labelLen);
                StringBuilder strLabel = new StringBuilder();
                for (byte ascii: label) {
                    strLabel.append((char) ascii);
                }
                dnsQuestionList[dnsQuesCounter].domainNameArr.add(String.valueOf(strLabel));
                dnsQuestionList[dnsQuesCounter].labelCount++;
                labelLen = inputStream.readNBytes(1)[0];
                if (labelLen == 0) {
                    break;
                }
            }
        }
        catch (Exception ex) {

        }
        return joinDomainName(dnsQuestionList[dnsQuesCounter].domainNameArr);
    }

//    public String[] readDomainName(int firstByte) {
//
//    }

    public static DNSMessage buildResponse(DNSMessage request, DNSRecord[] answers) {
        DNSMessage dnsResponse = new DNSMessage();
        dnsResponse.dnsHeader = DNSHeader.buildHeaderForResponse(request, dnsResponse);
        dnsResponse.dnsQuestionList = request.dnsQuestionList;
        dnsResponse.dnsAnswers = answers;
        dnsResponse.dnsAdditionalRecords = request.dnsAdditionalRecords;
        return dnsResponse;
    }

    public byte[] toBytes() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            dnsHeader.writeBytes(outputStream);
            for (DNSQuestion question: dnsQuestionList) {
                question.writeBytes(outputStream, domainLocations);
            }
            for (DNSRecord dnsAnswer: dnsAnswers) {
                dnsAnswer.writeBytes(outputStream, domainLocations);
            }
            outputStream.write(dnsAdditionalRecords);
        }
        catch (Exception ex) {

        }
        return outputStream.toByteArray();
    }

    //write bytes DNS Question and DNS Header
    //Integer - offset
    //string - domainname
    public static void writeDomainName(ByteArrayOutputStream byteArrOutStream, HashMap<String,Integer> domainLocations, ArrayList<String> domainPieces) {
        try {
            String domainName = joinDomainName(domainPieces);
            //int domainLocation = domainLocations.get(domainName);
            byteArrOutStream.write(domainName.getBytes());
        }
        catch (Exception ex) {

        }
    }

    public static String joinDomainName(ArrayList<String> pieces) {
        return pieces.stream().map(Object::toString).collect(Collectors.joining("."));
    }

//    public String toString() {
//
//    }
}
