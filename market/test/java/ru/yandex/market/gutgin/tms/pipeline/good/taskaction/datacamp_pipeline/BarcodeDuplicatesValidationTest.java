package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMeta;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.PreparedOfferState;
import ru.yandex.market.partner.content.common.csku.KnownParameters;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.entity.goodcontent.TicketValidationResult;
import ru.yandex.market.partner.content.common.message.Messages;

import static org.assertj.core.api.Assertions.assertThat;

public class BarcodeDuplicatesValidationTest extends DBDcpStateGenerator {
    private static final String BARCODE_EXIST = "barcode_exist";

    private BarcodeDuplicatesValidation barcodeDuplicatesValidation;

    @Before
    public void init() {

        barcodeDuplicatesValidation = new BarcodeDuplicatesValidation(
                gcSkuValidationDao, gcSkuTicketDao, Collections.emptySet());
    }

    @Test
    public void whenNoDuplicateThenValid() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(2, states -> {
            initOffer(states.get(0), "barcode_1");
            initOffer(states.get(1), "barcode_2");
        });

        ProcessTaskResult<List<TicketValidationResult>> validate = barcodeDuplicatesValidation.validate(gcSkuTickets);

        TicketValidationResult res1 = validate.getResult().get(0);
        TicketValidationResult res2 = validate.getResult().get(1);

        assertThat(res1.isValid()).isTrue();
        assertThat(res1.getValidationMessages()).isEmpty();
        assertThat(res2.isValid()).isTrue();
        assertThat(res2.getValidationMessages()).isEmpty();
    }

    @Test
    public void whenDuplicateWithTicketThenMessage() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(2, states -> {
            initOffer(states.get(0), BARCODE_EXIST);
            initOffer(states.get(1), BARCODE_EXIST);
        });

        ProcessTaskResult<List<TicketValidationResult>> validate = barcodeDuplicatesValidation.validate(gcSkuTickets);

        TicketValidationResult invalid1 = validate.getResult().get(0);
        TicketValidationResult invalid2 = validate.getResult().get(1);

        String shopSku1 = gcSkuTickets.get(0).getShopSku();
        String shopSku2 = gcSkuTickets.get(1).getShopSku();

        assertThat(invalid1.isValid()).isFalse();
        assertThat(invalid1.getValidationMessages())
            .hasSize(1)
            .containsOnly(Messages.get().dcpDuplicateTicketBarcode(
                shopSku1,
                BARCODE_EXIST,
                new String[] {shopSku2},
                KnownParameters.BARCODE.getId()));

        assertThat(invalid2.isValid()).isFalse();
        assertThat(invalid2.getValidationMessages())
            .hasSize(1)
            .containsOnly(Messages.get().dcpDuplicateTicketBarcode(
                shopSku2,
                BARCODE_EXIST,
                new String[] {shopSku1},
                KnownParameters.BARCODE.getId()));
    }

    @Test
    public void whenLotsOfDuplicatesThenCutMessage() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(13, datacampOffers -> {
            datacampOffers.forEach(state ->  initOffer(state, BARCODE_EXIST));
        });

        ProcessTaskResult<List<TicketValidationResult>> validate = barcodeDuplicatesValidation.validate(gcSkuTickets);

        TicketValidationResult validationResult = validate.getResult().get(0);

        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getValidationMessages())
                .hasSize(1);
        Map<String, Object> params = validationResult.getValidationMessages().get(0).getParams();
        String[] otherOfferIds = (String[]) params.get("otherOfferIds");
        assertThat(otherOfferIds[otherOfferIds.length - 1]).isEqualTo("так же в 2 других");
    }

    private void initOffer(PreparedOfferState state, String barcode) {
        state.getDcpOfferBuilder().withBarCodes(barcode).withCategory((int) CATEGORY_ID);
    }
}
