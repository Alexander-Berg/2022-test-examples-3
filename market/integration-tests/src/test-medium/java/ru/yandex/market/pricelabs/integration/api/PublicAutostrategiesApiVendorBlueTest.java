package ru.yandex.market.pricelabs.integration.api;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyDrr;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilter;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilter.TypeEnum;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilterVendor;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyLoad;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyMaxBid;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyModelMaxBid;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySave;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySaveWithId;
import ru.yandex.market.pricelabs.generated.server.pub.model.BusinessNameItem;
import ru.yandex.market.pricelabs.generated.server.pub.model.BusinessNameResponse;
import ru.yandex.market.pricelabs.generated.server.pub.model.Pager;
import ru.yandex.market.pricelabs.model.BrandBusiness;
import ru.yandex.market.pricelabs.model.Business;
import ru.yandex.market.pricelabs.model.ShopsDat;
import ru.yandex.market.pricelabs.model.VendorBrandMap;
import ru.yandex.market.pricelabs.model.VendorModelBid;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;
import ru.yandex.market.pricelabs.model.types.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.VPOS;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTest.checkResponse;
import static ru.yandex.market.pricelabs.tms.processing.autostrategies.AbstractAutostrategiesMetaProcessorTest.autostrategy;

@Slf4j
public class PublicAutostrategiesApiVendorBlueTest extends AbstractAutostrategiesApiTest {

    private static final int VENDOR_ID = 1001;
    private static final int BRAND_ID = 2002;

    private static final int SHOP4 = 465852; // Синий на синем
    private static final int SHOP11 = 11;

    private static final long BUSINESS1 = 1;
    private static final long BUSINESS2 = 2;
    private static final long BUSINESS3 = 3;
    private static final long BUSINESS4 = 465852;
    private static final long BUSINESS11 = 11;

    private static final BusinessNameItem B1 =
            new BusinessNameItem().businessId(BUSINESS1).name("Название 1").active(false).updated(1L);
    private static final BusinessNameItem B2 =
            new BusinessNameItem().businessId(BUSINESS2).name("Название 2").active(true).updated(2L);
    private static final BusinessNameItem B3 =
            new BusinessNameItem().businessId(BUSINESS3).name("Название 3").active(true).updated(3L);
    private static final BusinessNameItem B4 =
            new BusinessNameItem().businessId(BUSINESS4).name("Название X").active(true).updated(4L);
    private static final BusinessNameItem B11 =
            new BusinessNameItem().businessId(BUSINESS11).name("Название 11").active(true).updated(11L);

    private static AutostrategyFilter modelFilter(Integer... models) {
        return new AutostrategyFilter()
                .type(TypeEnum.VENDOR)
                .vendor(new AutostrategyFilterVendor()
                        .models(List.of(models)));
    }

    @BeforeEach
    void init() {
        this.init(AutostrategyTarget.vendorBlue, () -> {
            var now = getInstant();
            testControls.executeInParallel(
                    () -> executors.vendorBrandMap().insert(List.of(
                            new VendorBrandMap(VENDOR_ID, BRAND_ID, now))),
                    () -> executors.shopsDat().insert(List.of(
                            new ShopsDat(SHOP1, "Название 1", "w1.domain1.ru", "RUR", 1, now, false, 1),
                            new ShopsDat(SHOP2, "Название 2", "w2.domain2.ru", "RUR", 1, now, false, 1),
                            new ShopsDat(SHOP3, "Название 3", "w3.domain3.ru", "RUR", 1, now, false, 1),
                            new ShopsDat(SHOP4, "Название X", "domainx.ru", "RUR", 1, now, false, 1),
                            new ShopsDat(SHOP11, "Название 11", "w11.domain11.ru", "RUR", 1, now, false, 1)
                    )),
                    () -> {
                        executors.vendorModelBid().clearTargetTable();
                        executors.vendorModelBid().insert(List.of(
                                new VendorModelBid(MODEL1, 160, now),
                                new VendorModelBid(MODEL2, 150, now),
                                new VendorModelBid(MODEL3, 160, now)
                        ));
                    },
                    () -> executors.business().insert(List.of(
                            new Business(BUSINESS1, "Название 1", Status.ACTIVE, Instant.ofEpochMilli(1)),
                            new Business(BUSINESS2, "Название 2", Status.DELETED, Instant.ofEpochMilli(2)),
                            new Business(BUSINESS3, "Название 3", Status.ACTIVE, Instant.ofEpochMilli(3)),
                            new Business(BUSINESS4, "Название X", Status.DELETED, Instant.ofEpochMilli(4)),
                            new Business(BUSINESS11, "Название 11", Status.ACTIVE, Instant.ofEpochMilli(11))
                    )),
                    () -> executors.brandBusiness().insert(List.of(
                            new BrandBusiness(BRAND_ID, BUSINESS1, Status.DELETED, Instant.ofEpochMilli(1)),
                            new BrandBusiness(BRAND_ID, BUSINESS2, Status.ACTIVE, Instant.ofEpochMilli(2)),
                            new BrandBusiness(BRAND_ID, BUSINESS3, Status.ACTIVE, Instant.ofEpochMilli(3)),
                            new BrandBusiness(BRAND_ID, BUSINESS4, Status.ACTIVE, Instant.ofEpochMilli(4)),
                            new BrandBusiness(BRAND_ID, BUSINESS11, Status.ACTIVE, Instant.ofEpochMilli(11))
                    ))
            );
        });
    }

