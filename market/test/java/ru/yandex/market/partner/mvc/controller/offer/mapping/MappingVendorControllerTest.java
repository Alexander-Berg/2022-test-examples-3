package ru.yandex.market.partner.mvc.controller.offer.mapping;

import java.util.Arrays;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.partner.util.FunctionalTestHelper;

class MappingVendorControllerTest extends AbstractMappingControllerFunctionalTest {

    @Autowired
    private MboMappingsService patientMboMappingsService;

    @DisplayName("Запросить список производителей партнера, есть элементы в списке")
    @Test
    void testVendorList() {
        Mockito.when(patientMboMappingsService.searchVendorsByShopId(Mockito.any()))
                .thenReturn(MboMappings.SearchVendorsResponse.newBuilder()
                        .addAllVendors(Arrays.asList("Coca-Cola", "Nestle"))
                        .build());
        String url = String.format("%s", searchVendorsUrl(CAMPAIGN_ID));
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        MatcherAssert.assertThat(
                response,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches(
                                "result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ "[\"Coca-Cola\",\"Nestle\"]"))));
    }

    @DisplayName("Запросить список производителей партнера, пустой список")
    @Test
    void testEmptyVendors() {
        Mockito.when(patientMboMappingsService.searchVendorsByShopId(Mockito.any()))
                .thenReturn(MboMappings.SearchVendorsResponse.newBuilder()
                        .build());
        String url = String.format("%s", searchVendorsUrl(CAMPAIGN_ID));
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        MatcherAssert.assertThat(
                response,
                MoreMbiMatchers.responseBodyMatches(
                        MbiMatchers.jsonPropertyMatches(
                                "result",
                                MbiMatchers.jsonEquals(/*language=JSON*/ "[]"))));
    }
}
