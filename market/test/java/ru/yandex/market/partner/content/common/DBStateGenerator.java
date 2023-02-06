package ru.yandex.market.partner.content.common;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.db.dao.PartnerPictureService;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessFileData;
import ru.yandex.market.partner.content.common.entity.goodcontent.SkuTicket;

public abstract class DBStateGenerator
        extends BaseDBStateGenerator {

    @Autowired
    protected PartnerPictureService partnerPictureService;

    @Before
    public void setUp() {
        super.setUp();

        requestId = createFileDataProcessRequest(SOURCE_ID);
        processId = createFileProcessId(requestId);
        processFileData = new ProcessFileData(requestId, processId);

        dataBucketId = dataBucketDao.getOrCreateDataBucket(CATEGORY_ID, processId, SOURCE_ID, Timestamp.from(Instant.now()));
        processDataBucketData = new ProcessDataBucketData(dataBucketId);
    }

    public void insertPartnerPictures(List<SkuTicket> skuTickets) {
        skuTickets.forEach(this::insertPartnerPicture);
    }

    public void insertPartnerPicture(SkuTicket skuTicket) {
        insertPartnerPicture(SOURCE_ID, skuTicket.getMainPictureUrl());
        skuTicket.getOtherPictureUrls().forEach(url -> insertPartnerPicture(SOURCE_ID, url));
    }

    public void insertPartnerPicture(int sourceId, String url) {
        partnerPictureService.insertPartnerPicture(
            sourceId, url, null, null, null,
            ModelStorage.Picture.newBuilder().setUrlSource(url).build());
    }
}
