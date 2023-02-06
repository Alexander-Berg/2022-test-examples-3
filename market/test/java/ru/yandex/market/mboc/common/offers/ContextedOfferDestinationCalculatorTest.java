package ru.yandex.market.mboc.common.offers;

import java.time.LocalDate;
import java.util.Objects;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ContextedOfferDestinationCalculatorTest extends BaseDbTestClass {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private SupplierRepository supplierRepository;

    private ContextedOfferDestinationCalculator calculator;

    private Supplier supplier;
    private LocalDate sinceDate = LocalDate.of(2022, 5, 5);

    @Before
    public void setup() {
        supplierRepository.insert(OfferTestUtils.simpleSupplier().setType(MbocSupplierType.DSBS));
        categoryRepository.insert(OfferTestUtils.defaultCategory());
        categoryInfoRepository.insert(OfferTestUtils.categoryInfoWithManualAcceptance().addTag(CategoryInfo.CategoryTag.FASHION));
        calculator = new ContextedOfferDestinationCalculator(categoryInfoCache, storageKeyValueService);
        supplier = supplierRepository.findById(OfferTestUtils.TEST_SUPPLIER_ID);
        storageKeyValueService.putValue(
            ContextedOfferDestinationCalculator.ENABLED_TAGS,
            "{\"FASHION\":\"" + sinceDate + "\",\"MEDICINE\":\"" + sinceDate + "\"}"
        );
        storageKeyValueService.invalidateCache();
    }

    @Test
    public void calculateTest() {
        testCalculationWithTag(CategoryInfo.CategoryTag.FASHION, true);
        testCalculationWithTag(CategoryInfo.CategoryTag.MEDICINE, true);
        // непротэганная категория
        testCalculationWithTag(CategoryInfo.CategoryTag.JEWELRY, false);
    }

    private void testCalculationWithTag(CategoryInfo.CategoryTag tag, boolean shouldBeValid) {
        categoryInfoRepository.deleteAll();
        categoryInfoRepository.insert(OfferTestUtils.categoryInfoWithManualAcceptance().addTag(tag));

        // дата невалидная, но грузим с ассортиментным статусом
        var offer = OfferTestUtils.simpleOkOffer(supplier)
            .setDsbsAssortmentStatus(Offer.DsbsAssortmentStatus.ACTIVE)
            .setCreated(sinceDate.minusDays(2).atStartOfDay())
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED);
        Assertions.assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.DSBS);

        offer.addNewServiceOfferIfNotExists(calculator, supplier);
        assertTrue(Objects.equals(offer.getOfferDestination(), Offer.MappingDestination.BLUE) || !shouldBeValid);

        // дата невалидная и без ассортиментного статуса
        offer = OfferTestUtils.simpleOkOffer(supplier)
            .setCreated(sinceDate.minusDays(2).atStartOfDay())
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED);
        Assertions.assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.DSBS);

        offer.addNewServiceOfferIfNotExists(calculator, supplier);
        Assertions.assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.DSBS);

        // дата валидная и с ассортиментным статусом
        offer = OfferTestUtils.simpleOkOffer(supplier)
            .setDsbsAssortmentStatus(Offer.DsbsAssortmentStatus.ACTIVE)
            .setCreated(sinceDate.plusDays(2).atStartOfDay())
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED);
        Assertions.assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.DSBS);

        offer.addNewServiceOfferIfNotExists(calculator, supplier);
        assertTrue(Objects.equals(offer.getOfferDestination(), Offer.MappingDestination.BLUE) || !shouldBeValid);

        // дата валидная и без ассортиментного статуса
        offer = OfferTestUtils.simpleOkOffer(supplier)
            .setCreated(sinceDate.plusDays(2).atStartOfDay())
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED);
        Assertions.assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.DSBS);

        offer.addNewServiceOfferIfNotExists(calculator, supplier);
        assertTrue(Objects.equals(offer.getOfferDestination(), Offer.MappingDestination.BLUE) || !shouldBeValid);

        // БУ оффер
        offer = OfferTestUtils.simpleOkOffer(supplier)
            .setDsbsAssortmentStatus(Offer.DsbsAssortmentStatus.ACTIVE)
            .setCreated(sinceDate.plusDays(2).atStartOfDay())
            .setCategoryIdForTests(OfferTestUtils.TEST_CATEGORY_INFO_ID, Offer.BindingKind.APPROVED)
            .setResale(true);
        Assertions.assertThat(offer.getOfferDestination()).isEqualTo(Offer.MappingDestination.DSBS);

        offer.addNewServiceOfferIfNotExists(calculator, supplier);
        assertEquals(offer.getOfferDestination(), Offer.MappingDestination.BLUE);
    }
}
