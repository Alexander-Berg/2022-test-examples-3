package ru.yandex.market.core.yt.dynamic.samovar.feed;

import java.time.Instant;

import com.google.protobuf.Int64Value;
import com.google.protobuf.Timestamp;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.misc.resource.ResourceAccessCredentials;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

/**
 * Тесты проверяют правильно ли создается SamovarFeed.
 */
class SamovarFeedTest extends FunctionalTest {
    private static final String REQUEST_ID = "1577337982716/36565e2a2f0e3bc6288c269e949a0500";
    private static final long TIMING = Instant.now().toEpochMilli();

    private static SamovarContextOuterClass.SamovarContext buildContext(boolean withRequestIdAndTiming) {
        SamovarContextOuterClass.FeedInfo feedInfo = SamovarContextOuterClass.FeedInfo.newBuilder()
                .setUpdatedAt(Timestamp.getDefaultInstance())
                .setFeedId(111)
                .setCampaignType("SUPPLIER")
                .setShopId(222)
                .build();

        SamovarContextOuterClass.SamovarContext.Builder builder =
                SamovarContextOuterClass.SamovarContext.newBuilder()
                        .setEnvironment(EnvironmentType.DEVELOPMENT.getValue())
                        .addFeeds(feedInfo);
        return withRequestIdAndTiming ? builder
                .setRequestId(REQUEST_ID)
                .setForceRefreshStart(Int64Value.of(TIMING))
                .build() : builder.build();
    }

    @Test
    @DisplayName("Построение SamovarFeed с добавлением requestId")
    void samovarFieldBuildWithRequestIdTest() {
        ResourceAccessCredentials credentials = new ResourceAccessCredentials("login", "pwd");
        SamovarFeed samovarFeed = SamovarFeed.builder()
                .setUrl("url")
                .setCredentials(credentials)
                .setPeriodMinutes(20)
                .setTimeoutSeconds(100)
                .setContext(buildContext(false))
                .build();

        samovarFeed = SamovarFeed.builder()
                .of(samovarFeed)
                .setRequestIdAndTiming(REQUEST_ID, TIMING)
                .build();

        Assertions.assertThat(samovarFeed.getCredentials())
                .isEqualTo(credentials);
        Assertions.assertThat(samovarFeed.getContext())
                .isEqualTo(buildContext(true));
    }

    @Test
    @DisplayName("В билдере SamovarFeed копируются все поля")
    void testSamovarFeedBuilder() {
        SamovarFeed original = SamovarFeed.builder()
                .setEnabled(true)
                .setTimeoutSeconds(1)
                .setContext(SamovarContextOuterClass.SamovarContext.newBuilder().setEnvironment("a").build())
                .setPeriodMinutes(2)
                .setCredentials(ResourceAccessCredentials.of("b", "c"))
                .setUrl("url")
                .setDisabledTimestamp(Instant.now())
                .setForcedPeriodMinutes(1440)
                .build();

        SamovarFeed copy = SamovarFeed.builder()
                .of(original)
                .build();

        Assertions.assertThat(copy)
                .isEqualTo(original)
                .hasNoNullFieldsOrProperties();

    }
}
