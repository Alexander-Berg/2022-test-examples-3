package ru.yandex.market.tpl.core.service.company;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.company.CompanyBalanceRegistrationStatusDto;
import ru.yandex.market.tpl.api.model.company.CompanyRoleEnum;
import ru.yandex.market.tpl.api.model.company.PartnerCompanyRequestDto;
import ru.yandex.market.tpl.api.model.company.PartnerCompanyResponseDto;
import ru.yandex.market.tpl.api.model.company.PermissionEnum;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.dsm.client.api.EmployerApi;
import ru.yandex.market.tpl.common.dsm.client.model.EmployerBalanceRegistrationStatusDto;
import ru.yandex.market.tpl.common.dsm.client.model.EmployerContactInfo;
import ru.yandex.market.tpl.common.dsm.client.model.EmployerDto;
import ru.yandex.market.tpl.common.dsm.client.model.EmployerTypeDto;
import ru.yandex.market.tpl.common.dsm.client.model.EmployerUpsertDto;
import ru.yandex.market.tpl.common.dsm.client.model.EmployersSearchResultDto;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerEmployerDto;
import ru.yandex.market.tpl.common.dsm.client.model.LogbrokerEmployerTypeDto;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.company.CompanyPermissionsProjection;
import ru.yandex.market.tpl.core.domain.company.CompanyRepository;
import ru.yandex.market.tpl.core.domain.company.CompanyRoleRepository;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.COMPANY_DSM_INTEGRATION_FOR_EDIT_ENABLED;

@RequiredArgsConstructor
class PartnerCompanyServiceTest extends TplAbstractTest {

    private final UserRepository userRepository;
    private final PartnerCompanyDtoMapper mapper;
    private final TestUserHelper testHelper;
    private final PartnerCompanyService companyService;
    private final TransactionTemplate transactionTemplate;
    private final CompanyRepository companyRepository;
    private final CompanyRoleRepository companyRoleRepository;
    private final CompanyCachingService companyCachingService;
    private final EmployerApi employerApi;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final BlackboxClient blackboxClient;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final Clock clock;

    @MockBean
    private UserScheduleService userScheduleService;
    @MockBean
    private PartnerCompanyRoleService partnerCompanyRoleService;

    private CompanyPermissionsProjection company;
    private User courier1;
    private User courier2;
    private Company realCompany;

    @BeforeEach
    void init() {
        courier1 = testHelper.findOrCreateUser(1L);
        courier2 = testHelper.findOrCreateUser(2L);
        Set<User> couriers = new HashSet<>(2);
        couriers.add(courier1);
        couriers.add(courier2);
        realCompany = testHelper.findOrCreateSuperCompany();
        this.company = companyCachingService.getProjectionForCompany(realCompany.getCampaignId());
        testHelper.addCouriersToCompany(realCompany.getName(), couriers);
        when(partnerCompanyRoleService.isRoleEnabled()).thenReturn(false);
        Mockito.reset(employerApi);
        Mockito.reset(configurationProviderAdapter);
    }

    @AfterEach
    void after() {
        Mockito.reset(employerApi);
        Mockito.reset(configurationProviderAdapter);
        Mockito.reset(partnerCompanyRoleService);
    }

