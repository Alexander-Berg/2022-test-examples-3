package ru.yandex.market.tpl.billing.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tpl.billing.model.PartnerType;
import ru.yandex.market.tpl.billing.model.entity.IncomingPayment;
import ru.yandex.market.tpl.billing.model.entity.Partner;
import ru.yandex.market.tpl.billing.repository.EnvironmentRepository;
import ru.yandex.market.tpl.billing.repository.IncomingPaymentRepository;
import ru.yandex.market.tpl.billing.repository.PartnerRepository;
import ru.yandex.market.tpl.billing.service.yt.YtService;
import ru.yandex.market.tpl.billing.service.yt.imports.YtIncomingPaymentImportService;
import ru.yandex.market.tpl.billing.util.TestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

// TODO: Replace by the DBUnit test (https://st.yandex-team.ru/MARKETTPLBILL-82).
@ExtendWith(SpringExtension.class)
class YtIncomingPaymentImportServiceTest {

    private static final LocalDate TODAY = LocalDate.parse("2021-01-28");

    private static final LocalDate YESTERDAY = LocalDate.parse("2021-01-27");

    private static final LocalDate DATE24 = LocalDate.parse("2021-01-24");

    private static final LocalDate DATE25 = LocalDate.parse("2021-01-25");

    private static final LocalDate DATE26 = LocalDate.parse("2021-01-26");

    private static final String PATH = "//home/market/production/oebs";

    private static final String FOLDER = "payments";

    private static final String INCOMING_PAYMENTS_DAYS_FOR_IMPORT_PROPERTY_KEY = "incomingPaymentsDaysForImport";

    private static final int INCOMING_PAYMENTS_DAYS_FOR_IMPORT = 35;

    @Mock
    private YtService ytService;

    @Mock
    private IncomingPaymentRepository incomingPaymentRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    private YtIncomingPaymentImportService ytIncomingPaymentImportService;

