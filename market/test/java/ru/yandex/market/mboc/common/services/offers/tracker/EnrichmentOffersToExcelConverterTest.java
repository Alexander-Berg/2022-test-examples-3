package ru.yandex.market.mboc.common.services.offers.tracker;

import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.config.OffersToExcelFileConverterConfig;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.masterdata.services.category.MboTimeUnitAliasesService;
import ru.yandex.market.mboc.common.offers.ImportedOffer;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.converter.OffersExcelFileConverter;
import ru.yandex.market.mboc.common.services.converter.models.OffersParseResult;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;

/**
 * @author galaev@yandex-team.ru
 * @since 26/07/2018.
 */
@SuppressWarnings("checkstyle:magicnumber")
public class EnrichmentOffersToExcelConverterTest {
    private OffersExcelFileConverter<ImportedOffer> converter;

    @Before
    public void setUp() {
        OffersToExcelFileConverterConfig offersToExcelFileConverterConfig =
            new OffersToExcelFileConverterConfig(new CategoryCachingServiceMock());
        MboTimeUnitAliasesService timeUnitAliasesService = Mockito.mock(MboTimeUnitAliasesService.class);
        SupplierRepository supplierRepository = new SupplierRepositoryMock();
        var storageKeyValueServiceMock = new StorageKeyValueServiceMock();
        converter = offersToExcelFileConverterConfig.enrichmentConverter(
            new ModelStorageCachingServiceMock(),
            timeUnitAliasesService,
            supplierRepository,
            storageKeyValueServiceMock
        );
    }

    @Test
    public void testExcelContainsReferencePrice() {
        Offer offer = new Offer()
            .setId(1)
            .setTitle("Offer")
            .setVendor("Vendor")
            .setReferencePrice(1000.2)
            .setShopSku("101010")
            .setIsOfferContentPresent(true)
            .setShopCategoryName("Category")
            .storeOfferContent(OfferContent.builder().build())
            .setModelId(12020L)
            .setCategoryIdForTests(12321L, Offer.BindingKind.SUGGESTED)
            .setBeruPrice(100.1);

        ExcelFile excelFile = converter.convert(Collections.singletonList(offer));

        Integer referencePriceIndex = excelFile.getColumnIndex(ExcelHeaders.REFFERENCE_PRICE.getTitle());
        Assertions.assertThat(referencePriceIndex).isNotNull();
        MbocAssertions.assertThat(excelFile).containsValue(1, referencePriceIndex, 1000.2);

        Integer beruPriceIndex = excelFile.getColumnIndex(ExcelHeaders.BERU_PRICE.getTitle());
        Assertions.assertThat(beruPriceIndex).isNotNull();
        MbocAssertions.assertThat(excelFile).containsValue(1, beruPriceIndex, 100.1);
    }

    @Test
    public void testParsingWithoutShopSkuWorks() {
        Offer offer = new Offer()
            .setId(1)
            .setTitle("Offer")
            .setVendor("Vendor")
            .setReferencePrice(1000.2)
            .setIsOfferContentPresent(true)
            .setShopCategoryName("Category")
            .storeOfferContent(
                OfferContent.builder()
                    .urls(Collections.singletonList("http://asdad.ru/124"))
                    .setVat("0%")
                    .build())
            .setModelId(12020L)
            .setCategoryIdForTests(12321L, Offer.BindingKind.SUGGESTED);

        ExcelFile excelFile = converter.convert(Collections.singletonList(offer));

        OffersParseResult<ImportedOffer> result = converter.convert(excelFile);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getErrors()).isEmpty();
        Assertions.assertThat(result.isFailed()).isFalse();
    }
}
