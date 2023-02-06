package ru.yandex.market.logistics.lms.client.redis;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.lms.client.LmsLomRedisClient;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.request.schedule.LogisticSegmentInboundScheduleFilter;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsMethodFilter;

@DisplayName("Походы в редис выключены флагом")
@DatabaseSetup("/lms/client/redis/going_to_redis_disabled.xml")
class LmsLomRedisClientTest extends LmsLomLightClientAbstractTest {

    private static final LogisticsPointFilter POINTS_FILTER = LogisticsPointFilter.newBuilder().build();
    private static final PartnerRelationFilter PARTNER_RELATION_FILTER = PartnerRelationFilter.newBuilder().build();
    private static final LogisticSegmentInboundScheduleFilter INBOUND_SCHEDULE_FILTER =
        new LogisticSegmentInboundScheduleFilter();
    private static final SettingsMethodFilter SETTINGS_METHOD_FILTER = SettingsMethodFilter.newBuilder().build();

    @Autowired
    private LmsLomRedisClient redisClient;

    private List<Runnable> redisClientMethods;

    @BeforeEach
    public void setUp() {
        super.setUp();

        redisClientMethods = List.of(
            () -> redisClient.getLogisticsPoint(1L),
            () -> redisClient.getPartner(1L),
            () -> redisClient.getPartners(Set.of(1L)),
            () -> redisClient.getLogisticsPoints(POINTS_FILTER),
            () -> redisClient.getPartnerExternalParams(Set.of()),
            () -> redisClient.searchPartnerRelationWithCutoffs(PARTNER_RELATION_FILTER),
            () -> redisClient.searchPartnerRelationsWithReturnPartners(PARTNER_RELATION_FILTER),
            () -> redisClient.searchInboundSchedule(INBOUND_SCHEDULE_FILTER),
            () -> redisClient.searchPartnerApiSettingsMethods(SETTINGS_METHOD_FILTER)
        );
    }

    @Test
    @DisplayName("При выключенных походах в редис все методы клиента падают с ошибкой")
    void goingToRedisDisabled() {
        for (Runnable redisClientMethod : redisClientMethods) {
            softly.assertThatCode(redisClientMethod::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Going to redis disabled");
        }

        softly.assertThatCode(
                () -> redisClient.getScheduleDay(1L)
            )
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("Method getScheduleDay by id is not supported for redis client");
    }
}
