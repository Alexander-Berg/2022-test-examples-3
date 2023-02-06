package ru.yandex.market.billing.imports.partner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.orginfo.model.OrganizationType;

import static org.mockito.ArgumentMatchers.any;

/**
 * Тест для {@link PartnerLegalInfoImportService}
 */
class PartnerLegalInfoImportServiceTest extends FunctionalTest {

    private static final List<PartnerLegalInfo> PARTNER_LEGAL_INFOS = List.of(
            PartnerLegalInfo.builder()
                    .setDatasourceId(1L)
                    .setRequestId(3L)
                    .setOrganizationType(OrganizationType.IP)
                    .setInn("123456789qwerty")
                    .setAccountNum("1234567890123456")
                    .setBik("BIKXXXX")
                    .setOrgName("OrgName")
                    .setSellerClientId(2L)
                    .setUpdatedAt(LocalDateTime.of(2021, 9, 9, 9, 9, 9))
                    .setOgrn("OGRN")
                    .setJurAddress("JurAddress 1B")
                    .setCorrAccNum("1234567890123456")
                    .setKpp("KPP")
                    .setStartDate(LocalDateTime.of(2021, 8, 8, 8, 8, 8, 8000))
                    .build()
    );
    private static final LocalDate IMPORT_DATE = LocalDate.of(2021, 8, 31);
    @Autowired
    private PartnerLegalInfoDao partnerLegalInfoDao;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private static PartnerLegalInfoYtDao mockPartnerLegalInfoYtDao() {
        PartnerLegalInfoYtDao partnerLegalInfoYtDao = Mockito.mock(PartnerLegalInfoYtDao.class);

        Mockito.doAnswer(invocation -> true).when(partnerLegalInfoYtDao).verifyYtTablesExistence(any(LocalDate.class));

        Mockito.when(partnerLegalInfoYtDao.importPartnerLegalInfo(any(LocalDate.class)))
                .thenAnswer(invocation -> PARTNER_LEGAL_INFOS);

        return partnerLegalInfoYtDao;
    }

    @Test
    @DisplayName("Импорт юридических данных поставщика из ытя в постргес")
    @DbUnitDataSet(
            before = "partnerLegalInfoImportFromYt.before.csv",
            after = "partnerLegalInfoImportFromYt.after.csv"
    )
    public void testImportPartnerLegalInfo() {
        PartnerLegalInfoYtDao partnerLegalInfoYtDao = mockPartnerLegalInfoYtDao();
        PartnerLegalInfoImportService partnerLegalInfoImportService =
                new PartnerLegalInfoImportService(partnerLegalInfoYtDao, transactionTemplate, partnerLegalInfoDao);
        partnerLegalInfoImportService.process(IMPORT_DATE);

        Mockito.verify(partnerLegalInfoYtDao, Mockito.times(1))
                .verifyYtTablesExistence(Mockito.eq(IMPORT_DATE));
        Mockito.verify(partnerLegalInfoYtDao, Mockito.times(1))
                .importPartnerLegalInfo(Mockito.eq(IMPORT_DATE));
        Mockito.verifyNoMoreInteractions(partnerLegalInfoYtDao);
    }

    @Test
    @DisplayName("Полная очистка таблицы ПГ перед импортом из Ытя юридических данных поставщика")
    @DbUnitDataSet(
            before = "partnerLegalInfoDeleteBeforeImportFromYt.before.csv",
            after = "partnerLegalInfoDeleteBeforeImportFromYt.after.csv"
    )
    public void testDeleteBeforeImportPartnerLegalInfo() {
        PartnerLegalInfoYtDao partnerLegalInfoYtDao = mockPartnerLegalInfoYtDao();
        PartnerLegalInfoImportService partnerLegalInfoImportService =
                new PartnerLegalInfoImportService(partnerLegalInfoYtDao, transactionTemplate, partnerLegalInfoDao);
        partnerLegalInfoImportService.process(IMPORT_DATE);

        Mockito.verify(partnerLegalInfoYtDao, Mockito.times(1))
                .verifyYtTablesExistence(Mockito.eq(IMPORT_DATE));
        Mockito.verify(partnerLegalInfoYtDao, Mockito.times(1))
                .importPartnerLegalInfo(Mockito.eq(IMPORT_DATE));
        Mockito.verifyNoMoreInteractions(partnerLegalInfoYtDao);
    }
}
