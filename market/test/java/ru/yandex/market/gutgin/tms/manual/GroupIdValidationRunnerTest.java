package ru.yandex.market.gutgin.tms.manual;


import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.gutgin.tms.manual.config.ManualProductionDaoConfiguration;
import ru.yandex.market.gutgin.tms.manual.config.ManualTestConfiguration;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.databucket_validations.groupid.DcpGroupIdValidation;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.databucket_validations.groupid.DcpGroupIdValidationChecker;
import ru.yandex.market.http.ServiceClient;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.util.JooqUtils;
import ru.yandex.market.mbo.export.CategoryParametersServiceStub;
import ru.yandex.market.mbo.export.CategorySizeMeasureServiceStub;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.partner.content.common.csku.judge.Judge;
import ru.yandex.market.partner.content.common.db.dao.DataBucketDao;
import ru.yandex.market.partner.content.common.db.dao.SourceDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.DatacampOfferDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.DcpPartnerPictureDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcExternalServiceRequestDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuValidationDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.daos.MboPictureDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DcpPartnerPicture;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.helpers.DatacampOfferHelper;
import ru.yandex.market.request.httpclient.trace.TraceHttpRequestInterceptor;
import ru.yandex.market.request.httpclient.trace.TraceHttpResponseInterceptor;
import ru.yandex.market.request.trace.Module;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.MBO_PICTURE;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        initializers = PGaaSZonkyInitializer.class,
        classes = {
        ManualTestConfiguration.class,
        ManualProductionDaoConfiguration.class
})
@TestPropertySource(properties = {
        "mbo.http-exporter.url=http://mbo-http-exporter.yandex.net:8084",
        "ag-mbo.card.api.model.storage.host=http://mbo-card-api.http.yandex.net:33714/modelStorage/",
        "user.agent=manual_test",
        "market.gutgin.jdbc.driverClassName=org.postgresql.Driver",
        "market.gutgin.jdbc.readUrl=jdbc:postgresql://man-xeurx099myf9ts5u.db.yandex.net:6432,sas-asqjlknmyp34589d.db.yandex.net:6432,vla-prdz66ai6ebfwzm1.db.yandex.net:6432/market_partner_content_prod?&targetServerType=slave&ssl=true&sslmode=require&prepareThreshold=0&preparedStatementCacheQueries=0",
        "market.gutgin.username=market_partner_content_prod",
        "market.gutgin.password=" //https://yav.yandex-team.ru/secret/sec-01dbjhv1dg54pptmgnsrtn0nnk/explore/versions

})
public class GroupIdValidationRunnerTest {

    DcpGroupIdValidationChecker checker;

    @Qualifier("dao.config.dataBucket")
    @Autowired
    DataBucketDao dataBucketDao;
    @Autowired
    DcpPartnerPictureDao dcpPartnerPictureDao;
    @Autowired
    DatacampOfferDao datacampOfferDao;
    @Autowired
    SourceDao sourceDao;

    @Autowired
    GcSkuTicketDao gcSkuTicketDao;

    @Autowired
    GcSkuValidationDao gcSkuValidationDao;

    @Autowired
    GcExternalServiceRequestDao gcExternalServiceRequestDao;

    @Autowired
    @Qualifier("sku.ticket.dao.ro.production")
    GcSkuTicketDao gcSkuTicketDaoProduction;

    @Autowired
    @Qualifier("data.backet.dao.ro.production")
    DataBucketDao dataBucketDaoProduction;

    @Autowired
    @Qualifier("source.dao.ro.production")
    SourceDao sourceDaoProduction;

    @Autowired
    @Qualifier("datacamp.offer.dao.ro.production")
    DatacampOfferDao datacampOfferDaoProduction;

    @Autowired
    @Qualifier("dcp.partner.picture.dao.ro.production")
    DcpPartnerPictureDao dcpPartnerPictureDaoProduction;

    @Autowired
    @Qualifier("mbo.picture.dao.ro.production")
    MboPictureDao mboPictureDaoProduction;

    @Autowired
    ModelStorageHelper modelStorageHelper;

