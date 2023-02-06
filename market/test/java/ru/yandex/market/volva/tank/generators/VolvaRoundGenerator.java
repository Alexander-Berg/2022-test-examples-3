package ru.yandex.market.volva.tank.generators;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Value;

import ru.yandex.market.volva.serializer.VolvaJsonUtils;


/**
 * @author dzvyagin
 */
public class VolvaRoundGenerator {

    private final Random random = new Random();
    private List<Long> uids;

    public Round generateVolvaRound() throws Exception {
        String params = "?" +
                String.join("&", "uid=" + getUid(), "accept=PUID");
        return Round.builder()
                .label("/glues")
                .method("GET")
                .url("/glues" + params)
                .body(null)
                .build();
    }

    private Long getUid() throws URISyntaxException, IOException {
        if (uids == null){
            URL url = this.getClass().getClassLoader().getResource("test_uids.txt");
            uids = Files.readAllLines(Path.of(url.toURI())).stream().map(Long::parseLong).collect(Collectors.toList());
        }
        int pos = random.nextInt(uids.size());
        return uids.get(pos);
    }



    @Builder
    @Value
    public static class Round {
        private String label;
        private String method;
        private String url;
        @Builder.Default
        private String httpVersion = "HTTP/1.1";
        private Map<String, Supplier<String>> headers;
        private String body;

        public String getAmmoText() {
            StringBuilder sb = new StringBuilder();
            sb.append(label)
                    .append("\n")
                    .append(method)
                    .append(" ")
                    .append(url)
                    .append(" ")
                    .append(httpVersion)
                    .append("\n");
            for (var header : headers.entrySet()) {
                sb.append(header.getKey())
                        .append(": ")
                        .append(header.getValue().get())
                        .append("\n");
            }
            if (body != null) {
                sb.append("Content-Length: ")
                        .append(body.length())
                        .append("\n\n")
                        .append(body);
            }
            int length = getLength() ;
            return length + " " + sb.toString();
        }

        private int getLength(){
            int textLength = 0;
//        textLength += id.length() + 1;  // + new string symbol
            textLength += method.length() + 1 + url.length() +1 + httpVersion.length() +1;
            for (var entry : headers.entrySet()){
                textLength += entry.getKey().length();
                textLength += entry.getValue().get().length();
                textLength += 3; // + delimiter ": " and new string symbol
            }
            textLength += 2; // plus 2 strings delimiter
            if (body != null){
                textLength += body.length() +2;
            }
            return textLength;
        }
    }
}
