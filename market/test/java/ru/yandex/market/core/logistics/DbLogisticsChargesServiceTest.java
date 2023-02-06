package ru.yandex.market.core.logistics;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.dbunit.database.DatabaseConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.logistics.db.DbLogisticsChargesService;
import ru.yandex.market.core.logistics.model.LogisticsChargeBillingInfo;
import ru.yandex.market.core.logistics.model.LogisticsChargeInfo;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тест на {@link DbLogisticsChargesService}
 */
@DbUnitDataBaseConfig(@DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true"))
class DbLogisticsChargesServiceTest extends FunctionalTest {

    private static final String LOGIN = "login";
    private static final LocalDate MARCH_19 = LocalDate.of(2019, Month.MARCH, 18);

    @Autowired
    private DbLogisticsChargesService dbLogisticsChargesService;

    @Test
    @DbUnitDataSet(before = "DbLogisticsChargesService.before.csv")
    void testGetPreviouslyUploadedTrackCodes() {
        List<String> trackCodes = ImmutableList.of("track1", "track2", "track3");
        Set<String> previouslyUploaded = dbLogisticsChargesService.getPreviouslyUploadedTrackCodes(trackCodes);
        assertThat(previouslyUploaded, Matchers.containsInAnyOrder(trackCodes.toArray(new String[0])));
    }

    @Test
    @DbUnitDataSet(
            before = "DbLogisticsChargesService.before.csv",
            after = "DbLogisticsChargesService.after.csv"
    )
    void testUpload() {
        Instant instant = LocalDateTime.of(MARCH_19, LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant();
        dbLogisticsChargesService.setClock(Clock.fixed(instant, ZoneOffset.UTC));

        List<LogisticsChargeInfo> infos = Arrays.asList(
                new LogisticsChargeInfo("track5", 100, 1234),
                new LogisticsChargeInfo("track6", 112, 2345)
        );
        dbLogisticsChargesService.upload(infos, LOGIN);
    }

    @Test
    @DbUnitDataSet(before = "DbLogisticsChargesService.before.csv")
    void testGetChargesForBilling() {
        List<LogisticsChargeBillingInfo> results = dbLogisticsChargesService.getChargesForBilling(
                MARCH_19
        );
        assertEquals(results.size(), 1);

        LogisticsChargeBillingInfo result = results.get(0);
        assertThat(
                result,
                Matchers.allOf(
                        MbiMatchers.transformedBy(
                                LogisticsChargeBillingInfo::getTrackCode,
                                Matchers.equalTo("track2")
                        ),
                        MbiMatchers.transformedBy(
                                LogisticsChargeBillingInfo::getTotalChargeUSD,
                                Matchers.equalTo(1000)
                        ),
                        MbiMatchers.transformedBy(
                                LogisticsChargeBillingInfo::getBillingTrantime,
                                Matchers.equalTo(
                                        LocalDateTime.of(MARCH_19, LocalTime.MIDNIGHT).
                                                atZone(ZoneId.systemDefault())
                                                .toInstant()
                                )
                        ),
                        MbiMatchers.transformedBy(
                                LogisticsChargeBillingInfo::getOrderId,
                                Matchers.equalTo(2345L)
                        )
                )
        );
    }
}
