package ru.yandex.market.checkout.util.datacamp;

import org.mockito.Mockito;

import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;

public class DatacampConfigurer {

    private final DataCampClient dataCampClient;

    public DatacampConfigurer(DataCampClient dataCampClient) {
        this.dataCampClient = dataCampClient;
    }

    public void reset() {
        Mockito.reset(dataCampClient);
    }

    public void mockSearchBusinessOffers(
            SearchBusinessOffersRequest request,
            SearchBusinessOffersResult searchBusinessOffersResult
    ) {
        Mockito.when(dataCampClient.searchBusinessOffers(Mockito.eq(request))).thenReturn(searchBusinessOffersResult);
    }
}
