package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline;

import Market.DataCamp.DataCampOfferMeta;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.entity.goodcontent.FailData;
import ru.yandex.market.partner.content.common.entity.goodcontent.ParamInfo;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.robot.db.ParameterValueComposer;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.DcpOfferUtils.initOffer;

public class TitleLengthValidationTest extends DBDcpStateGenerator {

    private final TitleLengthValidation validation = new TitleLengthValidation(gcSkuValidationDao, gcSkuTicketDao);

    @Test
    public void testTitleLengthValidation() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(2, offers -> {
            offerWithTitle(offers, "Valid name", 0);
            offerWithTitle(offers, "Наименование больше 150 символов, Наименование больше 150 символов," +
                    " Наименование больше 150 символов, Наименование больше 150 символов," +
                    " Наименование больше 150 символов", 1);
        });
        MessageReporter messageReporter = new MessageReporter("test");
        for (GcSkuTicket gcSkuTicket : gcSkuTickets) {
            validation.validateTicket(gcSkuTicket, messageReporter, Set.of());
        }
        List<MessageInfo> messages = messageReporter.getMessages();
        Assertions.assertThat(messages.size()).isEqualTo(1);
        Assertions.assertThat(messages.get(0).getCode()).isEqualTo("ir.partner_content.dcp.validation.title_legth");
        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNotNull();
        assertThat(failData.getParams()).contains(new ParamInfo(ParameterValueComposer.NAME_ID, ParameterValueComposer.NAME, false));
    }

    private void offerWithTitle(List<DatacampOffer> offers, String title, int index) {
        initOffer(CATEGORY_ID, offers.get(index), builder -> builder.getContentBuilder().getPartnerBuilder().getActualBuilder()
                .setTitle(DataCampOfferMeta.StringValue.newBuilder().setValue(title).build()).build());
    }

}
