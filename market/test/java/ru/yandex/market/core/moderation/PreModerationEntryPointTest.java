package ru.yandex.market.core.moderation;

import java.util.List;
import java.util.Objects;

import Market.DataCamp.SyncAPI.SyncGetOffer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feed.datacamp.FeedParsingType;
import ru.yandex.market.core.feed.event.DataCampCreateUpdateFeedEventListener;
import ru.yandex.market.core.feed.event.PartnerParsingFeedEvent;
import ru.yandex.market.core.feed.model.FeedType;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.SystemActionContext;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Date: 02.10.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class PreModerationEntryPointTest extends FunctionalTest {

    @Autowired
    private PreModerationEntryPoint preModerationEntryPoint;
    @Qualifier("dataCampShopClient")
    @Autowired
    private DataCampClient dataCampShopClient;
    @Autowired
    private DataCampCreateUpdateFeedEventListener dataCampCreateUpdateFeedEventListener;

    @DbUnitDataSet(
            before = {
                    "PreModerationEntryPointTest/environmentSettingOn.before.csv",
                    "PreModerationEntryPointTest/startPreModeration.before.csv"
            },
            after = "PreModerationEntryPointTest/startPreModeration.after.csv"
    )
    @DisplayName("Отправка фида на парсинг включена")
    @Test
    void startPreModeration_environmentOn_withPushFeed() {
        mockSearchBusinessOffers(null);

        preModerationEntryPoint.startPreModeration(new SystemActionContext(ActionType.START_MODERATION),
                new TestingShop(1001L, 10331L));

        ArgumentCaptor<PartnerParsingFeedEvent> eventCaptor = ArgumentCaptor.forClass(PartnerParsingFeedEvent.class);
        verify(dataCampCreateUpdateFeedEventListener, times(3))
                .onApplicationEvent(eventCaptor.capture());

        Assertions.assertThat(eventCaptor.getAllValues())
                .usingRecursiveFieldByFieldElementComparator()
                .usingElementComparatorOnFields("businessId", "partnerId", "feedId", "feedParsingType")
                .containsExactlyInAnyOrderElementsOf(List.of(
                        new PartnerParsingFeedEvent.Builder()
                                .withBusinessId(11L)
                                .withPartnerId(10331L)
                                .withFeedId(14L)
                                .withFeedParsingType(FeedParsingType.COMPLETE_FEED)
                                .build(),
                        new PartnerParsingFeedEvent.Builder()
                                .withBusinessId(11L)
                                .withPartnerId(10331L)
                                .withFeedId(15L)
                                .withFeedParsingType(FeedParsingType.COMPLETE_FEED)
                                .withFeedType(FeedType.ASSORTMENT)
                                .build(),
                        new PartnerParsingFeedEvent.Builder()
                                .withBusinessId(11L)
                                .withPartnerId(10331L)
                                .withFeedId(18L)
                                .withFeedParsingType(FeedParsingType.COMPLETE_FEED)
                                .withFeedType(FeedType.ASSORTMENT)
                                .build()
                ));
    }

    @DbUnitDataSet(
            before = "PreModerationEntryPointTest/startPreModeration.before.csv",
            after = "PreModerationEntryPointTest/startPreModeration.after.csv"
    )
    @DisplayName("Отправка фида на парсинг выключена")
    @Test
    void startPreModeration_environmentOff_withoutPushFeed() {
        testStartPreModerationWithoutPushFeed(null);
    }

    @DbUnitDataSet(
            before = "PreModerationEntryPointTest/startPreModerationWithoutBusiness.before.csv",
            after = "PreModerationEntryPointTest/startPreModeration.after.csv"
    )
    @DisplayName("Отправка фида на парсинг выключена и бизнес не задан")
    @Test
    void startPreModeration_environmentOffWithoutBusiness_withoutPushFeed() {
        testStartPreModerationWithoutPushFeed(null);
    }

    @DbUnitDataSet(
            before = {
                    "PreModerationEntryPointTest/environmentSettingOn.before.csv",
                    "PreModerationEntryPointTest/startPreModerationWithoutBusiness.before.csv"
            },
            after = "PreModerationEntryPointTest/startPreModeration.after.csv"
    )
    @DisplayName("Отправка фида на парсинг включена, но бизнес не задан")
    @Test
    void startPreModeration_environmentOnWithoutBusiness_withoutPushFeed() {
        testStartPreModerationWithoutPushFeed(null);
    }

    @DbUnitDataSet(
            before = {
                    "PreModerationEntryPointTest/environmentSettingOn.before.csv",
                    "PreModerationEntryPointTest/startPreModeration.before.csv"
            },
            after = "PreModerationEntryPointTest/startPreModeration.after.csv"
    )
    @DisplayName("Отправка фида на парсинг включена, но в хранилище есть оферы")
    @Test
    void startPreModeration_environmentOnWithOffers_withoutPushFeed() {
        testStartPreModerationWithoutPushFeed(
                ProtoTestUtil.getProtoMessageByJson(SyncGetOffer.GetUnitedOffersResponse.class,
                        "PreModerationEntryPointTest/json/datacamp.filled.json", getClass())
        );
    }

    private void testStartPreModerationWithoutPushFeed(SyncGetOffer.GetUnitedOffersResponse unitedOffersResponse) {
        mockSearchBusinessOffers(unitedOffersResponse);

        preModerationEntryPoint.startPreModeration(new SystemActionContext(ActionType.START_MODERATION),
                new TestingShop(1001L, 10331L));

        verify(dataCampCreateUpdateFeedEventListener, times(0))
                .onApplicationEvent(any());
    }

    private void mockSearchBusinessOffers(SyncGetOffer.GetUnitedOffersResponse unitedOffersResponse) {
        if (unitedOffersResponse == null) {
            return;
        }

        Mockito.when(dataCampShopClient.searchBusinessOffers(
                argThat(dataCampRequest -> Objects.equals(dataCampRequest.getBusinessId(), 11L)
                        && Objects.equals(dataCampRequest.getPartnerId(), 10331L)
                        && Objects.equals(dataCampRequest.getPageRequest().limit(), 1)
                        && dataCampRequest.getOffset() == null
                )
        ))
                .thenReturn(DataCampStrollerConversions.fromStrollerResponse(unitedOffersResponse));
    }
}
