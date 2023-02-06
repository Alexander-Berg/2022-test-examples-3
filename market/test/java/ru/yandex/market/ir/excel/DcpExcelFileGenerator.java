package ru.yandex.market.ir.excel;

import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.rating.SkuRatingEvaluator;
import ru.yandex.market.ir.autogeneration_api.export.excel.DcpExcelModelGenerator;
import ru.yandex.market.ir.autogeneration_api.export.excel.DcpSkuDataGenerator;
import ru.yandex.market.ir.autogeneration_api.export.excel.SkuExampleService;
import ru.yandex.market.ir.excel.generator.CategoryInfoProducer;
import ru.yandex.market.ir.excel.generator.PartnerContentConverter;
import ru.yandex.market.partner.content.common.csku.judge.Judge;
import ru.yandex.market.partner.content.common.db.dao.SourceDao;
import ru.yandex.market.partner.content.common.service.mock.DataCampServiceMock;

import static org.mockito.Mockito.mock;

public class DcpExcelFileGenerator extends TestExcelFileGenerator {

    DcpExcelFileGenerator(CategoryInfoProducer categoryInfoProducer, DcpSkuDataGenerator dcpSkuDataGenerator) {
        super(categoryInfoProducer);

        generator = new DcpExcelModelGenerator(
            categoryInfoProducer, mock(SkuExampleService.class), dcpSkuDataGenerator, false);
    }

    DcpExcelFileGenerator(CategoryInfoProducer categoryInfoProducer, CategoryDataHelper categoryDataHelper,
                          SourceDao sourceDao, DataCampServiceMock dataCampServiceMock,
                          SkuRatingEvaluator skuRatingEvaluator) {
        super(categoryInfoProducer);

        DcpSkuDataGenerator dcpSkuDataGenerator = new DcpSkuDataGenerator(
            dataCampServiceMock, sourceDao,
            new PartnerContentConverter(categoryDataHelper), mock(ModelStorageHelper.class), new Judge(),
                skuRatingEvaluator);
        generator = new DcpExcelModelGenerator(
            categoryInfoProducer, mock(SkuExampleService.class), dcpSkuDataGenerator, false);
    }
}
