package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.fast_pipeline;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.MessageReporter;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMockBuilder;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.service.DataCampOfferBuilder;
import ru.yandex.market.robot.db.ParameterValueComposer;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus.VALIDATION_STARTED;

public class FCParameterExistenceValidationTest extends DBDcpStateGenerator {

    private static final Integer TICKETS_COUNT = 1;
    private static final Long REQUIRED_CATEGORY_PARAM_ID = 1234L;

    FCParameterExistenceValidation fcParameterExistenceValidation;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        CategoryDataKnowledge categoryDataKnowledgeMock = CategoryDataKnowledgeMockBuilder
                .builder()
                    .startCategory(CATEGORY_ID)
                        .createRandomBooleanParameterBuilder()
                        .setParamId(REQUIRED_CATEGORY_PARAM_ID)
                        .setMandatoryForPartner(true)
                        .setSkuMode(MboParameters.SKUParameterMode.SKU_INFORMATIONAL)
                        .setXlsName(CategoryData.VENDOR)
                        .build()
                    .build()
                .build();

        fcParameterExistenceValidation = new FCParameterExistenceValidation(
                gcSkuValidationDao, gcSkuTicketDao,
                new CategoryDataHelper(categoryDataKnowledgeMock, null)
        );
    }

    @Test
    public void offersInCategoryWithMandatoryParamsShouldProduceMessagesForThisMandatoryParams() {
        GcSkuTicket gcSkuTicket = generateTickets(TICKETS_COUNT, validOffersSettings(), VALIDATION_STARTED).get(0);
        MessageReporter messageReporter = new MessageReporter(gcSkuTicket.getShopSku());
        fcParameterExistenceValidation.validateTicket(gcSkuTicket, messageReporter, Set.of());
        List<MessageInfo> messages = messageReporter.getMessages();
        assertThat(messages.size()).isEqualTo(TICKETS_COUNT);
        // We should have only 1 message for failed
        // FCParameterExistenceValidation.validateForRequiredCategoricalParameters validation for each ticket
    }

    @Test
    public void offerWithoutTitleShouldProduceMessageContainingTitleParameter() {
        GcSkuTicket gcSkuTicket =
                generateTickets(TICKETS_COUNT, offersWithoutNameSettings(), VALIDATION_STARTED).get(0);
        MessageReporter messageReporter = new MessageReporter(gcSkuTicket.getShopSku());
        fcParameterExistenceValidation.validateTicket(gcSkuTicket, messageReporter, Set.of());
        // We should have only 1 message for failed validateForRequiredCategoricalParameters validation
        // And 1 message for FCParameterExistenceValidation.validateForParameter validation
        // for each ticket
        List<MessageInfo> messages = messageReporter.getMessages();
        assertThat(messages).hasSize(TICKETS_COUNT * 2);
        assertThat(messages.stream()
                .flatMap(el -> el.getParams().values().stream()).collect(Collectors.toList()))
                .contains(ParameterValueComposer.NAME_ID, REQUIRED_CATEGORY_PARAM_ID);
    }

    @Test
    public void whenDuplicateParamsDoNotFail() {
        GcSkuTicket gcSkuTicket =
                generateTickets(TICKETS_COUNT, duplicateParamsOffersSettings(), VALIDATION_STARTED).get(0);
        MessageReporter messageReporter = new MessageReporter(gcSkuTicket.getShopSku());
        fcParameterExistenceValidation.validateTicket(gcSkuTicket, messageReporter, Set.of());
        List<MessageInfo> messages = messageReporter.getMessages();
        assertThat(messages).hasSize(TICKETS_COUNT);
    }

    private Consumer<DatacampOffer> offersWithoutNameSettings() {
        return datacampOffer -> datacampOffer.setData(
                new DataCampOfferBuilder(
                        datacampOffer.getCreateTime(),
                        datacampOffer.getBusinessId(),
                        CATEGORY_ID,
                        datacampOffer.getOfferId()
                ).withActualDescription("test description").build()
        );
    }

    private Consumer<DatacampOffer> validOffersSettings() {
        return datacampOffer -> datacampOffer.setData(
                new DataCampOfferBuilder(
                        datacampOffer.getCreateTime(),
                        datacampOffer.getBusinessId(),
                        CATEGORY_ID,
                        datacampOffer.getOfferId()
                ).withActualNameAndTitle("test title").build()
        );
    }

    private Consumer<DatacampOffer> duplicateParamsOffersSettings() {
        return datacampOffer -> datacampOffer.setData(
                new DataCampOfferBuilder(
                        datacampOffer.getCreateTime(),
                        datacampOffer.getBusinessId(),
                        CATEGORY_ID,
                        datacampOffer.getOfferId()
                ).withActualNameAndTitle("test title")
                        .withEnumParam(17862203L, 17862206L, "осень", Optional.empty())
                        .withEnumParam(17862203L, 17862208L, "зима", Optional.empty())
                        .build()
        );
    }

}
