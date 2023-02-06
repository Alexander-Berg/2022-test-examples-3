package ru.yandex.market.logshatter.parser.slb;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.naming.ConfigurationException;

import com.google.common.primitives.UnsignedLong;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import ru.yandex.market.health.configs.logshatter.json.JsonParser;
import ru.yandex.market.health.configs.logshatter.json.JsonParserConfig;
import ru.yandex.market.logshatter.config.ConfigurationService;
import ru.yandex.market.logshatter.parser.LogParserChecker;

/**
 * @author Maksim Kupriianov <a href="mailto:maxk@yandex-team.ru"></a>
 * * @date 24/03/2020
 */
public class HaproxyJsonParserTest {

    @Test
    public void testParser() throws Exception {
        final LogParserChecker checker = getChecker("/configs/slb/haproxy.json");
        final URL resource = getClass().getClassLoader().getResource("market-slb-haproxy-json.log");
        final String line = FileUtils.readLines(new File(resource.toURI()), StandardCharsets.UTF_8).get(0);
        checker.check(
            line,
            new Date(1585038130856L),
            UnsignedLong.valueOf(58L),
            "market-partner.tst.vs.market.yandex.net:443",
            UnsignedLong.valueOf(0L),
            "2a02:6b8:c02:5be:0:633:b9ff:52e1",
            44202,
            UnsignedLong.valueOf(5L),
            "fdee:fdee:0:3400:0:3c9:0:1a0",
            49002,
            UnsignedLong.valueOf(1585038130857L),
            "2020-03-24T11:22:10.857+03:00",
            UnsignedLong.valueOf(6L),
            "fdee:fdee:0:3400:0:3c9:0:1a0",
            "market-partner.tst.vs.market.yandex.net:443",
            80,
            "partner.market.fslb.yandex.ru",
            "GET",
            "HTTP/1.1",
            "fslb01ht.market.yandex.net",
            UnsignedLong.valueOf(22578L),
            0L,
            0L,
            51L,
            UnsignedLong.valueOf(2214L),
            53L,
            UnsignedLong.valueOf(1585038130857L),
            "2020-03-24T11:22:10.857+03:00",
            0L,
            UnsignedLong.valueOf(633L),
            1L,
            UnsignedLong.valueOf(1585038130911L),
            "2020-03-24T11:22:10.911+03:00",
            UnsignedLong.valueOf(1L),
            "2a02:6b8:c08:579f:10b:51f8:0:492a",
            1L,
            "sas1-2415-1d0-sas-market-test--29a-18730.gencfg-c.yandex.net",
            18730,
            UnsignedLong.valueOf(1L),
            UnsignedLong.valueOf(0L),
            200L,
            "----",
            "/market-partner/_/8ea0bed789d1af0a4a84192f07aac45c.svg",
            "2a02:6b8:c13:2229:0:1400:8237:0",
            "1585038130856/3f526e7e86120978f3c0be7295a10500",
            "2a02:6b8:c13:2229:0:1400:8237:0"
        );
    }

    private LogParserChecker getChecker(String configFilePath) throws Exception {
        return new LogParserChecker(new JsonParser(readParserConfig(configFilePath)));
    }

    private JsonParserConfig readParserConfig(String configFilePath) throws IOException, ConfigurationException {
        JsonObject configObject;
        try (Reader confReader = new InputStreamReader(getClass().getResourceAsStream(configFilePath))) {
            configObject = new Gson().fromJson(confReader, JsonObject.class);
        }
        return ConfigurationService.getJsonParserConfig(configObject);
    }
}
