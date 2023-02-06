package ru.yandex.market.logistics.tarifficator.service.shop;

import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.PartnerPlacementProgramType;
import ru.yandex.market.logistics.tarifficator.model.shop.ShopMetaData;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@DatabaseSetup("/service/shop/shopMetaDataService.before.xml")
class ShopMetaDataServiceImplTest extends AbstractContextualTest {

    @Autowired
    private ShopMetaDataService tested;

    @Test
    @DisplayName("Создание новых метаданных")
    @ExpectedDatabase(
        value = "/service/shop/saveShopMetaCreate.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreateShopMetaData() {
        tested.saveShopMetaData(
            ShopMetaData.builder()
                .shopId(222L)
                .currency(Currency.EUR)
                .logisticPartnerId(2L)
                .placementPrograms(List.of(PartnerPlacementProgramType.CROSSDOCK))
                .build(),
            1L
        );
    }

    @Test
    @DisplayName("Апдейт уже существующих метаданных")
    @ExpectedDatabase(
        value = "/service/shop/saveShopMetaUpdate.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateShopMetaData() {
        tested.saveShopMetaData(
            ShopMetaData.builder()
                .shopId(111L)
                .currency(Currency.EUR)
                .logisticPartnerId(1L)
                .placementPrograms(List.of(PartnerPlacementProgramType.CROSSDOCK))
                .build(),
            1L
        );
    }

    @Test
    @DisplayName("Получение валюты магазина")
    void testGetShopCurrency() {
        softly
            .assertThat(tested.getShopCurrency(111L))
            .isEqualTo(Currency.USD);
    }

    @Test
    @DisplayName("Получение дефолтовой валюты, если метаданных нет в БД")
    void testGetShopCurrencyNoShop() {
        softly
            .assertThat(tested.getShopCurrency(222L))
            .isEqualTo(Currency.RUR);
    }

    @Test
    @DisplayName("Получение типа программы размещения партнёра")
    void testGetPartnerPlacementProgramType() {
        softly
            .assertThat(tested.getPartnerPlacementProgramTypes(111L))
            .isEqualTo(List.of(
                PartnerPlacementProgramType.CROSSDOCK,
                PartnerPlacementProgramType.DROPSHIP_BY_SELLER
            ));
    }

    @Test
    @DisplayName("Получение типа программы размещения партнёра, если метаданных нет в БД")
    void testGetPartnerPlacementProgramTypeNoShop() {
        softly
            .assertThat(tested.getPartnerPlacementProgramTypes(222L))
            .isEqualTo(Collections.emptyList());
    }
}
