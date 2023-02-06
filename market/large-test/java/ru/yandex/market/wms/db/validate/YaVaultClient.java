package ru.yandex.market.wms.db.validate;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.output.WriterOutputStream;

public class YaVaultClient implements VaultClient {
    @Override
    public Map<String, String> getSecretEntries(String secretId) {
        try {
            String secretValue = getSecret(secretId);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(secretValue);
            Map<String, String> entries = new HashMap<>();
            root.fields().forEachRemaining(e -> entries.put(e.getKey(), e.getValue().textValue()));
            return entries;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Получаем из секретницы содежимое секрета.
     *
     * @param secret ID секрета
     * @return содержимое секрета
     * @throws Exception
     */
    private String getSecret(String secret) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("ya", "vault", "get", "version", secret, "-o");
        Process p = pb.start();
        StringWriter sw = new StringWriter();
        OutputStream osw = new WriterOutputStream(sw, StandardCharsets.UTF_8);
        PrintWriter pw = new PrintWriter(System.err);
        OutputStream opw = new WriterOutputStream(pw, StandardCharsets.UTF_8);
        while (p.isAlive()) {
            p.getErrorStream().transferTo(opw);
            p.getInputStream().transferTo(osw);
        }
        p.getErrorStream().transferTo(opw);
        p.getInputStream().transferTo(osw);

        osw.close();
        opw.close();
        if (p.waitFor() == 0) {
            return sw.toString();
        } else {
            throw new RuntimeException(String.format("Failed to acquire secret %s", secret));
        }
    }
}
