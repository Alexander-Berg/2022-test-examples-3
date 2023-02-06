package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferMapping;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.gutgin.tms.db.dao.pipeline.DataBucketMessagesService;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.Tables;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuValidationType;
import ru.yandex.market.partner.content.common.db.jooq.enums.MappingConfidenceType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuValidation;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;

import static junit.framework.TestCase.assertFalse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus.CW_MANUAL_VALIDATION_OK;
import static ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus.RESULT_UPLOAD_STARTED;
import static ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus.VALIDATION_OK;
import static ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus.VALIDATION_STARTED;

public class ApplyCskuValidationResultsTaskActionTest extends DBDcpStateGenerator {
    private ApplyCskuValidationResultsTaskAction taskAction;
    private ModelStorageHelper modelStorageHelper = mock(ModelStorageHelper.class);

    @Before
    public void setUp() {
        super.setUp();
        DataBucketMessagesService messagesService = mock(DataBucketMessagesService.class);
        taskAction = new ApplyCskuValidationResultsTaskAction(gcSkuTicketDao, gcSkuValidationDao, messagesService,
                modelStorageHelper);
        when(modelStorageHelper.findModels(any(), eq(false)))
                .thenReturn(Collections.singletonList(ModelStorage.Model.newBuilder()
                        .setId(1L)
                        .setCategoryId(CATEGORY_ID).build()));
    }

    @Test
    public void testValidationOk() throws Exception {
        List<GcSkuTicket> tickets = generateTickets(1, emptySettings(), VALIDATION_STARTED);
        long ticketId = tickets.get(0).getId();

        List<GcSkuValidation> mandatoryOkValidations =
                ApplyCskuValidationResultsTaskAction.MANDATORY_VALIDATION_TYPES.stream()
                .map(type -> createValidation(ticketId, type, true))
                .collect(Collectors.toList());
        mandatoryOkValidations.forEach(validation -> gcSkuValidationDao.insert(validation));

        ProcessDataBucketData bucketData = new ProcessDataBucketData(dataBucketId);
        taskAction.doRun(bucketData);

        GcSkuTicket ticket = gcSkuTicketDao.fetchOneById(ticketId);
        assertThat(ticket.getValid()).isTrue();
        assertThat(ticket.getStatus()).isEqualTo(VALIDATION_OK);
    }

    @Test
    public void testValidationOkIfOnlyMandatoryValid() throws Exception {
        List<GcSkuTicket> tickets = generateTickets(1, emptySettings(), VALIDATION_STARTED);
        tickets.forEach(ticket -> {
            Long skuId = ticket.getId() * 1000;
            ticket.setExistingMboPskuId(skuId);
            gcSkuTicketDao.update(ticket);
            when(modelStorageHelper.findModels(any(), eq(false)))
                    .thenReturn(Collections.singletonList(ModelStorage.Model.newBuilder()
                            .setId(skuId)
                            .setCategoryId(CATEGORY_ID).build()));
        });
        long ticketId = tickets.get(0).getId();

        List<GcSkuValidation> mandatoryOkValidations =
                ApplyCskuValidationResultsTaskAction.MANDATORY_VALIDATION_TYPES.stream()
                .map(type -> createValidation(ticketId, type, true))
                .collect(Collectors.toList());
        List<GcSkuValidation> otherFailValidations = Stream.of(GcSkuValidationType.values())
                .filter(type -> !ApplyCskuValidationResultsTaskAction.MANDATORY_VALIDATION_TYPES.contains(type))
                .map(type -> createValidation(ticketId, type, false))
                .collect(Collectors.toList());

        mandatoryOkValidations.forEach(validation -> gcSkuValidationDao.insert(validation));
        otherFailValidations.forEach(validation -> gcSkuValidationDao.insert(validation));

        ProcessDataBucketData bucketData = new ProcessDataBucketData(dataBucketId);
        taskAction.doRun(bucketData);

        GcSkuTicket ticket = gcSkuTicketDao.fetchOneById(ticketId);
        assertThat(ticket.getValid()).isTrue();
        assertThat(ticket.getStatus()).isEqualTo(VALIDATION_OK);
    }

