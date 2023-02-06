package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferMeta;
import org.junit.Test;

import ru.yandex.market.gutgin.tms.utils.CategoryDataHelperMock;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.csku.judge.MarketParameterValueWrapper;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.entity.goodcontent.FailData;
import ru.yandex.market.partner.content.common.entity.goodcontent.ParamInfo;
import ru.yandex.market.partner.content.common.message.MessageInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.DcpOfferUtils.addStrParam;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.DcpOfferUtils.initOffer;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.VENDOR;

public class InvalidCharsValidationTest extends DBDcpStateGenerator {

    @Test
    public void invalidCharsAreNotAllowed() {
        final int paramId = 1;
        final String paramName = "xxx";
        String value = "Фрипсы Банан 🍌";

        MessageReporter messageReporter = new MessageReporter(null);

        InvalidCharsValidation invalidCharsValidation = new InvalidCharsValidation(gcSkuValidationDao,
                gcSkuTicketDao, new CategoryDataHelperMock());
        invalidCharsValidation.checkInvalidChars(value, messageReporter, paramName, paramId,
                MarketParameterValueWrapper.MarketParameterValueWrapperType.OFFER);

        assertThat(messageReporter.getMessages())
                .hasSize(1)
                .extracting(MessageInfo::getCode).containsExactly("ir.partner_content.dcp.validation.invalid_chars");

        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNotNull();
        assertThat(failData.getParams()).contains(new ParamInfo((long)paramId, paramName,
                false));
    }

    @Test
    public void validCharsAndNumericsAreAllowed() {
        final int paramId = 1;
        final String paramName = "xxx";

        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(2, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                addStrParam(paramId, paramName, "-10.567e1", builder);
            });
            initOffer(CATEGORY_ID, offers.get(1), builder -> {
                addStrParam(paramId, paramName, "Растворитель !@#$%^ ржавчины #с керамикой", builder);
            });
        });

        MessageReporter messageReporter = new MessageReporter(null);

        InvalidCharsValidation invalidCharsValidation = new InvalidCharsValidation(gcSkuValidationDao, gcSkuTicketDao,
                new CategoryDataHelperMock());
        gcSkuTickets.forEach(t -> invalidCharsValidation.validateTicket(t, messageReporter, Set.of()));

        assertThat(messageReporter.getMessages()).isEmpty();

        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNull();
    }

    @Test
    public void tabAndNlAreAllowed() {
        final int paramId = 1;
        final String paramName = "xxx";

        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                addStrParam(paramId, paramName, "Растворитель \nржавчины \tс керамикой", builder);
            });
        });

        MessageReporter messageReporter = new MessageReporter(null);

        InvalidCharsValidation invalidCharsValidation = new InvalidCharsValidation(gcSkuValidationDao, gcSkuTicketDao,
                new CategoryDataHelperMock());
        gcSkuTickets.forEach(t -> invalidCharsValidation.validateTicket(t, messageReporter, Set.of()));

        assertThat(messageReporter.getMessages()).isEmpty();

        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNull();
    }

    @Test
    public void invalidCharsInVendorAreNotAllowed() {
        String value = "Фрипсы Банан 🍌";

        MessageReporter messageReporter = new MessageReporter(null);


        InvalidCharsValidation invalidCharsValidation = new InvalidCharsValidation(gcSkuValidationDao,
                gcSkuTicketDao, new CategoryDataHelperMock());
        Set<Long> invalidParameters = new HashSet<>();

        DataCampOffer.Offer.Builder offerBuilder = DataCampOffer.Offer.newBuilder();
        offerBuilder.getContentBuilder().getPartnerBuilder().setActual(DataCampOfferContent.ProcessedSpecification.newBuilder()
                .setVendor(DataCampOfferMeta.StringValue.newBuilder().setValue(value).build())
                .build());
        GcSkuTicket ticket = new GcSkuTicket();
        ticket.setCategoryId(1L);
        ticket.setDatacampOffer(offerBuilder.build());
        invalidCharsValidation.validateTicket(ticket, messageReporter, invalidParameters);

        assertThat(messageReporter.getMessages())
                .hasSize(1)
                .extracting(MessageInfo::getCode).containsExactly("ir.partner_content.dcp.validation.invalid_chars");

        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNotNull();
        assertThat(failData.getParams()).contains(new ParamInfo(VENDOR.getId(), "Бренд",
                false));
    }
}
