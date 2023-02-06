package ru.yandex.msearch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ParseLogsTest {
    private static final int TIMEOUT = 10000;

    @Ignore
    @Test
    public void test() throws Exception {
        ProcessBuilder pb = new ProcessBuilder().
            command("/usr/bin/perl", "src/perseus/main/bundle/msearch2tskv.pl");

        Process process = pb.start();
        BufferedWriter writer =
            new BufferedWriter(
                new OutputStreamWriter(
                    process.getOutputStream(),
                    StandardCharsets.UTF_8));

        writer.write(
            "2a02:6b8:b000:100:225:90ff:fee3:a16e - - "
                + "[15/Apr/2018:11:40:54 +0300] \"GET "
                + "/search-async-mail-search?user-id-field=uid"
                + "&user-id-term=1130000027514633&get=mid,fid,hdr_subject,"
                + "hdr_from,received_date,folder_type,message_type,"
                + "has_attachments,clicks_serp_count,clicks_total_count,"
                + "received_date&prefix=1130000027514633&group=mid"
                + "&merge_func=none&text=(mid_p:sunil+OR+pure_body:"
                + "sunil+OR+hdr_subject:sunil+OR+hdr_from:sunil+OR+"
                + "hdr_from_normalized:sunil+OR+hdr_to:sunil+OR+"
                + "hdr_to_normalized:sunil+OR+hdr_cc:sunil+OR+"
                + "hdr_cc_normalized:sunil+OR+hdr_bcc:sunil+OR+"
                + "hdr_bcc_normalized:sunil+OR+reply_to:sunil+OR+"
                + "reply_to_normalized:sunil+OR+attachname:sunil+OR+"
                + "attachtype:sunil+OR+attachments:sunil+OR+body_text:sunil+"
                + "OR+album:sunil+OR+artist:sunil+OR+author:sunil+OR+comment:"
                + "sunil+OR+composer:sunil+OR+description:sunil+OR+genre:sunil"
                + "+OR+keywords:sunil+OR+subject:sunil+OR+title:sunil)"
                + "&service=change_log&sort=received_date&collector="
                + "pruning(received_day_p)&length=400&offset=0 HTTP/1.1\""
                + " 200 132994 183 3AY5O3 HCZ9RO \"/api/async/mail/suggest?"
                + "lang=en&tzoffset=240&twoSteps=1&types=history%2Csubject"
                + "%2Cimportant%2Cunread%2Cfolder%2Cql%2Ccontact%2Cmail%2C"
                + "label&request=sunil&limit=10&history=yes&highlight=1"
                + "&reqid=15237791653751130000027514633&mdb=pg"
                + "&remote_ip=94.206.72.62&side=webpdd&suid=1130000044717112"
                + "&uid=1130000027514633&timeout=200\" 1380");
        writer.newLine();
        writer.write(
            "0:0:0:0:0:0:0:1 - - [15/Apr/2018:11:20:49 +0300] "
                + "\"GET /search?json-type=dollar&IO_PRIO=3000&memory-limit"
                + "=33554432&prefix=51863&text=peach_url:1+AND+NOT+"
                + "peach_queue:*&sort=peach_sequence&get=url,peach_sequence,"
                + "peach_url,peach_payload&length=100&asc HTTP/1.1\" "
                + "200 30 1 3AY94W - \"-\" 0");
        writer.newLine();
        writer.write("0:0:0:0:0:0:0:1 - - [15/Apr/2018:11:21:57 +0300] "
            + "\"POST /update?senders&mid=165225811329156909&uid=343253416 "
            + "HTTP/1.1\" 200 - 20 3B2MQK 35RSRO \"-\"");
        writer.newLine();
        writer.flush();
        writer.close();

        BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(
                    process.getInputStream(),
                    StandardCharsets.UTF_8));
        List<String> records = new ArrayList<>();
        final int interval = 100;
        final long deadline = System.currentTimeMillis() + TIMEOUT;
        while (records.size() != 3 && deadline > System.currentTimeMillis()) {
            records.addAll(reader.lines().collect(Collectors.toList()));
            Thread.sleep(interval);
        }

        reader.close();
        Assert.assertEquals(
            "tskv\ttskv_format=access-log-msearch\t"
            + "vhost=lucene.mail.yandex.net\tip=2a02:6b8:b000:100:225:90ff:"
                + "fee3:a16e\ttimestamp=2018-04-15 11:40:54\ttimezone=+0300\t"
                + "method=GET\trequest=/search-async-mail-search?"
                + "user-id-field=uid&user-id-term=1130000027514633&get=mid,fid,"
                + "hdr_subject,hdr_from,received_date,folder_type,message_type,"
                + "has_attachments,clicks_serp_count,clicks_total_count,"
                + "received_date&prefix=1130000027514633&group=mid&merge_func="
                + "none&text=(mid_p:sunil+OR+pure_body:sunil+OR+"
                + "hdr_subject:sunil+OR+hdr_from:sunil+OR+hdr_from_normalized:"
                + "sunil+OR+hdr_to:sunil+OR+hdr_to_normalized:sunil+OR+"
                + "hdr_cc:sunil+OR+hdr_cc_normalized:sunil+OR+hdr_bcc:sunil+"
                + "OR+hdr_bcc_normalized:sunil+OR+reply_to:sunil+OR+"
                + "reply_to_normalized:sunil+OR+attachname:sunil+OR+"
                + "attachtype:sunil+OR+attachments:sunil+OR+body_text:sunil+"
                + "OR+album:sunil+OR+artist:sunil+OR+author:sunil+OR+"
                + "comment:sunil+OR+composer:sunil+OR+description:sunil+OR+"
                + "genre:sunil+OR+keywords:sunil+OR+subject:sunil+"
                + "OR+title:sunil)&service=change_log&sort=received_date"
                + "&collector=pruning(received_day_p)&length=400&offset=0\t"
                + "protocol=HTTP/1.1\tstatus=200\tresponse_size=132994\t"
                + "processing_millitime=183\tsession_id=3AY5O3\t"
                + "proxy_session_id=HCZ9RO\treferer=/api/async/mail/suggest?"
                + "lang=en&tzoffset=240&twoSteps=1&types=history%2Csubject%2C"
                + "important%2Cunread%2Cfolder%2Cql%2Ccontact%2Cmail%2Clabel"
                + "&request=sunil&limit=10&history=yes&highlight=1"
                + "&reqid=15237791653751130000027514633&mdb=pg&remote_ip="
                + "94.206.72.62&side=webpdd&suid=1130000044717112&uid="
                + "1130000027514633&timeout=200\tdocs_count=1380",
        records.get(0));
        String exp2 =
            "tskv\ttskv_format=access-log-msearch\tvhost=lucene.mail.yandex.net"
                + "\tip=0:0:0:0:0:0:0:1\ttimestamp=2018-04-15 11:20:49\t"
                + "timezone=+0300\tmethod=GET\trequest=/search?json-type=dollar"
                + "&IO_PRIO=3000&memory-limit=33554432&prefix=51863&text="
                + "peach_url:1+AND+NOT+peach_queue:*&sort=peach_sequence"
                + "&get=url,peach_sequence,peach_url,peach_payload"
                + "&length=100&asc\tprotocol=HTTP/1.1\tstatus=200\t"
                + "response_size=30\tprocessing_millitime=1\tsession_id=3AY94W"
                + "\tproxy_session_id=-\treferer=-\tdocs_count=0";
        Assert.assertEquals(exp2, records.get(1));

        String exp3 = "tskv\ttskv_format=access-log-msearch\tvhost="
            + "lucene.mail.yandex.net\tip=0:0:0:0:0:0:0:1\t"
            + "timestamp=2018-04-15 11:21:57\ttimezone=+0300\tmethod=POST\t"
            + "request=/update?senders&mid=165225811329156909&uid=343253416\t"
            + "protocol=HTTP/1.1\tstatus=200\tresponse_size=-\t"
            + "processing_millitime=20\tsession_id=3B2MQK\t"
            + "proxy_session_id=35RSRO\treferer=-\tdocs_count=0";
        Assert.assertEquals(exp3, records.get(2));
        process.destroyForcibly();
    }
}
