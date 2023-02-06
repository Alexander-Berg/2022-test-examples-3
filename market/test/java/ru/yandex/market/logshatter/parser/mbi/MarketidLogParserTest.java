package ru.yandex.market.logshatter.parser.mbi;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.trace.Environment;

public class MarketidLogParserTest {

    LogParserChecker checker = new LogParserChecker(new MarketidLogParser());

    @Test
    public void parse() throws Exception {

        String line1 = "tskv\tdate=2019-09-16T13:30:54" +
            ".969+03:00\ttype=IN\trequest_id=1568629854813/4aac32a2a84ee565addb929a36ba22ca\tsource_host=/2a02:6b8" +
            ":c02:442:0:577:7719:c67d:43876\ttarget_module=market_id\ttarget_host=sas3-1358-8a9-sas-market-prod--08e" +
            "-26834\tprotocol=grpc\trequest_method=ru.yandex.market.id" +
            ".MarketIdService/updateLegalInfo\ttime_millis=156";
        String line2 = "tskv\tdate=2019-09-16T13:30:55" +
            ".161+03:00\ttype=IN\trequest_id=1568629855143/6ba5511ee0f6f0506305365f3af0b35e\tsource_host=/2a02:6b8" +
            ":c02:442:0:577:7719:c67d:43876\ttarget_module=market_id\ttarget_host=sas3-1358-8a9-sas-market-prod--08e" +
            "-26834\tprotocol=grpc\trequest_method=ru.yandex.market.id" +
            ".MarketIdService/getOrCreateMarketId\ttime_millis=18\terror_code=INTERNAL";
        String line3 = "tskv\tdate=2019-09-16T13:30:55" +
            ".161+03:00\ttype=IN\trequest_id=1568629855143/6ba5511ee0f6f0506305365f3af0b35e\tsource_host=/2a02:6b8" +
            ":c02:442:0:577:7719:c67d:43876\ttarget_module=market_id\ttarget_host=sas3-1358-8a9-sas-market-prod--08e" +
            "-26834\tprotocol=http\trequest_method=ru.yandex.market.id" +
            ".MarketIdService/getOrCreateMarketId\ttime_millis=18\terror_code=INTERNAL";


        checker.check(
            line1,
            1568629854,
            checker.getHost(), Environment.UNKNOWN,
            "/2a02:6b8:c02:442:0:577:7719:c67d:43876",
            "sas3-1358-8a9-sas-market-prod--08e-26834",
            "1568629854813/4aac32a2a84ee565addb929a36ba22ca",
            "ru.yandex.market.id.MarketIdService/updateLegalInfo",
            156,
            ""
        );

        checker.check(
            line2,
            1568629855,
            checker.getHost(), Environment.UNKNOWN,
            "/2a02:6b8:c02:442:0:577:7719:c67d:43876",
            "sas3-1358-8a9-sas-market-prod--08e-26834",
            "1568629855143/6ba5511ee0f6f0506305365f3af0b35e",
            "ru.yandex.market.id.MarketIdService/getOrCreateMarketId",
            18,
            "INTERNAL"
        );

        checker.checkEmpty(
            line3
        );


    }
}
