package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.databucket_validations;

import Market.DataCamp.DataCampOfferPictures;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.DcpOfferUtils.initOffer;
import static ru.yandex.market.partner.content.common.utils.DcpPartnerPictureUtils.mboUploadPictureError;
import static ru.yandex.market.partner.content.common.utils.DcpPartnerPictureUtils.mboUploadPictureResult;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.dcp.DcpPartnerPictureDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.FakeDatacampOfferDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DcpPartnerPicture;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.entity.goodcontent.TicketValidationResult;
import ru.yandex.market.partner.content.common.message.MessageInfo;

public class DcpImageValidationTest extends DBDcpStateGenerator {

    private final DcpPartnerPictureDao dcpPartnerPictureDao = mock(DcpPartnerPictureDao.class);
    private DcpImageValidation dcpImageValidation;


    @Override
    @Before
    public void setUp() {
        super.setUp();
        FakeDatacampOfferDao fakeDatacampOfferDaoMock = Mockito.mock(FakeDatacampOfferDao.class);
        Mockito.when(fakeDatacampOfferDaoMock.getNonFake(Mockito.anyList()))
               .thenAnswer(invocation -> invocation.getArgument(0));
        this.dcpImageValidation = new DcpImageValidation(
            gcSkuValidationDao,
            gcSkuTicketDao,
            dcpPartnerPictureDao,
            mock(DcpImageValidation.CategoryAccess.class),
            fakeDatacampOfferDaoMock
        );
    }

    @Test
    public void noMainPicture() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1,
            offers -> initOffer(CATEGORY_ID, offers.get(0), offer -> {
            })
        );

        ProcessTaskResult<List<TicketValidationResult>> validate = dcpImageValidation.validate(gcSkuTickets);

        assertThat(validate.getResult().get(0).isValid()).isFalse();
        ImmutableList<MessageInfo> validationMessages = validate.getResult().get(0).getValidationMessages();
        assertThat(validationMessages).hasSize(1);
        assertThat(validationMessages.get(0).getCode())
            .isEqualTo("ir.partner_content.dcp.validation.image.missingMainImage");
        assertThat(validationMessages.get(0).getLevel())
            .isEqualTo(MessageInfo.Level.ERROR);
    }

    @Test
    public void hasOnlyMainPicture_valid() {
        List<DcpPartnerPicture> dcpPartnerPictures = Arrays.asList(
            mboUploadPictureResult("main_picture", true)
        );
        doReturn(dcpPartnerPictures).when(dcpPartnerPictureDao).fetchByIdxAvatarUrl(any());
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1,
            offers -> initOffer(CATEGORY_ID, offers.get(0), offer -> addPictureWithUrl(offer, "main_picture")
            ));

        ProcessTaskResult<List<TicketValidationResult>> validationResult = dcpImageValidation.validate(gcSkuTickets);

        assertThat(validationResult.getResult().get(0).isValid()).isTrue();
    }

    @Test
    public void hasOnlyMainPicture_notValid() {
        List<DcpPartnerPicture> dcpPartnerPictures = Arrays.asList(
            mboUploadPictureError("main_picture")
        );
        doReturn(dcpPartnerPictures).when(dcpPartnerPictureDao).fetchByIdxAvatarUrl(any());
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1,
            offers -> initOffer(CATEGORY_ID, offers.get(0), offer -> addPictureWithUrl(offer, "main_picture")
            ));

        ProcessTaskResult<List<TicketValidationResult>> validationResult = dcpImageValidation.validate(gcSkuTickets);

        TicketValidationResult ticketValidationResult = validationResult.getResult().get(0);
        assertThat(ticketValidationResult.isValid()).isFalse();
        assertThat(ticketValidationResult.getValidationMessages()).hasSize(1);
        assertThat(ticketValidationResult.getValidationMessages().get(0).getCode())
            .isEqualTo("ir.partner_content.dcp.validation.image.mboInvalidImageFormat");
        assertThat(ticketValidationResult.getValidationMessages().get(0).getLevel())
            .isEqualTo(MessageInfo.Level.ERROR);
    }

    @Test
    public void hasMultiplePictures_nonMainNotValid() {
        List<DcpPartnerPicture> dcpPartnerPictures = Arrays.asList(
            mboUploadPictureResult("main_picture", true),
            mboUploadPictureError("other_picture")
        );
        doReturn(dcpPartnerPictures).when(dcpPartnerPictureDao).fetchByIdxAvatarUrl(any());
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1,
            offers -> initOffer(CATEGORY_ID, offers.get(0), offer -> {
                    addPictureWithUrl(offer, "main_picture");
                    addPictureWithUrl(offer, "other_picture");
                }
            ));

        ProcessTaskResult<List<TicketValidationResult>> validationResult = dcpImageValidation.validate(gcSkuTickets);

        TicketValidationResult ticketValidationResult = validationResult.getResult().get(0);
        assertThat(ticketValidationResult.isValid()).isTrue();
        ImmutableList<MessageInfo> validationMessages = ticketValidationResult.getValidationMessages();
        assertThat(validationMessages).hasSize(1);
        assertThat(validationMessages.get(0).getParams().get("url")).isEqualTo("idx_other_picture");
        assertThat(validationMessages.get(0).getCode())
            .isEqualTo("ir.partner_content.dcp.validation.image.mboInvalidImageFormat");
        assertThat(validationMessages.get(0).getLevel())
                .isEqualTo(MessageInfo.Level.WARNING);
    }

    @Test
    public void hasMultiplePictures_mainNotValid() {
        List<DcpPartnerPicture> dcpPartnerPictures = Arrays.asList(
            mboUploadPictureError("main_picture"),
            mboUploadPictureResult("other_picture", true)
        );
        doReturn(dcpPartnerPictures).when(dcpPartnerPictureDao).fetchByIdxAvatarUrl(any());
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1,
            offers -> initOffer(CATEGORY_ID, offers.get(0), offer -> {
                    addPictureWithUrl(offer, "main_picture");
                    addPictureWithUrl(offer, "other_picture");
                }
            ));

        ProcessTaskResult<List<TicketValidationResult>> validationResult = dcpImageValidation.validate(gcSkuTickets);

        TicketValidationResult ticketValidationResult = validationResult.getResult().get(0);
        assertThat(ticketValidationResult.isValid()).isFalse();
        ImmutableList<MessageInfo> validationMessages = ticketValidationResult.getValidationMessages();
        assertThat(validationMessages).hasSize(1);
        assertThat(validationMessages.get(0).getParams().get("url")).isEqualTo("idx_main_picture");
        assertThat(validationMessages.get(0).getCode())
            .isEqualTo("ir.partner_content.dcp.validation.image.mboInvalidImageFormat");
        assertThat(validationMessages.get(0).getLevel())
            .isEqualTo(MessageInfo.Level.ERROR);
    }

    private void addPictureWithUrl(Market.DataCamp.DataCampOffer.Offer.Builder offer, String url) {
        String idxUrl = "idx_" + url;
        DataCampOfferPictures.PartnerPictures.Builder partner = offer.getPicturesBuilder().getPartnerBuilder();
        partner.getOriginalBuilder()
            .addSource(
                DataCampOfferPictures.SourcePicture.newBuilder()
                    .setUrl(url)
            );
        partner
            .putActual(
                url,
                DataCampOfferPictures.MarketPicture.newBuilder()
                    .setOriginal(DataCampOfferPictures.MarketPicture.Picture.newBuilder()
                        .setUrl(idxUrl))
                    .build()
            );
    }
}
