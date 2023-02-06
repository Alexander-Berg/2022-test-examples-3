package ru.yandex.market.gutgin.tms.manual.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.partner.content.common.db.dao.DataBucketDao;
import ru.yandex.market.partner.content.common.db.dao.PartnerPictureService;
import ru.yandex.market.partner.content.common.db.dao.PipelineDao;
import ru.yandex.market.partner.content.common.db.dao.SourceDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.DatacampOfferDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.DcpPartnerPictureDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.FakeDatacampOfferDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuToModelTargetStateDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.daos.MboPictureDao;

@Configuration
@Import(ManualProductionDatabaseConfig.class)
public class ManualProductionDaoConfiguration {

    final org.jooq.Configuration configuration;

    public ManualProductionDaoConfiguration(
        @Qualifier("jooq.config.configuration.ro.production") org.jooq.Configuration configuration
    ) {
        this.configuration = configuration;
    }

    @Bean
    @Qualifier("sku.ticket.dao.ro.production")
    GcSkuTicketDao gcSkuTicketDaoProduction() {
        return new GcSkuTicketDao(configuration);
    }

    @Bean
    @Qualifier("source.dao.ro.production")
    SourceDao sourceDaoProduction() {
        return new SourceDao(configuration);
    }

    @Bean
    @Qualifier("pipeline.dao.ro.production")
    PipelineDao pipelineDaoProduction() {
        return new PipelineDao(configuration);
    }

    @Bean(name = "dataBucketDaoProduction")
    @Qualifier("data.backet.dao.ro.production")
    DataBucketDao dataBucketDaoProduction() {
        return new DataBucketDao(configuration);
    }

    @Bean
    @Qualifier("datacamp.offer.dao.ro.production")
    DatacampOfferDao datacampOfferDaoProduction() {
        return new DatacampOfferDao(configuration);
    }

    @Bean
    FakeDatacampOfferDao fakeDatacampOfferDao(
            @Qualifier("datacampOfferDao")
            DatacampOfferDao datacampOfferDao,
            @Value("${environment}") String environment
    ) {
        return new FakeDatacampOfferDao(configuration, datacampOfferDao, environment);
    }

    @Bean
    @Qualifier("dcp.partner.picture.dao.ro.production")
    DcpPartnerPictureDao dcpPartnerPictureDaoProduction() {
        return new DcpPartnerPictureDao(configuration);
    }

    @Bean
    @Qualifier("mbo.picture.dao.ro.production")
    MboPictureDao mboPictureDaoProduction() {
        return new MboPictureDao(configuration);
    }

    @Bean
    GcSkuToModelTargetStateDao gcSkuToModelTargetStateDaoProduction() {
        return new GcSkuToModelTargetStateDao(configuration);
    }

    @Bean
    PartnerPictureService partnerPictureServiceProduction() {
        return new PartnerPictureService(configuration);
    }
}
