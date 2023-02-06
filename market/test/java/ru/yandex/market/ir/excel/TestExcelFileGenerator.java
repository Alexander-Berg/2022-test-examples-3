package ru.yandex.market.ir.excel;

import Market.DataCamp.DataCampOffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mockito.Mockito;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.autogeneration.common.rating.SkuRatingEvaluator;
import ru.yandex.market.ir.autogeneration_api.export.excel.DcpSkuDataGenerator;
import ru.yandex.market.ir.autogeneration_api.export.excel.ExcelFileContext;
import ru.yandex.market.ir.autogeneration_api.export.excel.ExcelModelGenerator;
import ru.yandex.market.ir.autogeneration_api.export.excel.SkuExampleService;
import ru.yandex.market.ir.excel.generator.CategoryInfoProducer;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.db.dao.SourceDao;
import ru.yandex.market.partner.content.common.service.mock.DataCampServiceMock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author danfertev
 * @since 05.07.2019
 */
public abstract class TestExcelFileGenerator {
    private static final Logger log = LogManager.getLogger();

    protected final CategoryDataKnowledgeMock categoryDataKnowledgeMock;
    protected final CategoryInfoProducer categoryInfoProducer;
    protected final SkuExampleService skuExampleService;
    protected ExcelModelGenerator generator;

    TestExcelFileGenerator(CategoryInfoProducer categoryInfoProducer) {
        this.categoryInfoProducer = categoryInfoProducer;

        categoryDataKnowledgeMock = new CategoryDataKnowledgeMock();
        skuExampleService = Mockito.mock(SkuExampleService.class);
    }

    public byte[] generate(long categoryId, int sourceId, List<String> shopSkus) {
        ExcelFileContext fileContext = generator.generate(Math.toIntExact(categoryId), sourceId, shopSkus, false);
        log.debug("File generated {}", fileContext.getFileName());
        return fileContext.getContent();
    }

    public static class Builder {
        private Map<String, ModelStorage.Model.Builder> models = new HashMap<>();
        private CategoryInfoProducer categoryInfoProducer;
        private CategoryDataHelper categoryDataHelper;
        private SkuRatingEvaluator skuRatingEvaluator;

        public Builder setCategoryInfoProducer(CategoryInfoProducer categoryInfoProducer) {
            this.categoryInfoProducer = categoryInfoProducer;
            return this;
        }

        public Builder setCategoryDataHelper(CategoryDataHelper categoryDataHelper) {
            this.categoryDataHelper = categoryDataHelper;
            return this;
        }

        public Builder setSkuRatingEvaluator(SkuRatingEvaluator skuRatingEvaluator) {
            this.skuRatingEvaluator = skuRatingEvaluator;
            return this;
        }

        public Builder addModel(String shopSku, int supplierId, ModelStorage.Model.Builder model) {
            model.setSupplierId(supplierId);
            this.models.put(shopSku, model);
            return this;
        }

        public TestExcelFileGenerator buildDcp(DcpSkuDataGenerator dcpSkuDataGenerator) {
            return new DcpExcelFileGenerator(categoryInfoProducer, dcpSkuDataGenerator);
        }

        public TestExcelFileGenerator buildDcp(SourceDao sourceDao, int businessId, DataCampOffer.Offer... offers) {
            DataCampServiceMock dataCampServiceMock = new DataCampServiceMock();
            dataCampServiceMock.setOffersForBusinessId(businessId, offers);
            return new DcpExcelFileGenerator(
                categoryInfoProducer, categoryDataHelper, sourceDao, dataCampServiceMock, skuRatingEvaluator);
        }

        public static Builder newInstance() {
            return new Builder();
        }
    }
}
