package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline;

import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMockBuilder;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.entity.goodcontent.FailData;
import ru.yandex.market.partner.content.common.entity.goodcontent.ParamInfo;
import ru.yandex.market.partner.content.common.message.MessageInfo;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.DcpOfferUtils.addNumericParam;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.DcpOfferUtils.initOffer;

public class ParameterTypeValidationTest extends DBDcpStateGenerator {

    @Test
    public void negativeNumbersAreAllowedInNumericParams() {
        final int paramId = 1;
        final String paramName = "negative";
        CategoryDataKnowledge categoryDataKnowledge = CategoryDataKnowledgeMockBuilder.builder()
            .startCategory(CATEGORY_ID)
            .vendorParameterBuilder().build()
            .numericParameterBuilder().setParamId(paramId).setXlsName(paramName).build()
            .build()
            .build();
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                addNumericParam(paramId, paramName, "-10", builder);
            });
        });
        GcSkuTicket ticket = gcSkuTickets.get(0);

        MessageReporter messageReporterMock =
            Mockito.mock(MessageReporter.class);

        ParameterTypeValidation parameterTypeValidation = new ParameterTypeValidation(gcSkuValidationDao,
                gcSkuTicketDao, categoryDataKnowledge);
        parameterTypeValidation.validateTicket(ticket, messageReporterMock, Set.of());

        Mockito.verify(messageReporterMock, Mockito.never()).throwIllegalArgumentException(Mockito.any());
    }

    @Test
    public void minMaxBoundsAreValidatedForNumericParams() {
        final int paramId = 1;
        final String paramName = "numeric";
        CategoryDataKnowledge categoryDataKnowledge = CategoryDataKnowledgeMockBuilder.builder()
            .startCategory(CATEGORY_ID)
            .vendorParameterBuilder().build()
            .numericParameterBuilder()
            .setParamId(paramId).setXlsName(paramName)
            .setMinValue(-20.0)
            .setMaxValue(80.0)
            .build()
            .build()
            .build();
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(3, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                addNumericParam(paramId, paramName, "-50", builder);
            });
            initOffer(CATEGORY_ID, offers.get(1), builder -> {
                addNumericParam(paramId, paramName, "150", builder);
            });
            initOffer(CATEGORY_ID, offers.get(2), builder -> {
                addNumericParam(paramId, paramName, "50", builder);
            });
        });

        MessageReporter messageReporter;
        ParameterTypeValidation parameterTypeValidation = new ParameterTypeValidation(gcSkuValidationDao,
                gcSkuTicketDao, categoryDataKnowledge);

        messageReporter = new MessageReporter(null);
        parameterTypeValidation.validateTicket(gcSkuTickets.get(0), messageReporter, Set.of());
        assertThat(messageReporter.getMessages())
            .hasSize(1)
            .extracting(MessageInfo::getCode).containsExactly("ir.partner_content.dcp.validation.value_below_minimum");
        FailData failData = messageReporter.getFailData();
        assertThat(failData).isNotNull();
        assertThat(failData.getParams()).containsExactly(new ParamInfo((long)paramId, "", false));

        messageReporter = new MessageReporter(null);
        parameterTypeValidation.validateTicket(gcSkuTickets.get(1), messageReporter, Set.of());
        assertThat(messageReporter.getMessages())
            .hasSize(1)
            .extracting(MessageInfo::getCode).containsExactly("ir.partner_content.dcp.validation.value_exceeds_maximum");
        failData = messageReporter.getFailData();
        assertThat(failData).isNotNull();
        assertThat(failData.getParams()).containsExactly(new ParamInfo((long)paramId, "", false));

        messageReporter = new MessageReporter(null);
        parameterTypeValidation.validateTicket(gcSkuTickets.get(2), messageReporter, Set.of());
        assertThat(messageReporter.getMessages()).isEmpty();

        failData = messageReporter.getFailData();
        assertThat(failData).isNull();
    }

    @Test
    public void commaIsAllowedInNumericParams() {
        final int paramId = 1;
        final String paramName = "paramWithComma";
        CategoryDataKnowledge categoryDataKnowledge = CategoryDataKnowledgeMockBuilder.builder()
                .startCategory(CATEGORY_ID)
                .vendorParameterBuilder().build()
                .numericParameterBuilder().setParamId(paramId).setXlsName(paramName).build()
                .build()
                .build();
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, offers -> {
            initOffer(CATEGORY_ID, offers.get(0), builder -> {
                addNumericParam(paramId, paramName, "1,8", builder);
            });
        });
        GcSkuTicket ticket = gcSkuTickets.get(0);

        MessageReporter messageReporterMock =
                Mockito.mock(MessageReporter.class);

        ParameterTypeValidation parameterTypeValidation = new ParameterTypeValidation(gcSkuValidationDao,
                gcSkuTicketDao, categoryDataKnowledge);
        parameterTypeValidation.validateTicket(ticket, messageReporterMock, Set.of());

        Mockito.verify(messageReporterMock, Mockito.never()).throwIllegalArgumentException(Mockito.any());
    }
}
