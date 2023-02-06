package ru.yandex.market.billing.api.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.billing.api.FunctionalTest;
import ru.yandex.market.billing.api.model.ContractFrequencyPair;
import ru.yandex.market.billing.api.model.CurrentAndNextMonthPayoutFrequency;
import ru.yandex.market.billing.core.OperatingUnit;
import ru.yandex.market.billing.core.Platform;
import ru.yandex.market.billing.core.factoring.ContractPayoutFrequency;
import ru.yandex.market.billing.core.factoring.ContractPayoutFrequencyDao;
import ru.yandex.market.billing.core.factoring.PayoutFrequency;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractPayoutFrequencyServiceTest extends FunctionalTest {

    @Autowired
    ContractPayoutFrequencyDao contractPayoutFrequencyDaoMock;

    @Autowired
    ContractPayoutFrequencyService service;

    @Autowired
    TestableClock clock;

    @Captor
    ArgumentCaptor<List<ContractPayoutFrequency>> recordsCaptor;

    @Test
    @DisplayName("Создание расписания для нового контракта")
    void addCurrentMonthFrequencyForNewContract() {
        long contractId = 23957L;
        PayoutFrequency frequency = PayoutFrequency.BI_WEEKLY;
        Instant now = Instant.parse("2021-09-14T16:16:16Z");
        clock.setFixed(now, ZoneId.of("UTC"));

        when(contractPayoutFrequencyDaoMock.getEndlessFrequencyRecords(
                List.of(contractId), List.of(Platform.YANDEX_MARKET))
        ).thenReturn(List.of());

        assertTrue(service.addCurrentMonthFrequencyForNewContract(contractId, frequency));

        verify(contractPayoutFrequencyDaoMock).upsertFrequencies(List.of(ContractPayoutFrequency.builder()
                .setContractId(contractId)
                .setStartDate(LocalDate.parse("2021-09-01"))
                .setFrequency(frequency)
                .setUpdatedAt(now)
                .setOrgId(OperatingUnit.YANDEX_MARKET)
                .setPlatform(Platform.YANDEX_MARKET)
                .build()
        ));
    }

    @Test
    @DisplayName("Запись уже существует, но она такая же - ок - нужна для идемпотентности создания")
    void addCurrentMonthFrequencyForNewContractAlreadyExists() {
        long contractId = 23957L;
        PayoutFrequency frequency = PayoutFrequency.MONTHLY;
        Instant now = Instant.parse("2021-09-14T16:16:16Z");
        clock.setFixed(now, ZoneId.of("UTC"));

        when(contractPayoutFrequencyDaoMock.getEndlessFrequencyRecords(
                List.of(contractId), List.of(Platform.YANDEX_MARKET))
        ).thenReturn(List.of(ContractPayoutFrequency.builder()
                        .setContractId(contractId)
                        .setStartDate(LocalDate.parse("2021-09-01"))
                        .setFrequency(frequency)
                        .setUpdatedAt(now)
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setPlatform(Platform.YANDEX_MARKET)
                        .build()));

        assertFalse(service.addCurrentMonthFrequencyForNewContract(contractId, frequency));

        verify(contractPayoutFrequencyDaoMock, never()).upsertFrequencies(any());
    }

    @Test
    @DisplayName("Для контракта уже существует расписание с другой периодичностью выплат")
    void addCurrentMonthFrequencyForNewContractConflictOtherFrequency() {
        long contractId = 23957L;
        Instant now = Instant.parse("2021-09-14T16:16:16Z");
        clock.setFixed(now, ZoneId.of("UTC"));

        when(contractPayoutFrequencyDaoMock.getEndlessFrequencyRecords(
                List.of(contractId), List.of(Platform.YANDEX_MARKET))
        ).thenReturn(List.of(ContractPayoutFrequency.builder()
                        .setContractId(contractId)
                        .setStartDate(LocalDate.parse("2021-09-01"))
                        .setFrequency(PayoutFrequency.WEEKLY)
                        .setUpdatedAt(now)
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setPlatform(Platform.YANDEX_MARKET)
                .build()));

        assertThrows(IllegalStateException.class,
                () -> service.addCurrentMonthFrequencyForNewContract(contractId, PayoutFrequency.DAILY));
    }


    @Test
    @DisplayName("Для контракта уже существует расписание на другой месяц до текущего")
    void addCurrentMonthFrequencyForNewContractConflictOtherMonthBefore() {
        long contractId = 23957L;
        PayoutFrequency frequency = PayoutFrequency.BI_WEEKLY;
        Instant now = Instant.parse("2021-09-14T16:16:16Z");
        clock.setFixed(now, ZoneId.of("UTC"));

        when(contractPayoutFrequencyDaoMock.getEndlessFrequencyRecords(
                List.of(contractId), List.of(Platform.YANDEX_MARKET))
        ).thenReturn(List.of(ContractPayoutFrequency.builder()
                        .setContractId(contractId)
                        .setStartDate(LocalDate.parse("2021-08-01"))
                        .setFrequency(frequency)
                        .setUpdatedAt(now)
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setPlatform(Platform.YANDEX_MARKET)
                .build()));


        assertThrows(IllegalStateException.class,
                () -> service.addCurrentMonthFrequencyForNewContract(contractId, frequency));
    }

    @Test
    @DisplayName("Для контракта уже существует расписание на другой месяц следующий за текущим")
    void addCurrentMonthFrequencyForNewContractNotConflictNextMonth() {
        long contractId = 23957L;
        PayoutFrequency frequency = PayoutFrequency.BI_WEEKLY;
        Instant now = Instant.parse("2021-09-14T16:16:16Z");
        clock.setFixed(now, ZoneId.of("UTC"));

        when(contractPayoutFrequencyDaoMock.getEndlessFrequencyRecords(
                List.of(contractId), List.of(Platform.YANDEX_MARKET))
        ).thenReturn(List.of(ContractPayoutFrequency.builder()
                        .setContractId(contractId)
                        .setStartDate(LocalDate.parse("2021-10-01"))
                        .setFrequency(frequency)
                        .setUpdatedAt(now)
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setPlatform(Platform.YANDEX_MARKET)
                .build()));

        assertTrue(service.addCurrentMonthFrequencyForNewContract(contractId, frequency));

        verify(contractPayoutFrequencyDaoMock).upsertFrequencies(List.of(ContractPayoutFrequency.builder()
                .setContractId(contractId)
                .setStartDate(LocalDate.parse("2021-09-01"))
                .setEndDate(LocalDate.parse("2021-10-01"))
                .setFrequency(frequency)
                .setUpdatedAt(now)
                .setOrgId(OperatingUnit.YANDEX_MARKET)
                .setPlatform(Platform.YANDEX_MARKET)
                .build()
        ));
    }

    @Test
    @DisplayName("Запись расписания на следующий месяц: пустой список контрактов с расписаниями")
    void setNextMonthPayoutFrequenciesEmptyContractList() {
        long contractId = 23957L;
        PayoutFrequency frequency = PayoutFrequency.BI_WEEKLY;
        Instant now = Instant.parse("2021-09-14T16:16:16Z");
        clock.setFixed(now, ZoneId.of("UTC"));

        service.setNextMonthPayoutFrequencies(Collections.emptyList());

        verify(contractPayoutFrequencyDaoMock, never()).upsertFrequencies(any());
    }

    @Test
    @DisplayName("Запись расписания на следующий месяц: запись на след мес уже есть с таким же расписанием")
    void setNextMonthPayoutFrequenciesNextMonthRecordWithSameFrequencyExists() {
        long contractId = 23957L;
        PayoutFrequency frequency = PayoutFrequency.BI_WEEKLY;
        Instant now = Instant.parse("2021-09-14T16:16:16Z");
        clock.setFixed(now, ZoneId.of("UTC"));

        when(contractPayoutFrequencyDaoMock.findFrequencies(List.of(contractId), LocalDate.parse("2021-10-01"),
                LocalDate.parse("2021-11-01"), List.of(Platform.YANDEX_MARKET)))
                .thenReturn(List.of(ContractPayoutFrequency.builder()
                        .setContractId(contractId)
                        .setStartDate(LocalDate.parse("2021-10-01"))
                        .setFrequency(frequency)
                        .setUpdatedAt(now)
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setPlatform(Platform.YANDEX_MARKET)
                        .build()));

        service.setNextMonthPayoutFrequencies(List.of(ContractFrequencyPair.of(contractId, frequency)));

        verify(contractPayoutFrequencyDaoMock, never()).upsertFrequencies(any());
    }

    @Test
    @DisplayName("Запись расписания на следующий месяц: запись на след мес уже есть с другим расписанием")
    void setNextMonthPayoutFrequenciesNextMonthRecordWithDifferentFrequencyExists() {
        long contractId = 23957L;
        Instant now = Instant.parse("2021-09-14T16:16:16Z");
        clock.setFixed(now, ZoneId.of("UTC"));

        when(contractPayoutFrequencyDaoMock.findFrequencies(List.of(contractId), LocalDate.parse("2021-10-01"),
                LocalDate.parse("2021-11-01"), List.of(Platform.YANDEX_MARKET)))
                .thenReturn(List.of(ContractPayoutFrequency.builder()
                        .setContractId(contractId)
                        .setStartDate(LocalDate.parse("2021-10-01"))
                        .setFrequency(PayoutFrequency.WEEKLY)
                        .setUpdatedAt(now)
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setPlatform(Platform.YANDEX_MARKET)
                        .build()));

        service.setNextMonthPayoutFrequencies(List.of(ContractFrequencyPair.of(contractId, PayoutFrequency.DAILY)));

        verify(contractPayoutFrequencyDaoMock).upsertFrequencies(eq(List.of(ContractPayoutFrequency.builder()
                .setContractId(contractId)
                .setStartDate(LocalDate.parse("2021-10-01"))
                .setFrequency(PayoutFrequency.DAILY)
                .setUpdatedAt(now)
                .setOrgId(OperatingUnit.YANDEX_MARKET)
                .setPlatform(Platform.YANDEX_MARKET)
                .build())));
    }

    @Test
    @DisplayName("Запись расписания на следующий месяц:" +
            " есть запись на текущий мес без конечной даты с таким же расписанием")
    void setNextMonthPayoutFrequenciesCurrentMonthRecordWithSameFrequencyExists() {
        long contractId = 23957L;
        PayoutFrequency frequency = PayoutFrequency.WEEKLY;
        Instant now = Instant.parse("2021-09-14T16:16:16Z");
        clock.setFixed(now, ZoneId.of("UTC"));

        when(contractPayoutFrequencyDaoMock.findFrequencies(List.of(contractId), LocalDate.parse("2021-10-01"),
                LocalDate.parse("2021-11-01"), List.of(Platform.YANDEX_MARKET)))
                .thenReturn(List.of(ContractPayoutFrequency.builder()
                        .setContractId(contractId)
                        .setStartDate(LocalDate.parse("2021-09-01"))
                        .setFrequency(frequency)
                        .setUpdatedAt(now)
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setPlatform(Platform.YANDEX_MARKET)
                        .build()));

        service.setNextMonthPayoutFrequencies(List.of(ContractFrequencyPair.of(contractId, frequency)));

        verify(contractPayoutFrequencyDaoMock, never()).upsertFrequencies(any());
    }

    @Test
    @DisplayName("Запись расписания на следующий месяц:" +
            " есть запись на текущий мес без конечной даты с другим расписанием")
    void setNextMonthPayoutFrequenciesCurrentMonthRecordWithDifferentFrequencyExists() {
        long contractId = 23957L;
        Instant now = Instant.parse("2021-09-14T16:16:16Z");
        clock.setFixed(now, ZoneId.of("UTC"));

        when(contractPayoutFrequencyDaoMock.findFrequencies(List.of(contractId), LocalDate.parse("2021-10-01"),
                LocalDate.parse("2021-11-01"), List.of(Platform.YANDEX_MARKET)))
                .thenReturn(List.of(ContractPayoutFrequency.builder()
                        .setContractId(contractId)
                        .setStartDate(LocalDate.parse("2021-09-01"))
                        .setFrequency(PayoutFrequency.BI_WEEKLY)
                        .setUpdatedAt(now)
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setPlatform(Platform.YANDEX_MARKET)
                        .build()));

        service.setNextMonthPayoutFrequencies(List.of(ContractFrequencyPair.of(contractId, PayoutFrequency.DAILY)));

        verify(contractPayoutFrequencyDaoMock).upsertFrequencies(recordsCaptor.capture());

        assertThat(recordsCaptor.getValue(), containsInAnyOrder(
                ContractPayoutFrequency.builder()
                        .setContractId(contractId)
                        .setStartDate(LocalDate.parse("2021-09-01"))
                        .setEndDate(LocalDate.parse("2021-10-01"))
                        .setFrequency(PayoutFrequency.BI_WEEKLY)
                        .setUpdatedAt(now)
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setPlatform(Platform.YANDEX_MARKET)
                        .build(),
                ContractPayoutFrequency.builder()
                        .setContractId(contractId)
                        .setStartDate(LocalDate.parse("2021-10-01"))
                        .setFrequency(PayoutFrequency.DAILY)
                        .setUpdatedAt(now)
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setPlatform(Platform.YANDEX_MARKET)
                        .build()
        ));
    }

    @Test
    @DisplayName("Запись расписания на следующий месяц: нет записей - создаем новую")
    void setNextMonthPayoutFrequenciesNoRecords() {
        long contractId = 23957L;
        PayoutFrequency frequency = PayoutFrequency.DAILY;
        Instant now = Instant.parse("2021-09-14T16:16:16Z");
        clock.setFixed(now, ZoneId.of("UTC"));

        when(contractPayoutFrequencyDaoMock.findFrequencies(List.of(contractId), LocalDate.parse("2021-10-01"),
                LocalDate.parse("2021-11-01"), List.of(Platform.YANDEX_MARKET)))
                .thenReturn(Collections.emptyList());

        service.setNextMonthPayoutFrequencies(List.of(ContractFrequencyPair.of(contractId, frequency)));

        verify(contractPayoutFrequencyDaoMock).upsertFrequencies(eq(List.of(ContractPayoutFrequency.builder()
                .setContractId(contractId)
                .setStartDate(LocalDate.parse("2021-10-01"))
                .setFrequency(frequency)
                .setUpdatedAt(now)
                .setOrgId(OperatingUnit.YANDEX_MARKET)
                .setPlatform(Platform.YANDEX_MARKET)
                .build())));
    }


    @Test
    @DisplayName("Запись расписания на следующий месяц: несколько контрактов")
    void setNextMonthPayoutFrequenciesMultipleContracts() {
        long id1 = 23957L;
        long id2 = 99792L;
        long id3 = 63568L;
        Instant now = Instant.parse("2021-09-14T16:16:16Z");
        clock.setFixed(now, ZoneId.of("UTC"));

        when(contractPayoutFrequencyDaoMock.findFrequencies(
                argThat(l -> l.containsAll(List.of(id1, id2, id3))),
                eq(LocalDate.parse("2021-10-01")),
                eq(LocalDate.parse("2021-11-01")),
                argThat(l -> l.contains(Platform.YANDEX_MARKET)))
        ).thenReturn(List.of(
                        ContractPayoutFrequency.builder()
                                .setContractId(id1)
                                .setStartDate(LocalDate.parse("2021-09-01"))
                                .setFrequency(PayoutFrequency.BI_WEEKLY)
                                .setUpdatedAt(now)
                                .setOrgId(OperatingUnit.YANDEX_MARKET)
                                .setPlatform(Platform.YANDEX_MARKET)
                                .build(),
                        ContractPayoutFrequency.builder()
                                .setContractId(id2)
                                .setStartDate(LocalDate.parse("2021-09-01"))
                                .setFrequency(PayoutFrequency.BI_WEEKLY)
                                .setUpdatedAt(now)
                                .setOrgId(OperatingUnit.YANDEX_MARKET)
                                .setPlatform(Platform.YANDEX_MARKET)
                                .build()
                ));

        service.setNextMonthPayoutFrequencies(List.of(
                ContractFrequencyPair.of(id1, PayoutFrequency.DAILY),
                ContractFrequencyPair.of(id2, PayoutFrequency.BI_WEEKLY),
                ContractFrequencyPair.of(id3, PayoutFrequency.WEEKLY)
        ));

        verify(contractPayoutFrequencyDaoMock).upsertFrequencies(recordsCaptor.capture());

        assertThat(recordsCaptor.getValue(), containsInAnyOrder(
                ContractPayoutFrequency.builder()
                        .setContractId(id1)
                        .setStartDate(LocalDate.parse("2021-09-01"))
                        .setEndDate(LocalDate.parse("2021-10-01"))
                        .setFrequency(PayoutFrequency.BI_WEEKLY)
                        .setUpdatedAt(now)
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setPlatform(Platform.YANDEX_MARKET)
                        .build(),
                ContractPayoutFrequency.builder()
                        .setContractId(id1)
                        .setStartDate(LocalDate.parse("2021-10-01"))
                        .setFrequency(PayoutFrequency.DAILY)
                        .setUpdatedAt(now)
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setPlatform(Platform.YANDEX_MARKET)
                        .build(),
                ContractPayoutFrequency.builder()
                        .setContractId(id3)
                        .setStartDate(LocalDate.parse("2021-10-01"))
                        .setFrequency(PayoutFrequency.WEEKLY)
                        .setUpdatedAt(now)
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setPlatform(Platform.YANDEX_MARKET)
                        .build()
        ));
    }

    @Test
    @DisplayName("Чтение частот выплат на текущи и след мес: одна запись на оба")
    void getCurrentAndNextMonthPayoutFrequenciesOneRecordForCurMonth() {
        long contractId = 23957L;
        PayoutFrequency frequency = PayoutFrequency.WEEKLY;
        Instant now = Instant.parse("2021-09-14T16:16:16Z");
        clock.setFixed(now, ZoneId.of("UTC"));

        when(contractPayoutFrequencyDaoMock.findFrequencies(List.of(contractId), LocalDate.parse("2021-09-01"),
                LocalDate.parse("2021-11-01"), List.of(Platform.YANDEX_MARKET)))
                .thenReturn(List.of(ContractPayoutFrequency.builder()
                        .setContractId(contractId)
                        .setStartDate(LocalDate.parse("2021-09-01"))
                        .setFrequency(frequency)
                        .setUpdatedAt(now)
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setPlatform(Platform.YANDEX_MARKET)
                        .build()));

        assertThat(service.getCurrentAndNextMonthPayoutFrequencies(List.of(contractId)), containsInAnyOrder(
                CurrentAndNextMonthPayoutFrequency.builder()
                        .setContractId(contractId)
                        .setCurrentMonthFrequency(PayoutFrequency.WEEKLY)
                        .setIsDefaultCurrentMonthFrequency(false)
                        .setNextMonthFrequency(PayoutFrequency.WEEKLY)
                        .build()
        ));
    }

    @Test
    @DisplayName("Чтение частот выплат на текущи и след мес: одна запись на оба")
    void getCurrentAndNextMonthPayoutFrequenciesRecordsForCurAndNextMonths() {
        long contractId = 23957L;
        Instant now = Instant.parse("2021-09-14T16:16:16Z");
        clock.setFixed(now, ZoneId.of("UTC"));

        when(contractPayoutFrequencyDaoMock.findFrequencies(List.of(contractId), LocalDate.parse("2021-09-01"),
                LocalDate.parse("2021-11-01"), List.of(Platform.YANDEX_MARKET)))
                .thenReturn(List.of(
                        ContractPayoutFrequency.builder()
                                .setContractId(contractId)
                                .setStartDate(LocalDate.parse("2021-01-01"))
                                .setEndDate(LocalDate.parse("2021-10-01"))
                                .setFrequency(PayoutFrequency.DAILY)
                                .setUpdatedAt(now)
                                .setOrgId(OperatingUnit.YANDEX_MARKET)
                                .setPlatform(Platform.YANDEX_MARKET)
                                .build(),
                        ContractPayoutFrequency.builder()
                                .setContractId(contractId)
                                .setStartDate(LocalDate.parse("2021-10-01"))
                                .setFrequency(PayoutFrequency.BI_WEEKLY)
                                .setUpdatedAt(now)
                                .setOrgId(OperatingUnit.YANDEX_MARKET)
                                .setPlatform(Platform.YANDEX_MARKET)
                                .build()
                ));

        assertThat(service.getCurrentAndNextMonthPayoutFrequencies(List.of(contractId)), containsInAnyOrder(
                CurrentAndNextMonthPayoutFrequency.builder()
                        .setContractId(contractId)
                        .setCurrentMonthFrequency(PayoutFrequency.DAILY)
                        .setIsDefaultCurrentMonthFrequency(false)
                        .setNextMonthFrequency(PayoutFrequency.BI_WEEKLY)
                        .build()
        ));
    }

    @Test
    @DisplayName("Чтение частот выплат на текущи и след мес: есть запись на след мес, текущий по дефолту")
    void getCurrentAndNextMonthPayoutFrequenciesRecordsCurMonthDefault() {
        long contractId = 23957L;
        Instant now = Instant.parse("2021-09-14T16:16:16Z");
        clock.setFixed(now, ZoneId.of("UTC"));

        when(contractPayoutFrequencyDaoMock.findFrequencies(List.of(contractId), LocalDate.parse("2021-09-01"),
                LocalDate.parse("2021-11-01"), List.of(Platform.YANDEX_MARKET)))
                .thenReturn(List.of(
                        ContractPayoutFrequency.builder()
                                .setContractId(contractId)
                                .setStartDate(LocalDate.parse("2021-10-01"))
                                .setFrequency(PayoutFrequency.WEEKLY)
                                .setUpdatedAt(now)
                                .setOrgId(OperatingUnit.YANDEX_MARKET)
                                .setPlatform(Platform.YANDEX_MARKET)
                                .build()
                ));

        when(contractPayoutFrequencyDaoMock.getDefaultFrequencies(LocalDate.parse("2021-09-01"), LocalDate.parse(
                "2021-11-01"), List.of(Platform.YANDEX_MARKET)))
                .thenReturn(List.of(
                        ContractPayoutFrequency.builder()
                                .setContractId(contractId)
                                .setStartDate(LocalDate.parse("2021-01-01"))
                                .setFrequency(PayoutFrequency.BI_WEEKLY)
                                .setUpdatedAt(now)
                                .setOrgId(OperatingUnit.YANDEX_MARKET)
                                .setPlatform(Platform.YANDEX_MARKET)
                                .build()
                ));

        assertThat(service.getCurrentAndNextMonthPayoutFrequencies(List.of(contractId)), containsInAnyOrder(
                CurrentAndNextMonthPayoutFrequency.builder()
                        .setContractId(contractId)
                        .setCurrentMonthFrequency(PayoutFrequency.BI_WEEKLY)
                        .setIsDefaultCurrentMonthFrequency(true)
                        .setNextMonthFrequency(PayoutFrequency.WEEKLY)
                        .build()
        ));
    }

    @Test
    @DisplayName("Чтение частот выплат на текущи и след мес: одна запись по дефолту")
    void getCurrentAndNextMonthPayoutFrequenciesRecordsOneDefaultRecord() {
        long contractId = 23957L;
        Instant now = Instant.parse("2021-09-14T16:16:16Z");
        clock.setFixed(now, ZoneId.of("UTC"));

        when(contractPayoutFrequencyDaoMock.getDefaultFrequencies(LocalDate.parse("2021-09-01"), LocalDate.parse(
                "2021-11-01"), List.of(Platform.YANDEX_MARKET)))
                .thenReturn(List.of(
                        ContractPayoutFrequency.builder()
                                .setContractId(contractId)
                                .setStartDate(LocalDate.parse("2021-01-01"))
                                .setFrequency(PayoutFrequency.BI_WEEKLY)
                                .setUpdatedAt(now)
                                .setOrgId(OperatingUnit.YANDEX_MARKET)
                                .setPlatform(Platform.YANDEX_MARKET)
                                .build()
                ));

        assertThat(service.getCurrentAndNextMonthPayoutFrequencies(List.of(contractId)), containsInAnyOrder(
                CurrentAndNextMonthPayoutFrequency.builder()
                        .setContractId(contractId)
                        .setCurrentMonthFrequency(PayoutFrequency.BI_WEEKLY)
                        .setIsDefaultCurrentMonthFrequency(true)
                        .setNextMonthFrequency(PayoutFrequency.BI_WEEKLY)
                        .build()
        ));
    }

    @Test
    @DisplayName("Чтение частот выплат на текущи и след мес: две дефолтные записи")
    void getCurrentAndNextMonthPayoutFrequenciesRecordsTwoDefaultRecords() {
        long contractId = 23957L;
        Instant now = Instant.parse("2021-09-14T16:16:16Z");
        clock.setFixed(now, ZoneId.of("UTC"));

        when(contractPayoutFrequencyDaoMock.getDefaultFrequencies(LocalDate.parse("2021-09-01"), LocalDate.parse(
                "2021-11-01"), List.of(Platform.YANDEX_MARKET)))
                .thenReturn(List.of(
                        ContractPayoutFrequency.builder()
                                .setContractId(contractId)
                                .setStartDate(LocalDate.parse("2021-01-01"))
                                .setEndDate(LocalDate.parse("2021-10-01"))
                                .setFrequency(PayoutFrequency.WEEKLY)
                                .setUpdatedAt(now)
                                .setOrgId(OperatingUnit.YANDEX_MARKET)
                                .setPlatform(Platform.YANDEX_MARKET)
                                .build(),
                        ContractPayoutFrequency.builder()
                                .setContractId(contractId)
                                .setStartDate(LocalDate.parse("2021-10-01"))
                                .setFrequency(PayoutFrequency.DAILY)
                                .setUpdatedAt(now)
                                .setOrgId(OperatingUnit.YANDEX_MARKET)
                                .setPlatform(Platform.YANDEX_MARKET)
                                .build()
                ));

        assertThat(service.getCurrentAndNextMonthPayoutFrequencies(List.of(contractId)), containsInAnyOrder(
                CurrentAndNextMonthPayoutFrequency.builder()
                        .setContractId(contractId)
                        .setCurrentMonthFrequency(PayoutFrequency.WEEKLY)
                        .setIsDefaultCurrentMonthFrequency(true)
                        .setNextMonthFrequency(PayoutFrequency.DAILY)
                        .build()
        ));
    }

    @Test
    @DisplayName("Чтение частот выплат на текущи и след мес: два контракта")
    void getCurrentAndNextMonthPayoutFrequenciesRecordsTwoContracts() {
        long id1 = 23957L;
        long id2 = 87263L;
        Instant now = Instant.parse("2021-09-14T16:16:16Z");
        clock.setFixed(now, ZoneId.of("UTC"));

        when(contractPayoutFrequencyDaoMock.findFrequencies(
                eq(List.of(id1, id2)),
                eq(LocalDate.parse("2021-09-01")),
                eq(LocalDate.parse("2021-11-01")),
                eq(List.of(Platform.YANDEX_MARKET)))
        ).thenReturn(List.of(
                        ContractPayoutFrequency.builder()
                                .setContractId(id1)
                                .setStartDate(LocalDate.parse("2021-08-01"))
                                .setFrequency(PayoutFrequency.WEEKLY)
                                .setUpdatedAt(now)
                                .setOrgId(OperatingUnit.YANDEX_MARKET)
                                .setPlatform(Platform.YANDEX_MARKET)
                                .build()
                ));

        when(contractPayoutFrequencyDaoMock.getDefaultFrequencies(LocalDate.parse("2021-09-01"), LocalDate.parse(
                "2021-11-01"), List.of(Platform.YANDEX_MARKET)))
                .thenReturn(List.of(
                        ContractPayoutFrequency.builder()
                                .setContractId(id2)
                                .setStartDate(LocalDate.parse("2021-01-01"))
                                .setFrequency(PayoutFrequency.DAILY)
                                .setUpdatedAt(now)
                                .setOrgId(OperatingUnit.YANDEX_MARKET)
                                .setPlatform(Platform.YANDEX_MARKET)
                                .build()
                ));

        assertThat(service.getCurrentAndNextMonthPayoutFrequencies(List.of(id1, id2)), containsInAnyOrder(
                CurrentAndNextMonthPayoutFrequency.builder()
                        .setContractId(id1)
                        .setCurrentMonthFrequency(PayoutFrequency.WEEKLY)
                        .setIsDefaultCurrentMonthFrequency(false)
                        .setNextMonthFrequency(PayoutFrequency.WEEKLY)
                        .build(),
                CurrentAndNextMonthPayoutFrequency.builder()
                        .setContractId(id2)
                        .setCurrentMonthFrequency(PayoutFrequency.DAILY)
                        .setIsDefaultCurrentMonthFrequency(true)
                        .setNextMonthFrequency(PayoutFrequency.DAILY)
                        .build()
        ));
    }

}