    @Test
    void testBusinessesAll() {
        assertEquals(new BusinessNameResponse()
                        .pager(new Pager().total(5).pages(1).page(1))
                        .items(List.of(B11, B2, B3, B4, B1)),
                checkResponse(getPublicApi().autostrategiesBusinessesPost(VENDOR_ID, null, null, null, null)));
    }

    @Test
    void testBusinessesNone() {
        assertEquals(new BusinessNameResponse()
                        .pager(new Pager().total(0).pages(0).page(1))
                        .items(List.of()),
                checkResponse(getPublicApi().autostrategiesBusinessesPost(100500, null, null, null, null)));
    }

    @Test
    void testBusinessesPage1() {
        assertEquals(new BusinessNameResponse()
                        .pager(new Pager().total(5).pages(3).page(1))
                        .items(List.of(B11, B2)),
                checkResponse(getPublicApi().autostrategiesBusinessesPost(VENDOR_ID, null, null, 1, 2)));
    }

    @Test
    void testBusinessesPage2() {
        assertEquals(new BusinessNameResponse()
                        .pager(new Pager().total(5).pages(3).page(2))
                        .items(List.of(B3, B4)),
                checkResponse(getPublicApi().autostrategiesBusinessesPost(VENDOR_ID, null, null, 2, 2)));
    }

    @Test
    void testBusinessesPage3() {
        assertEquals(new BusinessNameResponse()
                        .pager(new Pager().total(5).pages(3).page(3))
                        .items(List.of(B1)),
                checkResponse(getPublicApi().autostrategiesBusinessesPost(VENDOR_ID, null, null, 3, 2)));
    }

    @Test
    void testBusinessesPage4() {
        assertEquals(new BusinessNameResponse()
                        .pager(new Pager().total(5).pages(3).page(4))
                        .items(List.of()),
                checkResponse(getPublicApi().autostrategiesBusinessesPost(VENDOR_ID, null, null, 4, 2)));
    }

    @Test
    void testBusinessesPriority() {
        assertEquals(new BusinessNameResponse()
                        .pager(new Pager().total(5).pages(1).page(1))
                        .items(List.of(B3, B1, B11, B2, B4)),
                checkResponse(getPublicApi().autostrategiesBusinessesPost(VENDOR_ID, null, List.of(1, 3), null, null)));
    }

    @Test
    void testBusinessesPriorityPage1() {
        assertEquals(new BusinessNameResponse()
                        .pager(new Pager().total(5).pages(3).page(1))
                        .items(List.of(B3, B1)),
                checkResponse(getPublicApi().autostrategiesBusinessesPost(VENDOR_ID, null, List.of(1, 3), 1, 2)));
    }

    @Test
    void testBusinessesSearchId1Default() {
        assertEquals(new BusinessNameResponse()
                        .pager(new Pager().total(2).pages(1).page(1))
                        .items(List.of(B11, B1)),
                checkResponse(getPublicApi().autostrategiesBusinessesPost(VENDOR_ID, String.valueOf(BUSINESS1),
                        List.of(), 1, 2)));
    }

