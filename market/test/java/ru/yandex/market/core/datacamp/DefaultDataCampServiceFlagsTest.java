package ru.yandex.market.core.datacamp;

import java.util.List;

import Market.DataCamp.SyncAPI.SyncGetOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;

class DefaultDataCampServiceFlagsTest extends FunctionalTest {

    private final long PARTNER_ID = 1001L;

    private static final String DATACAMP_JSON_DIR = "proto/check_flags/";

    @Autowired
    private DefaultDataCampService defaultDataCampService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @BeforeEach
    public void init() {
        doReturn(new CampaignInfo(1, 1, 1, 1, CampaignType.SUPPLIER))
                .when(campaignService).getCampaignByDatasource(anyLong());
    }

    @DisplayName("Нет цены -> флаг цены в 0")
    @DbUnitDataSet(before = "DefaultDataCampFlagsTest.newflags.before.csv")
    @Test
    public void checkFlags_priceNotPresent() {

        //given
        doReturn(businessResponseFromFile("price_not_present/page.json"))
                .when(dataCampShopClient).searchBusinessOffers(
                any());

        //when
        boolean hasPrice = defaultDataCampService.hasOfferWithPriceInDatacamp(PARTNER_ID);

        //then
        assertThat(hasPrice).isFalse();
    }

    @DisplayName("Есть оффер с ценой -> флаг цены в 1")
    @DbUnitDataSet(before = "DefaultDataCampFlagsTest.newflags.before.csv")
    @Test
    public void checkFlags_pricePresent() {
        //given
        doReturn(businessResponseFromFile("price_present/page.json"))
                .when(dataCampShopClient).searchBusinessOffers(any());

        //when
        boolean hasPrice = defaultDataCampService.hasOfferWithPriceInDatacamp(PARTNER_ID);

        //then
        assertThat(hasPrice).isTrue();
    }

    @DisplayName("Нет оффера со стоками в /offers -> флаг стоков в 0")
    @Test
    @DbUnitDataSet(before = "DefaultDataCampFlagsTest.newflags.before.csv")
    public void checkFlags_stocksNotPresent() {
        //given
        doReturn(businessResponseFromFile("stocks_not_present.json"))
                .when(dataCampShopClient).searchBusinessOffers(any());

        boolean hasStocks = defaultDataCampService.hasOfferWithStocksInDatacamp(PARTNER_ID);

        assertThat(hasStocks).isFalse();
    }

    @DisplayName("Есть оффер со стоками в /offers -> флаг стоков в 1")
    @Test
    @DbUnitDataSet(before = "DefaultDataCampFlagsTest.newflags.before.csv")
    public void checkFlags_stocksPresent() {
        //given
        //В файле может быть любой оффер, это не важно, главное что нашелся какой-то
        doReturn(businessResponseFromFile("stocks_present.json"))
                .when(dataCampShopClient).searchBusinessOffers(any());

        boolean hasStocks = defaultDataCampService.hasOfferWithStocksInDatacamp(PARTNER_ID);

        assertThat(hasStocks).isTrue();
    }

    @DisplayName("Нет оффера с ценой и стоками в /offers -> флаг цены со стоками в 0")
    @Test
    @DbUnitDataSet(before = "DefaultDataCampFlagsTest.newflags.before.csv")
    public void checkFlags_priceAndStocksNotPresent() {
        //given
        doReturn(businessResponseFromFile("stocks_not_present.json"))
                .when(dataCampShopClient).searchBusinessOffers(any());

        boolean hasPriceWithStocks = defaultDataCampService.hasOfferWithPriceAndStocksInDatacamp(PARTNER_ID);

        assertThat(hasPriceWithStocks).isFalse();
    }

    @DisplayName("Есть оффер с ценой и стоками в /offers -> флаг цены со стоками в 1")
    @Test
    @DbUnitDataSet(before = "DefaultDataCampFlagsTest.newflags.before.csv")
    public void checkFlags_priceAndStocksPresent() {
        //given
        //В файле может быть любой оффер, это не важно, главное что нашелся какой-то
        doReturn(businessResponseFromFile("stocks_present.json"))
                .when(dataCampShopClient).searchBusinessOffers(any());

        boolean hasPriceWithStocks = defaultDataCampService.hasOfferWithPriceAndStocksInDatacamp(PARTNER_ID);

        assertThat(hasPriceWithStocks).isTrue();
    }

    private SearchBusinessOffersResult businessResponseFromFile(String fileName) {
        String path = DATACAMP_JSON_DIR + fileName;
        return DataCampStrollerConversions.fromStrollerResponse(
                ProtoTestUtil.getProtoMessageByJson(SyncGetOffer.GetUnitedOffersResponse.class, path, getClass()));
    }

    @DbUnitDataSet(before = "DefaultDataCampServiceTest.testUpdateFlags.before.csv",
            after = "DefaultDataCampServiceTest.testUpdateFlags.after.csv")
    @Test
    @DisplayName("Тест обновления флагов в БД")
    public void testUpdateFlags() {
        defaultDataCampService.updateDatacampPriceFlags(1, List.of(
                102L,
                202L,
                204L
        ));
    }

}