    @BeforeEach
    void setUp() {
        Clock clock = TestableClock.fixed(
            TODAY.atStartOfDay().toInstant(OffsetDateTime.now().getOffset()),
            ZoneId.systemDefault()
        );

        ytIncomingPaymentImportService = new YtIncomingPaymentImportService(
            ytService,
            incomingPaymentRepository,
            partnerRepository,
            environmentRepository,
            transactionTemplate,
            clock
        );

        ytIncomingPaymentImportService.setPath(PATH);
        ytIncomingPaymentImportService.setFolder(FOLDER);

        when(partnerRepository.findAll()).thenReturn(getPartners());

        when(ytService.importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(DATE25)),
            any(Function.class)
        )).thenReturn(Optional.of(getIncomingPayments25()));

        when(ytService.importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(DATE26)),
            any(Function.class)
        )).thenReturn(Optional.of(getIncomingPayments26()));

        when(transactionTemplate.execute(any())).thenAnswer(TestUtils::doInTransaction);
    }

    @Test
    void importForLastDates() {
        when(incomingPaymentRepository.findLastStatementDate()).thenReturn(Optional.of(DATE24));

        ytIncomingPaymentImportService.importForLastDates();

        verify(incomingPaymentRepository).findLastStatementDate();

        verify(partnerRepository, times(3)).findAll();

        verify(ytService).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(DATE25)),
            any(Function.class)
        );

        verify(ytService).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(DATE26)),
            any(Function.class)
        );

        verify(ytService).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(YESTERDAY)),
            any(Function.class)
        );

        verify(incomingPaymentRepository).saveAll(eq(getIncomingPayments25()));

        verify(incomingPaymentRepository).saveAll(eq(getIncomingPayments26()));

        verifyNoMoreInteractions(partnerRepository, ytService, incomingPaymentRepository);

        verifyNoInteractions(environmentRepository);
    }

    @Test
    void importForLastDatesRange() {
        when(environmentRepository.getIntValueOrThrow(INCOMING_PAYMENTS_DAYS_FOR_IMPORT_PROPERTY_KEY))
            .thenReturn(INCOMING_PAYMENTS_DAYS_FOR_IMPORT);

        ytIncomingPaymentImportService.importForLastDatesRange();

        verify(environmentRepository).getIntValueOrThrow(INCOMING_PAYMENTS_DAYS_FOR_IMPORT_PROPERTY_KEY);

        verify(partnerRepository, times(INCOMING_PAYMENTS_DAYS_FOR_IMPORT + 1)).findAll();

        verify(ytService, times(INCOMING_PAYMENTS_DAYS_FOR_IMPORT + 1)).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            anyString(),
            any(Function.class)
        );

        verify(incomingPaymentRepository).saveAll(eq(getIncomingPayments25()));

        verify(incomingPaymentRepository).saveAll(eq(getIncomingPayments26()));

        verifyNoMoreInteractions(partnerRepository, ytService, incomingPaymentRepository, environmentRepository);
    }

    @Test
    void importForDatesRange() {
        ytIncomingPaymentImportService.importForDatesRange(DATE25, DATE26);

        verify(partnerRepository, times(2)).findAll();

        verify(ytService).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(DATE25)),
            any(Function.class)
        );

        verify(ytService).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(DATE26)),
            any(Function.class)
        );

        verify(incomingPaymentRepository).saveAll(eq(getIncomingPayments25()));

        verify(incomingPaymentRepository).saveAll(eq(getIncomingPayments26()));

        verifyNoMoreInteractions(partnerRepository, ytService, incomingPaymentRepository);

        verifyNoInteractions(environmentRepository);
    }

    @Test
    void importForDatesRangeFirstDateEarlier() {
        ytIncomingPaymentImportService.importForDatesRange(DATE24, DATE26);

        verify(partnerRepository, times(3)).findAll();

        verify(ytService).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(DATE24)),
            any(Function.class)
        );

        verify(ytService).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(DATE25)),
            any(Function.class)
        );

        verify(ytService).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(DATE26)),
            any(Function.class)
        );

        verify(incomingPaymentRepository).saveAll(eq(getIncomingPayments25()));

        verify(incomingPaymentRepository).saveAll(eq(getIncomingPayments26()));

        verifyNoMoreInteractions(partnerRepository, ytService, incomingPaymentRepository);

        verifyNoInteractions(environmentRepository);
    }

    @Test
    void importForDatesRangeLastDateLater() {
        ytIncomingPaymentImportService.importForDatesRange(DATE25, YESTERDAY);

        verify(partnerRepository, times(3)).findAll();

        verify(ytService).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(DATE25)),
            any(Function.class)
        );

        verify(ytService).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(DATE26)),
            any(Function.class)
        );

        verify(ytService).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(YESTERDAY)),
            any(Function.class)
        );

        verify(incomingPaymentRepository).saveAll(eq(getIncomingPayments25()));

        verify(incomingPaymentRepository).saveAll(eq(getIncomingPayments26()));

        verifyNoMoreInteractions(partnerRepository, ytService, incomingPaymentRepository);

        verifyNoInteractions(environmentRepository);
    }

    @Test
    void importForDatesRangeFirstDateEarlierLastDateLater() {
        ytIncomingPaymentImportService.importForDatesRange(DATE24, YESTERDAY);

        verify(partnerRepository, times(4)).findAll();

        verify(ytService).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(DATE24)),
            any(Function.class)
        );

        verify(ytService).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(DATE25)),
            any(Function.class)
        );

        verify(ytService).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(DATE26)),
            any(Function.class)
        );

        verify(ytService).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(YESTERDAY)),
            any(Function.class)
        );

        verify(incomingPaymentRepository).saveAll(eq(getIncomingPayments25()));

        verify(incomingPaymentRepository).saveAll(eq(getIncomingPayments26()));

        verifyNoMoreInteractions(partnerRepository, ytService, incomingPaymentRepository);

        verifyNoInteractions(environmentRepository);
    }

    @Test
    void importForDatesRangeOneDate() {
        ytIncomingPaymentImportService.importForDatesRange(DATE25, DATE25);

        verifyOneDate(DATE25, getIncomingPayments25());
    }

    @Test
    void importForDatesRangeOneEmptyDate() {
        ytIncomingPaymentImportService.importForDatesRange(DATE24, DATE24);

        verifyOneEmptyDate(DATE24);
    }

    @Test
    void importForDatesRangeEmptyDates() {
        ytIncomingPaymentImportService.importForDatesRange(YESTERDAY, TODAY);

        verify(partnerRepository, times(2)).findAll();

        verify(ytService).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(YESTERDAY)),
            any(Function.class)
        );

        verify(ytService).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(TODAY)),
            any(Function.class)
        );

        verifyNoMoreInteractions(partnerRepository, ytService);

        verifyNoInteractions(incomingPaymentRepository, environmentRepository);
    }

    @Test
    void importForDatesFirstDateLaterThanLastDate() {
        ytIncomingPaymentImportService.importForDatesRange(DATE26, DATE25);

        verifyNoInteractions(partnerRepository, ytService, incomingPaymentRepository, environmentRepository);
    }

    @Test
    void importForDate() {
        ytIncomingPaymentImportService.importForDate(DATE25);

        verifyOneDate(DATE25, getIncomingPayments25());
    }

    @Test
    void importForEmptyDate() {
        ytIncomingPaymentImportService.importForDate(DATE24);

        verifyOneEmptyDate(DATE24);
    }

    private void verifyOneDate(LocalDate date, Collection<IncomingPayment> incomingPayments) {
        verify(partnerRepository).findAll();

        verify(ytService).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(date)),
            any(Function.class)
        );

        verify(incomingPaymentRepository).saveAll(eq(incomingPayments));

        verifyNoMoreInteractions(partnerRepository, ytService, incomingPaymentRepository);

        verifyNoInteractions(environmentRepository);
    }

    private void verifyOneEmptyDate(LocalDate date) {
        verify(partnerRepository).findAll();

        verify(ytService).importFromYt(
            eq(IncomingPayment.class),
            eq(PATH),
            eq(FOLDER),
            eq(YtService.getDateTableName(date)),
            any(Function.class)
        );

        verifyNoMoreInteractions(partnerRepository, ytService);

        verifyNoInteractions(incomingPaymentRepository, environmentRepository);
    }

    private List<Partner> getPartners() {
        return List.of(
            new Partner()
                .setId(10372297L)
                .setVirtualAccountNumber("DOSTAVKA_P_10372297")
                .setName("Partner 10372297")
                .setPartnerType(PartnerType.PVZ),
            new Partner()
                .setId(10372298L)
                .setVirtualAccountNumber("DOSTAVKA_P_10372298")
                .setName("Partner 10372298")
                .setPartnerType(PartnerType.PVZ)
        );
    }

    private Collection<IncomingPayment> getIncomingPayments25() {
        return List.of(
            new IncomingPayment()
                .setKey(
                    new IncomingPayment.IncomingPaymentKey().setStatementNumber("ABC123").setStatementLineNumber(1L)
                )
                .setStatementDate(LocalDate.parse("2021-01-25"))
                .setStatementTrxText("Statement Trx Text 1")
                .setCheckAppliedAmount(BigDecimal.valueOf(1000.0))
                .setPartnerId(10372297L),
            new IncomingPayment()
                .setKey(
                    new IncomingPayment.IncomingPaymentKey().setStatementNumber("DEF456").setStatementLineNumber(2L)
                )
                .setStatementDate(LocalDate.parse("2021-01-25"))
                .setStatementTrxText("Statement Trx Text 2")
                .setCheckAppliedAmount(BigDecimal.valueOf(2000.0))
                .setPartnerId(10372298L)
        );
    }

    private Collection<IncomingPayment> getIncomingPayments26() {
        return List.of(
            new IncomingPayment()
                .setKey(
                    new IncomingPayment.IncomingPaymentKey().setStatementNumber("ABC124").setStatementLineNumber(3L)
                )
                .setStatementDate(LocalDate.parse("2021-01-26"))
                .setStatementTrxText("Statement Trx Text 1")
                .setCheckAppliedAmount(BigDecimal.valueOf(1000.0))
                .setPartnerId(10372297L),
            new IncomingPayment()
                .setKey(
                    new IncomingPayment.IncomingPaymentKey().setStatementNumber("DEF457").setStatementLineNumber(4L)
                )
                .setStatementDate(LocalDate.parse("2021-01-26"))
                .setStatementTrxText("Statement Trx Text 2")
                .setCheckAppliedAmount(BigDecimal.valueOf(2000.0))
                .setPartnerId(10372298L)
        );
    }
}
