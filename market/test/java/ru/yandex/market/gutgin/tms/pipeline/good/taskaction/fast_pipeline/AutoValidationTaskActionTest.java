package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.fast_pipeline;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.databucket_validations.DataBucketTicketAutoValidation;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.HtmlTagsExistenceValidation;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.SizeParamsConsistencyValidation;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.TitleUpperCaseValidation;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataForSizeParametersMock;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMockBuilder;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuValidation;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.entity.goodcontent.FailData;
import ru.yandex.market.partner.content.common.entity.goodcontent.ParamInfo;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.service.DataCampOfferBuilder;
import ru.yandex.market.robot.db.ParameterValueComposer;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus.IMAGE_UPLOAD_OK;
import static ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus.RESULT_UPLOAD_STARTED;
import static ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus.VALIDATION_OK;
import static ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuValidationType.HTML_TAGS_EXISTENCE;
import static ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuValidationType.SIZE_PARAMS_CONSISTENCY;
import static ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuValidationType.TITLE_UPPER_CASE;
import static ru.yandex.market.partner.content.common.db.jooq.tables.GcSkuTicket.GC_SKU_TICKET;
import static ru.yandex.market.robot.db.ParameterValueComposer.VENDOR;

public class AutoValidationTaskActionTest extends DBDcpStateGenerator {

    private static final long VENDOR_SIZE_PARAM_ID = 1L;
    private static final long VENDOR_SIZE_PARAM_MIN_ID = 1_0L;
    private static final long VENDOR_SIZE_PARAM_MAX_ID = 1_1L;
    private static final long SIZE_PARAM_1_ID = 2L;
    private static final long SIZE_PARAM_1_MIN_ID = 2_0L;
    private static final long SIZE_PARAM_1_MAX_ID = 2_1L;
    private static final long SIZE_PARAM_2_ID = 3L;
    private static final long SIZE_PARAM_2_MIN_ID = 3_0L;
    private static final long SIZE_PARAM_2_MAX_ID = 3_1L;
    private static final long SIZE_PARAM_3_ID = 4L;
    private static final long SIZE_PARAM_3_MIN_ID = 4_0L;
    private static final long SIZE_PARAM_3_MAX_ID = 4_1L;

    private static final Integer TICKETS_COUNT = 15;
    private static final Long CATEGORY_FOR_SIZE_PARAMETERS_ID = 11321312L;

    AutoValidationTaskAction autoValidationTaskAction;
    List<DataBucketTicketAutoValidation> validations;

    @Before
    public void setUp() {
        super.setUp();
        CategoryDataKnowledge categoryDataKnowledge = CategoryDataKnowledgeMockBuilder.builder()
                .startCategory(CATEGORY_ID).createRandomEnumParameterBuilder().setXlsName(VENDOR).build().build()
                .startCategory(CATEGORY_FOR_SIZE_PARAMETERS_ID).buildWithCategoryData(buildCategoryDataWithSizes())
                .build();

        CategoryDataHelper categoryDataHelper = new CategoryDataHelper(categoryDataKnowledge, null);
        validations = Arrays.asList(
                new HtmlTagsExistenceValidation(gcSkuValidationDao, gcSkuTicketDao, categoryDataHelper),
                new TitleUpperCaseValidation(gcSkuValidationDao, gcSkuTicketDao),
                new SizeParamsConsistencyValidation(gcSkuValidationDao, gcSkuTicketDao, categoryDataHelper)
        );

        autoValidationTaskAction = new AutoValidationTaskAction(
                gcSkuTicketDao,
                gcSkuValidationDao,
                validations
        );
    }

    @Test
    public void testValidOffersShouldPass() {
        generateTickets(TICKETS_COUNT, onlyNameSettings(), IMAGE_UPLOAD_OK);
        ProcessTaskResult<ProcessDataBucketData> result = autoValidationTaskAction.apply(
                new ProcessDataBucketData(dataBucketId)
        );
        List<MessageInfo> gcValidationMessages = gcSkuValidationDao.getGcValidationMessages(
                gcSkuValidationDao.findAll().stream().map(GcSkuValidation::getId).toArray(Long[]::new)
        );

        assertThat(gcValidationMessages).isEmpty();
        assertThat(gcSkuValidationDao.count()).isEqualTo(validationsCount());
        assertThat(gcSkuTicketDao.getTicketsByDataBucket(dataBucketId))
                .extracting(GcSkuTicket::getStatus)
                .allMatch(VALIDATION_OK::equals);
        assertThat(result.hasProblems()).isFalse();
    }

