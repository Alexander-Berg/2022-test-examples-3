package ru.yandex.market.core.datacamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOfferPrice;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import Market.DataCamp.SyncAPI.SyncSearch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.currency.Currency;
import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;

import static Market.DataCamp.DataCampOfferMeta.DataSource.PUSH_PARTNER_OFFICE;
import static Market.DataCamp.DataCampOfferMeta.DataSource.UNKNOWN_SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

class DefaultDataCampServiceTest extends FunctionalTest {

    @Autowired
    private DefaultDataCampService defaultDataCampService;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampClient;

    @DisplayName("Построение цены для индексатора")
    @Test
    void buildPrice() {
        DataCampOfferPrice.OfferPrice price = defaultDataCampService.buildPrice(Currency.USD, VatRate.VAT_18,
                BigDecimal.TEN, BigDecimal.ONE, Instant.now(), PUSH_PARTNER_OFFICE);

        assertEquals(price.getBasic().getBinaryPrice().getPrice(), DataCampUtil.powToIdx(BigDecimal.TEN));
        assertEquals(price.getBasic().getBinaryOldprice().getPrice(), DataCampUtil.powToIdx(BigDecimal.ONE));
        assertEquals(price.getBasic().getBinaryPrice().getId(), Currency.USD.getId());
        assertEquals(price.getBasic().getVat(), 1);
        assertEquals(PUSH_PARTNER_OFFICE, price.getBasic().getMeta().getSource());

        DataCampOfferPrice.PriceBundle basic = price.getBasic();
        assertEquals(basic.getBinaryPrice().getPrice(), DataCampUtil.powToIdx(BigDecimal.TEN));
        assertEquals(basic.getBinaryOldprice().getPrice(), DataCampUtil.powToIdx(BigDecimal.ONE));

        price = defaultDataCampService.buildPrice(null, null, null, null, Instant.now(), null);
        basic = price.getBasic();
        assertEquals(price.getBasic().getBinaryPrice().getPrice(), 0L);
        assertEquals(price.getBasic().getBinaryOldprice().getPrice(), 0L);
        assertEquals(basic.getBinaryPrice().getPrice(), 0L);
        assertEquals(basic.getBinaryOldprice().getPrice(), 0L);
        assertEquals(UNKNOWN_SOURCE, price.getBasic().getMeta().getSource());

    }

    @DisplayName("Построение статуса для индексатора")
    @Test
    void buildStatus() {
        DataCampOfferStatus.OfferStatus status = defaultDataCampService.buildStatus(true, Instant.now());
        assertTrue(status.getDisabled(0).getFlag());
        assertEquals(
                status.getDisabled(0).getMeta().getSource(),
                PUSH_PARTNER_OFFICE);

        status = defaultDataCampService.buildStatus(false, Instant.now());
        Assertions.assertFalse(status.getDisabled(0).getFlag());
    }

    @Test
    void testSearchBusinessOffersStreamed() {
        SyncGetOffer.GetUnitedOffersResponse offersResponsePage1 = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/DefaultDataCampServiceTest.GetUnitedOfferResponsePage1.json",
                getClass()
        );

        doReturn(DataCampStrollerConversions.fromStrollerResponse(offersResponsePage1)).when(
                dataCampClient
        ).searchBusinessOffers(
                argThat(request -> request.getPageRequest().seekKey().isEmpty())
        );

        SyncGetOffer.GetUnitedOffersResponse offersResponsePage2 = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/DefaultDataCampServiceTest.GetUnitedOfferResponsePage2.json",
                getClass()
        );
        doReturn(DataCampStrollerConversions.fromStrollerResponse(offersResponsePage2)).when(
                dataCampClient
        ).searchBusinessOffers(
                argThat(request -> "0516465167".equals(request.getPageRequest().seekKey().orElse(null)))
        );

        SyncGetOffer.GetUnitedOffersResponse offersResponsePage3 = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/DefaultDataCampServiceTest.GetUnitedOfferResponsePage3.json",
                getClass()
        );
        doReturn(DataCampStrollerConversions.fromStrollerResponse(offersResponsePage3)).when(
                dataCampClient
        ).searchBusinessOffers(
                argThat(request -> "0516465169".equals(request.getPageRequest().seekKey().orElse(null)))
        );

        SearchBusinessOffersRequest searchRequest = SearchBusinessOffersRequest.builder()
                .setPartnerId(774L)
                .setPageRequest(SeekSliceRequest.firstN(2))
                .build();
        List<String> fullOfferResponse = defaultDataCampService.searchBusinessOffersStreamed(searchRequest)
                .map(offer -> offer.getServiceOrThrow(774)
                        .getIdentifiers()
                        .getExtra()
                        .getShopSku()

                )
                .collect(Collectors.toList());

        assertEquals(
                List.of(
                        "0516465165",
                        "0516465166",
                        "0516465167",
                        "0516465168",
                        "0516465169"
                ),
                fullOfferResponse
        );
    }



}
