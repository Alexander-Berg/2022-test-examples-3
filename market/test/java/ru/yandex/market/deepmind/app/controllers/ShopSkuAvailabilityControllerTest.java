package ru.yandex.market.deepmind.app.controllers;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deepmind.app.exportable.ShopSkuAvailabilityExportable;
import ru.yandex.market.deepmind.app.pojo.DisplayMsku;
import ru.yandex.market.deepmind.app.utils.DeepmindUtils;
import ru.yandex.market.deepmind.app.web.ssku_availability.DisplayShopSkuAvailability;
import ru.yandex.market.deepmind.app.web.ssku_availability.ShopSkuAvailabilityRequest;
import ru.yandex.market.deepmind.app.web.ssku_availability.ShopSkuAvailabilityUpdateRequest;
import ru.yandex.market.deepmind.app.web.ssku_availability.ShopSkuAvailabilityValue;
import ru.yandex.market.deepmind.app.web.ssku_availability.ShopSkuAvailabilityWebFilter;
import ru.yandex.market.deepmind.common.assertions.DeepmindAssertions;
import ru.yandex.market.deepmind.common.availability.ShopSkuWKey;
import ru.yandex.market.deepmind.common.availability.ssku.ShopSkuKeyLastFilter;
import ru.yandex.market.deepmind.common.background.BaseBackgroundExportable;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.BlockReasonKey;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.services.AssortType;
import ru.yandex.market.deepmind.common.services.LockType;
import ru.yandex.market.deepmind.common.services.MskuInfoFeatures;
import ru.yandex.market.deepmind.common.utils.MatrixAvailabilityUtils;
import ru.yandex.market.deepmind.common.utils.SecurityContextAuthenticationUtils;
import ru.yandex.market.deepmind.common.utils.excel.ExcelUtils;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionStatus;
import ru.yandex.market.mboc.common.exceptions.WarningException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.deepmind.common.background.BaseBackgroundExportable.AVAILABLE;
import static ru.yandex.market.deepmind.common.background.BaseBackgroundExportable.AVAILABLE_INHERITED;
import static ru.yandex.market.deepmind.common.background.BaseBackgroundExportable.NOT_AVAILABLE;
import static ru.yandex.market.deepmind.common.background.BaseBackgroundExportable.NOT_AVAILABLE_INHERITED;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.ACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.DELISTED;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE_TMP;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.PENDING;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.REAL_SUPPLIER;
import static ru.yandex.market.deepmind.common.pojo.SskuStatusReason.NO_PURCHASE_PRICE;
import static ru.yandex.market.deepmind.common.pojo.SskuStatusReason.UNDER_CONSIDERATION;
import static ru.yandex.market.deepmind.common.pojo.SskuStatusReason.WAITING_FOR_ENTER;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;

public class ShopSkuAvailabilityControllerTest extends BaseShopSkuControllerTest {

