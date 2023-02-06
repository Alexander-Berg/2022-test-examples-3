package ru.yandex.market.gutgin.tms.manual;


import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.gutgin.tms.manual.config.ManualProductionDaoConfiguration;
import ru.yandex.market.gutgin.tms.manual.config.ManualTestConfiguration;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku.CSKUDataPreparation;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku.CSKURequestCreator;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku.CSKUResponseProcessor;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku.TicketWrapper;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku.SaveCskuToMboTaskAction;
import ru.yandex.market.gutgin.tms.service.ResponseProcessingResult;
import ru.yandex.market.gutgin.tms.service.datacamp.savemodels.update.MboPictureService;
import ru.yandex.market.gutgin.tms.utils.ParameterCreator;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.util.JooqUtils;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.partner.content.common.csku.judge.Judge;
import ru.yandex.market.partner.content.common.csku.util.OfferParametersActualizer;
import ru.yandex.market.partner.content.common.db.dao.DataBucketDao;
import ru.yandex.market.partner.content.common.db.dao.PipelineDao;
import ru.yandex.market.partner.content.common.db.dao.SourceDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.DatacampOfferDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.DcpPartnerPictureDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcExternalServiceRequestDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuValidationDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.daos.MboPictureDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.daos.SkuDuplicatesDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DcpPartnerPicture;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Pipeline;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.helpers.DatacampOfferHelper;
import ru.yandex.market.partner.content.common.service.mappings.PartnerShopService;

import static org.mockito.Mockito.mock;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.GC_SKU_TICKET;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.MBO_PICTURE;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = PGaaSZonkyInitializer.class,
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

//        "mbo.http-exporter.url=http://mbo-http-exporter.yandex.net:8084",
//        "ag-mbo.card.api.model.storage.host=http://mbo-card-api-proxy.tst.vs.market.yandex.net/modelStorage/",
//        "user.agent=manual_test",
//        "market.gutgin.jdbc.driverClassName=org.postgresql.Driver",
//        "market.gutgin.jdbc.readUrl=jdbc:postgresql://man-k2dzfyih8prmrb0s.db.yandex.net:6432,vla-yf6n96aee7cyf25d.db" +
//                ".yandex.net:6432,sas-if3w998erbukvnxb.db.yandex" +
//                ".net:6432/market_partner_content_test?targetServerType=master&ssl=true&prepareThreshold=0" +
//                "&sslmode=require&prepareThreshold=0&preparedStatementCacheQueries=0",
//        "market.gutgin.username=market_partner_content_test",
//        "market.gutgin.password="
//        //https://yav.yandex-team.ru/secret/sec-01dg55zhm4nvktac4bxdmr6r03/explore/version/ver-01g12y81mr38yg8ey5js25afj6

})
public class CskuMboSaveDryRunnerTest {

    CSKURequestCreator cskuRequestCreator;
    CSKUResponseProcessor cskuResponseProcessor;
    CSKUDataPreparation cskuDataPreparation;

    @Autowired
    CategoryDataHelper categoryDataHelper;
    @Autowired
    ModelStorageHelper modelStorageHelper;
    @Autowired
    PartnerShopService partnerShopService;
    @Autowired
    ParameterCreator parameterCreator;
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
    @Qualifier("pipeline.dao.ro.production")
    PipelineDao pipelineDaoProduction;

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
    MboPictureService mboPictureService;

    @Value("${gc_save_dcp_psku_batch_size:10}")
    int batchSize;
    @Value("${gg.enable_on_the_fly_psku:false}")
    boolean enableOnTheFlyPsku;


    @Before
    public void setUp() throws Exception {
        OfferParametersActualizer offerParametersActualizer = new OfferParametersActualizer("testing","");
        this.cskuDataPreparation = new CSKUDataPreparation(modelStorageHelper, gcSkuValidationDao);
        this.cskuRequestCreator = new CSKURequestCreator(new Judge(), categoryDataHelper, modelStorageHelper,
                gcSkuValidationDao, mboPictureService);
        this.cskuResponseProcessor = new CSKUResponseProcessor(gcSkuTicketDao, mock(SkuDuplicatesDao.class));
    }

