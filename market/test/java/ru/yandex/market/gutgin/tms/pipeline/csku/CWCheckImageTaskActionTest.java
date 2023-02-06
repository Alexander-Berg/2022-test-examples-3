package ru.yandex.market.gutgin.tms.pipeline.csku;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.acw.api.AcwApiService;
import ru.yandex.market.acw.api.CheckImageVerdicts;
import ru.yandex.market.acw.api.CheckImagesRequest;
import ru.yandex.market.acw.api.CheckImagesResponse;
import ru.yandex.market.acw.api.CheckTextsRequest;
import ru.yandex.market.acw.api.CheckTextsResponse;
import ru.yandex.market.acw.api.Image;
import ru.yandex.market.acw.api.OverrideImageVerdictsRequest;
import ru.yandex.market.acw.api.OverrideImageVerdictsResponse;
import ru.yandex.market.acw.api.RequestMode;
import ru.yandex.market.gutgin.tms.manager.cleanweb.CleanWebSkipper;
import ru.yandex.market.gutgin.tms.service.CwResultCorrectionService;
import ru.yandex.market.gutgin.tms.service.GutginResourcesReader;
import ru.yandex.market.gutgin.tms.service.datacamp.savemodels.update.MboPictureService;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.CwSkippedTicketDao;
import ru.yandex.market.partner.content.common.db.dao.CwStatsDao;
import ru.yandex.market.partner.content.common.db.dao.ProtocolMessageDao;
import ru.yandex.market.partner.content.common.db.dao.SkipCwDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcCleanWebImageValidationDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcValidationMessageDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketType;
import ru.yandex.market.partner.content.common.db.jooq.enums.MessageLevel;
import ru.yandex.market.partner.content.common.db.jooq.tables.daos.ApplicationPropertyDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.daos.DcpPartnerPictureCwStatusDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DcpPartnerPicture;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DcpPartnerPictureCwStatus;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuValidation;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.ProtocolMessage;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.service.ApplicationPropertyService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.partner.content.common.utils.DcpPartnerPictureUtils.mboUploadPictureError;
import static ru.yandex.market.partner.content.common.utils.DcpPartnerPictureUtils.mboUploadPictureForFastCard;
import static ru.yandex.market.partner.content.common.utils.DcpPartnerPictureUtils.mboUploadPictureResult;

public class CWCheckImageTaskActionTest extends DBDcpStateGenerator {


    private static final long autoPartsCategory = 5000L;
    private static final long intimFlowCategory = 5100L;
    private static final long fashionCategory = 5200L;
    private static final long shoesCategory = 5300L;
    private static final long accessoriesCategory = 5400L;
    private static final long accessoriesJewelryCategory = 5500L;
    private static final long ignoredEroticaCategory = 5600L;
    @Autowired
    ProtocolMessageDao protocolMessageDao;
    @Autowired
    GcValidationMessageDao validationMessageDao;
    @Autowired
    SkipCwDao skipCwDao;
    @Autowired
    CwSkippedTicketDao cwSkippedTicketsDao;
    @Autowired
    CwStatsDao cwStatsDao;
    @Autowired
    ApplicationPropertyDao applicationPropertyDao;
    @Autowired
    GcCleanWebImageValidationDao gcCleanWebImageValidationDao;

    DcpPartnerPictureCwStatusDao dcpPartnerPictureCwStatusDao;

    ApplicationPropertyService applicationPropertyService;
    MboPictureService mboPictureService;
    AcwApiService acwApiService;
    CleanWebSkipper cleanWebSkipper;
    CWCheckImageTaskAction cwCheckImageTaskAction;
    CwResultCorrectionService cwResultCorrectionService;