    @Test
    void testBusinessesSearchId1Sorted() {
        assertEquals(new BusinessNameResponse()
                        .pager(new Pager().total(2).pages(1).page(1))
                        .items(List.of(B1, B11)),
                checkResponse(getPublicApi().autostrategiesBusinessesPost(VENDOR_ID, String.valueOf(BUSINESS1),
                        List.of(3, 1), 1, 2)));
    }

    @Test
    void testBusinessesSearchId4() {
        assertEquals(new BusinessNameResponse()
                        .pager(new Pager().total(1).pages(1).page(1))
                        .items(List.of(B4)),
                checkResponse(getPublicApi().autostrategiesBusinessesPost(VENDOR_ID, String.valueOf(BUSINESS4),
                        List.of(3, 1), 1, 2)));
    }

    @Test
    void testBusinessesSearchName() {
        assertEquals(new BusinessNameResponse()
                        .pager(new Pager().total(2).pages(1).page(1))
                        .items(List.of(B1, B11)),
                checkResponse(getPublicApi().autostrategiesBusinessesPost(VENDOR_ID, "название 1", List.of(3, 1), 1,
                        2)));
    }

    @Test
    void testBusinessesSearchNamePage2() {
        assertEquals(new BusinessNameResponse()
                        .pager(new Pager().total(2).pages(1).page(2))
                        .items(List.of()),
                checkResponse(getPublicApi().autostrategiesBusinessesPost(VENDOR_ID, "название 1", List.of(3, 1), 2,
                        2)));
    }

    @Test
    void testEstimateCpaDrr() {
        assertEquals(new AutostrategyDrr(),
                checkResponse(getPublicApi().autostrategyEstimateCpaDrrPost(SHOP1, null, getTarget(), null, null)));
    }

    @Test
    void testLookupMaxBidEmpty() {
        assertEquals(new AutostrategyMaxBid(),
                checkResponse(getPublicApi().autostrategyLookupMaxBidPost(SHOP1, null, getTarget())));
    }

    @Test
    void testLookupMaxBidWithModels1() {
        assertEquals(new AutostrategyMaxBid().maxBid(160L),
                checkResponse(getPublicApi().autostrategyLookupMaxBidPost(SHOP1, modelFilter(MODEL1), getTarget())));
    }

    @Test
    void testLookupMaxBidWithModels2() {
        assertEquals(new AutostrategyMaxBid().maxBid(150L),
                checkResponse(getPublicApi().autostrategyLookupMaxBidPost(SHOP1, modelFilter(MODEL2), getTarget())));
    }

    @Test
    void testLookupMaxBidWithModels3() {
        assertEquals(new AutostrategyMaxBid().maxBid(160L),
                checkResponse(getPublicApi().autostrategyLookupMaxBidPost(SHOP1, modelFilter(MODEL3), getTarget())));
    }

    @Test
    void testLookupMaxBidWithModelsAll() {
        assertEquals(new AutostrategyMaxBid().maxBid(160L),
                checkResponse(getPublicApi().autostrategyLookupMaxBidPost(SHOP1,
                        modelFilter(MODEL1, MODEL2, MODEL3), getTarget())));
    }

    @Test
    void testLookupMaxBidsWithModelsNone() {
        assertEquals(List.of(),
                checkResponse(getPublicApi().autostrategyLookupMaxBidsPost(SHOP1, modelFilter(), getTarget())));
    }

    @Test
    void testLookupMaxBidsWithModelsAll() {
        assertEquals(List.of(
                        new AutostrategyModelMaxBid().model(MODEL1).maxBid(160L),
                        new AutostrategyModelMaxBid().model(MODEL2).maxBid(150L),
                        new AutostrategyModelMaxBid().model(999),
                        new AutostrategyModelMaxBid().model(MODEL3).maxBid(160L)),
                checkResponse(getPublicApi().autostrategyLookupMaxBidsPost(SHOP1,
                        modelFilter(MODEL1, MODEL2, 999, MODEL3), getTarget())));
    }

    @Test
    public void testNoDuplicatesCreatedForSameVendorSettings() {
        String target = AutostrategyTarget.vendorBlue.name();

        AutostrategyLoadSave createdAutostrategy = createAutostrategy(target, null, null);
        Integer createdId = createdAutostrategy.getLoad().getId();

        // вторая попытка с идентичными настройками
        var secondTryAuto = createdAutostrategy.getSave();

        var rets = checkResponse(
                publicApi.autostrategyBatchPost(
                        SHOP1,
                        List.of(new AutostrategySaveWithId().autostrategy(secondTryAuto)),
                        target,
                        null,
                        null
                )
        );

        log.info("Second attempt Autostrategy: {}", rets);

        assertEquals(1, rets.size());
        assertEquals(createdId, rets.get(0).getId());
    }

