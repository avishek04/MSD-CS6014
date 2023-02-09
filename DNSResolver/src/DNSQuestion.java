import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class DNSQuestion {
//    public static int questionCount = 0;
    public static DNSQuestion decodeQuestion(ByteArrayInputStream inputStream, DNSMessage message) {
        DNSQuestion dnsQuestion = new DNSQuestion();
        try {
            while (true) {
                byte singleByte = inputStream.readNBytes(1)[0];
                dnsQuestion.questionByteArr.add(singleByte);
                if (singleByte == 0) {
                    break;
                }
            }
            dnsQuestion.questionByteArr.add(inputStream.readNBytes(1)[0]);
            dnsQuestion.questionByteArr.add(inputStream.readNBytes(1)[0]);
            dnsQuestion.questionByteArr.add(inputStream.readNBytes(1)[0]);
            dnsQuestion.questionByteArr.add(inputStream.readNBytes(1)[0]);
            byte[] quesArr = new byte[dnsQuestion.questionByteArr.size()];
            int i = 0;
            for (Byte qByte: dnsQuestion.questionByteArr) {
                quesArr[i] = qByte;
                i++;
            }
            ByteArrayInputStream quesStream = new ByteArrayInputStream(quesArr);
            dnsQuestion.domainName = message.readDomainName(quesStream);
            dnsQuestion.qType = quesStream.readNBytes(2);
            dnsQuestion.qClass = quesStream.readNBytes(2);
            dnsQuestion.nameLength = dnsQuestion.domainName.length();
            dnsQuestion.domainNameArr = message.dnsQuestionList[message.dnsQuesCounter].domainNameArr;
            dnsQuestion.labelCount = message.dnsQuestionList[message.dnsQuesCounter].labelCount;
            message.domainLocations.put(dnsQuestion.domainName, 12);
        }
        catch (Exception ex) {

        }
        return dnsQuestion;
    }

    public void writeBytes(ByteArrayOutputStream byteOutStream, HashMap<String,Integer> domainNameLocations) {
        try {
//            DNSMessage.writeDomainName(byteOutStream, domainNameLocations, domainNameArr);
//            byteOutStream.write(qType);
//            byteOutStream.write(qClass);
            for (Byte qByte: questionByteArr) {
                byteOutStream.write(qByte);
            }
        }
        catch (Exception ex) {

        }
    }
    public ArrayList<Byte> questionByteArr = new ArrayList<>();
    public int labelCount = 0;
    public byte[] qType;
    public byte[] qClass;
    public int nameLength;
    public String domainName;
    public ArrayList<String> domainNameArr = new ArrayList<>();

    public String toString() {
        return DNSMessage.joinDomainName(domainNameArr);
//        return domainNameArr.stream().map(Object::toString).collect(Collectors.joining());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DNSQuestion that)) return false;
        return labelCount == that.labelCount && nameLength == that.nameLength && Arrays.equals(qType, that.qType) && Arrays.equals(qClass, that.qClass) && domainName.equals(that.domainName);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(labelCount, nameLength, domainName, domainNameArr);
        result = 31 * result + Arrays.hashCode(qType);
        result = 31 * result + Arrays.hashCode(qClass);
        return result;
    }
}