    @Before
    public void setUp() throws Exception {
        CategoryDataKnowledge categoryDataKnowledge = new CategoryDataKnowledge();

        CategoryParametersServiceStub categoryParametersService = new CategoryParametersServiceStub();
//        categoryParametersService.setHost("http://mbo-http-exporter.tst.vs.market.yandex.net:8084/categoryParameters/");
        categoryParametersService.setHost("http://mbo-http-exporter.yandex.net:8084/categoryParameters/");
        initServiceClient(categoryParametersService, Module.MBO_HTTP_EXPORTER);

        categoryDataKnowledge.setCategoryParametersService(categoryParametersService);
        categoryDataKnowledge.setCategoryDataRefreshersCount(1);
        categoryDataKnowledge.setCacheLength(200);
        categoryDataKnowledge.setCacheStrategy(CategoryDataKnowledge.CacheStrategy.ALWAYS_USE_CACHE);

        CategorySizeMeasureServiceStub categorySizeMeasureService = new CategorySizeMeasureServiceStub();
        initServiceClient(categorySizeMeasureService, Module.MBO_HTTP_EXPORTER);
//        categorySizeMeasureService.setHost("http://mbo-http-exporter.tst.vs.market.yandex.net:8084/categorySizeMeasure/");
        categorySizeMeasureService.setHost("http://mbo-http-exporter.yandex.net:8084/categorySizeMeasure/");

        categoryDataKnowledge.setCategorySizeMeasureService(categorySizeMeasureService);
        categoryDataKnowledge.afterPropertiesSet();
        checker = new DcpGroupIdValidationChecker(categoryDataKnowledge, new Judge(), modelStorageHelper);
    }

    @Ignore
    @Test
    public void run() {
        List<GcSkuTicket> tickets = copyDataFromProduction(41351394);
        DcpGroupIdValidation validation = new DcpGroupIdValidation(
                gcSkuValidationDao,
                gcSkuTicketDao,
                checker
        );
        ProcessDataBucketData data = new ProcessDataBucketData(41351394);
        ProcessTaskResult<ProcessDataBucketData> taskResult =
                validation.runOnTickets(tickets, data);
        assertThat(gcSkuValidationDao.findAll()).hasSize(2);
        //тут можно добавить свои проверки
    }

    private List<GcSkuTicket> copyDataFromProduction(long dataBucketId) {
        List<GcSkuTicket> tickets = gcSkuTicketDaoProduction.getTicketsByDataBucket(dataBucketId);
        sourceDao.insert(sourceDaoProduction.fetchBySourceId(tickets.get(0).getSourceId()));
        dataBucketDao.insert(dataBucketDaoProduction.fetchById(dataBucketId));
        datacampOfferDao.insert(datacampOfferDaoProduction.fetchById(tickets.stream().map(GcSkuTicket::getDatacampOfferId).toArray(Long[]::new)));
        List<String> pictures = new ArrayList<>();
        tickets.forEach(ticket -> {
            DatacampOfferHelper.collectPicturesFromTicket(ticket, (url, isMain) -> pictures.add(url));
        });


        List<DcpPartnerPicture> dcpPartnerPictures = dcpPartnerPictureDaoProduction.fetchByIdxAvatarUrl(pictures.toArray(new String[0]));
        JooqUtils.batchInsert(
                gcSkuTicketDao.dsl(),
                MBO_PICTURE,
                mboPictureDaoProduction.fetchById(dcpPartnerPictures.stream().map(DcpPartnerPicture::getId).toArray(Long[]::new)),
                (table, object) -> table
                        .set(MBO_PICTURE.ID, object.getId())
                        .set(MBO_PICTURE.DATA, object.getData())
        );
        dcpPartnerPictureDao.insert(dcpPartnerPictures);
        dcpPartnerPictureDao.findAll();
        gcSkuTicketDao.insert(tickets);
        return tickets;
    }

    private void initServiceClient(ServiceClient serviceClient, Module traceModule) {
        serviceClient.setUserAgent("defaultUserAgent");
        if (traceModule != null) {
            serviceClient.setHttpRequestInterceptor(new TraceHttpRequestInterceptor(traceModule));
            serviceClient.setHttpResponseInterceptor(new TraceHttpResponseInterceptor());
        }
    }
}
