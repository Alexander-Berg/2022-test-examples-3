package ru.yandex.market.gutgin.tms.pipeline.dcp.taskaction.databucket;

import Market.DataCamp.DataCampOffer;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.partner.content.common.utils.DcpOfferBuilder;
import ru.yandex.market.partner.content.common.utils.DcpPartnerPictureUtils;
import ru.yandex.market.ir.autogeneration_api.http.service.ModelStorageServiceMock;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.dcp.DcpPartnerPictureDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.IdxAvatarDownloadFailureCode;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DcpPartnerPicture;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.ir.autogeneration.common.util.DcpOfferUtils.createDcpOfferWithPics;

/**
 * @author Nur-Magomed Dzhamiev <a href="mailto:n-mago@yandex-team.ru"></a>
 */
public class ImageUploaderTest extends DBDcpStateGenerator {

    private static final int LARGE_IMG_SIZE = 116_000_000;

    @Autowired
    private DcpPartnerPictureDao dcpPartnerPictureDao;

    @Mock
    private DcpPicturesFromModelCollector dcpPicturesFromModelCollector;

    @Mock
    private AvatarImageDownloader avatarImageDownloader;

    private ModelStorageServiceMock modelStorageServiceMock;
    private ImageUploader uploader;

    @Before
    public void setUp() {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        modelStorageServiceMock = new ModelStorageServiceMock();
        uploader = new ImageUploader(1, avatarImageDownloader, modelStorageServiceMock,
            Executors.newSingleThreadExecutor(),
            dcpPartnerPictureDao, dcpPicturesFromModelCollector);
    }

    @Test
    public void testWhenLargeImageThenValidationError() {
        String mboAvatarUrl = "avatars.mds.yandex.net/get-mpic/1961245/img_id8835968184976353325.jpeg/orig";
        String downloadedUrl = "some.com/downloaded/url";
        String fullMboAvatarUrl = "//" + mboAvatarUrl;
        String supplierUrl = "https://kupitcveti.com/uploads/store/product/91661319a170fcc1c9e3bafb9a7a8a7f.jpg";
        String idxUrl = "//avatars.mds.yandex.net/get-marketpic/1570741/market_ahovdeUyPyE_CkY_EpzbXA/orig";
        DataCampOffer.Offer offer = createDcpOfferWithPics(Arrays.asList(mboAvatarUrl, supplierUrl),
            ImmutableMap.of(mboAvatarUrl, "someurl", supplierUrl, idxUrl));
        GcSkuTicket skuTicket = makeTicket(1L, 1L, offer);
        List<String> problems = new ArrayList<>();

        Mockito.when(avatarImageDownloader.downloadImage(Mockito.any())).thenReturn(new AvatarImageDownloader.Result(
            new PictureContents(new byte[LARGE_IMG_SIZE], "", downloadedUrl), null
        ));
        mockImage(downloadedUrl);
        Mockito.when(dcpPicturesFromModelCollector.collectDcpPicturesFromPskus(
            eq(ImmutableMap.of(fullMboAvatarUrl, 1L)), eq(Collections.singletonList(skuTicket)))
        )
            .thenReturn(Collections.singletonList(new DcpPartnerPicture(
                null,
                fullMboAvatarUrl,
                null,
                null,
                ModelStorage.Picture.newBuilder().build(),
                1L,
                Timestamp.from(Instant.now()),
                true,
                true,
                null
            )));
        uploader.uploadForAllTickets(Collections.singletonList(skuTicket), problems);
        // mbo urls should not be downloaded
        Mockito.verify(avatarImageDownloader, Mockito.never()).downloadImage(fullMboAvatarUrl);
        DcpPartnerPicture dcpPartnerPicture = dcpPartnerPictureDao.fetchOneByIdxAvatarUrl(idxUrl);
        Assertions.assertThat(dcpPartnerPicture.getMboUploadStatus().getStatus())
                .isEqualTo(ModelStorage.OperationStatusType.VALIDATION_ERROR);
        // idx urls should be saved as usual
        Assertions.assertThat(dcpPartnerPicture).extracting(DcpPartnerPicture::getForceCwValidationOk).isEqualTo(false);
        DcpPartnerPicture dcpPartnerPictureFromMbo = dcpPartnerPictureDao.fetchOneByIdxAvatarUrl(fullMboAvatarUrl);
        // mbo avatars urls should be saved with force cw ok
        Assertions.assertThat(dcpPartnerPictureFromMbo).extracting(DcpPartnerPicture::getForceCwValidationOk).isEqualTo(true);
    }

