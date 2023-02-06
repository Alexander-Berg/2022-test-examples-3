package ru.yandex.market.psku.postprocessor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.mbo.export.CategorySizeMeasureService;
import ru.yandex.market.mbo.export.MboSizeMeasures;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.GenerationTaskType;
import ru.yandex.market.psku.postprocessor.msku_creation.PskuClusterDao;
import ru.yandex.market.psku.postprocessor.msku_creation.PskuResponse;
import ru.yandex.market.psku.postprocessor.service.logging.HealthLogManager;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Configuration
public class CommonTestConfig {

    @Bean()
    PskuClusterDao ytPskuClusterDao(
    ) {
        return Collections::emptyList;
    }

    @Bean
    CategoryDataKnowledge categoryDataKnowledge() {
        return null;
    }

    @Bean
    CategorySizeMeasureService categorySizeMeasureService() {
        return new CategorySizeMeasureService() {
            @Override
            public MboSizeMeasures.GetCategorySizeMeasureResponse getSizeMeasures(
                MboSizeMeasures.GetCategorySizeMeasuresRequest getCategorySizeMeasuresRequest) {
                return null;
            }

            @Override
            public MboSizeMeasures.GetSizeMeasuresInfoResponse getSizeMeasuresInfo(
                MboSizeMeasures.GetSizeMeasuresInfoRequest getSizeMeasuresInfoRequest) {
                return MboSizeMeasures.GetSizeMeasuresInfoResponse.newBuilder().build();
            }

            @Override
            public MboSizeMeasures.GetSizeMeasuresInfoVendorResponse getSizeMeasuresVendorsInfo(
                MboSizeMeasures.GetSizeMeasureInfoVendorRequest getSizeMeasureInfoVendorRequest) {
                return null;
            }

            @Override
            public MonitoringResult ping() {
                return null;
            }

            @Override
            public MonitoringResult monitoring() {
                return null;
            }
        };
    }

    @Bean
    HealthLogManager healthLogManager() {
        return new HealthLogManager() {
            @Override
            public void logPskuProcessingReceivedMetrics(Map<Long, List<PskuResponse>> resultsByCategory) {
            }

            @Override
            public void logPskuProcessingSendMetrics(Map<Long, Integer> pskuCountsByCategory,
                                                     GenerationTaskType generationTaskType) {
            }
        };
    }
}