    @Override
    public void setUp() {
        super.setUp();

        dcpPartnerPictureCwStatusDao = new DcpPartnerPictureCwStatusDao(configuration);
        acwApiService = new AcwApiServiceMock();

        applicationPropertyService = new ApplicationPropertyService(new GutginResourcesReader(),
                applicationPropertyDao);

        cwResultCorrectionService = new CwResultCorrectionService(Set.of(ignoredEroticaCategory), Set.of(),
                Set.of(intimFlowCategory), Set.of(fashionCategory), Set.of(shoesCategory),
                Set.of(accessoriesCategory),
                Set.of(accessoriesJewelryCategory), true);

        mboPictureService = new MboPictureService(dcpPartnerPictureDao, gcCleanWebImageValidationDao,  skipCwDao,
                Set.of(), true, Set.of(autoPartsCategory), cwResultCorrectionService);

        cleanWebSkipper = new CleanWebSkipper(cwResultCorrectionService,
                cwSkippedTicketsDao, cwStatsDao, applicationPropertyService,
                dcpPartnerPictureDao, new HashSet<>());

        cwCheckImageTaskAction = new CWCheckImageTaskAction(
                gcSkuTicketDao, gcSkuValidationDao,
                acwApiService, dcpPartnerPictureDao,
                protocolMessageDao, validationMessageDao,
                mboPictureService, cleanWebSkipper,
                cwStatsDao,
                Set.of(intimFlowCategory),
                Set.of(fashionCategory),
                Set.of(shoesCategory),
                Set.of(accessoriesCategory),
                Set.of(accessoriesJewelryCategory),
                Set.of(ignoredEroticaCategory),
                Set.of(autoPartsCategory)
        );
    }

    @Test
    public void happyPath() {
        Long ticketId = generateDBDcpInitialStateNew(state -> {
            state.getDcpOfferBuilder().withPictures(
                    "url-main",
                    "url-other",
                    "url-other-with-error",
                    "url-other-2"
            );
        }).getId();
        List<DcpPartnerPicture> mboResults = Arrays.asList(
                mboUploadPictureResult("url-main", true),
                mboUploadPictureResult("url-other", true),
                mboUploadPictureError("url-other-with-error"),
                mboUploadPictureResult("url-other-2", true)
        );
        dcpPartnerPictureDao.saveData(mboResults);

        GcSkuTicket ticket = gcSkuTicketDao.fetchOneById(ticketId);
        ticket.setType(GcSkuTicketType.CSKU);
        gcSkuTicketDao.update(ticket);

        cwCheckImageTaskAction.runOnTickets(List.of(ticket), new ProcessDataBucketData(ticket.getDataBucketId()));

        List<GcSkuValidation> validationList = gcSkuValidationDao.findAll();

        assertThat(validationList.size()).isEqualTo(1);
        assertThat(validationList.get(0).getIsOk()).isEqualTo(true);

        List<DcpPartnerPicture> dcpPartnerPictureList = dcpPartnerPictureDao.findAll();
        Map<String, Long> urlToPictureId = dcpPartnerPictureList.stream()
                .collect(Collectors.toMap(DcpPartnerPicture::getIdxAvatarUrl, DcpPartnerPicture::getId));
        Map<Long, DcpPartnerPictureCwStatus> pictureCwStatus = dcpPartnerPictureCwStatusDao.findAll()
                .stream().collect(Collectors.toMap(DcpPartnerPictureCwStatus::getPictureId, Function.identity()));

        assertThat(pictureCwStatus.get(urlToPictureId.get("idx_url-main")).getStatus())
                .isTrue();
        assertThat(pictureCwStatus.get(urlToPictureId.get("idx_url-main")).getRequestMode())
                .isEqualTo("");

        assertThat(pictureCwStatus.get(urlToPictureId.get("idx_url-other")).getStatus())
                .isFalse();
        assertThat(pictureCwStatus.get(urlToPictureId.get("idx_url-other")).getRequestMode())
                .isEqualTo("");

        assertThat(pictureCwStatus.get(urlToPictureId.get("idx_url-other-2")).getStatus())
                .isFalse();
        assertThat(pictureCwStatus.get(urlToPictureId.get("idx_url-other-2")).getRequestMode())
                .isEqualTo("");

        List<ProtocolMessage> protocolMessageList = protocolMessageDao.findAll();

        assertThat(protocolMessageList).hasSize(2);
        assertThat(protocolMessageList).allMatch(protocolMessage -> "ir.partner_content.goodcontent.validation.cw.image.watermark"
                .equals(protocolMessage.getCode()) && MessageLevel.WARNING.equals(protocolMessage.getLevel()));
    }

