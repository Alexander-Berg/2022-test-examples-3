package ru.yandex.market.core.feed.validation.impl;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import Market.DataCamp.API.UpdateTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.feed.assortment.model.AssortmentFeedValidationRequest;
import ru.yandex.market.core.feed.assortment.model.AssortmentValidationType;
import ru.yandex.market.core.feed.validation.AsyncFeedValidationService;
import ru.yandex.market.core.feed.validation.model.FeedValidationLogbrokerEvent;
import ru.yandex.market.core.misc.resource.RemoteResource;
import ru.yandex.market.logbroker.LogbrokerService;

import static ru.yandex.market.common.test.util.ProtoTestUtil.getProtoMessageByJson;
import static ru.yandex.market.core.campaign.model.CampaignType.SHOP;
import static ru.yandex.market.core.campaign.model.CampaignType.SUPPLIER;
import static ru.yandex.market.core.feed.assortment.model.AssortmentValidationType.MAPPING_WITH_PRICES;
import static ru.yandex.market.core.feed.assortment.model.AssortmentValidationType.PRICES;
import static ru.yandex.market.core.feed.validation.FeedValidationTestUtils.createUnitedValidationInfoWrapper;

/**
 * Date: 27.10.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@DbUnitDataSet(before = {
        "regions.before.csv",
        "UnitedQParserFeedValidationService/before.csv"
})
class UnitedQParserFeedValidationServiceTest extends FunctionalTest {

    @Autowired
    @Qualifier("unitedAsyncFeedValidationService")
    private AsyncFeedValidationService<AssortmentFeedValidationRequest> asyncFeedValidationService;
    @Autowired
    @Qualifier("qParserLogBrokerService")
    private LogbrokerService logbrokerService;

    @DisplayName("Проверка отправки запроса на валидацию фида в логброкер")
    @ParameterizedTest(name = "{2}: {3}. validation: {0}")
    @CsvSource({
            "41,1000,SHOP,MAPPING_WITH_PRICES,",
            "42,1001,SHOP,MAPPING_WITH_PRICES,",
            "48,1005,SHOP,MAPPING_WITH_PRICES,",
            "49,1007,SHOP,MAPPING_WITH_PRICES,",
            "46,1002,SUPPLIER,MAPPING_WITH_PRICES,123",
            "47,1003,SUPPLIER,PRICES,",
            "50,1010,SHOP,MAPPING_WITH_PRICES,"
    })
    void validate_withoutVatMockAndBusiness_correct(long validationId, long partnerId,
                                                    CampaignType type, AssortmentValidationType validationType,
                                                    Long uploadId) {
        assertValidate(validationId, partnerId, type, validationType, ".withoutCpa", uploadId, null);
    }

    static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(41, 1000, SHOP, MAPPING_WITH_PRICES, null, null),
                Arguments.of(42, 1001, SHOP, MAPPING_WITH_PRICES, null, null),
                Arguments.of(48, 1005, SHOP, MAPPING_WITH_PRICES, null, null),
                Arguments.of(49, 1007, SHOP, MAPPING_WITH_PRICES, null, null),
                Arguments.of(46, 1002, SUPPLIER, MAPPING_WITH_PRICES, 123L, null),
                Arguments.of(47, 1003, SUPPLIER, PRICES, null, null),
                Arguments.of(50, 1002, SUPPLIER, MAPPING_WITH_PRICES, 123L, List.of("id", "price", "adult"))
        );
    }

    @DisplayName("Проверка отправки запроса на валидацию фида в логброкер")
    @ParameterizedTest(name = "{2}: {3}. validation: {0}")
    @MethodSource("args")
    @DbUnitDataSet(before = "UnitedQParserFeedValidationService/environment.csv")
    void validate_withoutVatMockAndBusinessWithCpa_correct(long validationId, long partnerId,
                                                           CampaignType type, AssortmentValidationType validationType,
                                                           Long uploadId, List<String> parsingFields) {
        assertValidate(validationId, partnerId, type, validationType, "", uploadId, parsingFields);
    }

    @Test
    @DisplayName("Проверка отправки запроса на валидацию фида в логброкер с флагом чтения стримом")
    @DbUnitDataSet(before = {
            "UnitedQParserFeedValidationService/environment.csv",
            "UnitedQParserFeedValidationService/environmentStream.csv"})
    void validate_stream_flag() {
        assertValidate(41, 1000, SHOP, MAPPING_WITH_PRICES, ".stream", null, null);
    }

    private void assertValidate(long validationId, long partnerId, CampaignType type,
                                AssortmentValidationType validationType, String suffix, Long uploadId,
                                List<String> parsingFields) {
        asyncFeedValidationService.validate(
                createUnitedValidationInfoWrapper(
                        validationId, partnerId,
                        RemoteResource.of("http://ya", "login", "pass"),
                        type, validationType, uploadId, parsingFields
                ),
                Collections.emptyList(),
                Instant.now()
        );

        ArgumentCaptor<FeedValidationLogbrokerEvent> ec = ArgumentCaptor.forClass(FeedValidationLogbrokerEvent.class);
        Mockito.verify(logbrokerService, Mockito.times(1))
                .publishEvent(ec.capture());

        var feedParsingTask = getProtoMessageByJson(UpdateTask.FeedParsingTask.class,
                "UnitedQParserFeedValidationService/proto/" + type.name().toLowerCase() + suffix + "."
                        + validationId + ".json",
                getClass());

        ProtoTestUtil.assertThat(ec.getValue().getPayload().getFeedParsingTask())
                .ignoringFields("timestamp_")
                .isEqualTo(feedParsingTask);
    }
}
