package ru.yandex.market.tpl.billing.service;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.client.billing.BillingClient;
import ru.yandex.market.tpl.client.billing.dto.BillingBalanceInfoDto;
import ru.yandex.market.tpl.client.billing.dto.BillingCompanyContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingCompanyDto;
import ru.yandex.market.tpl.client.billing.dto.BillingPagingInfoDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserDto;
import ru.yandex.market.tpl.client.billing.dto.BillingUserType;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TplCompaniesAndUsersImportServiceTest extends AbstractFunctionalTest {

    private static final long COMPANY_1_ID = 1;
    private static final long COMPANY_2_ID = 2;
    private static final int PAGE = 0;
    private static final int BATCH = 1000;

    @Autowired
    private BillingClient billingClient;

    @Autowired
    private TplCompaniesAndUsersImportService tplCompaniesAndUsersImportService;

    @AfterEach
    public void tearDown() {
        verify(billingClient).findCompanies(PAGE, BATCH);
        verifyNoMoreInteractions(billingClient);
    }

    @Test
    @DbUnitDataSet(after = "/database/service/tplcompaniesandusersimport/after/companies_and_users_imported.csv")
    void importTplCompaniesAndUsers() {
        testImportTplCompaniesAndUsers();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplcompaniesandusersimport/before/company_1_user_1.csv",
            after = "/database/service/tplcompaniesandusersimport/after/companies_and_users_imported.csv")
    void importTplCompaniesAndUsersIncludingOneWhichAlreadyExists() {
        testImportTplCompaniesAndUsers();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplcompaniesandusersimport/before/company_3_user_5.csv",
            after = "/database/service/tplcompaniesandusersimport/after/companies_and_users_imported_3_companies.csv")
    void importTplCompaniesAndUsersAdditionallyToOneWhichAlreadyExists() {
        testImportTplCompaniesAndUsers();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplcompaniesandusersimport/before/companies_and_users.csv",
            after = "/database/service/tplcompaniesandusersimport/after/companies_and_users_imported.csv")
    void importTplCompaniesAndUsersNotAllUsersForCompany2() {
        when(billingClient.findCompanies(PAGE, BATCH)).thenReturn(getBillingCompanyContainerDto());
        when(billingClient.findUsers(COMPANY_1_ID, PAGE, BATCH)).thenReturn(getBillingUserContainerDtoForCompany1());
        when(billingClient.findUsers(COMPANY_2_ID, PAGE, BATCH)).thenReturn(getBillingUserContainerDtoForCompany2NotAllUsers());

        tplCompaniesAndUsersImportService.importTplCompaniesAndUsers();

        verify(billingClient).findUsers(COMPANY_1_ID, PAGE, BATCH);
        verify(billingClient).findUsers(COMPANY_2_ID, PAGE, BATCH);
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplcompaniesandusersimport/before/companies_and_users.csv",
            after = "/database/service/tplcompaniesandusersimport/after/companies_and_users_imported.csv")
    void importTplCompaniesAndUsersNoUsersForCompany2() {
        when(billingClient.findCompanies(PAGE, BATCH)).thenReturn(getBillingCompanyContainerDto());
        when(billingClient.findUsers(COMPANY_1_ID, PAGE, BATCH)).thenReturn(getBillingUserContainerDtoForCompany1());
        when(billingClient.findUsers(COMPANY_2_ID, PAGE, BATCH)).thenReturn(new BillingUserContainerDto(
                new BillingPagingInfoDto(0, 2, 1, 2L), List.of()));

        tplCompaniesAndUsersImportService.importTplCompaniesAndUsers();

        verify(billingClient).findUsers(COMPANY_1_ID, PAGE, BATCH);
        verify(billingClient).findUsers(COMPANY_2_ID, PAGE, BATCH);
    }

    @Test
    @DbUnitDataSet(after = "/database/service/tplcompaniesandusersimport/after/empty.csv")
    void importNoTplCompaniesAndUsers() {
        testImportNoTplCompaniesAndUsers();
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/tplcompaniesandusersimport/before/companies_and_users.csv",
            after = "/database/service/tplcompaniesandusersimport/after/companies_and_users_imported.csv")
    void importNoTplCompaniesAndUsersWhenExists() {
        testImportNoTplCompaniesAndUsers();
    }

    private void testImportTplCompaniesAndUsers() {
        when(billingClient.findCompanies(PAGE, BATCH)).thenReturn(getBillingCompanyContainerDto());
        when(billingClient.findUsers(COMPANY_1_ID, PAGE, BATCH)).thenReturn(getBillingUserContainerDtoForCompany1());
        when(billingClient.findUsers(COMPANY_2_ID, PAGE, BATCH)).thenReturn(getBillingUserContainerDtoForCompany2());

        tplCompaniesAndUsersImportService.importTplCompaniesAndUsers();

        verify(billingClient).findUsers(COMPANY_1_ID, PAGE, BATCH);
        verify(billingClient).findUsers(COMPANY_2_ID, PAGE, BATCH);
    }

    private void testImportNoTplCompaniesAndUsers() {
        when(billingClient.findCompanies(PAGE, BATCH)).thenReturn(new BillingCompanyContainerDto(
                new BillingPagingInfoDto(0, 0, 0, 0L),
                List.of())
        );

        tplCompaniesAndUsersImportService.importTplCompaniesAndUsers();
    }

    private BillingCompanyContainerDto getBillingCompanyContainerDto() {
        return new BillingCompanyContainerDto(
                new BillingPagingInfoDto(0, 2, 1, 2L),
                List.of(
                        new BillingCompanyDto(
                                COMPANY_1_ID, "1", "Хорошая компания", "77212345",
                                false, true, "12345772", null
                        ),
                        new BillingCompanyDto(
                                COMPANY_2_ID, "2", "Такая себе компания", "77212346",
                                true, false, "12346772",
                                new BillingBalanceInfoDto("companyClientId", "companyPersonId", "companyContractId")
                        )
                ));
    }

    private BillingUserContainerDto getBillingUserContainerDtoForCompany1() {
        return new BillingUserContainerDto(
                new BillingPagingInfoDto(0, 2, 1, 2L),
                List.of(
                        new BillingUserDto(
                                1L, "1", 111L, "a@a.a",
                                "Аааев Ааай Аааевич", BillingUserType.SELF_EMPLOYED, "8-800-555-35-35",
                                new BillingBalanceInfoDto("userClientId1", "userPersonId1", "userContractId1")
                        ),
                        new BillingUserDto(
                                2L, "2", 222L, "b@b.b",
                                "Бббев Бббй Бббевич", BillingUserType.PARTNER, "8-800-555-35-35",
                                new BillingBalanceInfoDto("userClientId2", "userPersonId2", "userContractId2")
                        )
                ));
    }

    private BillingUserContainerDto getBillingUserContainerDtoForCompany2() {
        return new BillingUserContainerDto(
                new BillingPagingInfoDto(0, 2, 1, 2L),
                List.of(
                        new BillingUserDto(
                                3L, "3", 333L, "c@c.c",
                                "Вввев Вввй Вввевич", BillingUserType.PARTNER, null, null
                        ),
                        new BillingUserDto(
                                4L, "4", 444L, "d@d.d",
                                "Дддев Дддй Дддевич", BillingUserType.PARTNER, null, null
                        )
                ));
    }

    private BillingUserContainerDto getBillingUserContainerDtoForCompany2NotAllUsers() {
        return new BillingUserContainerDto(
                new BillingPagingInfoDto(0, 1, 1, 1L),
                List.of(
                        new BillingUserDto(
                                3L, "3", 333L, "c@c.c",
                                "Вввев Вввй Вввевич", BillingUserType.PARTNER, null, null
                        )
                ));
    }
}
