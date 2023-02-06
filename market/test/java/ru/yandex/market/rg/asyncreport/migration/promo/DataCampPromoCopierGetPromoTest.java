package ru.yandex.market.rg.asyncreport.migration.promo;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.PromoDatacampRequest;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DataCampPromoCopierGetPromoTest extends FunctionalTest {

    @Autowired
    private DataCampPromoCopier dataCampPromoCopier;
    @Autowired
    private DataCampClient dataCampShopClient;

    @ParameterizedTest(name = "{2}")
    @DisplayName("Тест получения стрима промо.")
    @MethodSource("getPagesTypes")
    void getPromoPageTest(int pageCount, int pageSize, String testName) {
        initGetPromoMock(pageCount, pageSize);
        var promo = dataCampPromoCopier.getPromo(DataCampPromo.PromoType.MARKET_PROMOCODE, 100L, 200L)
                .collect(Collectors.toList());
        assertEquals(pageCount * pageSize, promo.size());
        verify(dataCampShopClient, times(pageCount)).getPromos(any(PromoDatacampRequest.class));
    }

    @Test
    @DisplayName("Пустой ответ")
    void getPromoEmptyResponse() {
        var req = new PromoDatacampRequest.Builder(200L)
                .withPartnerId(100L)
                .withOnlyUnfinished(true)
                .withLimit(100)
                .withPromoType(Set.of(DataCampPromo.PromoType.MARKET_PROMOCODE));
        doReturn(generatePromoResponse(0, 0, null))
                .when(dataCampShopClient).getPromos(eq(req.withPosition(null).build()));
        var promo = dataCampPromoCopier.getPromo(DataCampPromo.PromoType.MARKET_PROMOCODE, 100L, 200L)
                .collect(Collectors.toList());
        assertEquals(0, promo.size());
        verify(dataCampShopClient, times(1)).getPromos(any(PromoDatacampRequest.class));
    }

    @Test
    @DisplayName("Две страницы по 1, одна 70")
    void getPromoTwoPage100One70Test() {
        var req = new PromoDatacampRequest.Builder(200L)
                .withPartnerId(100L)
                .withOnlyUnfinished(true)
                .withLimit(100)
                .withPromoType(Set.of(DataCampPromo.PromoType.MARKET_PROMOCODE));
        doReturn(generatePromoResponse(0, 100, "0"))
                .when(dataCampShopClient).getPromos(eq(req.withPosition(null).build()));
        doReturn(generatePromoResponse(100, 200, "1"))
                .when(dataCampShopClient).getPromos(eq(req.withPosition("0").build()));
        doReturn(generatePromoResponse(200, 270, null))
                .when(dataCampShopClient).getPromos(eq(req.withPosition("1").build()));
        var promo = dataCampPromoCopier.getPromo(DataCampPromo.PromoType.MARKET_PROMOCODE, 100L, 200L)
                .collect(Collectors.toList());
        assertEquals(270, promo.size());
        verify(dataCampShopClient, times(3)).getPromos(any(PromoDatacampRequest.class));
    }

    private static Stream<Arguments>  getPagesTypes() {
        return Stream.of(
                Arguments.of(2, 100, "Две страницы по 100"),
                Arguments.of(1, 100, "Одна страница по 100"),
                Arguments.of(1, 70, "Одна страница по 70")
        );
    }

    private void initGetPromoMock(int pageCount, int pageSize) {
        var req = new PromoDatacampRequest.Builder(200L)
                .withPartnerId(100L)
                .withOnlyUnfinished(true)
                .withLimit(100)
                .withPromoType(Set.of(DataCampPromo.PromoType.MARKET_PROMOCODE));
        String prevPageToken = null;
        int startPagePromoId = 0;
        int endPagePromoId = pageSize;
        for (int i = 0; i < pageCount; i++) {
            String nextPageToken = i + 1 == pageCount ? null : String.valueOf(i);
            doReturn(generatePromoResponse(startPagePromoId, endPagePromoId, nextPageToken))
                    .when(dataCampShopClient).getPromos(eq(req.withPosition(prevPageToken).build()));
            prevPageToken = nextPageToken;
            startPagePromoId = endPagePromoId;
            endPagePromoId += pageSize;
        }
    }


    private SyncGetPromo.GetPromoBatchResponse generatePromoResponse(int startId, int endId, String nextPageHash) {
        var response = SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(generatePromo(startId, endId));
        if (nextPageHash != null) {
            response.setNextPagePosition(nextPageHash);
        }
        return response.build();
    }

    private DataCampPromo.PromoDescriptionBatch generatePromo(int startId, int endId) {

        var promoDesc = new ArrayList<DataCampPromo.PromoDescription>();
        for (int promoId = startId; promoId < endId; promoId++) {
            var promo = DataCampPromo.PromoDescription.newBuilder()
                    .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                            .setPromoId(String.valueOf(promoId)))
                    .build();
            promoDesc.add(promo);
        }
        return DataCampPromo.PromoDescriptionBatch.newBuilder().addAllPromo(promoDesc).build();
    }


}
