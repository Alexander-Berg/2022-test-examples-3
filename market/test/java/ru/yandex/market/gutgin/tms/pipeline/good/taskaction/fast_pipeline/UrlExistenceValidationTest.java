package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.fast_pipeline;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.MessageReporter;
import ru.yandex.market.gutgin.tms.service.datacamp.savemodels.update.MboPictureService;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.service.DataCampOfferBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus.VALIDATION_STARTED;

public class UrlExistenceValidationTest extends DBDcpStateGenerator {

    private UrlExistenceValidation urlExistenceValidation;
    private GcSkuTicket AUTO_PARTS_CATEGORY_TICKET;
    private GcSkuTicket NOT_AUTO_PARTS_CATEGORY_TICKET;
    private GcSkuTicket NOT_AUTO_PARTS_CATEGORY_TICKET_NO_URL;

    @Before
    public void setUp() {
        super.setUp();
        AUTO_PARTS_CATEGORY_TICKET = generateTickets(1, offersSettings(false),
                VALIDATION_STARTED).get(0);

        NOT_AUTO_PARTS_CATEGORY_TICKET = generateTickets(1, offersSettings(false),
                VALIDATION_STARTED).get(0);

        NOT_AUTO_PARTS_CATEGORY_TICKET_NO_URL = generateTickets(1, offersSettings(true),
                VALIDATION_STARTED).get(0);

        MboPictureService mboPictureService = mock(MboPictureService.class);
        when(mboPictureService.hasValidUrl(AUTO_PARTS_CATEGORY_TICKET)).thenReturn(true);
        when(mboPictureService.hasValidUrl(NOT_AUTO_PARTS_CATEGORY_TICKET)).thenReturn(true);
        when(mboPictureService.hasValidUrl(NOT_AUTO_PARTS_CATEGORY_TICKET_NO_URL)).thenReturn(false);
        urlExistenceValidation = new UrlExistenceValidation(gcSkuValidationDao, gcSkuTicketDao, mboPictureService);
    }

    @Test
    public void offersWithUrlProcessedWithoutErrorsTest() {
        GcSkuTicket gcSkuTicket = NOT_AUTO_PARTS_CATEGORY_TICKET;
        MessageReporter messageReporter = new MessageReporter(gcSkuTicket.getShopSku());
        urlExistenceValidation.validateTicket(gcSkuTicket, messageReporter, Set.of());
        List<MessageInfo> messages = messageReporter.getMessages();
        assertThat(messages).isEmpty();
    }

    @Test
    public void offersWithoutUrlProcessedWithErrorsTest() {
        GcSkuTicket gcSkuTicket = NOT_AUTO_PARTS_CATEGORY_TICKET_NO_URL;
        MessageReporter messageReporter = new MessageReporter(gcSkuTicket.getShopSku());
        urlExistenceValidation.validateTicket(gcSkuTicket, messageReporter, Set.of());
        List<MessageInfo> messages = messageReporter.getMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages.stream()
                .allMatch(message -> message.getCode().equals("ir.partner_content.error.missingImageUrl")))
                .isTrue();
    }

    @Test
    public void autoPartsOffersWithoutUrlProcessedWithoutErrorsTest() {
        GcSkuTicket gcSkuTicket = AUTO_PARTS_CATEGORY_TICKET;
        MessageReporter messageReporter = new MessageReporter(gcSkuTicket.getShopSku());
        urlExistenceValidation.validateTicket(gcSkuTicket, messageReporter, Set.of());
        List<MessageInfo> messages = messageReporter.getMessages();
        assertThat(messages).isEmpty();
    }

    @Test
    public void notAutoPartsOffersWithUrlProcessedWithoutErrorsTest() {
        GcSkuTicket gcSkuTicket = NOT_AUTO_PARTS_CATEGORY_TICKET;
        MessageReporter messageReporter = new MessageReporter(gcSkuTicket.getShopSku());
        urlExistenceValidation.validateTicket(gcSkuTicket, messageReporter, Set.of());
        List<MessageInfo> messages = messageReporter.getMessages();
        assertThat(messages).isEmpty();
    }

    private Consumer<DatacampOffer> offersSettings(boolean hasUrl) {
        return datacampOffer -> {
            DataCampOfferBuilder builder = new DataCampOfferBuilder(
                    datacampOffer.getCreateTime(),
                    datacampOffer.getBusinessId(),
                    CATEGORY_ID,
                    datacampOffer.getOfferId())
                    .withActualNameAndTitle("test title");
            if (hasUrl) {
                builder.withActualPictures(Collections.singletonList("url"));
            }

            datacampOffer.setData(builder.build());
        };
    }
}
