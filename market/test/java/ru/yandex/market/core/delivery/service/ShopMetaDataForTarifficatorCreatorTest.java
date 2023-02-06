package ru.yandex.market.core.delivery.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.tariff.service.DeliveryTariffService;
import ru.yandex.market.core.ds.DatasourceService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramService;
import ru.yandex.market.core.supplier.dao.PartnerFulfillmentLinkDao;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.PartnerPlacementProgramType;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ShopMetaDataDto;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DbUnitDataSet(before = "ShopMetaDataForTarifficator.before.csv")
public class ShopMetaDataForTarifficatorCreatorTest extends FunctionalTest {

    @Autowired
    private DeliveryTariffService deliveryTariffService;
    @Autowired
    private DatasourceService datasourceService;
    @Autowired
    private PartnerFulfillmentLinkDao partnerFulfillmentLinkDao;
    @Autowired
    private PartnerPlacementProgramService partnerPlacementProgramService;

    private ShopMetaDataForTarifficatorCreator tested;

    @BeforeEach
    void setUp() {
        tested = new ShopMetaDataForTarifficatorCreator(
                deliveryTariffService,
                datasourceService,
                partnerFulfillmentLinkDao,
                partnerPlacementProgramService);
    }

    @Test
    void testSuccessfulMetaDataCreation() {
        assertThat(tested.createShopMetaData(1000L))
                .isNotNull()
                .usingRecursiveComparison()
                .isEqualTo(new ShopMetaDataDto()
                        .currency(Currency.RUR)
                        .localRegion(213L)
                        .logisticPartnerId(1000L)
                        .addPlacementProgramsItem(PartnerPlacementProgramType.DROPSHIP_BY_SELLER));
    }

    @Test
    void testSuccessfulMetaDataCreationWithNullLocalRegion() {
        assertThat(tested.createShopMetaData(2000L))
                .isNotNull()
                .usingRecursiveComparison()
                .isEqualTo(new ShopMetaDataDto()
                        .currency(Currency.RUR)
                        .localRegion(213L)
                        .addPlacementProgramsItem(PartnerPlacementProgramType.CPC));
    }
}