    @Test
    public void testValidationFailedForSkuModification() throws Exception {
        List<GcSkuTicket> tickets = generateTickets(1, emptySettings(), VALIDATION_STARTED);
        long ticketId = tickets.get(0).getId();
        tickets.forEach(ticket -> {
            Long skuId = ticket.getId() * 1000;
            ticket.setExistingMboPskuId(skuId);
            gcSkuTicketDao.update(ticket);
        });

        GcSkuValidation mandatoryFailValidation = createValidation(ticketId,
                GcSkuValidationType.MAPPING_ON_VALID_MODEL_VALIDATION, false);
        gcSkuValidationDao.insert(mandatoryFailValidation);
        GcSkuValidation otherOkValidation = createValidation(ticketId,
                GcSkuValidationType.SIZE_PARAMS_CONSISTENCY, true);
        gcSkuValidationDao.insert(otherOkValidation);

        ProcessDataBucketData bucketData = new ProcessDataBucketData(dataBucketId);
        taskAction.doRun(bucketData);

        GcSkuTicket ticket = gcSkuTicketDao.fetchOneById(ticketId);
        assertThat(ticket.getValid()).isFalse();
        assertThat(ticket.getStatus()).isEqualTo(RESULT_UPLOAD_STARTED);
    }

    @Test
    public void testValidationFailedForSkuCreation() throws Exception {
        List<GcSkuTicket> tickets = generateTickets(1, emptySettings(), VALIDATION_STARTED);
        long ticketId = tickets.get(0).getId();

        GcSkuValidation failValidation = createValidation(ticketId,
                GcSkuValidationType.HTML_TAGS_EXISTENCE, false);
        gcSkuValidationDao.insert(failValidation);
        GcSkuValidation otherOkValidation = createValidation(ticketId,
                GcSkuValidationType.SIZE_PARAMS_CONSISTENCY, true);
        gcSkuValidationDao.insert(otherOkValidation);

        ProcessDataBucketData bucketData = new ProcessDataBucketData(dataBucketId);
        taskAction.doRun(bucketData);

        GcSkuTicket ticket = gcSkuTicketDao.fetchOneById(ticketId);
        assertThat(ticket.getValid()).isFalse();
        assertThat(ticket.getStatus()).isEqualTo(RESULT_UPLOAD_STARTED);
    }

    @Test
    public void testConditionalValidationFailed() throws Exception {
        List<GcSkuTicket> tickets = generateTickets(1, emptySettings(), VALIDATION_STARTED);
        long ticketId = tickets.get(0).getId();
        // валидация BARCODE_GTIN_VALIDATION применяется для новых csku
        setTicketExistingMboPskuId(ticketId, null);

        GcSkuValidation conditionalFailValidation = createValidation(ticketId,
                GcSkuValidationType.BARCODE_GTIN_VALIDATION, false);
        gcSkuValidationDao.insert(conditionalFailValidation);

        ProcessDataBucketData bucketData = new ProcessDataBucketData(dataBucketId);
        taskAction.doRun(bucketData);

        GcSkuTicket resultTicket = gcSkuTicketDao.fetchOneById(ticketId);
        assertThat(resultTicket.getValid()).isFalse();
        assertThat(resultTicket.getStatus()).isEqualTo(RESULT_UPLOAD_STARTED);
    }

    @Test
    public void testConditionalValidationFailedButNoCondition() throws Exception {
        List<GcSkuTicket> tickets = generateTickets(1, emptySettings(), VALIDATION_STARTED);
        long ticketId = tickets.get(0).getId();
        // валидация BARCODE_GTIN_VALIDATION не применяется изменяемых csku
        setTicketExistingMboPskuId(ticketId, 1L);

        GcSkuValidation conditionalFailValidation = createValidation(ticketId,
                GcSkuValidationType.BARCODE_GTIN_VALIDATION, false);
        gcSkuValidationDao.insert(conditionalFailValidation);

        ProcessDataBucketData bucketData = new ProcessDataBucketData(dataBucketId);
        taskAction.doRun(bucketData);

        GcSkuTicket resultTicket = gcSkuTicketDao.fetchOneById(ticketId);
        assertThat(resultTicket.getValid()).isTrue();
        assertThat(resultTicket.getStatus()).isEqualTo(VALIDATION_OK);
    }