    @Test
    void getById_withDsm() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.COMPANY_DSM_INTEGRATION_FOR_VIEW_ENABLED)
        ).thenReturn(true);

        String name = "TEST_COMPANY";
        String campaignId = "34646883";
        String login = "login@yandex.ru";
        String phoneNumber = "89867599188";
        String taxpayerNumber = "89867599188";
        String juridicalAddress = "Пушкина Колотушкина";
        String kpp = "475964064";
        String bik = "3487604";
        String account = "3476903674";
        Integer nds = 567;
        Long personId = 34634646L;

        String fullNameOfResponsible = "fullName";
        String phoneNumberOfResponsible = "phone";
        String emailOfResponsible = "email";
        String emailForDocuments = "email-for-doc";
        String contractNumber = "number";
        LocalDate contractDate = LocalDate.now(clock);
        String fullNameOfCeo = "ceo";

        EmployerBalanceRegistrationStatusDto status = EmployerBalanceRegistrationStatusDto.LINK_CONFIGURATION;
        EmployerDto resultE = new EmployerDto()
                .name(name)
                .companyCabinetMbiId(campaignId)
                .login(login)
                .phoneNumber(phoneNumber)
                .taxpayerNumber(taxpayerNumber)
                .juridicalAddress(juridicalAddress)
                .kpp(kpp)
                .bik(bik)
                .account(account)
                .nds(nds)
                .balancePersonId(personId)
                .balanceRegistrationStatus(status);
        resultE.setEmployerContactInfo(new EmployerContactInfo()
                .fullNameOfResponsible(fullNameOfResponsible)
                .phoneNumberOfResponsible(phoneNumberOfResponsible)
                .emailOfResponsible(emailOfResponsible)
                .emailForDocuments(emailForDocuments)
                .contractNumber(contractNumber)
                .contractDate(contractDate)
                .fullNameOfCeo(fullNameOfCeo));
        when(employerApi.employersIdGet(realCompany.getDsmExternalId()))
                .thenReturn(
                        resultE
                );

        var result = companyService.findById(company.getId(), company);

        verify(employerApi, times(1)).employersIdGet(realCompany.getDsmExternalId());

        assertThat(result.getId()).isEqualTo(company.getId());
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getCampaignId().toString()).isEqualTo(campaignId);
        assertThat(result.getLogin()).isEqualTo(login);
        assertThat(result.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(result.getTaxpayerNumber()).isEqualTo(taxpayerNumber);
        assertThat(result.getJuridicalAddress()).isEqualTo(juridicalAddress);
        assertThat(result.getPartnerCompanyBalanceInfo()).isNotNull();
        assertThat(result.getPartnerCompanyBalanceInfo().getKpp()).isEqualTo(kpp);
        assertThat(result.getPartnerCompanyBalanceInfo().getBik()).isEqualTo(bik);
        assertThat(result.getPartnerCompanyBalanceInfo().getAccount()).isEqualTo(account);
        assertThat(result.getPartnerCompanyBalanceInfo().getNds()).isEqualTo(nds);
        assertThat(result.getPartnerCompanyBalanceInfo().getBalancePersonId()).isEqualTo(personId);
        assertThat(result.getPartnerCompanyBalanceInfo().getBalanceRegistrationStatusDto()).isEqualTo(CompanyBalanceRegistrationStatusDto.LINK_CONFIGURATION);

        assertThat(result.getPartnerCompanyContactInfo()).isNotNull();
        assertThat(result.getPartnerCompanyContactInfo().getFullNameOfResponsible()).isEqualTo(fullNameOfResponsible);
        assertThat(result.getPartnerCompanyContactInfo().getPhoneNumberOfResponsible()).isEqualTo(phoneNumberOfResponsible);
        assertThat(result.getPartnerCompanyContactInfo().getEmailOfResponsible()).isEqualTo(emailOfResponsible);
        assertThat(result.getPartnerCompanyContactInfo().getEmailForDocuments()).isEqualTo(emailForDocuments);
        assertThat(result.getPartnerCompanyContactInfo().getContractNumber()).isEqualTo(contractNumber);
        assertThat(result.getPartnerCompanyContactInfo().getContractDate()).isEqualTo(contractDate);
        assertThat(result.getPartnerCompanyContactInfo().getFullNameOfCeo()).isEqualTo(fullNameOfCeo);
    }

    @Test
    void findCompany() {
        //when
        PartnerCompanyResponseDto actual = companyService.findById(company.getId(), null);

        //then
        assertThat(actual).isNotNull();
    }

    @Test
    void companyNotFound() {
        assertThatExceptionOfType(TplEntityNotFoundException.class)
                .isThrownBy(() -> companyService.findById(100L, null));
    }

    @Test
    void deactivateCompany() {
        //given
        var company = transactionTemplate.execute(action -> {
            var companyOfCourier1 = companyRepository.findByIdOrThrow(courier1.getCompany().getId());
            var companyOfCourier2 = companyRepository.findByIdOrThrow(courier2.getCompany().getId());
            assertThat(companyOfCourier1).isEqualTo(companyOfCourier2);
            PartnerCompanyRequestDto companyDto = mapper.mapRequest(companyOfCourier1);
            companyDto.setDeactivated(true);

            return companyDto;
        });

        //when
        companyService.update(courier1.getCompany().getId(), company);

        //then
        verify(userScheduleService, times(1)).fireUser(courier1.getId());
        verify(userScheduleService, times(1)).fireUser(courier2.getId());
        assertThat(userRepository.findById(courier1.getId()).isPresent()).isTrue();
        assertThat(userRepository.findById(courier1.getId()).get().isDeleted()).isTrue();
        assertThat(userRepository.findById(courier2.getId()).isPresent()).isTrue();
        assertThat(userRepository.findById(courier2.getId()).get().isDeleted()).isTrue();
    }

    @Test
    void findPartnerCompany() {
        //when
        when(partnerCompanyRoleService.isRoleEnabled()).thenReturn(true);
        PartnerCompanyResponseDto actual = companyService.findById(company.getId(),
                company);
        //then
        assertThat(actual).isNotNull();
    }

    @Test
    void findPartnerCompanyWithWrongId() {
        //when
        when(partnerCompanyRoleService.isRoleEnabled()).thenReturn(true);
        var exception = catchThrowable(() -> companyService.findById(company.getId() + 1,
                company));
        //then
        assertThat(exception).isInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void getPermissionsInCompanyFlags() {
        var permissions = new HashSet<PermissionEnum>();
        var permission1 = PermissionEnum.PARTNER_COMPANY_VIEW;
        var permission2 = PermissionEnum.PARTNER_COMPANY_CREATE;
        var permission3 = PermissionEnum.PARTNER_COMPANY_UPDATE;
        permissions.add(permission1);
        permissions.add(permission2);
        permissions.add(permission3);
        CompanyPermissionsProjection companyProjection =
                CompanyPermissionsProjection.builder()
                        .isSuperCompany(true)
                        .permissions(permissions)
                        .campaignId(1L)
                        .companyRoleName(CompanyRoleEnum.SUPER)
                        .build();
        var companyFlags = companyService.getCompanyFlags(
                companyProjection.getCampaignId(),
                companyProjection
        );

        assertThat(companyFlags.getPermissions()).isNotNull();
        assertThat(companyFlags.getPermissions()).hasSize(1);
        var permission = companyFlags.getPermissions().iterator().next();
        assertThat(permission.getName()).isEqualTo(PermissionEnum.PARTNER_COMPANY_VIEW.getGroupKey());
        assertThat(permission.getActions())
                .containsExactlyInAnyOrder(
                        PermissionEnum.PARTNER_COMPANY_VIEW.getAlias(),
                        PermissionEnum.PARTNER_COMPANY_CREATE.getAlias(),
                        PermissionEnum.PARTNER_COMPANY_UPDATE.getAlias()
                );
    }

    @Test
    void getRoleInCompanyFlags() {
        CompanyPermissionsProjection companyWithRole = CompanyPermissionsProjection.builder()
                .companyRoleName(CompanyRoleEnum.SUPER)
                .campaignId(1L)
                .isSuperCompany(true)
                .permissions(Set.of())
                .build();

        var companyFlags = companyService.getCompanyFlags(
                companyWithRole.getCampaignId(),
                companyWithRole
        );

        assertThat(companyFlags.getRole()).isNotNull();
        assertThat(companyFlags.getRole()).isEqualTo(CompanyRoleEnum.SUPER);
    }


    @Test
    void create_success() {
        String companyLogin = "dsm-company-login-test-create";
        PartnerCompanyRequestDto companyRequestDto = getPartnerCompanyRequestDto(companyLogin);
        companyRequestDto.setUid(756L);

        companyService.create(companyRequestDto);

        Optional<Company> companyOpt = companyRepository.findCompanyByLogin(companyRequestDto.getLogin());
        assertThat(companyOpt.isEmpty()).isFalse();

        Company company = companyOpt.get();
        assertThat(company.getUid()).isEqualTo(companyRequestDto.getUid());
    }

    @Test
    void createWithDsmIntegrationWhenDsmEmployerDoesNotExist() {
        configurationServiceAdapter.mergeValue(COMPANY_DSM_INTEGRATION_FOR_EDIT_ENABLED, true);

        String companyLogin = "dsm-company-login-test-1";
        String dsmEmployerId = "test-id";
        PartnerCompanyRequestDto companyRequestDto = getPartnerCompanyRequestDto(companyLogin);

        when(
                employerApi.employersGet(
                        eq(0),
                        eq(1),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(companyRequestDto.getLogin()),
                        eq(null),
                        eq(null),
                        eq(null)
                )
        ).thenReturn(
                new EmployersSearchResultDto()
        );

        when(
                employerApi.employersPutWithHttpInfo(
                        getEmployerUpsertDto(null, companyRequestDto)
                )
        ).thenReturn(
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(new EmployerDto().id(dsmEmployerId))
        );

        companyService.create(companyRequestDto);

        verify(employerApi, times(1)).employersPutWithHttpInfo(any());

        Optional<Company> companyOpt = companyRepository.findCompanyByLogin(companyRequestDto.getLogin());
        assertThat(companyOpt.isEmpty()).isFalse();

        Company company = companyOpt.get();
        assertThat(company.getDsmExternalId()).isEqualTo(dsmEmployerId);
    }

    @Test
    void createWithDsmIntegrationWhenDsmEmployerExists() {
        configurationServiceAdapter.mergeValue(COMPANY_DSM_INTEGRATION_FOR_EDIT_ENABLED, true);

        String companyLogin = "dsm-company-login-test-2";
        String dsmEmployerId = "test-id";
        PartnerCompanyRequestDto companyRequestDto = getPartnerCompanyRequestDto(companyLogin);

        EmployersSearchResultDto searchDto = new EmployersSearchResultDto();
        EmployerDto searchEmployerDto = new EmployerDto();
        searchEmployerDto.setId(dsmEmployerId);
        searchDto.setContent(List.of(
                searchEmployerDto
        ));
        when(
                employerApi.employersGet(
                        eq(0),
                        eq(1),
                        eq(null),
                        eq(null),
                        eq(null),
                        eq(companyRequestDto.getLogin()),
                        eq(null),
                        eq(null),
                        eq(null)
                )
        ).thenReturn(searchDto);

        when(
                employerApi.employersPutWithHttpInfo(
                        getEmployerUpsertDto(dsmEmployerId, companyRequestDto)
                )
        ).thenReturn(
                ResponseEntity.status(HttpStatus.OK)
                        .body(new EmployerDto().id(dsmEmployerId))
        );

        companyService.create(companyRequestDto);

        verify(employerApi, times(1)).employersPutWithHttpInfo(any());

        Optional<Company> companyOpt = companyRepository.findCompanyByLogin(companyRequestDto.getLogin());
        assertThat(companyOpt.isEmpty()).isFalse();

        Company company = companyOpt.get();
        assertThat(company.getDsmExternalId()).isEqualTo(dsmEmployerId);
    }

    @Test
    void createWithTransactionFails() {
        PartnerCompanyRequestDto companyRequestDto = getPartnerCompanyRequestDto("dsm-test-login");
        assertThrows(
                IllegalTransactionStateException.class,
                () -> transactionTemplate.execute((status) -> companyService.create(companyRequestDto))
        );
    }

    @Test
    void update() {
        configurationServiceAdapter.mergeValue(COMPANY_DSM_INTEGRATION_FOR_EDIT_ENABLED, true);

        String companyLogin = "dsm-company-login-test-3";
        long companyUid = 123L;
        String dsmEmployerId = "test-id";
        PartnerCompanyRequestDto companyRequestDto = getPartnerCompanyRequestDto(companyLogin);
        companyRequestDto.setDsmExternalId(dsmEmployerId);
        Company companyOld = companyRepository.save(
                mapper.map(
                        companyRequestDto,
                        null,
                        new Company(),
                        companyRoleRepository.findByName(CompanyRoleEnum.PARTNER).get()
                )
        );
        String nameNew = "name-test-new";

        when(blackboxClient.getUidForLogin(companyLogin)).thenReturn(companyUid);

        companyRequestDto.setName(nameNew);
        when(
                employerApi.employersPutWithHttpInfo(
                        getEmployerUpsertDto(dsmEmployerId, companyRequestDto)
                )
        ).thenReturn(
                ResponseEntity.status(HttpStatus.OK)
                        .body(new EmployerDto().id(dsmEmployerId))
        );

        companyService.update(companyOld.getId(), companyRequestDto);

        Optional<Company> companyOpt = companyRepository.findCompanyByLogin(companyRequestDto.getLogin());
        assertThat(companyOpt.isEmpty()).isFalse();

        Company company = companyOpt.get();
        assertThat(company.getUid()).isEqualTo(companyUid);
        assertThat(company.getName()).isEqualTo(nameNew);
        assertThat(company.getDsmExternalId()).isEqualTo(dsmEmployerId);
    }

    @Test
    void updateWithTransactionFails() {
        PartnerCompanyRequestDto companyDto = getPartnerCompanyRequestDto("dsm-company-login-test-4");
        PartnerCompanyResponseDto partnerCompanyResponseDto = companyService.create(companyDto);
        assertThrows(
                IllegalTransactionStateException.class,
                () -> transactionTemplate.execute((status) -> companyService.update(
                        partnerCompanyResponseDto.getId(), companyDto
                ))
        );
    }

    @Test
    void upsertByDsm_success() {
        var dto = new LogbrokerEmployerDto();
        String dsmId = "435465643";
        dto.setId(dsmId);
        dto.setType(LogbrokerEmployerTypeDto.LINEHAUL);
        dto.setName("upsertByDsm_test");
        dto.setLogin("LOGIN_4579033");
        dto.setPhoneNumber("709034209");
        dto.setTaxpayerNumber("7485349");
        dto.setJuridicalAddress("JUR_AD_346579");
        dto.setActive(true);

        companyService.upsertByDsm(dto);

        assertThat(companyRepository.findCompanyByDsmExternalId(dsmId).isPresent()).isTrue();
    }

    @NotNull
    private PartnerCompanyRequestDto getPartnerCompanyRequestDto(String login) {
        PartnerCompanyRequestDto companyDto = new PartnerCompanyRequestDto();
        companyDto.setName(login);
        companyDto.setLogin(login);
        companyDto.setPhoneNumber("phone-test");
        companyDto.setTaxpayerNumber("tax-test");
        companyDto.setJuridicalAddress("address-test");
        return companyDto;
    }

    private EmployerUpsertDto getEmployerUpsertDto(
            @Nullable String id,
            PartnerCompanyRequestDto updateRequestDto
    ) {
        Long campaignId = updateRequestDto.getCampaignId();
        return new EmployerUpsertDto()
                .id(id)
                .type(EmployerTypeDto.SUPPLY)
                .companyMbiId(campaignId != null ? campaignId.toString() : null)
                .name(updateRequestDto.getName())
                .login(updateRequestDto.getLogin())
                .phoneNumber(updateRequestDto.getPhoneNumber())
                .taxpayerNumber(updateRequestDto.getTaxpayerNumber())
                .juridicalAddress(updateRequestDto.getJuridicalAddress())
                .naturalAddress(updateRequestDto.getNaturalAddress())
                .active(!updateRequestDto.isDeactivated());
    }
}
