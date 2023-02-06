package ru.yandex.market.partner.mvc.controller.wizard.utils;

import Market.DataCamp.SyncAPI.SyncGetOffer;
import Market.DataCamp.SyncAPI.SyncSearch;

import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

/**
 * Класс для создания mock-ответов по запросам флагов наличия цен/стоков в Datacamp
 */
public class DatacampFlagResponseMocker {

    private static final String JSON_DIR = "../proto/";

    private final DataCampClient dataCampShopClient;

    public DatacampFlagResponseMocker(DataCampClient dataCampShopClient) {
        this.dataCampShopClient = dataCampShopClient;
    }

    public void setHasStocksResponse(boolean value) {
        String responseJson = value ? "hasStocksInDatacamp.proto.json" : "hasNoStocksInDatacamp.proto.json";

        SearchBusinessOffersResult result = businessOffersResultFromJson(JSON_DIR + responseJson);

        doReturn(result)
                .when(dataCampShopClient)
                .searchBusinessOffers(argThat(req ->
                        req.getPricePresence() == null));
    }

    public void setHasPriceWithStocksResponse(boolean value) {
        String responseJson = value ?
                "hasPriceWithStocksInDatacamp.proto.json" : "hasNoPriceWithStocksInDatacamp.proto.json";

        SearchBusinessOffersResult result = businessOffersResultFromJson(JSON_DIR + responseJson);

        doReturn(result)
                .when(dataCampShopClient)
                .searchBusinessOffers(argThat(req ->
                        req.getPricePresence() != null && req.getMarketStocksPresence() != null
                                && req.getPricePresence() && req.getMarketStocksPresence()));
    }


    private SearchBusinessOffersResult businessOffersResultFromJson(String jsonPath) {

        return DataCampStrollerConversions.fromStrollerResponse(
                ProtoTestUtil.getProtoMessageByJson(SyncGetOffer.GetUnitedOffersResponse.class, jsonPath, getClass()));
    }

    public void setHasPriceResponse(boolean value) {
        String responseJson = value ? "hasPriceInDatacamp.proto.json" : "hasNoPriceInDatacamp.proto.json";

        SearchBusinessOffersResult result = businessOffersResultFromJson(JSON_DIR + responseJson);

        doReturn(result)
                .when(dataCampShopClient)
                .searchBusinessOffers(argThat(req ->
                        req.getPricePresence() != null && req.getPricePresence() &&
                                req.getMarketStocksPresence() == null));
    }

}