    @Test
    public void fashionGood() {
        String urlFashionMain = "url-fashion-main";
        String idxUrlFashionMain = "idx_" + urlFashionMain;

        Long ticketId = generateDBDcpInitialStateNew(state -> {
            state.getDcpOfferBuilder()
                    .withCategory(Math.toIntExact(fashionCategory))
                    .withPictures(
                            urlFashionMain
            );
        }).getId();
        List<DcpPartnerPicture> mboResults = Arrays.asList(
                mboUploadPictureResult(urlFashionMain, true)
        );
        dcpPartnerPictureDao.saveData(mboResults);

        GcSkuTicket ticket = gcSkuTicketDao.fetchOneById(ticketId);
        ticket.setType(GcSkuTicketType.CSKU);
        ticket.setCategoryId(fashionCategory);
        gcSkuTicketDao.update(ticket);

        cwCheckImageTaskAction.runOnTickets(List.of(ticket), new ProcessDataBucketData(ticket.getDataBucketId()));

        List<GcSkuValidation> validationList = gcSkuValidationDao.findAll();

        assertThat(validationList.size()).isEqualTo(1);
        assertThat(validationList.get(0).getIsOk()).isEqualTo(true);

        List<DcpPartnerPicture> dcpPartnerPictureList = dcpPartnerPictureDao.findAll();
        Map<String, Long> urlToPictureId = dcpPartnerPictureList.stream()
                .collect(Collectors.toMap(DcpPartnerPicture::getIdxAvatarUrl, DcpPartnerPicture::getId));
        Map<Long, DcpPartnerPictureCwStatus> pictureCwStatus = dcpPartnerPictureCwStatusDao.findAll()
                .stream().collect(Collectors.toMap(DcpPartnerPictureCwStatus::getPictureId, Function.identity()));

        assertThat(pictureCwStatus.get(urlToPictureId.get(idxUrlFashionMain)).getStatus())
                .isTrue();
        assertThat(pictureCwStatus.get(urlToPictureId.get(idxUrlFashionMain)).getRequestMode())
                .isEqualTo("moderation=fashion");

        List<ProtocolMessage> protocolMessageList = protocolMessageDao.findAll();

        assertThat(protocolMessageList).hasSize(0);
    }

    @Test
    public void shoesBad() {
        String urlShoesMain = "url-shoes-main";
        String idxUrlShoesMain = "idx_" + urlShoesMain;

        Long ticketId = generateDBDcpInitialStateNew(state -> {
            state.getDcpOfferBuilder()
                    .withCategory(Math.toIntExact(shoesCategory))
                    .withPictures(
                            urlShoesMain
                    );
        }).getId();
        List<DcpPartnerPicture> mboResults = Arrays.asList(
                mboUploadPictureResult(urlShoesMain, true)
        );
        dcpPartnerPictureDao.saveData(mboResults);

        GcSkuTicket ticket = gcSkuTicketDao.fetchOneById(ticketId);
        ticket.setType(GcSkuTicketType.CSKU);
        ticket.setCategoryId(shoesCategory);
        gcSkuTicketDao.update(ticket);

        cwCheckImageTaskAction.runOnTickets(List.of(ticket), new ProcessDataBucketData(ticket.getDataBucketId()));

        List<GcSkuValidation> validationList = gcSkuValidationDao.findAll();

        assertThat(validationList.size()).isEqualTo(1);
        assertThat(validationList.get(0).getIsOk()).isFalse();

        List<DcpPartnerPicture> dcpPartnerPictureList = dcpPartnerPictureDao.findAll();
        Map<String, Long> urlToPictureId = dcpPartnerPictureList.stream()
                .collect(Collectors.toMap(DcpPartnerPicture::getIdxAvatarUrl, DcpPartnerPicture::getId));
        Map<Long, DcpPartnerPictureCwStatus> pictureCwStatus = dcpPartnerPictureCwStatusDao.findAll()
                .stream().collect(Collectors.toMap(DcpPartnerPictureCwStatus::getPictureId, Function.identity()));

        assertThat(pictureCwStatus.get(urlToPictureId.get(idxUrlShoesMain)).getStatus())
                .isFalse();
        assertThat(pictureCwStatus.get(urlToPictureId.get(idxUrlShoesMain)).getRequestMode())
                .isEqualTo("moderation=shoes");

        List<ProtocolMessage> protocolMessageList = protocolMessageDao.findAll();

        assertThat(protocolMessageList).hasSize(1);
        assertThat(protocolMessageList).allMatch(protocolMessage ->
                "ir.partner_content.goodcontent.validation.cw.image.shoes.background_white"
                        .equals(protocolMessage.getCode()));
    }