    @Test
    public void testWhenCategoryIsChangingThenAllValidationsCritical() throws Exception {
        List<GcSkuTicket> tickets = generateTickets(1, emptySettings(), VALIDATION_STARTED);
        tickets.forEach(ticket -> {
            Long skuId = ticket.getId() * 1000;
            ticket.setExistingMboPskuId(skuId);
            gcSkuTicketDao.update(ticket);
            when(modelStorageHelper.findModels(any(), eq(false)))
                    .thenReturn(Collections.singletonList(ModelStorage.Model.newBuilder()
                            .setId(skuId)
                            .setCategoryId(CATEGORY_ID + 1).build()));
        });
        long ticketId = tickets.get(0).getId();

        List<GcSkuValidation> mandatoryOkValidations =
                ApplyCskuValidationResultsTaskAction.MANDATORY_VALIDATION_TYPES.stream()
                        .map(type -> createValidation(ticketId, type, true))
                        .collect(Collectors.toList());
        List<GcSkuValidation> otherFailValidations = Stream.of(GcSkuValidationType.values())
                .filter(type -> !ApplyCskuValidationResultsTaskAction.MANDATORY_VALIDATION_TYPES.contains(type))
                .map(type -> createValidation(ticketId, type, false))
                .collect(Collectors.toList());

        mandatoryOkValidations.forEach(validation -> gcSkuValidationDao.insert(validation));
        otherFailValidations.forEach(validation -> gcSkuValidationDao.insert(validation));

        ProcessDataBucketData bucketData = new ProcessDataBucketData(dataBucketId);
        taskAction.doRun(bucketData);

        GcSkuTicket ticket = gcSkuTicketDao.fetchOneById(ticketId);
        assertThat(ticket.getValid()).isFalse();
        assertThat(ticket.getStatus()).isEqualTo(RESULT_UPLOAD_STARTED);
    }

    @Test
    public void testWhenCskuCreationThenAllValidationsCritical() throws Exception {
        List<GcSkuTicket> tickets = generateTickets(1, emptySettings(),
                GcSkuTicketStatus.CW_MANUAL_VALIDATION_OK);

        long ticketId = tickets.get(0).getId();

        List<GcSkuValidation> mandatoryOkValidations =
                ApplyCskuValidationResultsTaskAction.MANDATORY_VALIDATION_TYPES.stream()
                        .map(type -> createValidation(ticketId, type, true))
                        .collect(Collectors.toList());
        List<GcSkuValidation> otherFailValidations = Stream.of(GcSkuValidationType.CLEAN_WEB_TEXT,
                        GcSkuValidationType.HTML_TAGS_EXISTENCE)
                .map(type -> createValidation(ticketId, type, false))
                .collect(Collectors.toList());

        mandatoryOkValidations.forEach(validation -> gcSkuValidationDao.insert(validation));
        otherFailValidations.forEach(validation -> gcSkuValidationDao.insert(validation));

        ProcessDataBucketData bucketData = new ProcessDataBucketData(dataBucketId);
        taskAction.doRun(bucketData);

        GcSkuTicket ticket = gcSkuTicketDao.fetchOneById(ticketId);
        assertThat(ticket.getValid()).isFalse();
        assertThat(ticket.getStatus()).isEqualTo(RESULT_UPLOAD_STARTED);
    }

    @Test
    public void testWhenCskuModificationThenAdditionalValidationsNotCritical() throws Exception {
        List<GcSkuTicket> tickets = generateTickets(1, emptySettings(),
                GcSkuTicketStatus.CW_MANUAL_VALIDATION_OK);

        tickets.forEach(ticket -> {
            Long skuId = ticket.getId() * 1000;
            ticket.setExistingMboPskuId(skuId);
            gcSkuTicketDao.update(ticket);
            when(modelStorageHelper.findModels(any(), eq(false)))
                    .thenReturn(Collections.singletonList(ModelStorage.Model.newBuilder()
                            .setId(skuId)
                            .setCategoryId(CATEGORY_ID).build()));
        });
        long ticketId = tickets.get(0).getId();

        List<GcSkuValidation> mandatoryOkValidations =
                ApplyCskuValidationResultsTaskAction.MANDATORY_VALIDATION_TYPES.stream()
                        .map(type -> createValidation(ticketId, type, true))
                        .collect(Collectors.toList());
        List<GcSkuValidation> otherFailValidations = Stream.of(GcSkuValidationType.CLEAN_WEB_TEXT,
                        GcSkuValidationType.HTML_TAGS_EXISTENCE)
                .map(type -> createValidation(ticketId, type, false))
                .collect(Collectors.toList());

        mandatoryOkValidations.forEach(validation -> gcSkuValidationDao.insert(validation));
        otherFailValidations.forEach(validation -> gcSkuValidationDao.insert(validation));

        ProcessDataBucketData bucketData = new ProcessDataBucketData(dataBucketId);
        taskAction.doRun(bucketData);

        GcSkuTicket ticket = gcSkuTicketDao.fetchOneById(ticketId);
        assertThat(ticket.getValid()).isTrue();
        assertThat(ticket.getStatus()).isEqualTo(CW_MANUAL_VALIDATION_OK);
    }

