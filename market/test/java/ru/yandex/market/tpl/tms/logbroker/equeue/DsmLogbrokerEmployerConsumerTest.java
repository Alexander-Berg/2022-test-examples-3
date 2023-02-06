package ru.yandex.market.tpl.tms.logbroker.equeue;

import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.common.logbroker.consumer.LogbrokerMessage;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.company.CompanyRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.tms.logbroker.consumer.dsm.DsmLogbrokerEmployerConsumer;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@RequiredArgsConstructor
public class DsmLogbrokerEmployerConsumerTest extends TplTmsAbstractTest {
    private final DsmLogbrokerEmployerConsumer dsmEmployerUpsertDtoConsumer;
    private final CompanyRepository companyRepository;
    private final TestUserHelper testUserHelper;

    private final static String ID = "8783973";
    private final static Long COMPANY_MBY_ID = 8783974L;
    private final static String NAME = "test9875423";
    private final static String LOGIN = "login0984504";
    private final static String PHONE_NUMBER = "789876789";
    private final static String TAXPAYER_NUMBER = "8783934";
    private final static String JURIDICAL_ADDRESS = "address345646";
    private final static String NATURAL_ADDRESS = "address345646";
    private final static String OGRN = "8783973";
    private final static Long COMPANY_CABINET_MBI_ID = 8783971L;
    private final static Boolean ACTIVE = true;

    @Test
    void createCompany() {
        var message = getMessage(ID, COMPANY_CABINET_MBI_ID);
        assertDoesNotThrow(() -> dsmEmployerUpsertDtoConsumer.accept(message));
        Optional<Company> optCompany = companyRepository.findCompanyByDsmExternalId(ID);
        assertThat(optCompany).isPresent();
        Company company = optCompany.get();
        assertThat(company.getDsmExternalId()).isEqualTo(ID);
        assertThat(company.getBusinessId()).isEqualTo(COMPANY_MBY_ID);
        assertThat(company.getName()).isEqualTo(NAME);
        assertThat(company.getLogin()).isEqualTo(LOGIN);
        assertThat(company.getPhoneNumber()).isEqualTo(PHONE_NUMBER);
        assertThat(company.getTaxpayerNumber()).isEqualTo(TAXPAYER_NUMBER);
        assertThat(company.getJuridicalAddress()).isEqualTo(JURIDICAL_ADDRESS);
        assertThat(company.getNaturalAddress()).isEqualTo(NATURAL_ADDRESS);
        assertThat(company.isDeactivated()).isEqualTo(!ACTIVE);
        companyRepository.delete(company);
    }

    @Test
    void updateCompany() {
        Company realCompany = testUserHelper.createCompany(
                Set.of(testUserHelper.sortingCenter(SortingCenter.DEFAULT_SC_ID)),
                COMPANY_CABINET_MBI_ID,
                "NAME5437898",
                null);
        var message = getMessage(realCompany.getDsmExternalId(), realCompany.getCampaignId());
        assertDoesNotThrow(() -> dsmEmployerUpsertDtoConsumer.accept(message));
        Optional<Company> optCompany = companyRepository.findCompanyByDsmExternalId(realCompany.getDsmExternalId());
        assertThat(optCompany).isPresent();
        Company company = optCompany.get();
        assertThat(company.getBusinessId()).isEqualTo(COMPANY_MBY_ID);
        assertThat(company.getName()).isEqualTo(NAME);
        assertThat(company.getLogin()).isEqualTo(LOGIN);
        assertThat(company.getPhoneNumber()).isEqualTo(PHONE_NUMBER);
        assertThat(company.getTaxpayerNumber()).isEqualTo(TAXPAYER_NUMBER);
        assertThat(company.getJuridicalAddress()).isEqualTo(JURIDICAL_ADDRESS);
        assertThat(company.getNaturalAddress()).isEqualTo(NATURAL_ADDRESS);
        assertThat(company.isDeactivated()).isEqualTo(!ACTIVE);
        companyRepository.delete(company);
    }


    @NotNull
    private LogbrokerMessage getMessage(String id, long campaignId) {
        return new LogbrokerMessage(
                "",
                "{" +
                        "\"id\":\"" + id + "\"," +
                        "\"companyMbiId\":\"" + COMPANY_MBY_ID + "\"," +
                        "\"type\":null," +
                        "\"name\":\"" + NAME + "\"," +
                        "\"login\":\"" + LOGIN + "\"," +
                        "\"phoneNumber\":\"" + PHONE_NUMBER + "\"," +
                        "\"taxpayerNumber\":\"" + TAXPAYER_NUMBER + "\"," +
                        "\"juridicalAddress\":\"" + JURIDICAL_ADDRESS + "\"," +
                        "\"naturalAddress\":\"" + NATURAL_ADDRESS + "\"," +
                        "\"ogrn\":\"" + OGRN + "\"," +
                        "\"companyCabinetMbiId\":\"" + campaignId + "\"," +
                        "\"active\":\"" + ACTIVE + "\"" +
                        "}"
        );
    }
}