    @Test
    public void intimGood() {
        String urlIntimMain = "url-intim-main";
        String urlIntimSecond = "url-intim-second";
        String idxUrlIntimMain = "idx_" + urlIntimMain;
        String idxUrlIntimSecond = "idx_" + urlIntimSecond;

        Long ticketId = generateDBDcpInitialStateNew(state -> {
            state.getDcpOfferBuilder()
                    .withCategory(Math.toIntExact(intimFlowCategory))
                    .withPictures(
                            urlIntimMain,
                            urlIntimSecond
                    );
        }).getId();
        List<DcpPartnerPicture> mboResults = Arrays.asList(
                mboUploadPictureResult(urlIntimMain, true),
                mboUploadPictureResult(urlIntimSecond, true)
        );
        dcpPartnerPictureDao.saveData(mboResults);

        GcSkuTicket ticket = gcSkuTicketDao.fetchOneById(ticketId);
        ticket.setType(GcSkuTicketType.CSKU);
        ticket.setCategoryId(intimFlowCategory);
        gcSkuTicketDao.update(ticket);

        cwCheckImageTaskAction.runOnTickets(List.of(ticket), new ProcessDataBucketData(ticket.getDataBucketId()));

        List<GcSkuValidation> validationList = gcSkuValidationDao.findAll();

        assertThat(validationList.size()).isEqualTo(1);
        assertThat(validationList.get(0).getIsOk()).isTrue();

        List<DcpPartnerPicture> dcpPartnerPictureList = dcpPartnerPictureDao.findAll();
        Map<String, Long> urlToPictureId = dcpPartnerPictureList.stream()
                .collect(Collectors.toMap(DcpPartnerPicture::getIdxAvatarUrl, DcpPartnerPicture::getId));
        Map<Long, DcpPartnerPictureCwStatus> pictureCwStatus = dcpPartnerPictureCwStatusDao.findAll()
                .stream().collect(Collectors.toMap(DcpPartnerPictureCwStatus::getPictureId, Function.identity()));

        assertThat(pictureCwStatus.get(urlToPictureId.get(idxUrlIntimMain)).getStatus())
                .isTrue();
        assertThat(pictureCwStatus.get(urlToPictureId.get(idxUrlIntimMain)).getRequestMode())
                .isEqualTo("moderation=intim");

        assertThat(pictureCwStatus.get(urlToPictureId.get(idxUrlIntimSecond)).getStatus())
                .isTrue();
        assertThat(pictureCwStatus.get(urlToPictureId.get(idxUrlIntimSecond)).getRequestMode())
                .isEqualTo("moderation=intim");

        List<ProtocolMessage> protocolMessageList = protocolMessageDao.findAll();

        assertThat(protocolMessageList).hasSize(0);
    }