    @Test
    public void strictCheckTestCritical() {
        List<GcSkuTicket> tickets = generateTickets(1, emptySettings(),
                GcSkuTicketStatus.CW_MANUAL_VALIDATION_OK);

        tickets.forEach(ticket -> {
            Long skuId = ticket.getId() * 1000;
            ticket.setExistingMboPskuId(skuId);
            gcSkuTicketDao.update(ticket);
            when(modelStorageHelper.findModels(any(), eq(false)))
                    .thenReturn(Collections.singletonList(ModelStorage.Model.newBuilder()
                            .setId(skuId)
                            .setStrictChecksRequired(true) //  <----- Here is the magic
                            .setCategoryId(CATEGORY_ID).build()));
        });
        long ticketId = tickets.get(0).getId();

        List<GcSkuValidation> mandatoryOkValidations =
                ApplyCskuValidationResultsTaskAction.MANDATORY_VALIDATION_TYPES.stream()
                        .map(type -> createValidation(ticketId, type, true))
                        .collect(Collectors.toList());
        List<GcSkuValidation> otherFailValidations = Stream.of(GcSkuValidationType.CLEAN_WEB_TEXT,
                GcSkuValidationType.HTML_TAGS_EXISTENCE)
                .map(type -> createValidation(ticketId, type, false))
                .collect(Collectors.toList());

        mandatoryOkValidations.forEach(validation -> gcSkuValidationDao.insert(validation));
        otherFailValidations.forEach(validation -> gcSkuValidationDao.insert(validation));

        ProcessDataBucketData bucketData = new ProcessDataBucketData(dataBucketId);
        taskAction.doRun(bucketData);

        GcSkuTicket ticket = gcSkuTicketDao.fetchOneById(ticketId);
        assertFalse(ticket.getValid());
        assertThat(ticket.getStatus()).isEqualTo(RESULT_UPLOAD_STARTED);
    }

    @Test
    public void whenBrokenThenAllChecksAreCritical() {
        List<GcSkuTicket> tickets = generateTickets(1, emptySettings(),
                GcSkuTicketStatus.CW_MANUAL_VALIDATION_OK);

        tickets.forEach(ticket -> {
            Long skuId = ticket.getId() * 1000;
            ticket.setExistingMboPskuId(skuId);
            gcSkuTicketDao.update(ticket);
            when(modelStorageHelper.findModels(any(), eq(false)))
                    .thenReturn(Collections.singletonList(ModelStorage.Model.newBuilder()
                            .setId(skuId)
                            .setBroken(true)
                            .setCategoryId(CATEGORY_ID).build()));
        });
        long ticketId = tickets.get(0).getId();

        List<GcSkuValidation> mandatoryOkValidations =
                ApplyCskuValidationResultsTaskAction.MANDATORY_VALIDATION_TYPES.stream()
                        .map(type -> createValidation(ticketId, type, true))
                        .collect(Collectors.toList());
        List<GcSkuValidation> otherFailValidations = Stream.of(GcSkuValidationType.CLEAN_WEB_TEXT,
                        GcSkuValidationType.HTML_TAGS_EXISTENCE)
                .map(type -> createValidation(ticketId, type, false))
                .collect(Collectors.toList());

        mandatoryOkValidations.forEach(validation -> gcSkuValidationDao.insert(validation));
        otherFailValidations.forEach(validation -> gcSkuValidationDao.insert(validation));

        ProcessDataBucketData bucketData = new ProcessDataBucketData(dataBucketId);
        taskAction.doRun(bucketData);

        GcSkuTicket ticket = gcSkuTicketDao.fetchOneById(ticketId);
        assertFalse(ticket.getValid());
        assertThat(ticket.getStatus()).isEqualTo(RESULT_UPLOAD_STARTED);
    }

