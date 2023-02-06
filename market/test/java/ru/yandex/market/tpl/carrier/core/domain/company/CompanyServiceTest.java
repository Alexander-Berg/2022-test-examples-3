package ru.yandex.market.tpl.carrier.core.domain.company;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.domain.company.requests.CompanyCreateRequest;
import ru.yandex.market.tpl.common.dsm.client.model.EmployerLegalFormDto;
import ru.yandex.market.tpl.common.dsm.client.model.EmployerTypeDto;
import ru.yandex.market.tpl.mock.EmployerApiEmulator;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor_ = @Autowired)

@CoreTestV2
public class CompanyServiceTest {

    private final CompanyCommandService companyCommandService;
    private final RichCompanyQueryService richCompanyQueryService;
    private final InMemoryCompanyService inMemoryCompanyService;
    private final EmployerApiEmulator employerApiEmulator;

    @Test
    void readCompanyFromDsm() {
        var createRequest = companyCreateRequest(
                1234L,
                Company.DEFAULT_COMPANY_NAME,
                "anotherLogin@yandex.ru"
        );
        var created = companyCommandService.create(createRequest);

        var richCompany = richCompanyQueryService.findByIdOrThrow(created.getId());

        assertThat(richCompany).isNotNull();
        assertThat(richCompany.getId()).isEqualTo(created.getId());
        assertThat(richCompany.getDsmId()).isEqualTo(created.getDsmId());
        assertThat(richCompany.getCampaignId()).isEqualTo(createRequest.getCampaignId());
        assertThat(richCompany.getName()).isEqualTo(createRequest.getName());
        assertThat(richCompany.getLogin()).isEqualTo(createRequest.getLogin());
        assertThat(richCompany.getPhoneNumber()).isEqualTo(createRequest.getPhoneNumber());
        assertThat(richCompany.getTaxpayerNumber()).isEqualTo(createRequest.getTaxpayerNumber());
        assertThat(richCompany.getJuridicalAddress()).isEqualTo(createRequest.getJuridicalAddress());
        assertThat(richCompany.getNaturalAddress()).isEqualTo(createRequest.getNaturalAddress());
        assertThat(richCompany.isDeactivated()).isEqualTo(false);
        assertThat(richCompany.isSuperCompany()).isEqualTo(false);
        assertThat(richCompany.getLegalForm()).isEqualTo(LegalForm.OOO);
        assertThat(richCompany.getOgrn()).isEqualTo(createRequest.getOgrn());
        assertThat(richCompany.getType()).isEqualTo(CompanyType.LINEHAUL);
        assertThat(richCompany.getContractId()).isEqualTo(createRequest.getContractId());
        assertThat(richCompany.getContractDate()).isEqualTo(createRequest.getContractDate());


        var storedEmployer = employerApiEmulator.getByIdTest(created.getDsmId());
        assertThat(storedEmployer.getId()).isEqualTo(created.getDsmId());
        assertThat(storedEmployer.getCompanyCabinetMbiId()).isEqualTo(String.valueOf(createRequest.getCampaignId()));
        assertThat(storedEmployer.getName()).isEqualTo(createRequest.getName());
        assertThat(storedEmployer.getLogin()).isEqualTo(createRequest.getLogin());
        assertThat(storedEmployer.getPhoneNumber()).isEqualTo(createRequest.getPhoneNumber());
        assertThat(storedEmployer.getTaxpayerNumber()).isEqualTo(createRequest.getTaxpayerNumber());
        assertThat(storedEmployer.getJuridicalAddress()).isEqualTo(createRequest.getJuridicalAddress());
        assertThat(storedEmployer.getNaturalAddress()).isEqualTo(createRequest.getNaturalAddress());
        assertThat(storedEmployer.getActive()).isEqualTo(!createRequest.isDeactivated());
        assertThat(storedEmployer.getLegalForm()).isEqualTo(EmployerLegalFormDto.OOO);
        assertThat(storedEmployer.getOgrn()).isEqualTo(createRequest.getOgrn());
        assertThat(storedEmployer.getType()).isEqualTo(EmployerTypeDto.LINEHAUL);
    }

    @Test
    void readCompaniesFromDsm() {
        var createRequest1 = companyCreateRequest(
                1234L,
                "company 1",
                "anotherLogin@yandex.ru"
        );
        var company1 = companyCommandService.create(createRequest1);


        var createRequest2 = companyCreateRequest(
                1235L,
                "company 2",
                "anotherLogin2@yandex.ru"
        );
        var company2 = companyCommandService.create(createRequest2);

        inMemoryCompanyService.loadSync();

        List<RichCompany> richCompanies = richCompanyQueryService.get(
                        new CompanyFilter("company"), PageRequest.of(0, 10)
                )
                .getContent();

        assertThat(richCompanies)
                .isNotNull()
                .hasSize(2);
    }

    private static CompanyCreateRequest companyCreateRequest(long campaignId, String name, String login) {
        return CompanyCreateRequest.builder()
                .campaignId(campaignId)
                .name(name)
                .login(login)
                .phoneNumber("88005353535")
                .taxpayerNumber("1919191922")
                .juridicalAddress("не указан")
                .naturalAddress("не указан")
                .deactivated(false)
                .isSuperCompany(false)
                .legalForm(LegalForm.OOO)
                .ogrn("1231232452345")
                .type(CompanyType.LINEHAUL)
                .contractId(null)
                .contractDate(null)
                .build();
    }

}
