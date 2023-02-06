package ru.yandex.market.core.test.utils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Functions;
import com.google.protobuf.Descriptors;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.Assertions;

import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.misc.resource.RemoteResource;
import ru.yandex.market.core.yt.dynamic.samovar.feed.SamovarFeed;
import ru.yandex.market.core.yt.dynamic.samovar.feed.SamovarFeedInfo;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Утилита для тестирования фидов самовара
 * Date: 01.10.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
public class SamovarFeedTestUtils {

    private static final Map<String, Descriptors.FieldDescriptor> DEFINED_FEED_INFO_FIELDS =
            SamovarContextOuterClass.FeedInfo.getDescriptor()
                    .getFields()
                    .stream()
                    .collect(Collectors.toMap(Descriptors.FieldDescriptor::getJsonName, Function.identity()));

    public static void checkFeedInfo(@Nonnull List<SamovarContextOuterClass.FeedInfo> expected,
                                     @Nonnull List<SamovarContextOuterClass.FeedInfo> actual) {
        Assertions.assertEquals(expected.size(), actual.size());
        final Map<Long, SamovarContextOuterClass.FeedInfo> actualMap = actual.stream()
                .collect(Collectors.toMap(SamovarContextOuterClass.FeedInfo::getFeedId, Function.identity()));

        for (final SamovarContextOuterClass.FeedInfo feedInfo : expected) {
            final SamovarContextOuterClass.FeedInfo actualFeedInfo = actualMap.get(feedInfo.getFeedId());
            checkSamovarFeedInfo(feedInfo, actualFeedInfo);
        }
    }

    /**
     * Здесь нагорожен кастомный ассерт потому что оригинальное сообщение от hamcrest в методе
     * {@code containsInAnyOrder} не печатет нормально объекты внутри коллекции.
     */
    public static void assertSamovarFeedInfoLists(List<SamovarFeedInfo> actual, List<SamovarFeedInfo> expected) {
        assertThat(actual, hasSize(expected.size()));

        var expectedMap = expected.stream()
                .collect(Collectors.toMap(SamovarFeedInfo::getFeedId, Functions.identity()));

        for (var actualSamovarFeedInfo: actual) {
            var feedId = actualSamovarFeedInfo.getFeedId();
            var expectedSamovarFeedInfo = expectedMap.get(feedId);

            assertThat(
                    "Expected collection doesn't contains element "
                            + SamovarFeedInfoMatcher.toString(actualSamovarFeedInfo),
                    expectedSamovarFeedInfo,
                    notNullValue()
            );

            assertThat(actualSamovarFeedInfo, SamovarFeedInfoMatcher.from(expectedSamovarFeedInfo));
        }
    }

    public static void checkSamovarFeed(@Nonnull SamovarFeed expectedFeed, @Nonnull SamovarFeed actualFeed) {
        Assertions.assertNotNull(actualFeed);

        Assertions.assertEquals(expectedFeed.getUrl(), actualFeed.getUrl());
        Assertions.assertEquals(expectedFeed.getCredentials(), actualFeed.getCredentials());
        Assertions.assertEquals(expectedFeed.getPeriodMinutes(), actualFeed.getPeriodMinutes());
        Assertions.assertEquals(expectedFeed.getTimeoutSeconds(), actualFeed.getTimeoutSeconds());
        Assertions.assertEquals(expectedFeed.isEnabled(), actualFeed.isEnabled());
        Assertions.assertEquals(expectedFeed.getDisabledTimestamp(), actualFeed.getDisabledTimestamp());
        Assertions.assertEquals(expectedFeed.getForcedPeriodMinutes(), actualFeed.getForcedPeriodMinutes());

        checkSamovarContexts(expectedFeed.getContext(), actualFeed.getContext());
    }

    private static void checkSamovarContexts(@Nonnull SamovarContextOuterClass.SamovarContext expected,
                                             @Nonnull SamovarContextOuterClass.SamovarContext actual) {
        Assertions.assertEquals(expected.getEnvironment(), actual.getEnvironment());
        checkSamovarFeedInfos(expected.getFeedsList(), actual.getFeedsList());
    }

    private static void checkSamovarFeedInfos(@Nonnull List<SamovarContextOuterClass.FeedInfo> expected,
                                              @Nonnull List<SamovarContextOuterClass.FeedInfo> actual) {
        Assertions.assertEquals(expected.size(), actual.size());

        final Map<Long, SamovarContextOuterClass.FeedInfo> actualMap = actual.stream()
                .collect(Collectors.toMap(SamovarContextOuterClass.FeedInfo::getFeedId, Function.identity()));
        for (final SamovarContextOuterClass.FeedInfo expectedFeed : expected) {
            checkSamovarFeedInfo(expectedFeed, actualMap.get(expectedFeed.getFeedId()));
        }
    }

    public static void checkSamovarFeedInfo(@Nonnull SamovarContextOuterClass.FeedInfo expectedFeed,
                                            SamovarContextOuterClass.FeedInfo actualFeed) {
        Assertions.assertNotNull(actualFeed);

        Assertions.assertEquals(expectedFeed.getCampaignType(), actualFeed.getCampaignType());
        Assertions.assertEquals(expectedFeed.getShopId(), actualFeed.getShopId());
        Assertions.assertEquals(expectedFeed.getFeedId(), actualFeed.getFeedId());
        Assertions.assertEquals(
                expectedFeed.hasField(DEFINED_FEED_INFO_FIELDS.get("businessId")),
                actualFeed.hasField(DEFINED_FEED_INFO_FIELDS.get("businessId"))
        );
        Assertions.assertEquals(expectedFeed.getBusinessId(), actualFeed.getBusinessId());
        Assertions.assertEquals(expectedFeed.getUrl(), actualFeed.getUrl());
        Assertions.assertTrue(actualFeed.getWarehousesList().isEmpty());
        Assertions.assertNotNull(actualFeed.getUpdatedAt());
        Assertions.assertEquals(expectedFeed.getForcedPeriodMinutes(), actualFeed.getForcedPeriodMinutes());
        Assertions.assertEquals(expectedFeed.getVerticalShare(), actualFeed.getVerticalShare());
    }

    public static SamovarFeedInfo createSamovarFeedInfo(
            long partnerId,
            CampaignType campaignType,
            long feedId,
            RemoteResource resource,
            @Nullable Integer timeout,
            @Nullable Integer period,
            @Nullable Integer forcedReparseIntervalMinutes
    ) {
        var result = mock(SamovarFeedInfo.class);

        when(result.getPartnerId()).thenReturn(partnerId);
        when(result.getCampaignType()).thenReturn(campaignType);
        when(result.getFeedId()).thenReturn(feedId);
        when(result.getResource()).thenReturn(resource);
        when(result.getTimeout()).thenReturn(timeout);
        when(result.getPeriod()).thenReturn(period);
        when(result.getForcedReparseIntervalMinutes()).thenReturn(forcedReparseIntervalMinutes);

        return result;
    }

    private SamovarFeedTestUtils() {
    }
}
