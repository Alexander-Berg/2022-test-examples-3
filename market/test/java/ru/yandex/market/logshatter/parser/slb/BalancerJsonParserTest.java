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
 * * @date 23/04/2020
 */
public class BalancerJsonParserTest {

    @Test
    public void testParser() throws Exception {
        final LogParserChecker checker = getChecker("/configs/slb/balancer.json");
        final URL resource = getClass().getClassLoader().getResource("market-slb-balancer-json.log");
        final String line = FileUtils.readLines(new File(resource.toURI()), StandardCharsets.UTF_8).get(0);
        checker.check(
            line,
            new Date(1587582857425L),
            UnsignedLong.valueOf(1L),
            "2a02:6b8:c07:a2b:10b:11e5:3a0c:0",
            50836,
            5.207,
            "kadavr2.vs.market.yandex.net",
            "",
            "GET",
            "HTTP/1.1",
            "",
            UnsignedLong.valueOf(0L),
            5.298,
            UnsignedLong.valueOf(1587582857420L),
            "2020-04-22T22:14:17.420358+03:00",
            UnsignedLong.valueOf(448L),
            5.165,
            UnsignedLong.valueOf(1587582857425L),
            "2020-04-22T22:14:17.425656+03:00",
            "[fdee:fdee:0:3400:0:3c9:0:15d]:80",
            "kadavr2",
            200L,
            "succ 200",
            "/tarantino/getcontextpage?format=json&rearr-factors=&type=mp_corona_delivery&region=213",
            "AHC/2.1",
            "2a02:6b8:0:3400:0:3c9:0:15d",
            80,
            "2a02:6b8:c07:a2b:10b:11e5:3a0c:0",
            "1587582857379/3f40c9c0d80ba971336a4042cf6501ae",
            "2a02:6b8:c07:a2b:10b:11e5:3a0c:0",
            "771,49196-49195-49199-49200-49171-49172-156-47-53,0-5-10-11-13-50-23-43-65281,23-24-25,0",
            "1027-1283-1539-2052-2053-2054-2057-2058-2059-1025-1281-1537-1026-771-769-770-515-513-514," +
                "1027-1283-1539-2052-2053-2054-2057-2058-2059-1025-1281-1537-1026-771-769-770-515-513-514," +
                "772-771-770-769,,23,1"
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
