package ru.yandex.market.core.datacamp;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.SyncAPI.SyncSearch;
import org.apache.commons.lang.ArrayUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.web.paging.SeekSliceRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataCampClientStubTest {

    private static SyncSearch.SearchResponse buildStub(int count, int total, boolean first, boolean last, String... keys) {
        SyncSearch.PageInfo.Builder pageInfo = SyncSearch.PageInfo.newBuilder()
                .setIsLastPage(last)
                .setIsFirstPage(first);
        if (!ArrayUtils.isEmpty(keys)) {
            pageInfo.setStart(keys[0]).setEnd(keys[keys.length - 1]);
        }

        return SyncSearch.SearchResponse.newBuilder().addAllOffer(
                Stream.of(keys)
                        .map(key -> DataCampOffer.Offer.newBuilder()
                                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder().setOfferId(key))
                                .build())
                        .collect(Collectors.toList()))
                .setMeta(SyncSearch.SearchMeta.newBuilder()
                        .setTotalAvailable(total)
                        .setTotalResponse(count)
                        .setPaging(pageInfo)
                )
                .build();

    }

    /**
     * Просто тест stub-клиента с постраничным выводом по строковому offer-id.
     */
    @Test
    void searchOffers() throws IOException {
        DataCampClientStub stub = new DataCampClientStub("datacamp.stub.json");

        assertEquals(
                buildStub(3, 5, true, false, "b", "c", "d"),
                stub.searchOffers(0, null, false, DataCampService.buildRequest(SeekSliceRequest.firstN(3)))
        );

        assertEquals(
                buildStub(2, 5, false, true, "e", "f"),
                stub.searchOffers(0, null, false, DataCampService.buildRequest(SeekSliceRequest.firstNAfter(3, "d")))
        );

        assertEquals(
                buildStub(3, 5, false, true, "d", "e", "f"),
                stub.searchOffers(0, null, false, DataCampService.buildRequest(SeekSliceRequest.lastN(3)))
        );

        assertEquals(
                buildStub(2, 5, true, false, "b", "c"),
                stub.searchOffers(0, null, false, DataCampService.buildRequest(SeekSliceRequest.lastNBefore(3, "d")))
        );

        assertEquals(
                buildStub(3, 5, true, false, "b", "c", "d"),
                stub.searchOffers(0, null, false, DataCampService.buildRequest(SeekSliceRequest.firstNAfter(3, "a")))
        );

        assertEquals(
                buildStub(0, 5, false, true),
                stub.searchOffers(0, null, false, DataCampService.buildRequest(SeekSliceRequest.firstNAfter(3, "g")))
        );

    }

    @Test
    void searchEmptyDataCamp() {
        DataCampClientStub stub = new DataCampClientStub(Collections.emptyList());
        assertEquals(
                buildStub(0, 0, true, true),
                stub.searchOffers(0, null, false, DataCampService.buildRequest(SeekSliceRequest.firstNAfter(3, "g")))
        );
    }

    @Test
    void addWarehouseToDataCampStub() {
        DataCampClientStub stub = new DataCampClientStub(Collections.emptyList());
        stub.addWarehouse(1010, 11, 147);
        assertEquals("add_warehouse: 1010, 11, 147", stub.getRequestParams().get(0));
    }

}
