package ru.yandex.market.gutgin.tms.manual;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.gutgin.tms.manual.config.ManualProductionDaoConfiguration;
import ru.yandex.market.gutgin.tms.manual.config.ManualTestConfiguration;
import ru.yandex.market.gutgin.tms.pipeline.dcp.taskaction.databucket.AvatarImageDownloader;
import ru.yandex.market.gutgin.tms.pipeline.dcp.taskaction.databucket.DcpPicturesFromModelCollector;
import ru.yandex.market.gutgin.tms.pipeline.dcp.taskaction.databucket.ImageUploader;
import ru.yandex.market.gutgin.tms.pipeline.dcp.taskaction.databucket.UploadImagesTaskAction;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.util.JooqUtils;
import ru.yandex.market.mbo.http.ModelStorageService;
import ru.yandex.market.partner.content.common.db.dao.DataBucketDao;
import ru.yandex.market.partner.content.common.db.dao.SourceDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.DatacampOfferDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.DcpPartnerPictureDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.FakeDatacampOfferDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.daos.MboPictureDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DcpPartnerPicture;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.helpers.DatacampOfferHelper;

import static ru.yandex.market.partner.content.common.db.jooq.Tables.MBO_PICTURE;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        ManualTestConfiguration.class,
        ManualProductionDaoConfiguration.class
})
@TestPropertySource(properties = {
        "mbo.http-exporter.url=http://mbo-http-exporter.yandex.net:8084",
        "ag-mbo.card.api.model.storage.host=http://mbo-card-api.http.yandex.net:33714/modelStorage/",
        "user.agent=manual_test",
        "market.gutgin.jdbc.driverClassName=org.postgresql.Driver",
        "market.gutgin.jdbc.readUrl=jdbc:postgresql://man-xeurx099myf9ts5u.db.yandex.net:6432,sas-asqjlknmyp34589d.db.yandex.net:6432,vla-prdz66ai6ebfwzm1.db.yandex.net:6432/market_partner_content_prod?&targetServerType=slave&ssl=true&sslmode=require&prepareThreshold=0&preparedStatementCacheQueries=0",
        // jdbc:postgresql://man-xeurx099myf9ts5u.db.yandex.net:6432,sas-asqjlknmyp34589d.db.yandex.net:6432,vla-prdz66ai6ebfwzm1.db.yandex.net:6432/market_partner_content_prod?&targetServerType=master&ssl=true&sslmode=require
        "market.gutgin.username=market_partner_content_prod",
        "market.gutgin.password=" //https://yav.yandex-team.ru/secret/sec-01dbjhv1dg54pptmgnsrtn0nnk/explore/versions

        //testing
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
public class UploadImagesDryRunner {

    UploadImagesTaskAction uploadImagesTaskAction;

    @Autowired
    ModelStorageHelper modelStorageHelper;

    @Qualifier("dao.config.dataBucket")
    @Autowired
    DataBucketDao dataBucketDao;
    @Autowired
    DcpPartnerPictureDao dcpPartnerPictureDao;

    @Autowired
    @Qualifier("datacampOfferDao")
    DatacampOfferDao datacampOfferDao;

    @Autowired
    SourceDao sourceDao;

    @Autowired
    GcSkuTicketDao gcSkuTicketDao;

    @Autowired
    @Qualifier("gcSkuTicketDaoProduction")
    GcSkuTicketDao gcSkuTicketDaoProduction;

    @Autowired
    DataBucketDao dataBucketDaoProduction;

    @Autowired
    SourceDao sourceDaoProduction;

    @Autowired
    DatacampOfferDao datacampOfferDaoProduction;

    @Autowired
    DcpPartnerPictureDao dcpPartnerPictureDaoProduction;

    @Autowired
    MboPictureDao mboPictureDaoProduction;

    @Autowired
    FakeDatacampOfferDao fakeDatacampOfferDao;

    @Autowired
    ModelStorageService modelStorageService;


    @Before
    public void setUp() throws Exception {
        uploadImagesTaskAction = new UploadImagesTaskAction(gcSkuTicketDao, new ImageUploader(10,
                new AvatarImageDownloader(),
                modelStorageService,
                Executors.newSingleThreadExecutor(),
                dcpPartnerPictureDao, new DcpPicturesFromModelCollector(modelStorageHelper)),
                fakeDatacampOfferDao);
    }

    @Ignore
    @Test
    public void runUploadImg() {
        long dataBucketId = copyDataFromProduction(10409307);
        uploadImagesTaskAction.doRun(new ProcessDataBucketData(dataBucketId));
    }

    private long copyDataFromProduction(long dataBucketId) {
        List<GcSkuTicket> tickets = gcSkuTicketDaoProduction.getValidTicketsByDataBucket(dataBucketId);
        sourceDao.insert(sourceDaoProduction.fetchBySourceId(tickets.get(0).getSourceId()));
        dataBucketDao.insert(dataBucketDaoProduction.fetchById(dataBucketId));
        datacampOfferDao.insert(datacampOfferDaoProduction.fetchById(tickets.stream().map(GcSkuTicket::getDatacampOfferId).toArray(Long[]::new)));
        List<String> pictures = new ArrayList<>();
        tickets.forEach(ticket -> {
            DatacampOfferHelper.collectPicturesFromTicket(ticket, (url, isMain) -> pictures.add(url));
        });


        List<DcpPartnerPicture> dcpPartnerPictures =
                dcpPartnerPictureDaoProduction.fetchByIdxAvatarUrl(pictures.toArray(new String[0]));
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
        return dataBucketId;
    }
}
