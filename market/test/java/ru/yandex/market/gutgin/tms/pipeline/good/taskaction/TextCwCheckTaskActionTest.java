package ru.yandex.market.gutgin.tms.pipeline.good.taskaction;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.acw.api.AcwApiService;
import ru.yandex.market.acw.api.CheckTextVerdict;
import ru.yandex.market.acw.api.CheckTextsResponse;
import ru.yandex.market.acw.api.RequestMode;
import ru.yandex.market.gutgin.tms.config.TestCleanWebConfig;
import ru.yandex.market.gutgin.tms.exceptions.ProblemsException;
import ru.yandex.market.gutgin.tms.service.CwTextExtractor;
import ru.yandex.market.gutgin.tms.service.CwTextValidator;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.CwTextOfferSkipDao;
import ru.yandex.market.partner.content.common.db.dao.ProtocolMessageDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuTicketDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuValidationDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcValidationMessageDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuValidationType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuValidation;
import ru.yandex.market.partner.content.common.db.jooq.tables.records.CwTextOfferSkipRecord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.acw.api.Text.TextVerdict.TEXT_AUTO_GOOD;
import static ru.yandex.market.acw.api.Text.TextVerdict.TEXT_AUTO_PORNO;
import static ru.yandex.market.acw.api.Text.TextVerdict.TEXT_TOLOKA_NO_SENSE;
import static ru.yandex.market.gutgin.tms.service.CwTextValidator.FOREIGN_BOOK_CATEGORY_ID;

@ContextConfiguration(classes = {TestCleanWebConfig.class})
public class TextCwCheckTaskActionTest extends DBDcpStateGenerator {

    private TextCwCheckTaskAction action;
    private AcwApiService acwApiService;

    @Autowired
    GcSkuValidationDao gcSkuValidation;

    @Autowired
    GcSkuTicketDao gcSkuTicketDao;

    @Autowired
    CwTextOfferSkipDao cwTextOfferSkipDao;

    CwTextExtractor textExtractor;
    CwTextValidator textValidator;

    @Autowired
    ProtocolMessageDao protocolMessageDao;

    @Autowired
    GcValidationMessageDao gcValidationMessageDao;

    @Autowired
    CategoryDataKnowledge categoryDataKnowledge;

    @Before
    public void setup() {
        acwApiService = mock(AcwApiService.class);
        textExtractor = new CwTextExtractor(categoryDataKnowledge);
        textValidator = new CwTextValidator(Set.of(1L, 2L, 3L), Set.of(4L, 5L, 6L));

        action = new TextCwCheckTaskAction(gcSkuValidation, gcSkuTicketDao, acwApiService, cwTextOfferSkipDao,
                textExtractor, textValidator, protocolMessageDao, gcValidationMessageDao);
    }

    @Test
    public void textIsSkipped() throws ProblemsException {
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now().plusDays(1));

        List<GcSkuTicket> tickets = generateDBDcpInitialStateNew(2);
        gcSkuTicketDao.setStatus(tickets.stream()
                .map(GcSkuTicket::getId).collect(Collectors.toList()), GcSkuTicketStatus.CW_MANUAL_VALIDATION_STARTED);

        tickets.forEach(ticket ->
                cwTextOfferSkipDao.putInCache(
                        new CwTextOfferSkipRecord((long) ticket.getPartnerShopId(),
                                ticket.getShopSku(),
                                timestamp))
        );

        var res = action.doRun(processDataBucketData);

