package ru.yandex.market.tpl.core.domain.company;

import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.company.PartnerCompanyRequestDto;
import ru.yandex.market.tpl.common.dsm.client.model.EmployerDto;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerEmployerDto;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerEmployerLegalFormDto;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerEmployerTypeDto;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.service.company.PartnerCompanyDtoMapper;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class PartnerCompanyDtoMapperTest extends TplAbstractTest {
    private final PartnerCompanyDtoMapper partnerCompanyDtoMapper;
    private final TestUserHelper userHelper;
    private final TransactionTemplate transactionTemplate;
    private final CompanyRepository companyRepository;


    @ParameterizedTest
    @MethodSource("logbrokerEmployerDtos")
    void fromLogbrokerEmployerDtoToPartnerCompanyRequestDto(LogbrokerEmployerDto logbrokerEmployerDto) {
        PartnerCompanyRequestDto partnerCompanyRequestDto = partnerCompanyDtoMapper.mapRequest(logbrokerEmployerDto);
        assertThat(partnerCompanyRequestDto.getDsmExternalId()).isEqualTo(logbrokerEmployerDto.getId());
        if (logbrokerEmployerDto.getCompanyMbiId() != null) {
            assertThat(partnerCompanyRequestDto.getBusinessId().toString()).isEqualTo(logbrokerEmployerDto.getCompanyMbiId());
        } else {
            assertThat(partnerCompanyRequestDto.getBusinessId()).isNull();
        }
        assertThat(partnerCompanyRequestDto.getName()).isEqualTo(logbrokerEmployerDto.getName());
        assertThat(partnerCompanyRequestDto.getLogin()).isEqualTo(logbrokerEmployerDto.getLogin());
        assertThat(partnerCompanyRequestDto.getPhoneNumber()).isEqualTo(logbrokerEmployerDto.getPhoneNumber());
        assertThat(partnerCompanyRequestDto.getTaxpayerNumber()).isEqualTo(logbrokerEmployerDto.getTaxpayerNumber());
        assertThat(partnerCompanyRequestDto.getJuridicalAddress()).isEqualTo(logbrokerEmployerDto.getJuridicalAddress());
        assertThat(partnerCompanyRequestDto.getNaturalAddress()).isEqualTo(logbrokerEmployerDto.getNaturalAddress());
        if (logbrokerEmployerDto.getCompanyCabinetMbiId() != null) {
            assertThat(partnerCompanyRequestDto.getCampaignId().toString()).isEqualTo(logbrokerEmployerDto.getCompanyCabinetMbiId());
        } else {
            assertThat(partnerCompanyRequestDto.getCampaignId()).isNull();
        }
        assertThat(partnerCompanyRequestDto.isDeactivated()).isEqualTo(!logbrokerEmployerDto.getActive());
        logbrokerEmployerDto.setActive(true);
    }

    @ParameterizedTest
    @MethodSource("employerDtos")
    void fromEmployerDtoPartnerCompanyResponseDto(EmployerDto employerDto) {
        String companyName = "fffjdfhdjhfddbsa";
        var result = transactionTemplate.execute(status -> {
            Company company = userHelper.findOrCreateCompany(companyName);
            return partnerCompanyDtoMapper.map(employerDto, company);
        });
        assertThat(result).isNotNull();
        assertThat(employerDto.getCompanyCabinetMbiId()).isEqualTo(result.getCampaignId().toString());
        assertThat(employerDto.getName()).isEqualTo(result.getName());
        assertThat(employerDto.getLogin()).isEqualTo(result.getLogin());
        assertThat(employerDto.getPhoneNumber()).isEqualTo(result.getPhoneNumber());
        assertThat(employerDto.getTaxpayerNumber()).isEqualTo(result.getTaxpayerNumber());
        assertThat(employerDto.getJuridicalAddress()).isEqualTo(result.getJuridicalAddress());
        assertThat(employerDto.getNaturalAddress()).isEqualTo(result.getNaturalAddress());
        var balanceInfo = result.getPartnerCompanyBalanceInfo();
        if (balanceInfo != null) {
            assertThat(employerDto.getPostCode()).isEqualTo(balanceInfo.getPostCode());
            assertThat(employerDto.getLongName()).isEqualTo(balanceInfo.getLongName());
            assertThat(employerDto.getKpp()).isEqualTo(balanceInfo.getKpp());
            assertThat(employerDto.getBik()).isEqualTo(balanceInfo.getBik());
            assertThat(employerDto.getAccount()).isEqualTo(balanceInfo.getAccount());
            assertThat(employerDto.getLegalAddressPostCode()).isEqualTo(balanceInfo.getLegalAddressPostCode());
            assertThat(employerDto.getLegalAddressCity()).isEqualTo(balanceInfo.getLegalAddressCity());
            assertThat(employerDto.getLegalAddressStreet()).isEqualTo(balanceInfo.getLegalAddressStreet());
            assertThat(employerDto.getLegalAddressHome()).isEqualTo(balanceInfo.getLegalAddressHome());
            assertThat(employerDto.getLegalFiasGuid()).isEqualTo(balanceInfo.getLegalFiasGuid());
            assertThat(employerDto.getBalanceClientId()).isEqualTo(balanceInfo.getBalanceClientId());
            assertThat(employerDto.getBalancePersonId()).isEqualTo(balanceInfo.getBalancePersonId());
            assertThat(employerDto.getBalanceContractId()).isEqualTo(balanceInfo.getBalanceContractId());
            assertThat(partnerCompanyDtoMapper.map(employerDto.getBalanceRegistrationStatus()))
                    .isEqualTo(balanceInfo.getBalanceRegistrationStatusDto());
        }

        transactionTemplate.execute(status -> {
            Company company = companyRepository.findCompanyByName(companyName).orElseThrow();
            companyRepository.delete(company);
            return status;
        });
    }

    private static Stream<Arguments> employerDtos() {
        EmployerDto employerDto = new EmployerDto();
        employerDto.setCompanyCabinetMbiId("743595843790");
        employerDto.setName("name name");
        employerDto.setLogin("login login");
        employerDto.setPhoneNumber("4576903403");
        employerDto.setTaxpayerNumber("7649067409");
        employerDto.setJuridicalAddress("улица Пушкина дом Колотушкина");
        employerDto.setNaturalAddress("улица Пушкина дом Колотушкина");
        employerDto.setPostCode("32232");
        employerDto.setLongName("long name");
        employerDto.setKpp("32345353");
        employerDto.setBik("32232389743");
        employerDto.setAccount("account");
        employerDto.setLegalAddressPostCode("3223232");
        employerDto.setLegalAddressCity("Москва");
        employerDto.setLegalAddressStreet("Пушкина");
        employerDto.setLegalAddressHome("Колотушкина");
        employerDto.setLegalFiasGuid("sjdfslsdfsf");
        employerDto.setBalanceClientId(367459543L);
        employerDto.setBalancePersonId(4678593L);
        employerDto.setBalanceContractId(4646453L);
        employerDto.setBalanceRegistrationStatus(null);

        return Stream.of(
                Arguments.of(employerDto)
        );
    }


    private static Stream<Arguments> logbrokerEmployerDtos() {
        LogbrokerEmployerDto completelyFilledDto = new LogbrokerEmployerDto();
        completelyFilledDto.setId("435465643");
        completelyFilledDto.setCompanyMbiId("234347746");
        completelyFilledDto.setType(LogbrokerEmployerTypeDto.LINEHAUL);
        completelyFilledDto.setName("COMPANY_NAME_56574");
        completelyFilledDto.setLogin("LOGIN_4579033");
        completelyFilledDto.setPhoneNumber("709034209");
        completelyFilledDto.setTaxpayerNumber("7485349");
        completelyFilledDto.setJuridicalAddress("JUR_AD_346579");
        completelyFilledDto.setNaturalAddress("NAT_AD_3564789");
        completelyFilledDto.setOgrn("OGRN_49034989034");
        completelyFilledDto.setLegalForm(LogbrokerEmployerLegalFormDto.AO);
        completelyFilledDto.setCompanyCabinetMbiId("890999348290");
        completelyFilledDto.setActive(true);

        LogbrokerEmployerDto minDto = new LogbrokerEmployerDto();
        minDto.setId("435465643");
        minDto.setType(LogbrokerEmployerTypeDto.LINEHAUL);
        minDto.setName("COMPANY_NAME_56574");
        minDto.setLogin("LOGIN_4579033");
        minDto.setPhoneNumber("709034209");
        minDto.setTaxpayerNumber("7485349");
        minDto.setJuridicalAddress("JUR_AD_346579");
        minDto.setActive(true);

        return Stream.of(
                Arguments.of(completelyFilledDto),
                Arguments.of(minDto)
        );
    }
}
