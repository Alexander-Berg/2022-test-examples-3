package ru.yandex.market.mboc.processing.assignment;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.SettingsForOfferProcessingInTolokaType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.SettingsForOfferProcessingInToloka;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.services.category.models.Category;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.processing.BaseOfferProcessingTest;

import static org.assertj.core.api.Assertions.assertThat;


public class TolokaHidingSettingsTest extends BaseOfferProcessingTest {
    @Autowired
    private SupplierRepository supplierRepository;
    private Supplier supplier;

    @Autowired
    private CategoryRepository categoryRepository;
    private CategoryInfo categoryInfo;

    @Autowired
    private SettingsForOfferProcessingInTolokaRepository settingsRepository;

    private TolokaHidingSettings tolokaHidingSettings;

    private Offer offer;

    @Before
    public void setUp() throws Exception {
        supplier = OfferTestUtils.simpleSupplier();
        supplierRepository.insert(supplier);

        Category category = OfferTestUtils.defaultCategory();
        categoryRepository.insert(category);
        Category root = OfferTestUtils.defaultCategory().setCategoryId(90401L);
        categoryRepository.insert(root);

        categoryInfo = OfferTestUtils.categoryInfoWithManualAcceptance().setHideFromToloka(false);
        categoryInfoRepository.insert(categoryInfo);

        tolokaHidingSettings = new TolokaHidingSettings(categoryInfoRepository, supplierRepository, settingsRepository);

        offer = OfferTestUtils.simpleOkOffer(supplier)
            .setCategoryIdForTests(categoryInfo.getCategoryId(), Offer.BindingKind.APPROVED);
    }

    @Test
    public void dontHideIfNoFlagsOrDisablesSpecified() {
        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isFalse();

        supplier.setType(MbocSupplierType.DSBS);
        supplierRepository.update(supplier);
        offer = OfferTestUtils.simpleOkOffer(supplier);
        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isFalse();

        supplier.setType(MbocSupplierType.FIRST_PARTY);
        supplierRepository.update(supplier);
        offer = OfferTestUtils.simpleOkOffer(supplier);
        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isFalse();

        supplier.setType(MbocSupplierType.REAL_SUPPLIER);
        supplier.setRealSupplierId("real-supplier-id");
        supplierRepository.update(supplier);
        offer = OfferTestUtils.simpleOkOffer(supplier);
        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isFalse();

        var psku = new Offer.Mapping(1L, LocalDateTime.now(), Offer.SkuType.PARTNER20);
        offer.setSupplierSkuMapping(psku);
        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isFalse();

        var msku = new Offer.Mapping(1L, LocalDateTime.now(), Offer.SkuType.MARKET);
        offer.setSupplierSkuMapping(msku);
        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isFalse();
    }

    @Test
    public void hideByOffer() {
        offer.setHideFromToloka(true);

        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isTrue();
    }

    @Test
    public void hideBySupplier() {
        supplier.setHideFromToloka(true);
        supplierRepository.update(supplier);

        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isTrue();
    }

    @Test
    public void hideByCategory() {
        categoryInfo.setHideFromToloka(true);
        categoryInfoRepository.update(categoryInfo);

        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isTrue();
    }

    @Test
    public void nullCategory() {
        offer.setCategoryIdForTests(null, Offer.BindingKind.SUPPLIER);

        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isFalse();
    }

    @Test
    public void hideByDSBSDisabled() {
        settingsRepository.updateSettings(List.of(
            new SettingsForOfferProcessingInToloka(SettingsForOfferProcessingInTolokaType.DSBS, false)));

        supplier.setType(MbocSupplierType.DSBS);
        supplierRepository.update(supplier);
        offer = OfferTestUtils.simpleOkOffer(supplier);

        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isTrue();
    }

    @Test
    public void hideByNotDSBSDisabled() {
        settingsRepository.updateSettings(List.of(
            new SettingsForOfferProcessingInToloka(SettingsForOfferProcessingInTolokaType.NOT_DSBS, false)));

        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isTrue();
    }

    @Test
    public void hideByMSKUDisabled() {
        settingsRepository.updateSettings(List.of(
            new SettingsForOfferProcessingInToloka(SettingsForOfferProcessingInTolokaType.MSKU, false)));

        // no mapping present, no explicit hide_from_toloka set
        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isFalse();

        var msku = new Offer.Mapping(1L, LocalDateTime.now(), Offer.SkuType.MARKET);
        offer.setSupplierSkuMapping(msku);
        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isTrue();

        offer.setSupplierSkuMapping(null);
        offer.setSuggestSkuMapping(msku);
        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isTrue();
    }

    @Test
    public void hideByPSKUDisabled() {
        settingsRepository.updateSettings(List.of(
            new SettingsForOfferProcessingInToloka(SettingsForOfferProcessingInTolokaType.PSKU, false)));

        // no mapping present, no explicit hide_from_toloka set
        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isFalse();

        var psku = new Offer.Mapping(1L, LocalDateTime.now(), Offer.SkuType.PARTNER20);
        offer.setSupplierSkuMapping(psku);
        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isTrue();

        offer.setSupplierSkuMapping(null);
        offer.setSuggestSkuMapping(psku);
        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isTrue();
    }

    @Test
    public void hideByFirstPartyDisabled() {
        settingsRepository.updateSettings(List.of(
            new SettingsForOfferProcessingInToloka(SettingsForOfferProcessingInTolokaType.FIRST_PARTY, false)));

        supplier.setType(MbocSupplierType.FIRST_PARTY);
        supplierRepository.update(supplier);
        offer = OfferTestUtils.simpleOkOffer(supplier);

        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isTrue();

        supplier.setType(MbocSupplierType.REAL_SUPPLIER);
        supplier.setRealSupplierId("real-supplier-id");
        supplierRepository.update(supplier);
        offer = OfferTestUtils.simpleOkOffer(supplier);

        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isTrue();
    }

    @Test
    public void hideByThirdPartyDisabled() {
        settingsRepository.updateSettings(List.of(
            new SettingsForOfferProcessingInToloka(SettingsForOfferProcessingInTolokaType.THIRD_PARTY, false)));

        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isTrue();
    }

    @Test
    public void hideByThirdPartyAndFirstPartyDisabledButDSBSOffer() {
        settingsRepository.updateSettings(List.of(
            new SettingsForOfferProcessingInToloka(SettingsForOfferProcessingInTolokaType.THIRD_PARTY, false),
            new SettingsForOfferProcessingInToloka(SettingsForOfferProcessingInTolokaType.FIRST_PARTY, false)
        ));

        supplier.setType(MbocSupplierType.DSBS);
        supplierRepository.update(supplier);
        offer = OfferTestUtils.simpleOkOffer(supplier);

        assertThat(tolokaHidingSettings.shouldHideFromToloka(offer)).isFalse();
    }
}
