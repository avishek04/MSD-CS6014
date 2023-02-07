import java.util.HashMap;

public class DNSCache {
    public static HashMap<DNSQuestion, DNSRecord> domainCache;

    public static void dnsHash(DNSQuestion dnsQuestion, DNSRecord dnsRecord) {
        if (domainCache == null || domainCache.isEmpty()) {
            domainCache = new HashMap<>();
        }
        domainCache.put(dnsQuestion, dnsRecord);
    }

    public static DNSRecord checkDnsCache(DNSQuestion dnsQuestion) {
        if (domainCache != null && !domainCache.isEmpty() && domainCache.containsKey(dnsQuestion)) {
            DNSRecord dnsRecord = domainCache.get(dnsQuestion);
            if (!dnsRecord.isExpired()) {
                return dnsRecord;
            }
        }
        return null;
    }
}
