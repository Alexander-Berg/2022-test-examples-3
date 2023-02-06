package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.fast_pipeline;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import Market.DataCamp.DataCampOffer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.gutgin.tms.assertions.GutginAssertions;
import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.partner.content.common.utils.DcpOfferBuilder;
import ru.yandex.market.partner.content.common.utils.DcpPartnerPictureUtils;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.DBStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.dcp.DcpPartnerPictureDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.IdxAvatarDownloadFailureCode;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DcpPartnerPicture;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;

import static org.assertj.core.api.Assertions.assertThat;

public class DcpPartnerPictureTaskActionTest extends DBStateGenerator {

    @Autowired
    private DcpPartnerPictureDao dcpPartnerPictureDao;
    private DcpPartnerPictureTaskAction taskAction;


    @Override
    @Before
    public void setUp() {
        super.setUp();
        this.taskAction = new DcpPartnerPictureTaskAction(gcSkuTicketDao, dcpPartnerPictureDao);
    }

    @Test
    public void addNewUrlsAndDontChangeOld() {
        GcSkuTicket ticket1 = createDatacampTicket(1, Arrays.asList("url-1", "url-2"));
        String alreadyCreatedUrl = "url-4";
        String alreadyCreatedUrlByFastPipe = "url-5";
        GcSkuTicket ticket2 = createDatacampTicket(2, Arrays.asList(
                "url-2",
                "url-3",
                alreadyCreatedUrl,
                alreadyCreatedUrlByFastPipe));
        List<GcSkuTicket> tickets = Arrays.asList(ticket1, ticket2);
        createRowInDcpPartnerPicture(alreadyCreatedUrl);
        createRowInDcpPartnerPictureCreatedByFastPipe(alreadyCreatedUrlByFastPipe);
        ProcessDataBucketData dataBucket = new ProcessDataBucketData(555);

        ProcessTaskResult<ProcessDataBucketData> result = taskAction.runOnTickets(tickets, dataBucket);

        GutginAssertions.assertThat(result).doesntHaveProblems();
        assertThat(dcpPartnerPictureDao.findAll()).hasSize(5);
        List<DcpPartnerPicture> createdRows =
                dcpPartnerPictureDao.fetchByIdxAvatarUrl("idx_url-1", "idx_url-2", "idx_url-3");
        assertThat(createdRows).hasSize(3);
        assertThat(createdRows).extracting(DcpPartnerPicture::getGcSkuTicketId).containsOnly(1L, 2L);
        assertThat(createdRows).extracting(DcpPartnerPicture::getMboPicture).containsOnlyNulls();
        assertThat(createdRows).extracting(DcpPartnerPicture::getIdxAvatarFailure).containsOnlyNulls();
        assertThat(createdRows).extracting(DcpPartnerPicture::getMboUploadStatus).containsOnlyNulls();
        assertThat(createdRows).extracting(DcpPartnerPicture::getUploadDate).doesNotContainNull();
        assertThat(createdRows).extracting(DcpPartnerPicture::getIsCwValidationOk).containsOnly(true);
        assertThat(createdRows).extracting(DcpPartnerPicture::getForceCwValidationOk).containsOnly(false);

        List<DcpPartnerPicture> oldPicByFastPipe = dcpPartnerPictureDao.fetchByIdxAvatarUrl(
                "idx_" + alreadyCreatedUrlByFastPipe);
        assertThat(oldPicByFastPipe).hasSize(1);
        assertThat(oldPicByFastPipe).extracting(DcpPartnerPicture::getGcSkuTicketId).containsOnly(1L);
        assertThat(oldPicByFastPipe).extracting(DcpPartnerPicture::getMboPicture).containsOnlyNulls();
        assertThat(oldPicByFastPipe).extracting(DcpPartnerPicture::getIsCwValidationOk).containsOnly(false);

        List<DcpPartnerPicture> oldPic = dcpPartnerPictureDao.fetchByIdxAvatarUrl("idx_" + alreadyCreatedUrl);
        assertThat(oldPic).hasSize(1);
        assertThat(oldPic).extracting(DcpPartnerPicture::getGcSkuTicketId).containsOnly(666L);
        assertThat(oldPic).extracting(DcpPartnerPicture::getMboPicture).doesNotContainNull();
    }

    @Test
    public void dontTouchPictureWithIdxAvatarFailure() {
        GcSkuTicket ticket1 = createDatacampTicket(1, Collections.singletonList("url-4"));
        String alreadyCreatedUrl = "url-4";
        List<GcSkuTicket> tickets = Collections.singletonList(ticket1);
        createRowInDcpPartnerPictureWithIdxError(alreadyCreatedUrl);

        ProcessDataBucketData dataBucket = new ProcessDataBucketData(555);

        List<DcpPartnerPicture> oldPicBefore = dcpPartnerPictureDao.fetchByIdxAvatarUrl("idx_" + alreadyCreatedUrl);
        assertThat(oldPicBefore).extracting(DcpPartnerPicture::getIdxAvatarFailure).doesNotContainNull();

        ProcessTaskResult<ProcessDataBucketData> result = taskAction.runOnTickets(tickets, dataBucket);

        List<DcpPartnerPicture> oldPicAfter = dcpPartnerPictureDao.fetchByIdxAvatarUrl("idx_" + alreadyCreatedUrl);
        assertThat(oldPicAfter).extracting(DcpPartnerPicture::getIdxAvatarFailure).doesNotContainNull();
    }

    @Test
    public void doesntFailOnEmptyInput() {
        ProcessDataBucketData dataBucket = new ProcessDataBucketData(555);
        ProcessTaskResult<ProcessDataBucketData> result = taskAction.runOnTickets(Collections.emptyList(), dataBucket);
        GutginAssertions.assertThat(result).doesntHaveProblems();
    }

    private void createRowInDcpPartnerPicture(String url) {
        Timestamp creationTs = Timestamp.from(Instant.now());
        DcpPartnerPicture dcpPartnerPicture =
                new DcpPartnerPicture(null,
                        "idx_" + url,
                        null,
                        null,
                        ModelStorage.Picture.getDefaultInstance(),
                        666L,
                        creationTs,
                        true,
                        true,
                        null);
        dcpPartnerPictureDao.saveData(Collections.singletonList(dcpPartnerPicture));
    }

    private void createRowInDcpPartnerPictureWithIdxError(String url) {
        DcpPartnerPicture dcpPartnerPicture = DcpPartnerPictureUtils.mboUploadPictureForFastCard(url);
        dcpPartnerPicture.setIdxAvatarFailure(IdxAvatarDownloadFailureCode.FAILED_DOWNLOAD);
        dcpPartnerPictureDao.saveData(Collections.singletonList(dcpPartnerPicture));
    }

    private void createRowInDcpPartnerPictureCreatedByFastPipe(String url) {
        DcpPartnerPicture dcpPartnerPicture = DcpPartnerPictureUtils.mboUploadPictureForFastCard(url);
        dcpPartnerPictureDao.saveData(Collections.singletonList(dcpPartnerPicture));
    }

    private GcSkuTicket createDatacampTicket(long id, List<String> url) {
        GcSkuTicket gcSkuTicket = new GcSkuTicket();
        DataCampOffer.Offer offer = new DcpOfferBuilder(
                1,
                "offerid")
                .withName("name")
                .withPictures(url)
                .build();
        gcSkuTicket.setDatacampOffer(offer);
        gcSkuTicket.setId(id);
        return gcSkuTicket;
    }
}