        assertThat(res.hasProblems()).isFalse();
        verify(acwApiService, times(0)).checkText(any());
    }

    @Test
    public void acwTextReponseEmpty() throws ProblemsException {
        List<GcSkuTicket> tickets = generateDBDcpInitialStateNew(2);
        gcSkuTicketDao.setStatus(tickets.stream()
                .map(GcSkuTicket::getId).collect(Collectors.toList()), GcSkuTicketStatus.CW_MANUAL_VALIDATION_STARTED);

        CheckTextsResponse response = CheckTextsResponse.newBuilder()
                .addAllTextVerdicts(tickets.stream()
                        .map(ticket -> CheckTextVerdict.newBuilder()
                                .setBusinessId(ticket.getPartnerShopId())
                                .setOfferId(ticket.getShopSku())
                                .addAllVerdicts(new ArrayList<>())
                                .setRequestMode(RequestMode.DEFAULT).build())
                        .collect(Collectors.toList()))
                .build();

        when(acwApiService.checkText(any())).thenReturn(response);

        var res = action.doRun(processDataBucketData);

        assertThat(res.hasProblems()).isTrue();
        assertThat(res.getProblems().get(0).getDescription()).contains("Waiting for clean web");

        var validations = gcSkuValidation.getGcSkuValidations(GcSkuValidationType.CLEAN_WEB_TEXT,
                Iterables.toArray(tickets.stream().map(GcSkuTicket::getId).collect(Collectors.toList()), Long.class));

        assertThat(validations).isEmpty();
    }

    @Test
    public void acwTextResponseNonEmptyPassed() throws ProblemsException {
        List<GcSkuTicket> tickets = generateDBDcpInitialStateNew(2);
        setCategoryForTicketsAndDatabucket(tickets, 1L);
        gcSkuTicketDao.setStatus(tickets.stream()
                .map(GcSkuTicket::getId).collect(Collectors.toList()), GcSkuTicketStatus.CW_MANUAL_VALIDATION_STARTED);

        CheckTextsResponse response = CheckTextsResponse.newBuilder()
                .addAllTextVerdicts(tickets.stream()
                        .map(ticket -> CheckTextVerdict.newBuilder()
                                .setBusinessId(ticket.getPartnerShopId())
                                .setOfferId(ticket.getShopSku())
                                .addAllVerdicts(List.of(TEXT_AUTO_GOOD))
                                .setRequestMode(RequestMode.DEFAULT).build())
                        .collect(Collectors.toList()))
                .build();

        when(acwApiService.checkText(any())).thenReturn(response);

        var res = action.doRun(processDataBucketData);

        assertThat(res.hasProblems()).isFalse();

        var validations = gcSkuValidation.getGcSkuValidations(GcSkuValidationType.CLEAN_WEB_TEXT,
                Iterables.toArray(tickets.stream().map(GcSkuTicket::getId).collect(Collectors.toList()), Long.class));

        assertThat(validations.size()).isEqualTo(2);
        assertThat(validations.stream().allMatch(GcSkuValidation::getIsOk)).isTrue();

        var validationMessages = gcValidationMessageDao.fetchByValidationId(validations.stream()
                .map(GcSkuValidation::getId)
                .collect(Collectors.toList()));

        assertThat(validationMessages).isEmpty();
    }

    @Test
    public void skippedVerdictsForBookCategoryPassed() throws ProblemsException {
        List<GcSkuTicket> tickets = generateDBDcpInitialStateNew(2);
        setCategoryForTicketsAndDatabucket(List.of(tickets.get(0)), FOREIGN_BOOK_CATEGORY_ID);
        setCategoryForTicketsAndDatabucket(List.of(tickets.get(1)), 1111L);
        gcSkuTicketDao.setStatus(tickets.stream()
                .map(GcSkuTicket::getId).collect(Collectors.toList()), GcSkuTicketStatus.CW_MANUAL_VALIDATION_STARTED);

        CheckTextsResponse response = CheckTextsResponse.newBuilder()
                .addAllTextVerdicts(tickets.stream()
                        .map(ticket -> CheckTextVerdict.newBuilder()
                                .setBusinessId(ticket.getPartnerShopId())
                                .setOfferId(ticket.getShopSku())
                                .addAllVerdicts(List.of(TEXT_AUTO_GOOD, TEXT_TOLOKA_NO_SENSE))
                                .setRequestMode(RequestMode.DEFAULT).build())
                        .collect(Collectors.toList()))
                .build();

        when(acwApiService.checkText(any())).thenReturn(response);

        var res = action.doRun(processDataBucketData);

        assertThat(res.hasProblems()).isFalse();

        var validations = gcSkuValidation.getGcSkuValidations(GcSkuValidationType.CLEAN_WEB_TEXT,
                Iterables.toArray(tickets.stream().map(GcSkuTicket::getId).collect(Collectors.toList()), Long.class));

        assertThat(validations.size()).isEqualTo(2);
        assertThat(validations.stream().filter(GcSkuValidation::getIsOk).count()).isOne();
        assertThat(validations.stream().filter(validation -> !validation.getIsOk()).count()).isOne();

        var validationMessages = gcValidationMessageDao.fetchByValidationId(validations.stream()
                .map(GcSkuValidation::getId)
                .collect(Collectors.toList()));

        assertThat(validationMessages.size()).isOne();
        assertThat(protocolMessageDao.getMessageInfo(validationMessages.get(0).getProtocolMessageId()).getCode())
                .isEqualTo("ir.partner_content.goodcontent.validation.cw.text.no_sense");
    }

    @Test
    public void skippedVerdictsForIntimCategoryPassed() throws ProblemsException {
        List<GcSkuTicket> tickets = generateDBDcpInitialStateNew(2);
        setCategoryForTicketsAndDatabucket(List.of(tickets.get(0)), 1L);
        setCategoryForTicketsAndDatabucket(List.of(tickets.get(1)), 1111L);
        gcSkuTicketDao.setStatus(tickets.stream()
                .map(GcSkuTicket::getId).collect(Collectors.toList()), GcSkuTicketStatus.CW_MANUAL_VALIDATION_STARTED);

        CheckTextsResponse response = CheckTextsResponse.newBuilder()
                .addAllTextVerdicts(tickets.stream()
                        .map(ticket -> CheckTextVerdict.newBuilder()
                                .setBusinessId(ticket.getPartnerShopId())
                                .setOfferId(ticket.getShopSku())
                                .addAllVerdicts(List.of(TEXT_AUTO_GOOD, TEXT_AUTO_PORNO))
                                .setRequestMode(RequestMode.DEFAULT).build())
                        .collect(Collectors.toList()))
                .build();

        when(acwApiService.checkText(any())).thenReturn(response);

        var res = action.doRun(processDataBucketData);

        assertThat(res.hasProblems()).isFalse();

        var validations = gcSkuValidation.getGcSkuValidations(GcSkuValidationType.CLEAN_WEB_TEXT,
                Iterables.toArray(tickets.stream().map(GcSkuTicket::getId).collect(Collectors.toList()), Long.class));

        assertThat(validations.size()).isEqualTo(2);
        assertThat(validations.stream().filter(GcSkuValidation::getIsOk).count()).isOne();
        assertThat(validations.stream().filter(validation -> !validation.getIsOk()).count()).isOne();

        var validationMessages = gcValidationMessageDao.fetchByValidationId(validations.stream()
                .map(GcSkuValidation::getId)
                .collect(Collectors.toList()));

        assertThat(validationMessages.size()).isOne();
        assertThat(protocolMessageDao.getMessageInfo(validationMessages.get(0).getProtocolMessageId()).getCode())
                .isEqualTo("ir.partner_content.goodcontent.validation.cw.text.porno");
    }
}
