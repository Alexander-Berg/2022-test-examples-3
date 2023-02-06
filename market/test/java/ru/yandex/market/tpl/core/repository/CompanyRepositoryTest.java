package ru.yandex.market.tpl.core.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

import ru.yandex.market.tpl.api.model.company.CompanyRoleEnum;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.company.CompanyRepository;
import ru.yandex.market.tpl.core.domain.company.CompanyRole;
import ru.yandex.market.tpl.core.domain.company.CompanyRoleRepository;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author kukabara
 */
@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
class CompanyRepositoryTest {

    private final CompanyRepository companyRepository;
    private final SortingCenterRepository sortingCenterRepository;
    private final DsRepository dsRepository;
    private final CompanyRoleRepository companyRoleRepository;


    @Test
    void save() {
        Company company = new Company();
        company.setName("ООО Логистика");
        company.setLogin("logistica_comp@yandex.ru");
        company.setPhoneNumber("+79999999999");
        company.setTaxpayerNumber("1111222229");
        company.setJuridicalAddress("г. Москва, ул. Люблинская, д.8");
        company.setNaturalAddress("г. Москва, ул. Южнопортовая, д.12");
        company.setDeactivated(false);

        companyRepository.saveAndFlush(company);

        assertThat(company.getId()).isNotNull();
        assertThat(company.getCreatedAt()).isNotNull();

        Optional<Company> foundCompany = companyRepository.findById(company.getId());
        assertThat(foundCompany).isPresent();
        assertThat(foundCompany.get()).isEqualTo(company);
        assertThat(companyRepository.findAll()).contains(company);
    }

    @Test
    void saveWithId() {
        Company company = new Company();
        long campaignId = 123456789L;
        company.setCampaignId(campaignId);
        company.setName("ООО Логистика");
        company.setLogin("logistica_comp@yandex.ru");
        company.setPhoneNumber("+79999999999");
        company.setTaxpayerNumber("1111222229");
        company.setJuridicalAddress("г. Москва, ул. Люблинская, д.8");
        company.setNaturalAddress("г. Москва, ул. Южнопортовая, д.12");
        company.setDeactivated(false);

        companyRepository.saveAndFlush(company);

        assertThat(company.getId()).isNotNull();
        assertThat(company.getCampaignId()).isEqualTo(campaignId);

        assertThat(company.getCreatedAt()).isNotNull();

        Optional<Company> foundCompany = companyRepository.findCompanyByCampaignId(company.getCampaignId());
        assertThat(foundCompany).isPresent();
        assertThat(foundCompany.get()).isEqualTo(company);
        assertThat(companyRepository.findAll()).contains(company);
    }

    @Test
    void requiredFieldsAbsent() {
        Company company = new Company();
        company.setName("ООО Логистика");
        company.setLogin("logistica_comp@yandex.ru");
        company.setPhoneNumber("+79999999999");
        company.setJuridicalAddress("г. Москва, ул. Люблинская, д.8");
        company.setNaturalAddress("г. Москва, ул. Южнопортовая, д.12");
        company.setDeactivated(false);

        assertThatExceptionOfType(DataAccessException.class).isThrownBy(() -> companyRepository.saveAndFlush(company));
    }

    @Test
    void findByDeliveryPartnerNameAndCompanyRole() {
        String partnerName = "Test Sorting center";
        SortingCenter sortingCenter = new SortingCenter();
        sortingCenter.setId(19386L);
        sortingCenter.setAddress("address");
        sortingCenter.setName("SC");
        sortingCenter.setLongitude(BigDecimal.ONE);
        sortingCenter.setLatitude(BigDecimal.ONE);
        sortingCenterRepository.saveAndFlush(sortingCenter);
        DeliveryService partner = new DeliveryService();
        partner.setId(120L);
        partner.setName(partnerName);
        partner.setOrderAddressMonitoringEnabled(false);
        partner.setDeliveryAreaMarginWidth(10L);
        partner.setSortingCenter(sortingCenter);
        dsRepository.saveAndFlush(partner);
        CompanyRole companyRole = companyRoleRepository.findByName(CompanyRoleEnum.SUPER).get();
        Company company = Company.builder()
                .name("Name")
                .companyRole(companyRole)
                .login("login")
                .phoneNumber("phone")
                .taxpayerNumber("tax")
                .juridicalAddress("Address")
                .deactivated(false)
                .isSuperCompany(true)
                .sortingCenters(Set.of(sortingCenter))
                .build();

        companyRepository.saveAndFlush(company);
        Optional<Company> found = companyRepository.findByDeliveryServiceIdAndCompanyRoleName(partner.getId(),
                CompanyRoleEnum.SUPER);
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(company);
    }
}
