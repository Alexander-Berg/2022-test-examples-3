package ru.yandex.vendor.modeleditor.feed.template;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.junit.CartesianSource;
import ru.yandex.market.ir.http.PartnerContent;
import ru.yandex.market.ir.http.PartnerContentService;
import ru.yandex.market.vendor.AbstractCsPlacementTmsFunctionalTest;
import ru.yandex.vendor.modeleditor.feed.template.model.XlsFeed;

@ParametersAreNonnullByDefault
public class FeedTemplateServiceTest extends AbstractCsPlacementTmsFunctionalTest {
    @Autowired
    private IFeedTemplateService feedTemplateService;

    @Autowired
    private PartnerContentService partnerContentService;
    private static final Instant UPDATE_TIME = OffsetDateTime.parse("2018-12-02T15:16:17+03:00").toInstant();

    @ParameterizedTest
    @DisplayName("Проверка выбора подмножества фидов для обновления статусов")
    @CartesianSource({
            @ValueSource(strings = {
                    "2018-12-01T00:00:00+03:00,",
                    "2018-12-02T00:00:00+03:00,113:116:119",
                    "2018-12-03T00:00:00+03:00,113:116:119:123:126:129",
                    "2018-12-04T00:00:00+03:00,113:116:119:123:126:129:133:136:139"
            }),
            @ValueSource(ints = {0, 1, 3, 10, 1000})
    })
    @DbUnitDataSet(
            before = "FeedTemplateServiceTest/getNeedStatusUpdateFeedsTest/before.csv"
    )
    void getNeedStatusUpdateFeedsTest(
            @ConvertWith(DateAndIdSetParser.class) DateAndIdSet dateAndIdSet,
            int limit
    ) {
        Instant updatedBefore = dateAndIdSet.updatedBefore();
        Set<Long> expectedIdSet = dateAndIdSet.expectedIdSet();
        Set<Long> needUpdateFeedIds = feedTemplateService.getNeedStatusUpdateFeeds(updatedBefore, limit)
                .stream()
                .map(XlsFeed::getId)
                .collect(Collectors.toSet());
        Assertions.assertTrue(needUpdateFeedIds.size() >= Math.min(expectedIdSet.size(), limit));
        Assertions.assertTrue(needUpdateFeedIds.size() <= limit);
        Assertions.assertTrue(expectedIdSet.containsAll(needUpdateFeedIds));
    }