    @Test
    public void uploadImageWithRecordCreatedByFastPipeline() {
        //быстрый пайплайн не загружает картинки в мбо, а только создает записи в dcp_partner_picture,
        //что бы их можно было использовать в других пайпах - их нужно загрузить и положить урлы в существующую запись
        DcpPartnerPicture pictureBefore = DcpPartnerPictureUtils.mboUploadPictureForFastCard("url-1");
        dcpPartnerPictureDao.saveData(Collections.singletonList(pictureBefore));

        List<GcSkuTicket> tickets = generateDBDcpInitialState(1, datacampOffers -> {
            DatacampOffer datacampOffer = datacampOffers.get(0);
            DataCampOffer.Offer offer = new DcpOfferBuilder(
                    datacampOffer.getBusinessId(),
                    datacampOffer.getOfferId())
                    .withName("name")
                    .withPictures(Arrays.asList("url-1", "url-2"))
                    .build();
            datacampOffer.setData(offer);
        });
        doReturn(createResult("url-1")).when(avatarImageDownloader).downloadImage(eq("idx_url-1"));
        doReturn(createResult("url-2")).when(avatarImageDownloader).downloadImage(eq("idx_url-2"));
        mockImage("url-1");
        mockImage("url-2");
        List<String> problems = new ArrayList<>();

        uploader.uploadForAllTickets(tickets, problems);

        List<DcpPartnerPicture> all = dcpPartnerPictureDao.findAll();
        assertThat(all).hasSize(2);
        assertThat(all).extracting(DcpPartnerPicture::getMboPicture).extracting(ModelStorage.Picture::getUrl)
                .containsOnly("mbo_url-2", "mbo_url-1");
        List<DcpPartnerPicture> picturesAfter =
                dcpPartnerPictureDao.fetchByIdxAvatarUrl(pictureBefore.getIdxAvatarUrl());
        assertThat(picturesAfter).hasSize(1);
        DcpPartnerPicture pictureAfter = picturesAfter.get(0);
        //проверяем, что старые параметры остались
        assertThat(pictureAfter).extracting(DcpPartnerPicture::getGcSkuTicketId)
                .isEqualTo(pictureBefore.getGcSkuTicketId());
        assertThat(pictureAfter).extracting(DcpPartnerPicture::getUploadDate)
                .isEqualTo(pictureBefore.getUploadDate());
        assertThat(pictureAfter).extracting(DcpPartnerPicture::getIsCwValidationOk)
                .isEqualTo(pictureBefore.getIsCwValidationOk());
        assertThat(pictureAfter).extracting(DcpPartnerPicture::getForceCwValidationOk)
                .isEqualTo(pictureBefore.getForceCwValidationOk());
    }

