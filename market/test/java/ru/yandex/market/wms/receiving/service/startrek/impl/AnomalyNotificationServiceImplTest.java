package ru.yandex.market.wms.receiving.service.startrek.impl;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.wms.common.model.dto.startrek.AttachItem;
import ru.yandex.market.wms.common.model.dto.startrek.BaseIssueDto;
import ru.yandex.market.wms.common.spring.dao.entity.Receipt;
import ru.yandex.market.wms.common.spring.exception.NotFoundException;
import ru.yandex.market.wms.receiving.BaseReceivingTest;
import ru.yandex.market.wms.receiving.dao.entity.ReceiptAnomaly;
import ru.yandex.market.wms.receiving.repository.ReceiptSummaryRepository;
import ru.yandex.market.wms.receiving.service.AnomalyService;
import ru.yandex.market.wms.shared.libs.async.jms.DestNamingUtils;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnomalyNotificationServiceImplTest extends BaseReceivingTest {

    @InjectMocks
    private AnomalyNotificationServiceImpl anomalyNotificationService;
    @Mock
    private AnomalyService anomalyService;
    @Mock
    private ReceiptSummaryRepository summaryRepository;

    @Test
    public void notFoundReceipt() {
        //given
        String receiptKey = "rcpt234";
        when(receiptDao.findReceiptByKey(any())).thenReturn(Optional.empty());

        //when
        NotFoundException notFoundException = assertThrows(NotFoundException.class,
                () -> anomalyNotificationService.sendAnomalyNotification(receiptKey));
        assertThat("message of exception", notFoundException.getMessage(),
                equalTo("404 NOT_FOUND \"Cannot create startrek task for receipt " + receiptKey +
                        ". Receipt not found.\""));
    }

    @Test
    public void oneAnomaly() {
        //given
        String receiptKey = "rcpt234";
        mockReceipt(receiptKey, "Брак");

        //given
        anomalyNotificationService.sendAnomalyNotification(receiptKey);

        //then
        ArgumentCaptor<BaseIssueDto> issueDtoCaptor = ArgumentCaptor.forClass(BaseIssueDto.class);
        verify(jmsTemplate, times(1)).convertAndSend(
                eq(DestNamingUtils.WAREHOUSE_QUEUE_PREFIX + "anomalies"), issueDtoCaptor.capture(), any());
        BaseIssueDto issueDto = issueDtoCaptor.getValue();
        assertThat("receipt key", issueDto.getReceiptKey(), equalTo(receiptKey));
        assertThat("task description", issueDto.getDescription(), nullValue());
        assertThat("anomalies quantity", issueDto.getAnomaliesQuantity(), equalTo(1.0));
        assertThat("anomalies price", issueDto.getAnomaliesPrice(), equalTo(123.450));
        assertThat("недостача", issueDto.getDivergence(), equalTo(1.0));
        assertThat("anomalies price", issueDto.getExpectedQuantity(), equalTo(2.0));
        assertThat("supplier", issueDto.getSupplierName(), equalTo("anomaly supplier name"));
        assertThat("receipt type", issueDto.getXDoc(), equalTo("Прямая поставка"));

        //tags
        Set<String> tags = issueDto.getTags();
        assertThat("tags size", tags, hasSize(2));
        assertThat("tags size", tags, hasItem(receiptKey));
        assertThat("tags size", tags, hasItem("Аномалия на возврат"));

        //xls attachment
        assertThat("xls attached", issueDto.getAttachments().stream()
                .anyMatch(attachItem -> attachItem.getFilename().equals(issueDto.getReceiptKey() + "_anomalies.xlsx")));
    }

    @Test
    public void returnAnomaly() {
        //given
        String receiptKey = "rcpt234";
        mockReceipt(receiptKey, "Нет ШК");

        //given
        anomalyNotificationService.sendAnomalyNotification(receiptKey);

        //then
        ArgumentCaptor<BaseIssueDto> issueDtoCaptor = ArgumentCaptor.forClass(BaseIssueDto.class);
        verify(jmsTemplate, times(1)).convertAndSend(
                eq(DestNamingUtils.WAREHOUSE_QUEUE_PREFIX + "anomalies"), issueDtoCaptor.capture(), any());
        BaseIssueDto issueDto = issueDtoCaptor.getValue();
        //tags
        Set<String> tags = issueDto.getTags();
        assertThat("tags size", tags, hasSize(2));
        assertThat("Тэг аномалии на возврат", tags, hasItem("Аномалия на возврат"));
    }

    @Test
    public void anomalyAdditionalReceiptTag() {
        //given
        String receiptKey = "rcpt234";
        mockReceipt(receiptKey, "Товар не заявлен в поставке");

        //given
        anomalyNotificationService.sendAnomalyNotification(receiptKey);

        //then
        ArgumentCaptor<BaseIssueDto> issueDtoCaptor = ArgumentCaptor.forClass(BaseIssueDto.class);
        verify(jmsTemplate, times(1)).convertAndSend(
                eq(DestNamingUtils.WAREHOUSE_QUEUE_PREFIX + "anomalies"), issueDtoCaptor.capture(), any());
        BaseIssueDto issueDto = issueDtoCaptor.getValue();
        //tags
        Set<String> tags = issueDto.getTags();
        assertThat("tags size", tags, hasSize(2));
        assertThat("Тэг аномалии на допоставку", tags, hasItems("rcpt234", "Аномалия для допоставки"));
        assertThat("Вложения", issueDto.getAttachments().size(), is(2));
    }

    @Test
    public void emptyAnomalyList() {
        //given
        String receiptKey = "rcpt234";
        Receipt receipt = receipt(receiptKey);
        when(receiptDao.findReceiptByKey(any())).thenReturn(Optional.of(receipt));
        when(nSqlConfigDao.getStringConfigValue("YM_STARTREK_INTEGRATION")).thenReturn("1");

        //when
        anomalyNotificationService.sendAnomalyNotification(receiptKey);

        //then
        verify(jmsTemplate, times(0)).convertAndSend(
                eq(DestNamingUtils.WAREHOUSE_QUEUE_PREFIX + "anomalies"), any(), any());
    }

    @Test
    public void tagsAnomalyReturn() {
        //given
        String receiptKey = "rcpt234";
        Receipt receipt = receipt(receiptKey);
        when(receiptDao.findReceiptByKey(any())).thenReturn(Optional.of(receipt));
        when(nSqlConfigDao.getStringConfigValue("YM_STARTREK_INTEGRATION")).thenReturn("1");
        ArrayList<ReceiptAnomaly> receiptAnomalies = newArrayList(
                receiptAnomaly(receipt, "tranU1", "Брак"),
                receiptAnomaly(receipt, "tranU1", "Просрочено по ОСГ"));
        when(anomalyService.getReceiptAnomalies(receipt)).thenReturn(receiptAnomalies);

        //when
        anomalyNotificationService.sendAnomalyNotification(receiptKey);

        //then
        ArgumentCaptor<BaseIssueDto> issueDtoCaptor = ArgumentCaptor.forClass(BaseIssueDto.class);
        verify(jmsTemplate, times(1)).convertAndSend(
                eq(DestNamingUtils.WAREHOUSE_QUEUE_PREFIX + "anomalies"), issueDtoCaptor.capture(), any());
        BaseIssueDto issueDto = issueDtoCaptor.getValue();
        Set<String> tags = issueDto.getTags();
        assertThat("tags size", tags, hasSize(2));
        assertThat("Тэг аномалии на возврат", tags, hasItems("rcpt234", "Аномалия на возврат"));
    }

    @Test
    public void tagsAnomalyAdditionalReceipt() {
        //given
        String receiptKey = "rcpt234";
        Receipt receipt = receipt(receiptKey);
        when(receiptDao.findReceiptByKey(any())).thenReturn(Optional.of(receipt));
        when(nSqlConfigDao.getStringConfigValue("YM_STARTREK_INTEGRATION")).thenReturn("1");
        ArrayList<ReceiptAnomaly> receiptAnomalies = newArrayList(
                receiptAnomaly(receipt, "tranU1", "Не настроен в системе"),
                receiptAnomaly(receipt, "tranU1", "Товар не заявлен в поставке"));
        when(anomalyService.getReceiptAnomalies(receipt)).thenReturn(receiptAnomalies);

        //when
        anomalyNotificationService.sendAnomalyNotification(receiptKey);

        //then
        ArgumentCaptor<BaseIssueDto> issueDtoCaptor = ArgumentCaptor.forClass(BaseIssueDto.class);
        verify(jmsTemplate, times(1)).convertAndSend(
                eq(DestNamingUtils.WAREHOUSE_QUEUE_PREFIX + "anomalies"), issueDtoCaptor.capture(), any());
        BaseIssueDto issueDto = issueDtoCaptor.getValue();
        Set<String> tags = issueDto.getTags();
        assertThat("tags size", tags, hasSize(2));
        assertThat("Тэг аномалии на допоставку", tags, hasItems("rcpt234", "Аномалия для допоставки"));
    }

    @Test
    public void tagsAnomalyReturn_mixedTare() {
        //given
        String receiptKey = "rcpt234";
        Receipt receipt = receipt(receiptKey);
        when(receiptDao.findReceiptByKey(any())).thenReturn(Optional.of(receipt));
        when(nSqlConfigDao.getStringConfigValue("YM_STARTREK_INTEGRATION")).thenReturn("1");
        ArrayList<ReceiptAnomaly> receiptAnomalies = newArrayList(
                receiptAnomaly(receipt, "tranU1", "Неразрешенный излишек"),
                receiptAnomaly(receipt, "tranU1", "Товар не заявлен в поставке"));
        when(anomalyService.getReceiptAnomalies(receipt)).thenReturn(receiptAnomalies);

        //when
        anomalyNotificationService.sendAnomalyNotification(receiptKey);

        //then
        ArgumentCaptor<BaseIssueDto> issueDtoCaptor = ArgumentCaptor.forClass(BaseIssueDto.class);
        verify(jmsTemplate, times(1)).convertAndSend(
                eq(DestNamingUtils.WAREHOUSE_QUEUE_PREFIX + "anomalies"), issueDtoCaptor.capture(), any());
        BaseIssueDto issueDto = issueDtoCaptor.getValue();
        Set<String> tags = issueDto.getTags();
        assertThat("tags size", tags, hasSize(2));
        assertThat("Тэг аномалии на возврат", tags, hasItems("rcpt234", "Аномалия на возврат"));
    }

    @Test
    public void tagsAnomalyAdditionalReceipt_severalTares() {
        //given
        String receiptKey = "rcpt234";
        Receipt receipt = receipt(receiptKey);
        when(receiptDao.findReceiptByKey(any())).thenReturn(Optional.of(receipt));
        when(nSqlConfigDao.getStringConfigValue("YM_STARTREK_INTEGRATION")).thenReturn("1");
        ArrayList<ReceiptAnomaly> receiptAnomalies = newArrayList(
                receiptAnomaly(receipt, "tranU1", "Не настроен в системе"),
                receiptAnomaly(receipt, "tranU2", "Товар не заявлен в поставке"));
        when(anomalyService.getReceiptAnomalies(receipt)).thenReturn(receiptAnomalies);

        //when
        anomalyNotificationService.sendAnomalyNotification(receiptKey);

        //then
        ArgumentCaptor<BaseIssueDto> issueDtoCaptor = ArgumentCaptor.forClass(BaseIssueDto.class);
        verify(jmsTemplate, times(1)).convertAndSend(
                eq(DestNamingUtils.WAREHOUSE_QUEUE_PREFIX + "anomalies"), issueDtoCaptor.capture(), any());
        BaseIssueDto issueDto = issueDtoCaptor.getValue();
        Set<String> tags = issueDto.getTags();
        assertThat("tags size", tags, hasSize(2));
        assertThat("Тэг аномалии на допоставку", tags, hasItems("rcpt234", "Аномалия для допоставки"));
    }

    @Test
    public void tagsAnomalyAdditionalReceipt_bothTags() {
        //given
        String receiptKey = "rcpt234";
        Receipt receipt = receipt(receiptKey);
        when(receiptDao.findReceiptByKey(any())).thenReturn(Optional.of(receipt));
        when(nSqlConfigDao.getStringConfigValue("YM_STARTREK_INTEGRATION")).thenReturn("1");
        ArrayList<ReceiptAnomaly> receiptAnomalies = newArrayList(
                receiptAnomaly(receipt, "tranU1", "Не настроен в системе"),
                receiptAnomaly(receipt, "tranU1", "Не настроен в системе"),
                receiptAnomaly(receipt, "tranU1", "Брак"),
                receiptAnomaly(receipt, "tranU2", "Товар не заявлен в поставке"),
                receiptAnomaly(receipt, "tranU2", "Не настроен в системе"),
                receiptAnomaly(receipt, "tranU2", "Товар не заявлен в поставке"));
        when(anomalyService.getReceiptAnomalies(receipt)).thenReturn(receiptAnomalies);

        //when
        anomalyNotificationService.sendAnomalyNotification(receiptKey);

        //then
        ArgumentCaptor<BaseIssueDto> issueDtoCaptor = ArgumentCaptor.forClass(BaseIssueDto.class);
        verify(jmsTemplate, times(1)).convertAndSend(
                eq(DestNamingUtils.WAREHOUSE_QUEUE_PREFIX + "anomalies"), issueDtoCaptor.capture(), any());
        BaseIssueDto issueDto = issueDtoCaptor.getValue();
        Set<String> tags = issueDto.getTags();
        assertThat("tags size", tags, hasSize(3));
        assertThat("Тэг аномалии на допоставку", tags,
                hasItems("rcpt234", "Аномалия для допоставки", "Аномалия на возврат"));
    }

    protected void mockReceipt(String receiptKey, String anomalyType) {
        Receipt receipt = receipt(receiptKey);
        when(receiptDao.findReceiptByKey(receiptKey)).thenReturn(Optional.of(receipt));
        ReceiptAnomaly receiptAnomaly = receiptAnomaly(receipt, 1);
        receiptAnomaly.setAnomalyType(anomalyType);
        when(anomalyService.getReceiptAnomalies(any())).thenReturn(newArrayList(receiptAnomaly));
        when(receiptDetailDao.getSkuBalances(receiptKey)).thenReturn(newArrayList(sku(receipt, 1)));
        when(receiptPdfReportService.createReport(any(), any(), any())).thenReturn(new AttachItem(receiptKey + ".pdf",
                new byte[]{1, 0, 1, 0}, "application/pdf"));
        when(nSqlConfigDao.getStringConfigValue("YM_STARTREK_INTEGRATION")).thenReturn("1");
    }

    protected ReceiptAnomaly receiptAnomaly(Receipt receipt, String transportUnitId, String anomalyType) {
        ReceiptAnomaly receiptAnomaly = receiptAnomaly(receipt, 1);
        receiptAnomaly.setTransportUnitId(transportUnitId);
        receiptAnomaly.setAnomalyType(anomalyType);
        return receiptAnomaly;
    }
}