    @Test
    public void testUpdateVendorAutostrategiesForSameSettings() {
        String target = AutostrategyTarget.vendorBlue.name();
        List<Long> businesses = List.of(10L, 20L);

        AutostrategyLoadSave createdAutostrategy = createAutostrategy(target, 410L, false);
        Integer createdId = createdAutostrategy.getLoad().getId();

        // вторая попытка с новыми настройками, но той же моделью
        var secondTryAuto = autostrategy("17170_1730754948", TypeEnum.VENDOR, VPOS);
        AutostrategySave original = createdAutostrategy.getSave();
        secondTryAuto.enabled(false);
        secondTryAuto.getFilter().getVendor().setBusinesses(businesses);
        secondTryAuto.getFilter().getVendor().setShops(null);
        secondTryAuto.getFilter().getVendor().setModels(original.getFilter().getVendor().getModels());
        secondTryAuto.getSettings().getVpos().setPosition(2);
        secondTryAuto.getSettings().getVpos().setMaxBid(200L);

        var rets = checkResponse(
                publicApi.autostrategyBatchPost(
                        SHOP1,
                        List.of(new AutostrategySaveWithId().autostrategy(secondTryAuto)),
                        target,
                        492L,
                        false
                )
        );

        log.info("Second attempt Autostrategy: {}", rets);

        assertEquals(1, rets.size());

        // в КВ вернулись обновленные данные
        AutostrategyLoad updatedAutostrategy = rets.get(0);
        assertEquals(createdId, updatedAutostrategy.getId());
        assertFalse(updatedAutostrategy.getEnabled());
        assertEquals(492L, updatedAutostrategy.getUid());
        assertEquals(2, updatedAutostrategy.getSettings().getVpos().getPosition());
        assertEquals(200L, updatedAutostrategy.getSettings().getVpos().getMaxBid());
        assertEquals(businesses, updatedAutostrategy.getFilter().getVendor().getBusinesses());
        assertEquals(List.of(), updatedAutostrategy.getFilter().getVendor().getShops());

        // в бд лежат обновления
        ResponseEntity<AutostrategyLoad> response = publicApi.autostrategyGet(SHOP1, updatedAutostrategy.getId(),
                target);
        assertEquals(updatedAutostrategy, checkResponse(response));
    }

    private AutostrategyLoadSave createAutostrategy(String target, Long uid, Boolean lastOrder) {
        var auto0 = autostrategy("17170_1730754948", TypeEnum.VENDOR, VPOS);

        // настройки согласно https://st.yandex-team.ru/PL-4577#60e88b90ce2f5f72da4e3374
        AutostrategyFilterVendor vendorFilter = auto0.getFilter().getVendor();
        List<Integer> shops = List.of(7, 8);
        vendorFilter.setBusinesses(null);
        vendorFilter.setShops(shops);
        vendorFilter.setModels(List.of(5));
        auto0.setEnabled(true);

        var rets = checkResponse(
                publicApi.autostrategyBatchPost(
                        SHOP1,
                        List.of(new AutostrategySaveWithId().autostrategy(auto0)),
                        target,
                        uid,
                        lastOrder
                )
        );

        log.info("First attempt: Autostrategy: {}", rets);

        assertEquals(1, rets.size());
        AutostrategyLoad created = rets.get(0);
        assertTrue(created.getEnabled());
        assertEquals(1, created.getSettings().getVpos().getPosition());
        assertEquals(400L, created.getSettings().getVpos().getMaxBid());
        assertEquals(List.of(), created.getFilter().getVendor().getBusinesses());
        assertEquals(shops, created.getFilter().getVendor().getShops());
        if (uid != null) {
            assertEquals(uid, created.getUid());
        }

        var ret0 = rets.get(0);
        assertEquals(ret0, checkResponse(publicApi.autostrategyGet(SHOP1, ret0.getId(), target)));

        return new AutostrategyLoadSave(ret0, auto0);
    }

    @Getter
    @AllArgsConstructor
    private static class AutostrategyLoadSave {
        private final AutostrategyLoad load;
        private final AutostrategySave save;
    }
}