    @Test
    public void intimBad() {
        String urlIntimMain = "url-intim-main-bad";
        String idxUrlIntimMain = "idx_" + urlIntimMain;

        Long ticketId = generateDBDcpInitialStateNew(state -> {
            state.getDcpOfferBuilder()
                    .withCategory(Math.toIntExact(intimFlowCategory))
                    .withPictures(
                            urlIntimMain
                    );
        }).getId();
        List<DcpPartnerPicture> mboResults = Arrays.asList(
                mboUploadPictureResult(urlIntimMain, true)
        );
        dcpPartnerPictureDao.saveData(mboResults);

        GcSkuTicket ticket = gcSkuTicketDao.fetchOneById(ticketId);
        ticket.setType(GcSkuTicketType.CSKU);
        ticket.setCategoryId(intimFlowCategory);
        gcSkuTicketDao.update(ticket);

        cwCheckImageTaskAction.runOnTickets(List.of(ticket), new ProcessDataBucketData(ticket.getDataBucketId()));

        List<GcSkuValidation> validationList = gcSkuValidationDao.findAll();

        assertThat(validationList.size()).isEqualTo(1);
        assertThat(validationList.get(0).getIsOk()).isFalse();

        List<DcpPartnerPicture> dcpPartnerPictureList = dcpPartnerPictureDao.findAll();
        Map<String, Long> urlToPictureId = dcpPartnerPictureList.stream()
                .collect(Collectors.toMap(DcpPartnerPicture::getIdxAvatarUrl, DcpPartnerPicture::getId));
        Map<Long, DcpPartnerPictureCwStatus> pictureCwStatus = dcpPartnerPictureCwStatusDao.findAll()
                .stream().collect(Collectors.toMap(DcpPartnerPictureCwStatus::getPictureId, Function.identity()));

        assertThat(pictureCwStatus.get(urlToPictureId.get(idxUrlIntimMain)).getStatus())
                .isFalse();
        assertThat(pictureCwStatus.get(urlToPictureId.get(idxUrlIntimMain)).getRequestMode())
                .isEqualTo("moderation=intim");

        List<ProtocolMessage> protocolMessageList = protocolMessageDao.findAll();

        assertThat(protocolMessageList).hasSize(1);

        assertThat(protocolMessageList).allMatch(protocolMessage ->
                "ir.partner_content.goodcontent.validation.cw.image.explicit".equals(protocolMessage.getCode()));
    }

    @Test
    public void whenNonMainImageNotOkForFastCardThenFail() {
        Long ticketId = generateDBDcpInitialStateNew(state -> {
            state.getDcpOfferBuilder().withPictures(
                    "url-main",
                    "url-other",
                    "url-other-2"
            );
        }).getId();
        List<DcpPartnerPicture> mboResults = Arrays.asList(
                mboUploadPictureForFastCard("url-main"),
                mboUploadPictureForFastCard("url-other"),
                mboUploadPictureForFastCard("url-other-2")
        );
        dcpPartnerPictureDao.saveData(mboResults);

        GcSkuTicket ticket = gcSkuTicketDao.fetchOneById(ticketId);
        ticket.setType(GcSkuTicketType.FAST_CARD);
        gcSkuTicketDao.update(ticket);

        cwCheckImageTaskAction.runOnTickets(List.of(ticket), new ProcessDataBucketData(ticket.getDataBucketId()));

        List<GcSkuValidation> validationList = gcSkuValidationDao.findAll();

        assertThat(validationList.size()).isEqualTo(1);
        assertThat(validationList.get(0).getIsOk()).isFalse();

        List<DcpPartnerPicture> dcpPartnerPictureList = dcpPartnerPictureDao.findAll();

        List<ProtocolMessage> protocolMessageList = protocolMessageDao.findAll();

        assertThat(protocolMessageList).hasSize(2);
        assertThat(protocolMessageList).allMatch(protocolMessage ->
                "ir.partner_content.goodcontent.validation.cw.image.watermark"
                .equals(protocolMessage.getCode()) && MessageLevel.ERROR.equals(protocolMessage.getLevel()));
    }


    public static class AcwApiServiceMock implements AcwApiService {

        public static Map<String, CheckImageVerdicts> ANSWER = new HashMap<>();