    @Test
    public void uploadImageWithRecordCreatedByFastPipelineAndIdxAvatarFailure() {
        //быстрый пайплайн не загружает картинки в мбо, а только создает записи в dcp_partner_picture,
        //что бы их можно было использовать в других пайпах - их нужно загрузить и положить урлы в существующую запись
        DcpPartnerPicture pictureBefore = DcpPartnerPictureUtils.mboUploadPictureForFastCard("url-1");
        dcpPartnerPictureDao.saveData(Collections.singletonList(pictureBefore));

        List<GcSkuTicket> tickets = generateDBDcpInitialState(1, datacampOffers -> {
            DatacampOffer datacampOffer = datacampOffers.get(0);
            DataCampOffer.Offer offer = new DcpOfferBuilder(
                    datacampOffer.getBusinessId(),
                    datacampOffer.getOfferId())
                    .withName("name")
                    .withPictures(Collections.singletonList("url-1"))
                    .build();
            datacampOffer.setData(offer);
        });
        doReturn(createIdxFailureResult("url-1")).when(avatarImageDownloader).downloadImage(eq("idx_url-1"));
        List<String> problems = new ArrayList<>();
        uploader.uploadForAllTickets(tickets, problems);

        List<DcpPartnerPicture> all = dcpPartnerPictureDao.findAll();
        assertThat(all).hasSize(1);
        DcpPartnerPicture pictureWithIdxAvatarFailure = all.get(0);
        //статус сменился null -> FAILED_DOWNLOAD
        assertThat(pictureWithIdxAvatarFailure).extracting(DcpPartnerPicture::getIdxAvatarFailure)
                .isEqualTo(IdxAvatarDownloadFailureCode.FAILED_DOWNLOAD);
        //проверяем, что старые параметры остались
        assertThat(pictureWithIdxAvatarFailure).extracting(DcpPartnerPicture::getGcSkuTicketId)
                .isEqualTo(pictureBefore.getGcSkuTicketId());
        assertThat(pictureWithIdxAvatarFailure).extracting(DcpPartnerPicture::getUploadDate)
                .isEqualTo(pictureBefore.getUploadDate());
    }

    @Test
    public void dontUploadImagesWithSuccessOrValidationFailure() {
        DcpPartnerPicture pictureBeforeSuccess = DcpPartnerPictureUtils.mboUploadPictureError("url-1");
        DcpPartnerPicture pictureBeforeValidationFail = DcpPartnerPictureUtils.mboUploadPictureResult("url-2", true);
        dcpPartnerPictureDao.saveData(Arrays.asList(pictureBeforeSuccess, pictureBeforeValidationFail));

        List<GcSkuTicket> tickets = generateDBDcpInitialState(1, datacampOffers -> {
            DatacampOffer datacampOffer = datacampOffers.get(0);
            DataCampOffer.Offer offer = new DcpOfferBuilder(
                    datacampOffer.getBusinessId(),
                    datacampOffer.getOfferId())
                    .withName("name")
                    .withPictures(Arrays.asList("url-1", "url-2"))
                    .build();
            datacampOffer.setData(offer);
        });

        List<String> problems = new ArrayList<>();
        uploader.uploadForAllTickets(tickets, problems);

        Mockito.verifyNoInteractions(avatarImageDownloader);
    }

    private void mockImage(String url) {
        modelStorageServiceMock.putPicture(url, ModelStorage.Picture.newBuilder().setUrl("mbo_" + url).build(),
                ModelStorage.OperationStatus.newBuilder()
                        .setStatus(ModelStorage.OperationStatusType.OK)
                        .setType(ModelStorage.OperationType.UPLOAD_IMAGE)
                        .build());
    }

    private AvatarImageDownloader.Result createResult(String url) {
        return new AvatarImageDownloader.Result(
                new PictureContents(new byte[0], "", url), null);
    }

    private AvatarImageDownloader.Result createIdxFailureResult(String url) {
        return new AvatarImageDownloader.Result(null, IdxAvatarDownloadFailureCode.FAILED_DOWNLOAD);
    }

    private static GcSkuTicket makeTicket(long id, Long existingPskuId, DataCampOffer.Offer offer) {
        GcSkuTicket skuTicket = new GcSkuTicket();
        skuTicket.setId(id);
        skuTicket.setExistingMboPskuId(existingPskuId);
        skuTicket.setDatacampOffer(offer);
        return skuTicket;
    }

}
