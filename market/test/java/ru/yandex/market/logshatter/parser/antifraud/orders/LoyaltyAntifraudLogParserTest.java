package ru.yandex.market.logshatter.parser.antifraud.orders;

import java.text.SimpleDateFormat;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.EnvironmentMapper;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.trace.Environment;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 04.10.2019
 */
public class LoyaltyAntifraudLogParserTest {
    private LogParserChecker checker = new LogParserChecker(new LoyaltyAntifraudLogParser());

    {
        checker.setParam(EnvironmentMapper.LOGBROKER_PROTOCOL_PREFIX + checker.getOrigin(), "DEVELOPMENT");
    }

    @Test
    public void testRecordWithBlacklistVerdict() throws Exception {
        checker.check(
            "tskv\tdatetime=[2019-10-05T00:14:00 +0300]\tfraudStatus=BLACKLIST\tpromoVerdicts=\t" +
                "detectorName=BlackListDetector\trequest={coins=[CoinDto(coinId=88783817, promoId=27152), CoinDto" +
                "(coinId=88783738, promoId=27179)], uid=730661189, order_ids=[123, 345], reason=USER_CHECK }\t" +
                "users=[" +
                "userId=730661189, idType=uid, " +
                "userId=10295702479889631545, idType=crypta_id, ]",
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z").parse("2019-10-05T00:14:00 +0300"),
            checker.getHost(),
            Environment.DEVELOPMENT,
            "BLACKLIST",
            new String[0],
            "BlackListDetector",
            new String[]{
                "coinId=88783817@@promoId=27152",
                "coinId=88783738@@promoId=27179"
            },
            730661189L,
            new Long[]{123L, 345L},
            "USER_CHECK",
            new String[]{
                "userId=730661189@@idType=uid",
                "userId=10295702479889631545@@idType=crypta_id"
            }
        );
    }

    @Test
    public void testRecordWithOtherVerdict() throws Exception {
        checker.check("tskv\tdatetime=[2019-10-05T14:34:00 +0300]\tfraudStatus=OTHER\t" +
                "promoVerdicts=[coinId=88724321, promoId=11283, verdict=USED] " +
                "[coinId=88724322, promoId=11282, verdict=USED]\t" +
                "detectorName=UsedCoinsDetector\t" +
                "request={coins=[CoinDto(coinId=88793021, promoId=27176), CoinDto(coinId=88792044, promoId=27172)], " +
                "uid=124184654, order_ids=[], reason=USER_CHECK }\t" +
                "users=[" +
                "userId=856446617, idType=uid, " +
                "userId=12613665631911827745, idType=crypta_id, " +
                "userId=5db1d358f59448518cce11df79a3b560, idType=uuid, ]",
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z").parse("2019-10-05T14:34:00 +0300"),
            checker.getHost(),
            Environment.DEVELOPMENT,
            "OTHER",
            new String[]{
                "coinId=88724321@@promoId=11283@@verdict=USED",
                "coinId=88724322@@promoId=11282@@verdict=USED",
            },
            "UsedCoinsDetector",
            new String[]{
                "coinId=88793021@@promoId=27176",
                "coinId=88792044@@promoId=27172"
            },
            124184654L,
            new String[0],
            "USER_CHECK",
            new String[]{
                "userId=856446617@@idType=uid",
                "userId=12613665631911827745@@idType=crypta_id",
                "userId=5db1d358f59448518cce11df79a3b560@@idType=uuid"
            }
        );
    }
}