        static {
            ANSWER.put("idx_url-main", CheckImageVerdicts.newBuilder()
                    .addAllVerdicts(Set.of(Image.ImageVerdict.CP_AUTO_OTHER))
                    .setIdxUrl("idx_url-main")
                    .setRequestMode(RequestMode.DEFAULT)
                    .buildPartial());

            ANSWER.put("idx_url-other", CheckImageVerdicts.newBuilder()
                    .addAllVerdicts(Set.of(Image.ImageVerdict.HAS_WATERMARK))
                    .setIdxUrl("idx_url-other")
                    .setRequestMode(RequestMode.DEFAULT)
                    .buildPartial());

            ANSWER.put("idx_url-other-2", CheckImageVerdicts.newBuilder()
                    .addAllVerdicts(Set.of(Image.ImageVerdict.HAS_WATERMARK))
                    .setIdxUrl("idx_url-other-2")
                    .setRequestMode(RequestMode.DEFAULT)
                    .buildPartial());

            ANSWER.put("url-other-with-error", CheckImageVerdicts.newBuilder()
                    .addAllVerdicts(Set.of(Image.ImageVerdict.HAS_TEXT))
                    .setIdxUrl("url-other-with-error")
                    .setRequestMode(RequestMode.DEFAULT)
                    .buildPartial());

            ANSWER.put("idx_url-fashion-main", CheckImageVerdicts.newBuilder()
                    .addAllVerdicts(Set.of(Image.ImageVerdict.FASHION_BACKGROUND_GOOD))
                    .setIdxUrl("idx_url-fashion-main")
                    .setRequestMode(RequestMode.FASHION)
                    .buildPartial());

            ANSWER.put("idx_url-shoes-main", CheckImageVerdicts.newBuilder()
                    .addAllVerdicts(Set.of(Image.ImageVerdict.SHOES_BACKGROUND_WHITE))
                    .setIdxUrl("idx_url-shoes-main")
                    .setRequestMode(RequestMode.SHOES)
                    .buildPartial());

            ANSWER.put("idx_url-intim-main", CheckImageVerdicts.newBuilder()
                    .addAllVerdicts(Set.of(Image.ImageVerdict.CP_YANG_OTHER, Image.ImageVerdict.CP_AUTO_OTHER))
                    .setIdxUrl("idx_url-intim-main")
                    .setRequestMode(RequestMode.INTIM)
                    .buildPartial());

            ANSWER.put("idx_url-intim-second", CheckImageVerdicts.newBuilder()
                    .addAllVerdicts(Set.of(Image.ImageVerdict.CP_YANG_OTHER, Image.ImageVerdict.CP_AUTO_OTHER))
                    .setIdxUrl("idx_url-intim-second")
                    .setRequestMode(RequestMode.INTIM)
                    .buildPartial());

            ANSWER.put("idx_url-intim-main-bad", CheckImageVerdicts.newBuilder()
                    .addAllVerdicts(Set.of(Image.ImageVerdict.CP_YANG_CP))
                    .setIdxUrl("idx_url-intim-main-bad")
                    .setRequestMode(RequestMode.INTIM)
                    .buildPartial());
        }

        @Override
        public CheckImagesResponse checkImage(CheckImagesRequest request) {
            CheckImagesResponse.Builder responseBuilder = CheckImagesResponse.newBuilder();

            request.getImagesList().forEach(checkImageParameters -> {
                if (!ANSWER.containsKey(checkImageParameters.getIdxUrl())) {
                    throw new IllegalArgumentException("Add new url to AcwApiServiceMock: " +
                            checkImageParameters.getIdxUrl());
                }
                responseBuilder.addImageVerdicts(ANSWER.get(checkImageParameters.getIdxUrl()));
            });

            return responseBuilder
                    .build();
        }

        @Override
        public CheckTextsResponse checkText(CheckTextsRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OverrideImageVerdictsResponse overrideImageVerdicts(OverrideImageVerdictsRequest request) {
            throw new UnsupportedOperationException();
        }
    }

}