    @Test
    @DisplayName("Проверка обновления состояния фида прошедшего проверку")
    @DbUnitDataSet(
            before = "FeedTemplateServiceTest/updateFeedStatusTest/before.csv",
            after = "FeedTemplateServiceTest/updateFeedStatusOkTest/after.csv"
    )
    void updateFeedStatusOkTest() {
        Mockito.when(partnerContentService.getFileInfo(Mockito.any()))
                .thenReturn(PartnerContent.FileInfoResponse.newBuilder()
                        .setProcessRequestStatus(PartnerContent.ProcessRequestStatus.FINISHED)
                        .addBucketProcessInfo(PartnerContent.BucketProcessInfo.newBuilder()
                                .setCategoryId(12233)
                                .setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.FINISHED)
                                .setBucketProcessStatistics(PartnerContent.BucketProcessStatistics.newBuilder()
                                        .setModelCreated(11)
                                        .setModelAlreadyExists(0)
                                        .setSkuAlreadyExists(0)
                                        .setSkuCreated(22)))
                        .build()
                );
        Instant updateTime = OffsetDateTime.parse("2018-12-02T15:16:17+03:00").toInstant();
        feedTemplateService.updateFeedStatus(116L, updateTime);
    }

    @Test
    @DisplayName("Проверка обновления состояния фида не прошедшего проверку")
    @DbUnitDataSet(
            before = "FeedTemplateServiceTest/updateFeedStatusTest/before.csv",
            after = "FeedTemplateServiceTest/updateFeedStatusFailTest/after.csv"
    )
    void updateFeedStatusFailTest() {
        Mockito.when(partnerContentService.getFileInfo(Mockito.any()))
                .thenReturn(PartnerContent.FileInfoResponse.newBuilder()
                        .setProcessRequestStatus(PartnerContent.ProcessRequestStatus.FINISHED)
                        .addBucketProcessInfo(PartnerContent.BucketProcessInfo.newBuilder()
                                .setCategoryId(12233)
                                .setResultReportStatus(PartnerContent.BucketProcessInfo.BucketProcessInfoStatus.INVALID)
                                .setBucketProcessStatistics(PartnerContent.BucketProcessStatistics.newBuilder()
                                        .setModelCreated(0)
                                        .setModelAlreadyExists(0)
                                        .setSkuAlreadyExists(0)
                                        .setSkuCreated(0)))
                        .build()
                );
        Instant updateTime = OffsetDateTime.parse("2018-12-02T15:16:17+03:00").toInstant();
        feedTemplateService.updateFeedStatus(116L, updateTime);
    }

    @Test
    @DisplayName("Проверка обновления состояния всё ещё проверяющегося фида")
    @DbUnitDataSet(
            before = "FeedTemplateServiceTest/updateFeedStatusTest/before.csv",
            after = "FeedTemplateServiceTest/updateFeedStatusProcessingTest/after.csv"
    )
    void updateFeedStatusProcessingTest() {
        Mockito.when(partnerContentService.getFileInfo(Mockito.any()))
                .thenReturn(PartnerContent.FileInfoResponse.newBuilder()
                        .setProcessRequestStatus(PartnerContent.ProcessRequestStatus.PROCCESSING)
                        .build()
                );
        Instant updateTime = OffsetDateTime.parse("2018-12-02T15:16:17+03:00").toInstant();
        feedTemplateService.updateFeedStatus(116L, updateTime);
    }

    @Test
    @DisplayName("Проверка обновления состояния фида c уже установленным финальным статусом")
    @DbUnitDataSet(
            before = "FeedTemplateServiceTest/updateFeedStatusTest/before.csv",
            after = "FeedTemplateServiceTest/updateFeedStatusTest/before.csv"
    )
    void updateFeedStatusAlreadyProcessedTest() {
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> feedTemplateService.updateFeedStatus(117L, UPDATE_TIME)
        );
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> feedTemplateService.updateFeedStatus(118L, UPDATE_TIME)
        );
    }

    @Test
    @DisplayName("Проверка обновления состояния несуществующего фида")
    @DbUnitDataSet(
            before = "FeedTemplateServiceTest/updateFeedStatusTest/before.csv",
            after = "FeedTemplateServiceTest/updateFeedStatusTest/before.csv"
    )
    void updateFeedStatusNonExistingTest() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> feedTemplateService.updateFeedStatus(120L, UPDATE_TIME)
        );
    }

    @Test
    @DisplayName("Проверка установки времени обновления, без запроса актуального статуса для несуществующего фида")
    @DbUnitDataSet(
            before = "FeedTemplateServiceTest/updateFeedStatusTest/before.csv",
            after = "FeedTemplateServiceTest/updateFeedStatusTest/before.csv"
    )
    void markFeedAsUpdatedNonExistingTest() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> feedTemplateService.markFeedAsUpdated(120L, UPDATE_TIME)
        );
    }

    @Test
    @DisplayName("Проверка установки времени обновления, без запроса актуального статуса для фида в финальном статусе")
    @DbUnitDataSet(
            before = "FeedTemplateServiceTest/updateFeedStatusTest/before.csv",
            after = "FeedTemplateServiceTest/markFeedAsUpdatedTest/after.csv"
    )
    void markFeedAsUpdatedTest() {
        feedTemplateService.markFeedAsUpdated(116L, UPDATE_TIME);
    }

    @Test
    @DisplayName("Проверка установки времени обновления, без запроса актуального статуса для фида в финальном статусе")
    @DbUnitDataSet(
            before = "FeedTemplateServiceTest/updateFeedStatusTest/before.csv",
            after = "FeedTemplateServiceTest/updateFeedStatusTest/before.csv"
    )
    void markFeedAsUpdatedAlreadyProcessedTest() {
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> feedTemplateService.markFeedAsUpdated(117L, UPDATE_TIME)
        );
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> feedTemplateService.markFeedAsUpdated(118L, UPDATE_TIME)
        );
    }

    public static class DateAndIdSet {
        private final Instant updatedBefore;
        private final Set<Long> expectedIdSet;

        private DateAndIdSet(Instant updatedBefore, Set<Long> expectedIdSet) {
            this.updatedBefore = updatedBefore;
            this.expectedIdSet = Collections.unmodifiableSet(new HashSet<>(expectedIdSet));
        }

        private Instant updatedBefore() {
            return updatedBefore;
        }

        private Set<Long> expectedIdSet() {
            return expectedIdSet;
        }

        public static DateAndIdSet of(Instant updatedBefore, Set<Long> expectedIdSet) {
            return new DateAndIdSet(updatedBefore, expectedIdSet);
        }
    }

    public static class DateAndIdSetParser extends SimpleArgumentConverter {
        @Override
        protected Object convert(Object source, Class<?> targetType) throws ArgumentConversionException {
            if (!(source instanceof String) || !DateAndIdSet.class.isAssignableFrom(targetType)) {
                throw new ArgumentConversionException("String to DateAndIdSet conversion expected");
            } else {
                String definition = (String) source;
                String[] dateAndIdsFields = definition.split(",", -1);
                Instant updatedBefore = OffsetDateTime.parse(dateAndIdsFields[0]).toInstant();
                Set<Long> expectedIdSet = dateAndIdsFields[1].isEmpty()
                        ? Collections.emptySet()
                        : Stream.of(dateAndIdsFields[1].split(":", -1))
                                .map(Long::parseLong)
                                .collect(Collectors.toSet());
                return DateAndIdSet.of(updatedBefore, expectedIdSet);
            }
        }
    }
}