    @Test
    public void testValidationsCreated() {
        generateTickets(TICKETS_COUNT, emptySettings(), IMAGE_UPLOAD_OK);
        ProcessTaskResult<ProcessDataBucketData> result = autoValidationTaskAction.apply(
                new ProcessDataBucketData(dataBucketId)
        );
        assertThat(gcSkuValidationDao.count()).isEqualTo(validationsCount());
        assertThat(gcSkuTicketDao.getTicketsByDataBucket(dataBucketId))
                .extracting(GcSkuTicket::getStatus)
                .allMatch(RESULT_UPLOAD_STARTED::equals);
        assertThat(result.hasProblems()).isFalse();
    }

    @Test
    public void testProtocolMessagesCreatedForNonSizeCategory() {
        generateTickets(TICKETS_COUNT, titleUppercaseWithHtmlTagsAndNoDescriptionSettings(), IMAGE_UPLOAD_OK);
        ProcessTaskResult<ProcessDataBucketData> result = autoValidationTaskAction.apply(
                new ProcessDataBucketData(dataBucketId)
        );

        Long[] ticketsIds = getTicketsIds();
        List<MessageInfo> validationMessages = gcSkuValidationDao.getGcValidationMessages(
                Stream.of(
                        gcSkuValidationDao.getGcSkuValidations(TITLE_UPPER_CASE, ticketsIds),
                        gcSkuValidationDao.getGcSkuValidations(HTML_TAGS_EXISTENCE, ticketsIds),
                        gcSkuValidationDao.getGcSkuValidations(SIZE_PARAMS_CONSISTENCY, ticketsIds)
                ).flatMap(Collection::stream).map(GcSkuValidation::getId).toArray(Long[]::new)
        );

        // here we should see only 30 validation messages, as there we will not have messages for
        // SIZE_PARAMS_CONSISTENCY validation (we are in category without size parameters)
        assertThat(validationMessages.size()).isEqualTo(validationsCount() - TICKETS_COUNT);
        assertThat(gcSkuTicketDao.getTicketsByDataBucket(dataBucketId))
                .extracting(GcSkuTicket::getStatus)
                .allMatch(RESULT_UPLOAD_STARTED::equals);
        assertThat(result.hasProblems()).isFalse();
    }

    @Test
    public void testFailDataWritedForFailValidation() {
        generateTickets(TICKETS_COUNT, titleUppercaseWithHtmlTagsAndNoDescriptionSettings(), IMAGE_UPLOAD_OK);
        ProcessTaskResult<ProcessDataBucketData> result = autoValidationTaskAction.apply(
                new ProcessDataBucketData(dataBucketId)
        );
        Long[] ticketsIds = getTicketsIds();

        List<GcSkuValidation> validations = gcSkuValidationDao.getGcSkuValidations(TITLE_UPPER_CASE, ticketsIds);
        assertThat(validations)
                .extracting(GcSkuValidation::getFailData)
                .allMatch(Objects::nonNull);
        FailData failData = validations.get(0).getFailData();
        assertThat(failData.getParams())
                .containsExactly(new ParamInfo(ParameterValueComposer.NAME_ID, ParameterValueComposer.NAME,
                        false));
    }

    @Test
    public void testProtocolMessagesCreatedForSizeCategory() {
        generateTickets(TICKETS_COUNT, titleUppercaseWithHtmlTagsAndNoDescriptionSettings(), IMAGE_UPLOAD_OK);
        setNewCategoryId(getTicketsIds(), CATEGORY_FOR_SIZE_PARAMETERS_ID);
        ProcessTaskResult<ProcessDataBucketData> result = autoValidationTaskAction.apply(
                new ProcessDataBucketData(dataBucketId)
        );

        Long[] ticketsIds = getTicketsIds();
        List<MessageInfo> validationMessages = gcSkuValidationDao.getGcValidationMessages(
                Stream.of(
                        gcSkuValidationDao.getGcSkuValidations(TITLE_UPPER_CASE, ticketsIds),
                        gcSkuValidationDao.getGcSkuValidations(HTML_TAGS_EXISTENCE, ticketsIds),
                        gcSkuValidationDao.getGcSkuValidations(SIZE_PARAMS_CONSISTENCY, ticketsIds)
                ).flatMap(Collection::stream).map(GcSkuValidation::getId).toArray(Long[]::new)
        );

        assertThat(validationMessages.size()).isEqualTo(validationsCount());
        assertThat(gcSkuTicketDao.getTicketsByDataBucket(dataBucketId))
                .extracting(GcSkuTicket::getStatus)
                .allMatch(RESULT_UPLOAD_STARTED::equals);
        assertThat(result.hasProblems()).isFalse();
    }