    @Test
    public void testListOffers() {
        deepmindSupplierRepository.save(create1PSupplier(1, "000008"), create1PSupplier(2, "000009"));
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(1, "ssku-2", DELISTED);
        insertOffer(2, "ssku-1", INACTIVE);

        var list = availabilityController.list(ShopSkuAvailabilityRequest.all());

        assertThat(list)
            .extracting(DisplayShopSkuAvailability::getOffer)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                displayOffer(1, "ssku-1"),
                displayOffer(1, "ssku-2"),
                displayOffer(2, "ssku-1")
            );
    }

    @Test
    public void testListOffersWithMskuInfo() {
        deepmindMskuRepository.save(msku(111222L), msku(333444L));
        var mskuInfo1 = mskuInfo(111222L);
        var mskuInfo2 = mskuInfo(333444L);
        insertOffer(1, "ssku-1", ACTIVE, mskuInfo1.getMarketSkuId());
        insertOffer(1, "ssku-2", DELISTED, mskuInfo2.getMarketSkuId());
        insertOffer(2, "ssku-1", INACTIVE, mskuInfo1.getMarketSkuId());
        mskuInfoRepository.save(mskuInfo1, mskuInfo2);

        var list = availabilityController.list(ShopSkuAvailabilityRequest.all());

        assertThat(list)
            .extracting(DisplayShopSkuAvailability::getOffer)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                displayOffer(1, "ssku-1"),
                displayOffer(1, "ssku-2"),
                displayOffer(2, "ssku-1")
            );
        assertThat(list)
            .extracting(DisplayShopSkuAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo1, mskuInfo2);
    }

    @Test
    public void testListOffersWithCoreFixFilter() {
        deepmindMskuRepository.save(msku(111222L), msku(333444L), msku(444555L));
        var mskuInfo1 = mskuInfo(111222L);
        var mskuInfo2 = mskuInfo(333444L).setInTargetAssortment(true);
        var mskuInfo3 = mskuInfo(444555L).setInTargetAssortment(true);
        insertOffer(1, "ssku-1", ACTIVE, mskuInfo1.getMarketSkuId());
        insertOffer(1, "ssku-2", DELISTED, mskuInfo2.getMarketSkuId());
        insertOffer(2, "ssku-2", INACTIVE, mskuInfo3.getMarketSkuId());
        mskuInfoRepository.save(mskuInfo1, mskuInfo2, mskuInfo3);

        var coreFixList = availabilityController.list(
            new ShopSkuAvailabilityRequest()
                .setLastKeyFilter(new ShopSkuKeyLastFilter(null, 100))
                .setWebFilter(new ShopSkuAvailabilityWebFilter()
                    .setMskuInfoFeatures(new MskuInfoFeatures().setInTargetAssortment(true))));

        assertThat(coreFixList)
            .extracting(DisplayShopSkuAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo2, mskuInfo3);

        var emptyFilterResult = availabilityController.list(
            new ShopSkuAvailabilityRequest()
                .setLastKeyFilter(new ShopSkuKeyLastFilter(null, 100))
                .setWebFilter(new ShopSkuAvailabilityWebFilter()
                    .setMskuInfoFeatures(new MskuInfoFeatures())));

        assertThat(emptyFilterResult)
            .extracting(DisplayShopSkuAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo2, mskuInfo3);

        var negativeFilterResult = availabilityController.list(
            new ShopSkuAvailabilityRequest()
                .setLastKeyFilter(new ShopSkuKeyLastFilter(null, 100))
                .setWebFilter(new ShopSkuAvailabilityWebFilter()
                    .setMskuInfoFeatures(new MskuInfoFeatures().setInTargetAssortment(false))));

        assertThat(negativeFilterResult)
            .extracting(DisplayShopSkuAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1);
    }

    @Test
    public void testListOffersWithPriceFilter() {
        deepmindMskuRepository.save(msku(111222L), msku(333444L), msku(444555L));
        var mskuInfo1 = mskuInfo(111222L).setPricebandId(123L);
        var mskuInfo2 = mskuInfo(333444L).setPricebandId(123L).setPricebandLabel("123_label");
        var mskuInfo3 = mskuInfo(444555L)
            .setInTargetAssortment(true).setPricebandId(234L).setPricebandLabel("234_label");
        insertOffer(1, "ssku-1", ACTIVE, mskuInfo1.getMarketSkuId());
        insertOffer(1, "ssku-2", DELISTED, mskuInfo2.getMarketSkuId());
        insertOffer(2, "ssku-2", INACTIVE, mskuInfo3.getMarketSkuId());
        mskuInfoRepository.save(mskuInfo1, mskuInfo2, mskuInfo3);

        var list = availabilityController.list(
            new ShopSkuAvailabilityRequest()
                .setLastKeyFilter(new ShopSkuKeyLastFilter(null, 100))
                .setWebFilter(new ShopSkuAvailabilityWebFilter()
                    .setMskuInfoFeatures(new MskuInfoFeatures().setPricebandId(123L))));

        assertThat(list)
            .extracting(DisplayShopSkuAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo2);

        list = availabilityController.list(
            new ShopSkuAvailabilityRequest()
                .setLastKeyFilter(new ShopSkuKeyLastFilter(null, 100))
                .setWebFilter(new ShopSkuAvailabilityWebFilter()
                    .setMskuInfoFeatures(new MskuInfoFeatures().setPricebandLabel("234_label"))));

        assertThat(list)
            .extracting(DisplayShopSkuAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo3);

        list = availabilityController.list(
            new ShopSkuAvailabilityRequest()
                .setLastKeyFilter(new ShopSkuKeyLastFilter(null, 100))
                .setWebFilter(new ShopSkuAvailabilityWebFilter()
                    .setMskuInfoFeatures(new MskuInfoFeatures().setPricebandId(123L).setPricebandLabel("234_label"))));

        assertThat(list)
            .extracting(DisplayShopSkuAvailability::getMskuInfo)
            .isEmpty();

        list = availabilityController.list(
            new ShopSkuAvailabilityRequest()
                .setLastKeyFilter(new ShopSkuKeyLastFilter(null, 100))
                .setWebFilter(new ShopSkuAvailabilityWebFilter()
                    .setMskuInfoFeatures(new MskuInfoFeatures().setInTargetAssortment(true).setPricebandId(234L)
                        .setPricebandLabel("234_label"))));

        assertThat(list)
            .extracting(DisplayShopSkuAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo3);
    }

    @Test
    public void testListOffersWithPriceLimitsFilter() {
        deepmindMskuRepository.save(msku(111222L), msku(333444L), msku(444555L));
        var mskuInfo1 = mskuInfo(111222L).setPrice(300.0).setPricebandId(111L);
        var mskuInfo2 = mskuInfo(333444L).setPrice(1300.0).setPricebandId(111L);
        var mskuInfo3 = mskuInfo(444555L).setPrice(5000.0).setPricebandId(222L);
        insertOffer(1, "ssku-1", ACTIVE, mskuInfo1.getMarketSkuId());
        insertOffer(1, "ssku-2", DELISTED, mskuInfo2.getMarketSkuId());
        insertOffer(2, "ssku-2", INACTIVE, mskuInfo3.getMarketSkuId());
        mskuInfoRepository.save(mskuInfo1, mskuInfo2, mskuInfo3);

        var list = availabilityController.list(
            new ShopSkuAvailabilityRequest()
                .setLastKeyFilter(new ShopSkuKeyLastFilter(null, 100))
                .setWebFilter(new ShopSkuAvailabilityWebFilter()
                    .setMskuInfoFeatures(new MskuInfoFeatures().setFromPriceInclusive(200.0))));

        assertThat(list)
            .extracting(DisplayShopSkuAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo2, mskuInfo3);

        list = availabilityController.list(
            new ShopSkuAvailabilityRequest()
                .setLastKeyFilter(new ShopSkuKeyLastFilter(null, 100))
                .setWebFilter(new ShopSkuAvailabilityWebFilter()
                    .setMskuInfoFeatures(
                        new MskuInfoFeatures().setFromPriceInclusive(350.0).setToPriceInclusive(3000.0))));

        assertThat(list)
            .extracting(DisplayShopSkuAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo2);

        list = availabilityController.list(
            new ShopSkuAvailabilityRequest()
                .setLastKeyFilter(new ShopSkuKeyLastFilter(null, 100))
                .setWebFilter(new ShopSkuAvailabilityWebFilter()
                    .setMskuInfoFeatures(
                        new MskuInfoFeatures().setFromPriceInclusive(300.0).setToPriceInclusive(1300.0))));

        assertThat(list)
            .extracting(DisplayShopSkuAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo2);

        list = availabilityController.list(
            new ShopSkuAvailabilityRequest()
                .setLastKeyFilter(new ShopSkuKeyLastFilter(null, 100))
                .setWebFilter(new ShopSkuAvailabilityWebFilter()
                    .setMskuInfoFeatures(new MskuInfoFeatures().setToPriceInclusive(3000.0))));

        assertThat(list)
            .extracting(DisplayShopSkuAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo2);

        list = availabilityController.list(
            new ShopSkuAvailabilityRequest()
                .setLastKeyFilter(new ShopSkuKeyLastFilter(null, 100))
                .setWebFilter(new ShopSkuAvailabilityWebFilter()
                    .setMskuInfoFeatures(new MskuInfoFeatures().setFromPriceInclusive(200.0).setPricebandId(111L))));

        assertThat(list)
            .extracting(DisplayShopSkuAvailability::getMskuInfo)
            .containsExactlyInAnyOrder(mskuInfo1, mskuInfo2);
    }

    @Test
    public void testListWithAvailabilities() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(1, "ssku-2", DELISTED);
        insertOffer(2, "ssku-1", INACTIVE);
        insertOffer(3, "ssku-3", INACTIVE);
        insertOffer(4, "ssku-4", INACTIVE);

        sskuAvailabilityMatrixRepository.save(
            sskuAvailabilityMatrix(1, "ssku-1", ROSTOV_ID, false, null, null)
                .setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
            sskuAvailabilityMatrix(1, "ssku-2", SOFINO_ID, true, "21-02-2021", "21-02-2099"),
            sskuAvailabilityMatrix(1, "ssku-2", TOMILINO_ID, false, "22-02-2021", null),

            // will be skipped
            sskuAvailabilityMatrix(3, "ssku-3", TOMILINO_ID, false, null, "31-12-2020"),
            sskuAvailabilityMatrix(4, "ssku-4", TOMILINO_ID, false, "31-12-2019", "31-12-2020")
        );

        var list = availabilityController.list(ShopSkuAvailabilityRequest.all());

        assertThat(find(list, 1, "ssku-1").getAvailabilities())
            .usingElementComparatorIgnoringFields("auditInfo")
            .containsExactly(
                displayAvailability(ROSTOV_ID, false, null, null)
                    .setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK)
            );

        assertThat(find(list, 1, "ssku-2").getAvailabilities())
            .usingElementComparatorIgnoringFields("auditInfo")
            .containsExactlyInAnyOrder(
                displayAvailability(SOFINO_ID, true, "21-02-2021", "21-02-2099"),
                displayAvailability(TOMILINO_ID, false, "22-02-2021", null)
            );

        assertThat(find(list, 2, "ssku-1").getAvailabilities()).isEmpty();
        assertThat(find(list, 3, "ssku-3").getAvailabilities()).isEmpty();
        assertThat(find(list, 4, "ssku-4").getAvailabilities()).isEmpty();
    }

    @Test
    public void testListOffersWithoutMsku() {
        insertOffer(1, "ssku-1", ACTIVE, 2222);

        var list = availabilityController.list(ShopSkuAvailabilityRequest.all());

        assertThat(list.get(0).getMsku()).isNotNull();
        assertThat(list.get(0).getMsku().getSource()).isEqualTo(DisplayMsku.MskuSource.STUB_FROM_OFFER);
    }

    @Test
    public void testFilterWithAvailabilities() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(1, "ssku-2", DELISTED);
        insertOffer(2, "ssku-1", INACTIVE);
        insertOffer(3, "ssku-3", ACTIVE);

        sskuAvailabilityMatrixRepository.save(
            sskuAvailabilityMatrix(1, "ssku-1", ROSTOV_ID, false, null, null),
            sskuAvailabilityMatrix(1, "ssku-2", SOFINO_ID, true, "21-02-2021", "21-02-2099"),
            sskuAvailabilityMatrix(1, "ssku-2", TOMILINO_ID, false, "22-02-2021", null),
            sskuAvailabilityMatrix(3, "ssku-3", TOMILINO_ID, false, "22-02-2021", null)
        );

        var listWithAvailabilities = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setContainsAvailabilitiesOnAnyWarehouseId(true))
        );
        assertThat(listWithAvailabilities)
            .extracting(DisplayShopSkuAvailability::getOffer)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                displayOffer(1, "ssku-1"),
                displayOffer(1, "ssku-2"),
                displayOffer(3, "ssku-3")
            );

        var listWithoutAvailabilities = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setDoesntContainAvailabilitiesOnAnyWarehouseId(true))
        );
        assertThat(listWithoutAvailabilities)
            .extracting(DisplayShopSkuAvailability::getOffer)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactly(
                displayOffer(2, "ssku-1")
            );

        var listWithAvailabilitiesTomilino = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter()
                .setContainsAvailabilitiesOnWarehouseIds(List.of(TOMILINO_ID)))
        );
        assertThat(listWithAvailabilitiesTomilino)
            .extracting(DisplayShopSkuAvailability::getOffer)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                displayOffer(1, "ssku-2"),
                displayOffer(3, "ssku-3")
            );
    }

    @Test
    public void testListDeadstock() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(1, "ssku-2", DELISTED);
        insertOffer(2, "ssku-1", INACTIVE);

        deadstockStatusRepository.save(
            deadstockStatus(1, "ssku-2", ROSTOV_ID, "21-01-2020"),
            deadstockStatus(1, "ssku-2", SOFINO_ID, "22-01-2020"),
            deadstockStatus(2, "ssku-1", TOMILINO_ID, "23-01-2020")
        );

        var list = availabilityController.list(ShopSkuAvailabilityRequest.all());

        assertThat(find(list, 1, "ssku-1").getDeadstock()).isEmpty();

        assertThat(find(list, 1, "ssku-2").getDeadstock())
            .containsExactlyInAnyOrder(
                displayDeadstock(ROSTOV_ID, "21-01-2020"),
                displayDeadstock(SOFINO_ID, "22-01-2020")
            );

        assertThat(find(list, 2, "ssku-1").getDeadstock())
            .containsExactly(
                displayDeadstock(TOMILINO_ID, "23-01-2020")
            );
    }

    @Test
    public void testFilterDeadstock() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(1, "ssku-2", DELISTED);
        insertOffer(2, "ssku-1", INACTIVE);

        deadstockStatusRepository.save(
            deadstockStatus(1, "ssku-2", ROSTOV_ID, "21-01-2020"),
            deadstockStatus(1, "ssku-2", SOFINO_ID, "22-01-2020"),
            deadstockStatus(2, "ssku-1", TOMILINO_ID, "23-01-2020")
        );

        var listWithDeadstock = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setIsDeadstock(true))
        );
        var listWithoutDeadstock = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setIsDeadstock(false))
        );

        assertThat(listWithDeadstock)
            .extracting(DisplayShopSkuAvailability::getOffer)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                displayOffer(1, "ssku-2"),
                displayOffer(2, "ssku-1")
            );
        assertThat(listWithoutDeadstock)
            .extracting(DisplayShopSkuAvailability::getOffer)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactly(
                displayOffer(1, "ssku-1")
            );
    }

    @Test
    public void testFilterSelectedWarehousesDeadstock() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(1, "ssku-2", DELISTED);
        insertOffer(2, "ssku-1", INACTIVE);

        deadstockStatusRepository.save(
            deadstockStatus(1, "ssku-2", ROSTOV_ID, "21-01-2020"),
            deadstockStatus(1, "ssku-2", SOFINO_ID, "22-01-2020"),
            deadstockStatus(2, "ssku-1", TOMILINO_ID, "23-01-2020")
        );

        var listWithAllDeadstocks = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setIsDeadstock(true))
        );
        var listWithAllSelectedDeadstocks = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter()
                .setIsDeadstock(true)
                .setSelectedWarehouseIds(List.of(SOFINO_ID))
            )
        );

        assertThat(listWithAllDeadstocks)
            .extracting(DisplayShopSkuAvailability::getOffer)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                displayOffer(1, "ssku-2"),
                displayOffer(2, "ssku-1")
            );
        assertThat(listWithAllSelectedDeadstocks)
            .extracting(DisplayShopSkuAvailability::getOffer)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                displayOffer(1, "ssku-2")
            );
    }

    @Test
    public void testListAlmostDeadstock() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(1, "ssku-2", DELISTED);
        insertOffer(2, "ssku-1", INACTIVE);

        almostDeadstockStatusRepository.save(
            almostDeadstockStatus(1, "ssku-2", ROSTOV_ID, "21-01-2020"),
            almostDeadstockStatus(1, "ssku-2", SOFINO_ID, "22-01-2020"),
            almostDeadstockStatus(2, "ssku-1", TOMILINO_ID, "23-01-2020")
        );

        var list = availabilityController.list(ShopSkuAvailabilityRequest.all());

        assertThat(find(list, 1, "ssku-1").getAlmostDeadstock()).isEmpty();

        assertThat(find(list, 1, "ssku-2").getAlmostDeadstock())
            .containsExactlyInAnyOrder(
                displayAlmostDeadstock(ROSTOV_ID, "21-01-2020"),
                displayAlmostDeadstock(SOFINO_ID, "22-01-2020")
            );

        assertThat(find(list, 2, "ssku-1").getAlmostDeadstock())
            .containsExactly(
                displayAlmostDeadstock(TOMILINO_ID, "23-01-2020")
            );
    }

    @Test
    public void testFilterAlmostDeadstock() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(1, "ssku-2", DELISTED);
        insertOffer(2, "ssku-1", INACTIVE);

        almostDeadstockStatusRepository.save(
            almostDeadstockStatus(1, "ssku-2", ROSTOV_ID, "21-01-2020"),
            almostDeadstockStatus(1, "ssku-2", SOFINO_ID, "22-01-2020"),
            almostDeadstockStatus(2, "ssku-1", TOMILINO_ID, "23-01-2020")
        );

        var listWithAlmostDeadstock = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setIsAlmostDeadstock(true))
        );
        var listWithoutAlmostDeadstock = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setIsAlmostDeadstock(false))
        );

        assertThat(listWithAlmostDeadstock)
            .extracting(DisplayShopSkuAvailability::getOffer)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                displayOffer(1, "ssku-2"),
                displayOffer(2, "ssku-1")
            );
        assertThat(listWithoutAlmostDeadstock)
            .extracting(DisplayShopSkuAvailability::getOffer)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactly(
                displayOffer(1, "ssku-1")
            );
    }

    @Test
    public void testFilterSelectedWarehousesAlmostDeadstock() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(1, "ssku-2", DELISTED);
        insertOffer(2, "ssku-1", INACTIVE);

        almostDeadstockStatusRepository.save(
            almostDeadstockStatus(1, "ssku-2", ROSTOV_ID, "21-01-2020"),
            almostDeadstockStatus(1, "ssku-2", SOFINO_ID, "22-01-2020"),
            almostDeadstockStatus(2, "ssku-1", TOMILINO_ID, "23-01-2020")
        );

        var listWithAllAlmostDeadstocks = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setIsAlmostDeadstock(true))
        );
        var listWithAllSelectedAlmostDeadstocks = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter()
                .setIsAlmostDeadstock(true)
                .setSelectedWarehouseIds(List.of(SOFINO_ID))
            )
        );

        assertThat(listWithAllAlmostDeadstocks)
            .extracting(DisplayShopSkuAvailability::getOffer)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                displayOffer(1, "ssku-2"),
                displayOffer(2, "ssku-1")
            );
        assertThat(listWithAllSelectedAlmostDeadstocks)
            .extracting(DisplayShopSkuAvailability::getOffer)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                displayOffer(1, "ssku-2")
            );
    }

    @Test
    public void testFilterBySskuStatus() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(1, "ssku-2", DELISTED);
        insertOffer(2, "ssku-1", INACTIVE);
        insertOffer(3, "ssku-3", ACTIVE, 3333);

        var activeOffers = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setOfferAvailabilities(List.of(ACTIVE)))
        );
        assertThat(activeOffers)
            .extracting(DisplayShopSkuAvailability::getShopSkuKey)
            .containsExactly(
                new ServiceOfferKey(1, "ssku-1"),
                new ServiceOfferKey(3, "ssku-3")
            );
    }

    @Test
    public void testFilterBySskuReasonAndComment() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(1, "ssku-2", INACTIVE_TMP);
        insertOffer(2, "ssku-1", INACTIVE_TMP);
        insertOffer(3, "ssku-3", ACTIVE, 3333);

        sskuStatusRepository.save(sskuStatusRepository.findByKey(1, "ssku-1").get()
            .setComment("A1. Правило"));
        sskuStatusRepository.save(
            sskuStatusRepository.findByKey(1, "ssku-2").get()
                .setReason(WAITING_FOR_ENTER)
                .setComment(WAITING_FOR_ENTER.getLiteral())
        );
        sskuStatusRepository.save(
            sskuStatusRepository.findByKey(2, "ssku-1").get()
                .setReason(NO_PURCHASE_PRICE)
                .setComment(NO_PURCHASE_PRICE.getLiteral() + " A1. Правило")
        );

        var noPurchasePriceOffers = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setSskuStatusReasons(List.of(NO_PURCHASE_PRICE)))
        );
        assertThat(noPurchasePriceOffers)
            .extracting(DisplayShopSkuAvailability::getShopSkuKey)
            .containsExactly(
                new ServiceOfferKey(2, "ssku-1")
            );

        var assortCommitteeOffers = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setSskuStatusReasons(List.of(WAITING_FOR_ENTER)))
        );
        assertThat(assortCommitteeOffers)
            .extracting(DisplayShopSkuAvailability::getShopSkuKey)
            .containsExactly(
                new ServiceOfferKey(1, "ssku-2")
            );
    }

    @Test
    public void testBackToPendingFilter() {
        insertOffer(1, "ssku-1", INACTIVE_TMP);
        insertOffer(2, "ssku-2", INACTIVE_TMP);
        insertOffer(3, "ssku-3", INACTIVE);
        insertOffer(4, "ssku-4", DELISTED, 3333);
        insertOffer(5, "ssku-5", PENDING);
        insertOffer(6, "ssku-6", INACTIVE_TMP);
        insertOffer(7, "ssku-7", ACTIVE);


        sskuStatusRepository.save(
            sskuStatusRepository.findByKey(1, "ssku-1").get()
                .setReason(NO_PURCHASE_PRICE)
                .setComment(NO_PURCHASE_PRICE.getLiteral() + " A1. Правило")
        );
        sskuStatusRepository.save(
            sskuStatusRepository.findByKey(2, "ssku-2").get()
                .setReason(WAITING_FOR_ENTER)
                .setComment(WAITING_FOR_ENTER.getLiteral())
        );
        sskuStatusRepository.save(
            sskuStatusRepository.findByKey(3, "ssku-3").get()
                .setReason(NO_PURCHASE_PRICE)
                .setComment(NO_PURCHASE_PRICE.getLiteral())
        );
        sskuStatusRepository.save(
            sskuStatusRepository.findByKey(6, "ssku-6").get()
                .setReason(NO_PURCHASE_PRICE)
                .setComment(NO_PURCHASE_PRICE.getLiteral())
        );

        var noPurchasePriceOffers = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter()
                .setOfferAvailabilities(List.of(INACTIVE, DELISTED, INACTIVE_TMP))
                .setSskuStatusReasons(List.of(NO_PURCHASE_PRICE, UNDER_CONSIDERATION))
            )
        );
        assertThat(noPurchasePriceOffers)
            .extracting(DisplayShopSkuAvailability::getShopSkuKey)
            .containsExactly(
                new ServiceOfferKey(1, "ssku-1"),
                new ServiceOfferKey(3, "ssku-3"),
                new ServiceOfferKey(4, "ssku-4"),
                new ServiceOfferKey(6, "ssku-6")
            );

        var assortCommitteeOffers = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter()
                .setSskuStatusReasons(List.of(WAITING_FOR_ENTER)))
        );
        assertThat(assortCommitteeOffers)
            .extracting(DisplayShopSkuAvailability::getShopSkuKey)
            .containsExactly(
                new ServiceOfferKey(2, "ssku-2")
            );
    }

    @Test
    public void testsskuStatusAndReasonFilter() {
        insertOffer(1, "ssku-1", INACTIVE_TMP);
        insertOffer(2, "ssku-2", INACTIVE_TMP);
        insertOffer(3, "ssku-3", INACTIVE);
        insertOffer(4, "ssku-4", DELISTED, 3333);
        insertOffer(5, "ssku-5", PENDING);
        insertOffer(6, "ssku-6", INACTIVE_TMP);
        insertOffer(7, "ssku-7", ACTIVE);


        sskuStatusRepository.save(
            sskuStatusRepository.findByKey(1, "ssku-1").get()
                .setReason(NO_PURCHASE_PRICE)
                .setComment(NO_PURCHASE_PRICE.getLiteral() + " A1. Правило")
        );
        sskuStatusRepository.save(
            sskuStatusRepository.findByKey(2, "ssku-2").get()
                .setReason(WAITING_FOR_ENTER)
                .setComment(WAITING_FOR_ENTER.getLiteral())
        );
        sskuStatusRepository.save(
            sskuStatusRepository.findByKey(3, "ssku-3").get()
                .setReason(NO_PURCHASE_PRICE)
                .setComment(NO_PURCHASE_PRICE.getLiteral())
        );
        sskuStatusRepository.save(
            sskuStatusRepository.findByKey(6, "ssku-6").get()
                .setReason(NO_PURCHASE_PRICE)
                .setComment(NO_PURCHASE_PRICE.getLiteral())
        );


        var assortCommitteeOffers = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter()
                .setOfferAvailabilities(List.of(INACTIVE_TMP))
                .setSskuStatusReasons(List.of(WAITING_FOR_ENTER)))
        );
        assertThat(assortCommitteeOffers)
            .extracting(DisplayShopSkuAvailability::getShopSkuKey)
            .containsExactly(
                new ServiceOfferKey(2, "ssku-2")
            );
    }

    @Test
    public void testSomeOtherFilters() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(1, "ssku-2", DELISTED);
        insertOffer(2, "ssku-1", INACTIVE);
        insertOffer(3, "ssku-3", ACTIVE, 3333);

        insertHiding(1, "ssku-2", "SKK_45k", null, "Ivan");
        insertHiding(1, "ssku-2", "ABO_LEGAL", null, "Ivan");

        var offersSsku1 = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setShopSkuSearchText("ssku-1"))
        );
        assertThat(offersSsku1)
            .extracting(DisplayShopSkuAvailability::getOffer)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                displayOffer(1, "ssku-1"),
                displayOffer(2, "ssku-1")
            );

        var offersWithHidings = availabilityController.list(ShopSkuAvailabilityRequest.all()
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setHasStopWordHiding(true).setHasByAboLegalHiding(true)));
        assertThat(offersWithHidings)
            .extracting(DisplayShopSkuAvailability::getOffer)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                displayOffer(1, "ssku-2")
            );

        var offersByMsku = availabilityController.list(ShopSkuAvailabilityRequest.all()
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setMskuSearchText("test_search")));
        assertThat(offersByMsku)
            .extracting(DisplayShopSkuAvailability::getOffer)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                displayOffer(3, "ssku-3")
            );
    }

    @Test
    public void testListWithWarehouseIdAndLockedTypeTest() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(2, "ssku-2", ACTIVE);
        insertOffer(3, "ssku-3", ACTIVE);
        sskuAvailabilityMatrixRepository.save(
            sskuAvailabilityMatrix(1, "ssku-1", TOMILINO_ID, true, null, null),
            sskuAvailabilityMatrix(2, "ssku-2", TOMILINO_ID, false, null, null),
            sskuAvailabilityMatrix(3, "ssku-3", TOMILINO_ID, null, null, null)
        );

        var result = availabilityController
            .list(new ShopSkuAvailabilityRequest()
                .setWebFilter(new ShopSkuAvailabilityWebFilter()
                    .setShopSkuKeys(List.of(new ServiceOfferKey(1, "ssku-1"),
                        new ServiceOfferKey(2, "ssku-2"),
                        new ServiceOfferKey(3, "ssku-3"))))
                .setLastKeyFilter(new ShopSkuKeyLastFilter().setLimit(Integer.MAX_VALUE)));
        assertThat(result)
            .extracting(DisplayShopSkuAvailability::getShopSkuKey)
            .containsExactlyInAnyOrder(new ServiceOfferKey(1, "ssku-1"),
                new ServiceOfferKey(2, "ssku-2"), new ServiceOfferKey(3, "ssku-3"));

        result = availabilityController
            .list(new ShopSkuAvailabilityRequest()
                .setWebFilter(new ShopSkuAvailabilityWebFilter()
                    .setShopSkuKeys(List.of(new ServiceOfferKey(1, "ssku-1"),
                        new ServiceOfferKey(2, "ssku-2"), new ServiceOfferKey(3, "ssku-3")))
                    .setLockType(LockType.EXPLICIT_LOCK)
                    .setLockedAtWarehouseId(TOMILINO_ID))
                .setLastKeyFilter(new ShopSkuKeyLastFilter().setLimit(Integer.MAX_VALUE)));
        assertThat(result)
            .extracting(DisplayShopSkuAvailability::getShopSkuKey)
            .containsExactlyInAnyOrder(new ServiceOfferKey(2, "ssku-2"));

        result = availabilityController
            .list(new ShopSkuAvailabilityRequest()
                .setWebFilter(new ShopSkuAvailabilityWebFilter()
                    .setShopSkuKeys(List.of(new ServiceOfferKey(1, "ssku-1"),
                        new ServiceOfferKey(2, "ssku-2"), new ServiceOfferKey(3, "ssku-3")))
                    .setLockType(LockType.EXPLICIT_PERMISSION)
                    .setLockedAtWarehouseId(TOMILINO_ID))
                .setLastKeyFilter(new ShopSkuKeyLastFilter().setLimit(Integer.MAX_VALUE)));
        assertThat(result)
            .extracting(DisplayShopSkuAvailability::getShopSkuKey)
            .containsExactlyInAnyOrder(new ServiceOfferKey(1, "ssku-1"), new ServiceOfferKey(3, "ssku-3"));
    }

    @Test
    public void testListOffersWithBlockReasonExplicit() {
        insertOffer(1, "ssku-1", ACTIVE, 111);
        insertOffer(2, "ssku-2", ACTIVE, 222);
        insertOffer(3, "ssku-3", ACTIVE, 333);
        insertOffer(4, "ssku-3", ACTIVE, 333);
        insertOffer(5, "ssku-3", ACTIVE, 333);
        insertOffer(6, "ssku-3", ACTIVE, 333);
        insertOffer(7, "ssku-3", ACTIVE, 333);
        insertOffer(8, "ssku-3", ACTIVE, 333);
        insertOffer(9, "ssku-3", ACTIVE, 333);
        insertOffer(10, "ssku-3", ACTIVE, 333);
        insertOffer(11, "ssku-3", ACTIVE, 333);
        sskuAvailabilityMatrixRepository.save(
            sskuAvailabilityMatrix(1, "ssku-1", ROSTOV_ID, false, "01-01-2021", "01-02-2021")
                .setBlockReasonKey(BlockReasonKey.SSKU_PRIORITIZING_SALES),
            sskuAvailabilityMatrix(2, "ssku-2", ROSTOV_ID, null, "01-01-2021", "01-02-2021")
                .setBlockReasonKey(BlockReasonKey.SSKU_SUPPLIES_OPTIMIZATION_BLOCK),
            sskuAvailabilityMatrix(3, "ssku-3", ROSTOV_ID, false, "01-01-2021", "01-02-2021")
                .setBlockReasonKey(BlockReasonKey.SSKU_SUPPLIES_OPTIMIZATION_UNBLOCK),
            sskuAvailabilityMatrix(4, "ssku-3", ROSTOV_ID, false, "01-01-2021", "01-02-2021")
                .setBlockReasonKey(BlockReasonKey.SSKU_UNBLOCK_BY_AGREEMENT_WITH_WH),
            sskuAvailabilityMatrix(5, "ssku-3", ROSTOV_ID, false, "01-01-2021", "01-02-2021")
                .setBlockReasonKey(BlockReasonKey.SSKU_INVENTORY_UNBLOCK_TMP),
            sskuAvailabilityMatrix(6, "ssku-3", ROSTOV_ID, false, "01-01-2021", "01-02-2021")
                .setBlockReasonKey(BlockReasonKey.SSKU_RETURN_UNBLOCK_TMP),
            sskuAvailabilityMatrix(7, "ssku-3", ROSTOV_ID, false, "01-01-2021", "01-02-2021")
                .setBlockReasonKey(BlockReasonKey.SSKU_DELIVERY_ACCEPTANCE_AFTER_BLOCKING_UNBLOCK_TMP),
            sskuAvailabilityMatrix(8, "ssku-3", ROSTOV_ID, false, "01-01-2021", "01-02-2021")
                .setBlockReasonKey(BlockReasonKey.SSKU_FOR_TEST_UNBLOCK_TMP),
            sskuAvailabilityMatrix(9, "ssku-3", ROSTOV_ID, false, "01-01-2021", "01-02-2021")
                .setBlockReasonKey(BlockReasonKey.SSKU_AS_PROMOTION_UNBLOCK_TMP),
            sskuAvailabilityMatrix(10, "ssku-3", ROSTOV_ID, false, "01-01-2021", "01-02-2021")
                .setBlockReasonKey(BlockReasonKey.OTHER),
            sskuAvailabilityMatrix(11, "ssku-3", ROSTOV_ID, false, "01-01-2021", "01-02-2021")
        );

        var list = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter()
                .setBlockReasonKeys(Set.of(
                    BlockReasonKey.SSKU_PRIORITIZING_SALES,
                    BlockReasonKey.SSKU_SUPPLIES_OPTIMIZATION_BLOCK,
                    BlockReasonKey.SSKU_SUPPLIES_OPTIMIZATION_UNBLOCK,
                    BlockReasonKey.SSKU_UNBLOCK_BY_AGREEMENT_WITH_WH,
                    BlockReasonKey.SSKU_INVENTORY_UNBLOCK_TMP,
                    BlockReasonKey.SSKU_RETURN_UNBLOCK_TMP,
                    BlockReasonKey.SSKU_DELIVERY_ACCEPTANCE_AFTER_BLOCKING_UNBLOCK_TMP,
                    BlockReasonKey.SSKU_FOR_TEST_UNBLOCK_TMP,
                    BlockReasonKey.SSKU_AS_PROMOTION_UNBLOCK_TMP,
                    BlockReasonKey.OTHER
                ))));

        assertThat(list)
            .extracting(DisplayShopSkuAvailability::getShopSkuKey)
            .containsExactlyInAnyOrder(
                new ServiceOfferKey(1, "ssku-1"),
                new ServiceOfferKey(2, "ssku-2"),
                new ServiceOfferKey(3, "ssku-3"),
                new ServiceOfferKey(4, "ssku-3"),
                new ServiceOfferKey(5, "ssku-3"),
                new ServiceOfferKey(6, "ssku-3"),
                new ServiceOfferKey(7, "ssku-3"),
                new ServiceOfferKey(8, "ssku-3"),
                new ServiceOfferKey(9, "ssku-3"),
                new ServiceOfferKey(10, "ssku-3")
            );
    }

    @Test
    public void testHidings() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(1, "ssku-2", DELISTED);
        insertOffer(2, "ssku-1", INACTIVE);
        insertOffer(3, "ssku-3", ACTIVE);

        insertHiding(1, "ssku-1", "SKK_45k", "брак", "Иван");
        insertHiding(1, "ssku-1", "SKK_45k", "наркотики", "Александр");
        insertHiding(1, "ssku-2", "SKK_45k", "брак", "Олег");
        insertHiding(3, "ssku-3", "ABO_LEGAL", null, "Олег");

        var list = availabilityController.list(ShopSkuAvailabilityRequest.all());

        assertThat(find(list, 1, "ssku-1").getHidings())
            .usingElementComparatorOnFields("reasonKey", "userName", "stopWord")
            .containsExactlyInAnyOrder(
                displayHiding("SKK_45k", "Иван", "брак"),
                displayHiding("SKK_45k", "Александр", "наркотики")
            );
        assertThat(find(list, 1, "ssku-2").getHidings())
            .usingElementComparatorOnFields("reasonKey", "userName", "stopWord")
            .containsExactly(
                displayHiding("SKK_45k", "Олег", "брак")
            );
        assertThat(find(list, 2, "ssku-1").getHidings()).isEmpty();
        assertThat(find(list, 3, "ssku-3").getHidings())
            .usingElementComparatorOnFields("reasonKey", "userName", "stopWord")
            .containsExactly(
                displayHiding("ABO_LEGAL", "Олег", null)
            );
    }

    @Test
    public void testFilterHidings() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(1, "ssku-2", DELISTED);
        insertOffer(2, "ssku-1", INACTIVE);
        insertOffer(3, "ssku-3", ACTIVE);

        insertHiding(1, "ssku-1", "SKK_45k", "брак", "Иван");
        insertHiding(1, "ssku-1", "SKK_45k", "наркотики", "Александр");
        insertHiding(1, "ssku-2", "SKK_45k", "брак", "Олег");
        insertHiding(3, "ssku-3", "ABO_LEGAL", null, "Олег");

        var stopWordList = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setHasStopWordHiding(true))
        );
        assertThat(stopWordList)
            .extracting(DisplayShopSkuAvailability::getOffer)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                displayOffer(1, "ssku-1"),
                displayOffer(1, "ssku-2")
            );

        var aboLegalList = availabilityController.list(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setHasByAboLegalHiding(true))
        );
        assertThat(aboLegalList)
            .extracting(DisplayShopSkuAvailability::getOffer)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactly(
                displayOffer(3, "ssku-3")
            );
    }

    @Test
    public void testSave() {
        sskuAvailabilityMatrixRepository.save(
            sskuAvailabilityMatrix(1, "ssku-1", ROSTOV_ID, false, null, "20-01-2021"),
            sskuAvailabilityMatrix(3, "ssku-3", MARSHRUT_ID, false, null, null)
        );

        availabilityController.save(List.of(
            toSave(1, "ssku-1", ROSTOV_ID, ShopSkuAvailabilityValue.BLOCKED, null, null),
            toSave(1, "ssku-1", SOFINO_ID, ShopSkuAvailabilityValue.AVAILABLE, null, null),
            toSave(2, "ssku-2", TOMILINO_ID, ShopSkuAvailabilityValue.BLOCKED, "20-01-2021", "25-01-2021"),
            toSave(3, "ssku-3", MARSHRUT_ID, ShopSkuAvailabilityValue.NOT_SET, null, null)
        ), true);

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "modifiedAt")
            .containsExactlyInAnyOrder(
                sskuAvailabilityMatrix(1, "ssku-1", ROSTOV_ID, false, null, null)
                    .setModifiedLogin(DEEPMIND_APP_TEST_USER),
                sskuAvailabilityMatrix(1, "ssku-1", SOFINO_ID, true, null, null),
                sskuAvailabilityMatrix(2, "ssku-2", TOMILINO_ID, false, "20-01-2021", "25-01-2021"),
                sskuAvailabilityMatrix(3, "ssku-3", MARSHRUT_ID, null, null, null)
                    .setModifiedLogin(DEEPMIND_APP_TEST_USER)
            );
    }

    @Test
    public void testSaveWithWrongBlockReasons() {
        sskuAvailabilityMatrixRepository.save(
            sskuAvailabilityMatrix(1, "ssku-1", ROSTOV_ID, false, null, "20-01-2021"),
            sskuAvailabilityMatrix(3, "ssku-3", MARSHRUT_ID, false, null, null)
        );

        assertThatThrownBy(() -> availabilityController.save(List.of(
            toSave(1, "ssku-1", ROSTOV_ID, ShopSkuAvailabilityValue.AVAILABLE, null, null)
                .setBlockReasonKey(BlockReasonKey.MSKU_GOODS_STORAGE_CONDITIONS),
            toSave(1, "ssku-1", SOFINO_ID, ShopSkuAvailabilityValue.BLOCKED, null, null)
                .setBlockReasonKey(BlockReasonKey.CATEGORY_LEGAL_REQUIREMENTS),
            toSave(3, "ssku-3", MARSHRUT_ID, ShopSkuAvailabilityValue.NOT_SET, null, null)
        ), false))
            .hasMessageContaining(MbocErrors.get().invalidBlockReasonKeys(
                DeepmindUtils.AvailabilityMatrixType.SSKU.name(),
                BlockReasonKey.CATEGORY_LEGAL_REQUIREMENTS.getLiteral()
                    + ", " + BlockReasonKey.MSKU_GOODS_STORAGE_CONDITIONS.getLiteral()).toString());
        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "available")
            .contains(
                sskuAvailabilityMatrix(1, "ssku-1", ROSTOV_ID, false, null, null)
                    .setModifiedLogin(DEEPMIND_APP_TEST_USER),
                sskuAvailabilityMatrix(3, "ssku-3", MARSHRUT_ID, false, null, null)
                    .setModifiedLogin(DEEPMIND_APP_TEST_USER)
            );

        var actionId = availabilityController.saveAsync(new ShopSkuAvailabilityUpdateRequest()
            .setByFilter(
                new ShopSkuAvailabilityUpdateRequest.ShopSkuAvailabilityRequestByFilter()
                    .setFilter(new ShopSkuAvailabilityWebFilter())
                    .setAvailabilityByWarehouse(List.of(
                            new ShopSkuAvailabilityUpdateRequest.ShopSkuWarehouseAvailability()
                                .setWarehouseId(MARSHRUT_ID)
                                .setAvailable(ShopSkuAvailabilityValue.AVAILABLE)
                        )
                    )
                    .setBlockReasonKey(BlockReasonKey.MSKU_GOODS_STORAGE_CONDITIONS))
            .setForceUpdate(false));
        var action = backgroundServiceMock.getAction(actionId);
        assertThat(action).isNotNull();
        assertThat(action.getStatus()).isEqualTo(BackgroundActionStatus.ActionStatus.FAILED);
        assertThat(action.getMessage()).contains(MbocErrors.get().invalidBlockReasonKeys(
            DeepmindUtils.AvailabilityMatrixType.SSKU.name(),
            BlockReasonKey.MSKU_GOODS_STORAGE_CONDITIONS.getLiteral()).toString());

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "available")
            .contains(
                sskuAvailabilityMatrix(3, "ssku-3", MARSHRUT_ID, false, null, null)
                    .setModifiedLogin(DEEPMIND_APP_TEST_USER)
            );
    }

    @Test
    public void testSaveAsyncByShopSkus() {
        availabilityController.saveAsync(new ShopSkuAvailabilityUpdateRequest()
            .setByShopSku(List.of(
                toSave(1, "ssku-1", ROSTOV_ID, ShopSkuAvailabilityValue.BLOCKED, null, null)
                    .setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                toSave(1, "ssku-1", SOFINO_ID, ShopSkuAvailabilityValue.AVAILABLE, null, null)
            ))
            .setForceUpdate(true)
            .setComment("test_comment_async")
        );

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "modifiedAt", "comment")
            .containsExactlyInAnyOrder(
                sskuAvailabilityMatrix(1, "ssku-1", ROSTOV_ID, false, null, null)
                    .setComment("test_comment_async").setBlockReasonKey(BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK),
                sskuAvailabilityMatrix(1, "ssku-1", SOFINO_ID, true, null, null)
                    .setComment("test_comment_async")
            );
    }

    @Test
    public void testAsyncBlockAllOffersOnWarehouse() {
        insertOffer(2, "ssku-1", INACTIVE);
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(1, "ssku-2", DELISTED);

        var blockReasonKey = BlockReasonKey.SSKU_COPYRIGHT_HOLDER_BLOCK;
        // Блокируем все офферы в Ростове и разрешаем в Томилино
        availabilityController.saveAsync(new ShopSkuAvailabilityUpdateRequest()
            .setByFilter(
                new ShopSkuAvailabilityUpdateRequest.ShopSkuAvailabilityRequestByFilter()
                    .setFilter(new ShopSkuAvailabilityWebFilter())
                    .setAvailabilityByWarehouse(List.of(
                            new ShopSkuAvailabilityUpdateRequest.ShopSkuWarehouseAvailability()
                                .setWarehouseId(ROSTOV_ID)
                                .setAvailable(ShopSkuAvailabilityValue.BLOCKED),
                            new ShopSkuAvailabilityUpdateRequest.ShopSkuWarehouseAvailability()
                                .setWarehouseId(TOMILINO_ID)
                                .setAvailable(ShopSkuAvailabilityValue.AVAILABLE)
                        )
                    )
                    .setBlockReasonKey(blockReasonKey)
            )
            .setForceUpdate(true)
        );

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "modifiedAt")
            .containsExactlyInAnyOrder(
                sskuAvailabilityMatrix(1, "ssku-1", ROSTOV_ID, false, null, null)
                    .setBlockReasonKey(blockReasonKey),
                sskuAvailabilityMatrix(1, "ssku-2", ROSTOV_ID, false, null, null)
                    .setBlockReasonKey(blockReasonKey),
                sskuAvailabilityMatrix(2, "ssku-1", ROSTOV_ID, false, null, null)
                    .setBlockReasonKey(blockReasonKey),

                sskuAvailabilityMatrix(1, "ssku-1", TOMILINO_ID, true, null, null)
                    .setBlockReasonKey(blockReasonKey),
                sskuAvailabilityMatrix(1, "ssku-2", TOMILINO_ID, true, null, null)
                    .setBlockReasonKey(blockReasonKey),
                sskuAvailabilityMatrix(2, "ssku-1", TOMILINO_ID, true, null, null)
                    .setBlockReasonKey(blockReasonKey)
            );
    }

    @Test
    public void testAsyncBlockDeadstockOffersWithFilter() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(1, "ssku-2", DELISTED);
        insertOffer(2, "ssku-1", INACTIVE);

        deadstockStatusRepository.save(
            deadstockStatus(1, "ssku-2", ROSTOV_ID, "21-01-2020"),
            deadstockStatus(1, "ssku-2", SOFINO_ID, "22-01-2020"),
            deadstockStatus(2, "ssku-1", TOMILINO_ID, "23-01-2020")
        );

        // Разблокируем все дедсток офферы в Томилино
        availabilityController.saveAsync(new ShopSkuAvailabilityUpdateRequest()
            .setByFilter(
                new ShopSkuAvailabilityUpdateRequest.ShopSkuAvailabilityRequestByFilter()
                    .setFilter(new ShopSkuAvailabilityWebFilter()
                        .setIsDeadstock(true)
                    )
                    .setAvailabilityByWarehouse(List.of(
                            new ShopSkuAvailabilityUpdateRequest.ShopSkuWarehouseAvailability()
                                .setWarehouseId(TOMILINO_ID)
                                .setAvailable(ShopSkuAvailabilityValue.AVAILABLE)
                        )
                    )
            )
            .setForceUpdate(true)
        );

        assertThat(sskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorIgnoringFields("id", "createdAt", "modifiedAt")
            .containsExactlyInAnyOrder(
                sskuAvailabilityMatrix(1, "ssku-2", TOMILINO_ID, true, null, null),
                sskuAvailabilityMatrix(2, "ssku-1", TOMILINO_ID, true, null, null)
            );
    }

    @Test
    public void testWarningOnUnblockDeadstock() {
        deadstockStatusRepository.save(
            deadstockStatus(1, "ssku-1", ROSTOV_ID, "21-01-2020")
        );

        sskuAvailabilityMatrixRepository.save(
            sskuAvailabilityMatrix(1, "ssku-1", ROSTOV_ID, false, "21-01-2020", null)
        );

        Assertions.assertThatThrownBy(() -> {
                availabilityController.save(List.of(
                    toSave(1, "ssku-1", ROSTOV_ID, ShopSkuAvailabilityValue.AVAILABLE, null, null)
                ), false);
            }).isInstanceOf(WarningException.class)
            .hasMessageContaining("(1, ssku-1, 147)");
    }

    @Test
    public void testAsyncWarningOnUnblockDeadstock() {
        insertOffer(1, "ssku-1", ACTIVE);

        deadstockStatusRepository.save(
            deadstockStatus(1, "ssku-1", ROSTOV_ID, "21-01-2020")
        );

        sskuAvailabilityMatrixRepository.save(
            sskuAvailabilityMatrix(1, "ssku-1", ROSTOV_ID, false, null, null)
        );

        int actionId = availabilityController.saveAsync(new ShopSkuAvailabilityUpdateRequest()
            .setByFilter(
                new ShopSkuAvailabilityUpdateRequest.ShopSkuAvailabilityRequestByFilter()
                    .setFilter(new ShopSkuAvailabilityWebFilter())
                    .setAvailabilityByWarehouse(List.of(
                            new ShopSkuAvailabilityUpdateRequest.ShopSkuWarehouseAvailability()
                                .setWarehouseId(ROSTOV_ID)
                                .setAvailable(ShopSkuAvailabilityValue.NOT_SET)
                        )
                    )
            )
            .setForceUpdate(false)
        );

        var action = backgroundServiceMock.getAction(actionId);
        assertThat(action).isNotNull();
        assertThat(action.getStatus()).isEqualTo(BackgroundActionStatus.ActionStatus.WARNING);

        var params = (List<ShopSkuAvailabilityController.DeadstockWarning>) action.getParams();

        assertThat(params).hasSize(1);
        assertThat(params.get(0).getKeys())
            .containsExactlyInAnyOrder(new ShopSkuWKey(1, "ssku-1", ROSTOV_ID));
    }

    @Test
    public void testNoWarningIfAlreadyNotBlocked() {
        deadstockStatusRepository.save(
            deadstockStatus(1, "ssku-1", ROSTOV_ID, "21-01-2020")
        );

        availabilityController.save(List.of(
            toSave(1, "ssku-1", ROSTOV_ID, ShopSkuAvailabilityValue.AVAILABLE, null, null)
        ), false);

        // no exception
    }

    @Test
    public void testAuditInfo() {
        availabilityController.save(List.of(
            toSave(1, "ssku-1", ROSTOV_ID, ShopSkuAvailabilityValue.BLOCKED, "20-01-2021", "20-03-2021")
        ), false);

        var created = sskuAvailabilityMatrixRepository.findAll().get(0);
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getCreatedLogin()).isEqualTo(DEEPMIND_APP_TEST_USER);
        assertThat(created.getModifiedAt()).isNotNull();
        assertThat(created.getModifiedLogin()).isNull();

        SecurityContextAuthenticationUtils.setAuthenticationToken("unit_test_222");
        availabilityController.save(List.of(
            toSave(1, "ssku-1", ROSTOV_ID, ShopSkuAvailabilityValue.BLOCKED, "20-01-2021", "25-03-2021")
        ), false);

        var updated = sskuAvailabilityMatrixRepository.findAll().get(0);
        assertThat(updated.getCreatedAt()).isNotNull();
        assertThat(updated.getCreatedLogin()).isEqualTo(DEEPMIND_APP_TEST_USER);
        assertThat(updated.getModifiedAt()).isNotNull();
        assertThat(updated.getModifiedLogin()).isEqualTo("unit_test_222");
    }

    @Test
    public void exportEmptyExcel() {
        int id = availabilityController.exportToExcelAsync(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setShopSkuSearchText("1212121"))
        );

        ExcelFile excelFile = excelFileDownloader.downloadExport(id);
        DeepmindAssertions.assertThat(excelFile).isEmpty();
    }

    @Test
    public void exportSimpleExcel() {
        deepmindMskuRepository.save(msku(111222L));
        deepmindMskuRepository.save(msku(333444L));
        var mskuInfo1 = mskuInfo(111222L).setPricebandId(111L).setPricebandLabel("123-345");
        var mskuInfo2 = mskuInfo(333444L).setInTargetAssortment(true);
        mskuInfoRepository.save(mskuInfo1, mskuInfo2);
        insertOffer(1, "ssku-11", ACTIVE, mskuInfo1.getMarketSkuId());
        insertOffer(1, "ssku-12", DELISTED, mskuInfo2.getMarketSkuId());
        insertOffer(2, "ssku-21", INACTIVE);
        insertOffer(3, "ssku-31", ACTIVE);
        insertOffer(3, "ssku-41", ACTIVE);

        insertHiding(1, "ssku-11", "SKK_45k", "брак", "Иван");

        deadstockStatusRepository.save(
            deadstockStatus(1, "ssku-11", ROSTOV_ID, "21-01-2020")
        );

        almostDeadstockStatusRepository.save(
            almostDeadstockStatus(3, "ssku-41", ROSTOV_ID, "21-01-2020")
        );

        sskuStatusRepository.save(new SskuStatus()
            .setSupplierId(2)
            .setShopSku("ssku-21")
            .setAvailability(INACTIVE)
            .setStatusFinishAt(Instant.parse("2021-12-03T10:15:30.00Z"))
            .setModifiedByUser(true)
            .setModifiedAt(sskuStatusRepository.findByKey(2, "ssku-21").orElseThrow().getModifiedAt())
        );

        availabilityController.save(List.of(
            toSave(2, "ssku-21", SOFINO_ID, ShopSkuAvailabilityValue.BLOCKED, null, null),
            toSave(2, "ssku-21", TOMILINO_ID, ShopSkuAvailabilityValue.AVAILABLE, "20-01-2021", null)
        ), false);

        // save inherited
        shopSkuMatrixAvailabilityService.addAvailability(3, "ssku-31", 111,
            TOMILINO_ID, MatrixAvailabilityUtils.mskuArchived(msku(111L)));
        shopSkuMatrixAvailabilityService.addAvailability(3, "ssku-31", 111,
            SOFINO_ID, MatrixAvailabilityUtils.offerDelisted(3, "ssku-31"));

        int actionId = availabilityController.exportToExcelAsync(ShopSkuAvailabilityRequest.all());
        ExcelFile excelFile = excelFileDownloader.downloadExport(actionId);

        DeepmindAssertions.assertThat(excelFile)
            // 1
            .containsValue(1, BaseBackgroundExportable.SUPPLIER_ID_KEY, 1)
            .containsValue(1, BaseBackgroundExportable.SSKU_KEY, "ssku-11")
            // availability
            .containsValue(1, ExcelUtils.convertWarehouseToExcelHeader(marshrut), AVAILABLE_INHERITED)
            .containsValue(1, ExcelUtils.convertWarehouseToExcelHeader(sofino), AVAILABLE_INHERITED)
            .containsValue(1, ExcelUtils.convertWarehouseToExcelHeader(tomilino), AVAILABLE_INHERITED)
            .containsValue(1, ExcelUtils.convertWarehouseToExcelHeader(rostov), AVAILABLE_INHERITED)
            // deadstock
            .containsValue(1, ShopSkuAvailabilityExportable.getDeadstockWarehouseHeader(rostov), "Дедсток с " +
                "2020-01-21")
            .doesntContainValue(1, ShopSkuAvailabilityExportable.getDeadstockWarehouseHeader(tomilino))
            .containsValue(1, BaseBackgroundExportable.IN_TARGET_ASSORTMENT, "false")
            .containsValue(1, BaseBackgroundExportable.PRICEBAND_ID, "111")
            .containsValue(1, BaseBackgroundExportable.PRICEBAND_LABEL, "123-345")
            // 2
            .containsValue(2, BaseBackgroundExportable.SUPPLIER_ID_KEY, 1)
            .containsValue(2, BaseBackgroundExportable.SSKU_KEY, "ssku-12")
            .containsValue(2, BaseBackgroundExportable.IN_TARGET_ASSORTMENT, "true")
            // 3
            .containsValue(3, BaseBackgroundExportable.SUPPLIER_ID_KEY, 2)
            .containsValue(3, BaseBackgroundExportable.SSKU_KEY, "ssku-21")
            .containsValue(3, BaseBackgroundExportable.SSKU_STATUS_KEY, "INACTIVE")
            .containsValue(3, BaseBackgroundExportable.SSKU_STATUS_FINISH_TIME_KEY,
                BaseBackgroundExportable.format(Instant.parse("2021-12-03T10:15:30.00Z")))
            .containsValue(3, BaseBackgroundExportable.IN_TARGET_ASSORTMENT, "false")
            // availability
            .containsValue(3, ExcelUtils.convertWarehouseToExcelHeader(marshrut), AVAILABLE_INHERITED)
            .containsValue(3, ExcelUtils.convertWarehouseToExcelHeader(sofino), NOT_AVAILABLE)
            .containsValue(3, ExcelUtils.convertWarehouseToExcelHeader(tomilino),
                AVAILABLE + " с 20.01.2021")
            .containsValue(3, ExcelUtils.convertWarehouseToExcelHeader(rostov), AVAILABLE_INHERITED)
            // 4
            .containsValue(4, BaseBackgroundExportable.SUPPLIER_ID_KEY, 3)
            .containsValue(4, BaseBackgroundExportable.SSKU_KEY, "ssku-31")
            // availability
            .containsValue(4, ExcelUtils.convertWarehouseToExcelHeader(marshrut), AVAILABLE_INHERITED)
            // Хоть и на софьино стоит блокировка по delisted, она не учитывается ни в UI, ни в excel
            .containsValue(4, ExcelUtils.convertWarehouseToExcelHeader(sofino), AVAILABLE_INHERITED)
            .containsValue(4, ExcelUtils.convertWarehouseToExcelHeader(tomilino),
                NOT_AVAILABLE_INHERITED)
            .containsValue(4, ExcelUtils.convertWarehouseToExcelHeader(rostov), AVAILABLE_INHERITED)
            // 5
            // almost deadstock
            .containsValue(5, ShopSkuAvailabilityExportable.getAlmostDeadstockWarehouseHeader(rostov),
                "Почти Дедсток с 2020-01-21")
            .hasLastLine(5);
    }

    @Test
    public void exportSskuStatusesEmptyExcel() {
        int id = statusController.exportAsync(new ShopSkuAvailabilityRequest()
            .setLastKeyFilter(new ShopSkuKeyLastFilter())
            .setWebFilter(new ShopSkuAvailabilityWebFilter().setShopSkuSearchText("1212121"))
        );

        ExcelFile excelFile = excelFileDownloader.downloadExport(id);
        DeepmindAssertions.assertThat(excelFile).isEmpty();
    }

    @Test
    public void exportSskuStatusesSimpleExcel() {
        insertOffer(1, "ssku-11", ACTIVE);
        insertOffer(1, "ssku-12", DELISTED);
        insertOffer(2, "ssku-21", INACTIVE);
        insertOffer(3, "ssku-31", ACTIVE);

        // update status finish at
        sskuStatusRepository.save(sskuStatusRepository.findByKey(2, "ssku-21").orElseThrow()
            .setStatusFinishAt(Instant.parse("2007-12-03T10:15:30.00Z"))
        );

        var supplier = deepmindSupplierRepository.findById(3).orElseThrow()
            .setSupplierType(REAL_SUPPLIER)
            .setRealSupplierId("000040");
        deepmindSupplierRepository.save(supplier);

        int actionId = statusController.exportAsync(ShopSkuAvailabilityRequest.all());
        ExcelFile excelFile = excelFileDownloader.downloadExport(actionId);

        DeepmindAssertions.assertThat(excelFile)
            // 1
            .containsValue(1, BaseBackgroundExportable.SUPPLIER_ID_KEY, 1)
            .containsValue(1, BaseBackgroundExportable.SSKU_KEY, "ssku-11")
            .containsValue(1, BaseBackgroundExportable.REAL_SSKU_KEY, "ssku-11")
            .containsValue(1, BaseBackgroundExportable.SSKU_STATUS_KEY, ACTIVE)
            .containsValue(1, BaseBackgroundExportable.SSKU_STATUS_FINISH_TIME_KEY, null)
            // 2
            .containsValue(2, BaseBackgroundExportable.SUPPLIER_ID_KEY, 1)
            .containsValue(2, BaseBackgroundExportable.SSKU_KEY, "ssku-12")
            .containsValue(2, BaseBackgroundExportable.REAL_SSKU_KEY, "ssku-12")
            .containsValue(2, BaseBackgroundExportable.SSKU_STATUS_KEY, DELISTED)
            .containsValue(2, BaseBackgroundExportable.SSKU_STATUS_FINISH_TIME_KEY, null)
            // 3
            .containsValue(3, BaseBackgroundExportable.SUPPLIER_ID_KEY, 2)
            .containsValue(3, BaseBackgroundExportable.SSKU_KEY, "ssku-21")
            .containsValue(3, BaseBackgroundExportable.REAL_SSKU_KEY, "ssku-21")
            .containsValue(3, BaseBackgroundExportable.SSKU_STATUS_KEY, INACTIVE)
            .containsValue(3, BaseBackgroundExportable.SSKU_STATUS_FINISH_TIME_KEY)
            // 4
            .containsValue(4, BaseBackgroundExportable.SUPPLIER_ID_KEY, 3)
            .containsValue(4, BaseBackgroundExportable.SSKU_KEY, "ssku-31")
            .containsValue(4, BaseBackgroundExportable.REAL_SSKU_KEY, "000040.ssku-31")
            .containsValue(4, BaseBackgroundExportable.SSKU_STATUS_KEY, ACTIVE)
            .containsValue(4, BaseBackgroundExportable.SSKU_STATUS_FINISH_TIME_KEY, null)
            .hasLastLine(4);
    }

    @Test
    public void testSaveComment() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(2, "ssku-2", ACTIVE);
        insertOffer(3, "ssku-3", ACTIVE);
        insertOffer(4, "ssku-4", ACTIVE);

        sskuAvailabilityMatrixRepository.save(
            sskuAvailabilityMatrix(1, "ssku-1", ROSTOV_ID, false, null, null)
                .setComment("Will be modified"),
            sskuAvailabilityMatrix(2, "ssku-2", ROSTOV_ID, false, null, null),
            sskuAvailabilityMatrix(3, "ssku-3", ROSTOV_ID, false, null, null)
                .setComment("Will be deleted comment")

        );

        availabilityController.save(List.of(
            toSave(1, "ssku-1", ROSTOV_ID, ShopSkuAvailabilityValue.BLOCKED, null, null)
                .setComment("#1 Modified comment"),
            toSave(2, "ssku-2", ROSTOV_ID, ShopSkuAvailabilityValue.BLOCKED, null, null)
                .setComment("#2 Created comment"),
            toSave(3, "ssku-3", ROSTOV_ID, ShopSkuAvailabilityValue.BLOCKED, null, null),
            toSave(4, "ssku-4", ROSTOV_ID, ShopSkuAvailabilityValue.BLOCKED, null, null)
                .setComment("#3 New availability comment")
        ), false);

        var result = availabilityController.list(ShopSkuAvailabilityRequest.all()).stream().collect(Collectors.toMap(
            DisplayShopSkuAvailability::getSupplierId,
            it -> {
                String comment = it.getAvailabilities().get(0).getComment();
                return comment == null ? "" : comment;
            }
        ));

        assertThat(result)
            .containsOnly(
                Map.entry(1, "#1 Modified comment"),
                Map.entry(2, "#2 Created comment"),
                Map.entry(3, ""),
                Map.entry(4, "#3 New availability comment")
            );
    }

    @Test
    public void testCheckBeforeUpdateByFilter() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(2, "ssku-2", ACTIVE);

        deadstockStatusRepository.save(
            deadstockStatus(1, "ssku-1", ROSTOV_ID, "21-01-2020")
        );

        sskuAvailabilityMatrixRepository.save(
            sskuAvailabilityMatrix(1, "ssku-1", ROSTOV_ID, false, "21-01-2020", null),
            sskuAvailabilityMatrix(2, "ssku-2", TOMILINO_ID, false, "21-01-2020", null)
        );

        var result = availabilityController.checkBeforeSave(new ShopSkuAvailabilityUpdateRequest()
            .setByFilter(
                new ShopSkuAvailabilityUpdateRequest.ShopSkuAvailabilityRequestByFilter()
                    .setFilter(new ShopSkuAvailabilityWebFilter())
                    .setAvailabilityByWarehouse(List.of(
                        new ShopSkuAvailabilityUpdateRequest.ShopSkuWarehouseAvailability()
                            .setWarehouseId(ROSTOV_ID)
                            .setAvailable(ShopSkuAvailabilityValue.NOT_SET),
                        new ShopSkuAvailabilityUpdateRequest.ShopSkuWarehouseAvailability()
                            .setWarehouseId(TOMILINO_ID)
                            .setAvailable(ShopSkuAvailabilityValue.NOT_SET)
                    ))
            )
        );

        assertThat(result).isEqualTo(
            ShopSkuAvailabilityController.BeforeUpdateCheckResult.warning(
                List.of(new ShopSkuWKey(1, "ssku-1", ROSTOV_ID))
            )
        );
    }

    @Test
    public void testCheckBeforeUpdateOK() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(2, "ssku-2", ACTIVE);

        deadstockStatusRepository.save(
            deadstockStatus(1, "ssku-1", ROSTOV_ID, "21-01-2020")
        );

        sskuAvailabilityMatrixRepository.save(
            sskuAvailabilityMatrix(1, "ssku-1", ROSTOV_ID, null, "21-01-2020", null),
            sskuAvailabilityMatrix(1, "ssku-1", TOMILINO_ID, false, "21-01-2020", null),
            sskuAvailabilityMatrix(2, "ssku-2", TOMILINO_ID, false, "21-01-2020", null)
        );

        var result = availabilityController.checkBeforeSave(new ShopSkuAvailabilityUpdateRequest()
            .setByFilter(
                new ShopSkuAvailabilityUpdateRequest.ShopSkuAvailabilityRequestByFilter()
                    .setFilter(new ShopSkuAvailabilityWebFilter())
                    .setAvailabilityByWarehouse(List.of(
                        new ShopSkuAvailabilityUpdateRequest.ShopSkuWarehouseAvailability()
                            .setWarehouseId(ROSTOV_ID)
                            .setAvailable(ShopSkuAvailabilityValue.NOT_SET),
                        new ShopSkuAvailabilityUpdateRequest.ShopSkuWarehouseAvailability()
                            .setWarehouseId(TOMILINO_ID)
                            .setAvailable(ShopSkuAvailabilityValue.NOT_SET)
                    ))
            )
        );

        assertThat(result).isEqualTo(
            ShopSkuAvailabilityController.BeforeUpdateCheckResult.ok()
        );
    }

    @Test
    public void testCheckBeforeUpdateByShopSku() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(2, "ssku-2", ACTIVE);

        deadstockStatusRepository.save(
            deadstockStatus(1, "ssku-1", ROSTOV_ID, "21-01-2020"),
            deadstockStatus(2, "ssku-2", ROSTOV_ID, "21-01-2020")
        );

        sskuAvailabilityMatrixRepository.save(
            sskuAvailabilityMatrix(1, "ssku-1", ROSTOV_ID, false, "21-01-2020", null)
        );

        var result = availabilityController.checkBeforeSave(new ShopSkuAvailabilityUpdateRequest()
            .setByShopSku(List.of(
                toSave(1, "ssku-1", ROSTOV_ID, ShopSkuAvailabilityValue.NOT_SET, null, null)
            ))
        );

        assertThat(result).isEqualTo(
            ShopSkuAvailabilityController.BeforeUpdateCheckResult.warning(
                List.of(new ShopSkuWKey(1, "ssku-1", ROSTOV_ID))
            )
        );
    }

    @Test
    public void testCheckBeforeUpdateByShopSkuWithNoBlock() {
        insertOffer(1, "ssku-1", ACTIVE);
        insertOffer(2, "ssku-2", ACTIVE);

        deadstockStatusRepository.save(
            deadstockStatus(1, "ssku-1", ROSTOV_ID, "21-01-2020"),
            deadstockStatus(2, "ssku-2", ROSTOV_ID, "21-01-2020")
        );

        sskuAvailabilityMatrixRepository.save(
            sskuAvailabilityMatrix(1, "ssku-1", ROSTOV_ID, true, "21-01-2020", null)
        );

        var result = availabilityController.checkBeforeSave(new ShopSkuAvailabilityUpdateRequest()
            .setByShopSku(List.of(
                toSave(1, "ssku-1", ROSTOV_ID, ShopSkuAvailabilityValue.BLOCKED, null, null)
            ))
        );

        assertThat(result).isEqualTo(ShopSkuAvailabilityController.BeforeUpdateCheckResult.ok());
    }

    @Test
    public void testIsFilterByShopSku() {
        ShopSkuAvailabilityWebFilter filter;

        filter = new ShopSkuAvailabilityWebFilter()
            .setShopSkuKeys(List.of(new ServiceOfferKey(1, "sku1")));
        Assertions.assertThat(filter.isSearchOnlyByShopSkuKeys()).isTrue();

        filter = new ShopSkuAvailabilityWebFilter()
            .setShopSkuKeys(List.of(new ServiceOfferKey(1, "sku1")))
            .setSupplierIds(List.of(1));
        Assertions.assertThat(filter.isSearchOnlyByShopSkuKeys()).isFalse();

        filter = new ShopSkuAvailabilityWebFilter()
            .setShopSkuKeys(List.of(new ServiceOfferKey(1, "sku1")))
            .setSupplierIds(List.of());
        assertThat(filter.isSearchOnlyByShopSkuKeys()).isTrue();

        filter = new ShopSkuAvailabilityWebFilter();
        assertThat(filter.isSearchOnlyByShopSkuKeys()).isFalse();
    }

    @Test
    public void testSimplifyRequest() {
        var request = new ShopSkuAvailabilityUpdateRequest().setByFilter(
            new ShopSkuAvailabilityUpdateRequest.ShopSkuAvailabilityRequestByFilter()
                .setFilter(new ShopSkuAvailabilityWebFilter()
                    .setShopSkuKeys(List.of(
                        new ServiceOfferKey(1, "sku1"),
                        new ServiceOfferKey(2, "sku2"),
                        new ServiceOfferKey(3, "sku3")
                    ))
                )
                .setAvailabilityByWarehouse(List.of(
                    new ShopSkuAvailabilityUpdateRequest.ShopSkuWarehouseAvailability()
                        .setWarehouseId(ROSTOV_ID)
                        .setAvailable(ShopSkuAvailabilityValue.BLOCKED),
                    new ShopSkuAvailabilityUpdateRequest.ShopSkuWarehouseAvailability()
                        .setWarehouseId(TOMILINO_ID)
                        .setAvailable(ShopSkuAvailabilityValue.AVAILABLE)
                ))
        );
        var result = availabilityController.simplifyUpdateRequest(request);

        assertThat(result.getByShopSku()).containsExactlyInAnyOrder(
            toSave(1, "sku1", ROSTOV_ID, ShopSkuAvailabilityValue.BLOCKED, null, null),
            toSave(2, "sku2", ROSTOV_ID, ShopSkuAvailabilityValue.BLOCKED, null, null),
            toSave(3, "sku3", ROSTOV_ID, ShopSkuAvailabilityValue.BLOCKED, null, null),
            toSave(1, "sku1", TOMILINO_ID, ShopSkuAvailabilityValue.AVAILABLE, null, null),
            toSave(2, "sku2", TOMILINO_ID, ShopSkuAvailabilityValue.AVAILABLE, null, null),
            toSave(3, "sku3", TOMILINO_ID, ShopSkuAvailabilityValue.AVAILABLE, null, null)
        );
        assertThat(result.getByFilter()).isNull();
    }

    @Test
    public void testSkipSimplifyRequest() {
        var request = new ShopSkuAvailabilityUpdateRequest().setByFilter(
            new ShopSkuAvailabilityUpdateRequest.ShopSkuAvailabilityRequestByFilter()
                .setFilter(new ShopSkuAvailabilityWebFilter()
                    .setShopSkuKeys(List.of(
                        new ServiceOfferKey(1, "sku1"),
                        new ServiceOfferKey(2, "sku2"),
                        new ServiceOfferKey(3, "sku3")
                    ))
                    .setCategoryManagerLogin("catman")
                )
        );
        var result = availabilityController.simplifyUpdateRequest(request);

        // ничего не упростили, запрос остался как есть
        assertThat(result).isEqualTo(request);
    }

    @Test
    @DbUnitDataSet(dataSource = "deepmindDataSource", before = "ShopSkuAvailabilityControllerTest.assort_ssku.csv")
    public void testFilterByAssortSsku() {
        deepmindSupplierRepository.save(create1PSupplier(1, "000008"), create1PSupplier(2, "000009"),
            create3pSupplier(3));
        insertOffer(1, "offer-11", ACTIVE);
        insertOffer(1, "offer-12", ACTIVE);
        insertOffer(2, "offer-21", ACTIVE);
        insertOffer(3, "offer-31", ACTIVE);
        insertOffer(3, "offer-32", ACTIVE);
        insertOffer(3, "offer-33", ACTIVE);

        var assortList = availabilityController.list(
            new ShopSkuAvailabilityRequest()
                .setWebFilter(new ShopSkuAvailabilityWebFilter()
                    .setAssortTypes(List.of(AssortType.ASSORT)))
                .setLastKeyFilter(ShopSkuKeyLastFilter.all())
        );
        assertThat(assortList)
            .extracting(DisplayShopSkuAvailability::getShopSkuKey)
            .containsExactly(
                new ServiceOfferKey(1, "offer-12"),
                new ServiceOfferKey(3, "offer-31")
            );

        var subList = availabilityController.list(
            new ShopSkuAvailabilityRequest()
                .setWebFilter(new ShopSkuAvailabilityWebFilter()
                    .setAssortTypes(List.of(AssortType.SUB)))
                .setLastKeyFilter(ShopSkuKeyLastFilter.all())
        );
        assertThat(subList)
            .extracting(DisplayShopSkuAvailability::getShopSkuKey)
            .containsExactly(
                new ServiceOfferKey(1, "offer-11"),
                new ServiceOfferKey(3, "offer-32"),
                new ServiceOfferKey(3, "offer-33")
            );

        var allList = availabilityController.list(
            new ShopSkuAvailabilityRequest()
                .setWebFilter(new ShopSkuAvailabilityWebFilter()
                    .setAssortTypes(List.of(AssortType.ASSORT, AssortType.SUB)))
                .setLastKeyFilter(ShopSkuKeyLastFilter.all())
        );
        assertThat(allList)
            .extracting(DisplayShopSkuAvailability::getShopSkuKey)
            .containsExactly(
                new ServiceOfferKey(1, "offer-11"),
                new ServiceOfferKey(1, "offer-12"),
                new ServiceOfferKey(3, "offer-31"),
                new ServiceOfferKey(3, "offer-32"),
                new ServiceOfferKey(3, "offer-33")
            );
    }
}
