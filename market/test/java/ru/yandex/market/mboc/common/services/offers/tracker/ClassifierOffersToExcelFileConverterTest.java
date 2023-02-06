package ru.yandex.market.mboc.common.services.offers.tracker;

import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mboc.common.config.OffersToExcelFileConverterConfig;
import ru.yandex.market.mboc.common.offers.ClassifierOffer;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.converter.OffersExcelFileConverter;
import ru.yandex.market.mboc.common.services.converter.models.OffersParseResult;

import static ru.yandex.market.mboc.common.services.excel.ExcelHeaders.FIXED_CATEGORY_ID;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ClassifierOffersToExcelFileConverterTest {
    private OffersExcelFileConverter<ClassifierOffer> converter;

    @Before
    public void setUp() {
        CategoryCachingServiceMock categoryCachingService =
            new CategoryCachingServiceMock();
        OffersToExcelFileConverterConfig offersToExcelFileConverterConfig =
            new OffersToExcelFileConverterConfig(categoryCachingService);
        converter = offersToExcelFileConverterConfig.classifierConverter(categoryCachingService);
    }

    @Test
    public void testParseTrackerFileWithInvalidCategoryId() {
        Offer givenOffer = new Offer()
            .setId(1)
            .setTitle("Offer 1")
            .setVendor("Vendor 1")
            .setShopSku("224455")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setShopCategoryName("Category 1");

        ExcelFile.Builder excelFile = converter.convert(Collections.singletonList(givenOffer)).toBuilder();
        excelFile.setValue(1, FIXED_CATEGORY_ID.getTitle(), 1);

        OffersParseResult<ClassifierOffer> parseResult = converter.convert(excelFile.build());

        Assertions.assertThat(parseResult.isFailed()).isTrue();
        Assertions.assertThatThrownBy(parseResult::throwIfFailed)
            .hasMessageContaining("Ошибка на строке 2: Неизвестный category id: 1.");
    }
}