    private void setNewCategoryId(Long[] ticketsIds, Long categoryId) {
        dsl().update(GC_SKU_TICKET)
                .set(GC_SKU_TICKET.CATEGORY_ID, categoryId)
                .where(GC_SKU_TICKET.ID.in(ticketsIds))
                .execute();
    }

    private long validationsCount() {
        return (long) TICKETS_COUNT * validations.size();
    }

    private CategoryData buildCategoryDataWithSizes() {
        return new CategoryDataForSizeParametersMock(
                new HashMap<Long, Pair<Long, Long>>() {
                    {
                        put(VENDOR_SIZE_PARAM_ID, Pair.of(VENDOR_SIZE_PARAM_MIN_ID, VENDOR_SIZE_PARAM_MAX_ID));
                        put(SIZE_PARAM_1_ID, Pair.of(SIZE_PARAM_1_MIN_ID, SIZE_PARAM_1_MAX_ID));
                        put(SIZE_PARAM_2_ID, Pair.of(SIZE_PARAM_2_MIN_ID, SIZE_PARAM_2_MAX_ID));
                        put(SIZE_PARAM_3_ID, Pair.of(SIZE_PARAM_3_MIN_ID, SIZE_PARAM_3_MAX_ID));
                    }
                },
                MboParameters.Parameter
                        .newBuilder()
                        .setId(VENDOR_SIZE_PARAM_ID)
                        .build(),
                MboParameters.Parameter
                        .newBuilder()
                        .setId(VENDOR_SIZE_PARAM_MIN_ID)
                        .build(),
                MboParameters.Parameter
                        .newBuilder()
                        .setId(VENDOR_SIZE_PARAM_MAX_ID)
                        .build(),
                MboParameters.Parameter
                        .newBuilder()
                        .setId(SIZE_PARAM_1_ID)
                        .build(),
                MboParameters.Parameter
                        .newBuilder()
                        .setId(SIZE_PARAM_1_MIN_ID)
                        .build(),
                MboParameters.Parameter
                        .newBuilder()
                        .setId(SIZE_PARAM_1_MAX_ID)
                        .build(),
                MboParameters.Parameter
                        .newBuilder()
                        .setId(SIZE_PARAM_2_ID)
                        .build(),
                MboParameters.Parameter
                        .newBuilder()
                        .setId(SIZE_PARAM_2_MIN_ID)
                        .build(),
                MboParameters.Parameter
                        .newBuilder()
                        .setId(SIZE_PARAM_2_MAX_ID)
                        .build(),
                MboParameters.Parameter
                        .newBuilder()
                        .setId(SIZE_PARAM_3_ID)
                        .build(),
                MboParameters.Parameter
                        .newBuilder()
                        .setId(SIZE_PARAM_3_MIN_ID)
                        .build(),
                MboParameters.Parameter
                        .newBuilder()
                        .setId(SIZE_PARAM_3_MAX_ID)
                        .build()
        );
    }

    private Consumer<DatacampOffer> emptySettings() {
        return offer -> {
        };
    }

    private Consumer<DatacampOffer> onlyNameSettings() {
        return offer -> {
            offer.setData(
                    new DataCampOfferBuilder(
                            offer.getCreateTime(),
                            offer.getBusinessId(),
                            CATEGORY_ID,
                            offer.getOfferId()
                    ).withActualNameAndTitle("valid title").build()
            );
        };
    }

    private Consumer<DatacampOffer> titleUppercaseWithHtmlTagsAndNoDescriptionSettings() {
        return datacampOffer -> datacampOffer.setData(
                new DataCampOfferBuilder(
                        datacampOffer.getCreateTime(),
                        datacampOffer.getBusinessId(),
                        CATEGORY_ID,
                        datacampOffer.getOfferId()
                ).withActualNameAndTitle("TITLE_IN_UPPER_CASE_WITH<div></div>_HTML_TAGS")
                        .build()
        );
    }

    private Long[] getTicketsIds() {
        return gcSkuTicketDao.getTicketsByDataBucket(dataBucketId).stream()
                .map(ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket::getId)
                .toArray(Long[]::new);
    }
}
