package ru.yandex.market.ir.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.market.gutgin.tms.config.pipeline.xls.dcp.DCPSingleXLSPipelineV1;
import ru.yandex.market.gutgin.tms.engine.pipeline.PipelineTemplateHolder;
import ru.yandex.market.gutgin.tms.engine.pipeline.template.PipelineTemplate;
import ru.yandex.market.gutgin.tms.service.OfferContentStateMbocApiService;
import ru.yandex.market.gutgin.tms.service.OfferContentStateMbocApiServiceMock;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.rating.DefaultRatingEvaluator;
import ru.yandex.market.ir.autogeneration.common.rating.SkuRatingEvaluator;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.logbroker.model.LogbrokerCluster;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;
import ru.yandex.market.partner.content.common.service.DataCampService;
import ru.yandex.market.partner.content.common.service.mock.DataCampServiceMock;

@Configuration
@Import({
    DCPSingleXLSPipelineV1.class,
    CommonPipelineConfig.class
})
public class DcpExcelPipelineTestConfig {

    @Bean
    public DataCampServiceMock dataCampServiceMock() {
        return new DataCampServiceMock();
    }

    @Bean
    public DataCampService dataCampService() {
        return dataCampServiceMock();
    }

    @Bean
    public OfferContentStateMbocApiServiceMock offerContentStateMbocApiServiceMock() {
        return new OfferContentStateMbocApiServiceMock();
    }

    @Bean
    public OfferContentStateMbocApiService offerContentStateMbocApiService() {
        return offerContentStateMbocApiServiceMock();
    }

    @Bean
    public PipelineTemplateHolder pipelineTemplateHolder(
        PipelineTemplate<?, ?> dcpSingleXLSPipelineV1
    ) {
        PipelineTemplateHolder pipelineTemplateHolder = new PipelineTemplateHolder();
        pipelineTemplateHolder.addPipeline(PipelineType.DCP_SINGLE_XLS, 1, dcpSingleXLSPipelineV1);
        return pipelineTemplateHolder;
    }

    @Bean
    public LogbrokerService offerLogbrokerService() {
        return Mockito.mock(LogbrokerService.class);
    }

    @Bean
    public LogbrokerClientFactory logbrokerClientFactory() {
        return Mockito.mock(LogbrokerClientFactory.class);
    }

    @Bean
    public LogbrokerCluster logbrokerCluster() {
        return Mockito.mock(LogbrokerCluster.class);
    }

    @Bean
    public SkuRatingEvaluator skuRatingEvaluator(CategoryDataKnowledge categoryDataKnowledge){
        return new DefaultRatingEvaluator(categoryDataKnowledge);
    }
}