    @Test
    public void testWhenFCModificationThenAllValidationsAreCritical() {
        List<GcSkuTicket> tickets = generateTickets(1,
                offer -> {
            offer.setData(DataCampOffer.Offer.newBuilder()
                            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                                    .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                                            .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                                                    .setMarketSkuType(DataCampOfferMapping.Mapping.MarketSkuType
                                                            .MARKET_SKU_TYPE_FAST)
                                                    .build())
                                            .build())
                                    .build())
                    .build());
                },
                GcSkuTicketStatus.CW_MANUAL_VALIDATION_OK);

        tickets.forEach(ticket -> {
            Long skuId = ticket.getId() * 1000;
            ticket.setExistingMboPskuId(skuId);
            gcSkuTicketDao.update(ticket);
            when(modelStorageHelper.findModels(any(), eq(false)))
                    .thenReturn(Collections.singletonList(ModelStorage.Model.newBuilder()
                            .setId(skuId)
                            .setCategoryId(CATEGORY_ID).build()));
        });
        long ticketId = tickets.get(0).getId();

        List<GcSkuValidation> mandatoryOkValidations =
                ApplyCskuValidationResultsTaskAction.MANDATORY_VALIDATION_TYPES.stream()
                        .map(type -> createValidation(ticketId, type, true))
                        .collect(Collectors.toList());
        List<GcSkuValidation> otherFailValidations = Stream.of(GcSkuValidationType.CLEAN_WEB_TEXT,
                        GcSkuValidationType.HTML_TAGS_EXISTENCE)
                .map(type -> createValidation(ticketId, type, false))
                .collect(Collectors.toList());

        mandatoryOkValidations.forEach(validation -> gcSkuValidationDao.insert(validation));
        otherFailValidations.forEach(validation -> gcSkuValidationDao.insert(validation));

        ProcessDataBucketData bucketData = new ProcessDataBucketData(dataBucketId);
        taskAction.doRun(bucketData);

        GcSkuTicket ticket = gcSkuTicketDao.fetchOneById(ticketId);
        assertThat(ticket.getValid()).isFalse();
        assertThat(ticket.getStatus()).isEqualTo(RESULT_UPLOAD_STARTED);
    }

    @Test
    public void testWhenFCModificationByConfidenceThenAllValidationsAreCritical() {
        List<GcSkuTicket> tickets = generateTickets(1,
                offer -> {},
                GcSkuTicketStatus.CW_MANUAL_VALIDATION_OK);

        tickets.forEach(ticket -> {
            Long skuId = ticket.getId() * 1000;
            ticket.setExistingMboPskuId(skuId);
            ticket.setMappingConfidence(MappingConfidenceType.PARTNER_FAST);
            gcSkuTicketDao.update(ticket);
            when(modelStorageHelper.findModels(any(), eq(false)))
                    .thenReturn(Collections.singletonList(ModelStorage.Model.newBuilder()
                            .setId(skuId)
                            .setCategoryId(CATEGORY_ID).build()));
        });
        long ticketId = tickets.get(0).getId();

        List<GcSkuValidation> mandatoryOkValidations =
                ApplyCskuValidationResultsTaskAction.MANDATORY_VALIDATION_TYPES.stream()
                        .map(type -> createValidation(ticketId, type, true))
                        .collect(Collectors.toList());
        List<GcSkuValidation> otherFailValidations = Stream.of(GcSkuValidationType.CLEAN_WEB_TEXT,
                        GcSkuValidationType.HTML_TAGS_EXISTENCE)
                .map(type -> createValidation(ticketId, type, false))
                .collect(Collectors.toList());

        mandatoryOkValidations.forEach(validation -> gcSkuValidationDao.insert(validation));
        otherFailValidations.forEach(validation -> gcSkuValidationDao.insert(validation));

        ProcessDataBucketData bucketData = new ProcessDataBucketData(dataBucketId);
        taskAction.doRun(bucketData);

        GcSkuTicket ticket = gcSkuTicketDao.fetchOneById(ticketId);
        assertThat(ticket.getValid()).isFalse();
        assertThat(ticket.getStatus()).isEqualTo(RESULT_UPLOAD_STARTED);
    }

    private Consumer<DatacampOffer> emptySettings() {
        return offer -> {
        };
    }

    private void setTicketExistingMboPskuId(Long ticketId, Long pskuId) {
        dsl().update(Tables.GC_SKU_TICKET)
                .set(Tables.GC_SKU_TICKET.EXISTING_MBO_PSKU_ID, pskuId)
                .where(Tables.GC_SKU_TICKET.ID.eq(ticketId))
                .execute();
    }

    private GcSkuValidation createValidation(long ticketId, GcSkuValidationType type, boolean isValid) {
        GcSkuValidation validation = new GcSkuValidation();
        validation.setSkuTicketId(ticketId);
        validation.setValidationType(type);
        validation.setCheckDate(Timestamp.from(Instant.now()));
        validation.setIsOk(isValid);
        return validation;
    }
}