    @Test
    @Ignore
    public void run() {
        //при копировании больших пайплайнов стоит фильтровать только нужные тикеты,
        //что бы не делать запрос по всем 10_000 моделей
        long databucket = copyDataFromProduction(39469794);
        //goStepByStep(databucket);
        SaveCskuToMboTaskAction action = new SaveCskuToMboTaskAction(gcSkuTicketDao, modelStorageHelper, gcExternalServiceRequestDao,
                gcSkuValidationDao, cskuRequestCreator, cskuResponseProcessor, cskuDataPreparation, 10);
        action.doRun(new ProcessDataBucketData(databucket));
        printTicketStatusToLog();
    }

    //Только для негрупповых офферов. Для групп использовать createRequestForGroup
    private void goStepByStep(long databucket){
        //выгружаем скопированные из production среды оферы
        List<GcSkuTicket> ticketsByDataBucket = gcSkuTicketDao.getTicketsByDataBucket(databucket);
        //собираем из тикетов данные для запроса
        List<TicketWrapper> ticketWrappers = cskuDataPreparation.collectDataForRequest(ticketsByDataBucket);
        //формируем запрос
        ModelCardApi.SaveModelsGroupRequest request = cskuRequestCreator.createRequest(ticketWrappers);
//        request.getModelsRequestList().get(7).getModels(1).
        System.out.println("request = " + request);
        //отправляем запрос
        ModelStorageHelper.SaveGroupResponse response = executeRequests(request);
        System.out.println("response = " + response);
        //парсим статусы ответов
        ResponseProcessingResult result = cskuResponseProcessor.processResults(response, ticketsByDataBucket);
        System.out.println("result = " + result);
    }

    private void printTicketStatusToLog() {
        gcSkuTicketDao.dsl()
                .select(GC_SKU_TICKET.ID, GC_SKU_TICKET.EXISTING_MBO_PSKU_ID, GC_SKU_TICKET.RESULT_MBO_PSKU_ID,
                        GC_SKU_TICKET.RESULT_MBO_PMODEL_ID,
                        GC_SKU_TICKET.STATUS)
                .from(GC_SKU_TICKET)
                .execute();
    }

    private long copyDataFromProduction(long pipelineId) {
        Pipeline pipeline = pipelineDaoProduction.fetchById(pipelineId).get(0);
        ProcessDataBucketData data = (ProcessDataBucketData) pipeline.getInputData();
        long dataBucketId = data.getDataBucketId();
        List<GcSkuTicket> tickets = gcSkuTicketDaoProduction.getTicketsByDataBucket(dataBucketId);
        if (tickets.isEmpty()) {
            throw new IllegalStateException("Cant find tickets with pipelineId =" + pipelineId);
        }

        tickets.forEach(gcSkuTicket -> {
            if (gcSkuTicket.getDatacampOffer() == null) {
                throw new IllegalStateException(" There's no DatacampOffer in ticket " + gcSkuTicket.getId());
            }
        });

        sourceDao.insert(sourceDaoProduction.fetchBySourceId(tickets.get(0).getSourceId()));
        dataBucketDao.insert(dataBucketDaoProduction.fetchById(dataBucketId));
        datacampOfferDao.insert(datacampOfferDaoProduction.fetchById(
                tickets.stream().map(GcSkuTicket::getDatacampOfferId).toArray(Long[]::new)));
        List<String> pictures = new ArrayList<>();
        tickets.forEach(ticket -> {
            DatacampOfferHelper.collectPicturesFromTicket(ticket, (url, isMain) -> pictures.add(url));
        });


        List<DcpPartnerPicture> dcpPartnerPictures =
                dcpPartnerPictureDaoProduction.fetchByIdxAvatarUrl(pictures.toArray(new String[0]));
        JooqUtils.batchInsert(
                gcSkuTicketDao.dsl(),
                MBO_PICTURE,
                mboPictureDaoProduction.fetchById(
                        dcpPartnerPictures.stream().map(DcpPartnerPicture::getId).toArray(Long[]::new)),
                (table, object) -> table
                        .set(MBO_PICTURE.ID, object.getId())
                        .set(MBO_PICTURE.DATA, object.getData())
        );
        dcpPartnerPictureDao.insert(dcpPartnerPictures);
        dcpPartnerPictureDao.findAll();
        gcSkuTicketDao.insert(tickets);
        return dataBucketId;
    }

    private ModelStorageHelper.SaveGroupResponse executeRequests(ModelCardApi.SaveModelsGroupRequest requests) {
        return modelStorageHelper.executeSaveModelRequest(requests);
    }
}
