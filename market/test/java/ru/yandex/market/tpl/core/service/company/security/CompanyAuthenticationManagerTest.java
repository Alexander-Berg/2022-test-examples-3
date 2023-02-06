package ru.yandex.market.tpl.core.service.company.security;

import java.util.HashSet;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.company.CompanyRoleEnum;
import ru.yandex.market.tpl.api.model.company.PermissionEnum;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.company.CompanyRoleRepository;
import ru.yandex.market.tpl.core.domain.company.Permission;
import ru.yandex.market.tpl.core.domain.company.PermissionRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.service.company.CompanyCachingService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.COMPANY_PERMISSION_ENABLED;

@RequiredArgsConstructor
class CompanyAuthenticationManagerTest extends TplAbstractTest {

    private final CompanyCachingService companyCachingService;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final TestUserHelper testUserHelper;
    private final CompanyRoleRepository companyRoleRepository;
    private final TransactionTemplate transactionTemplate;
    private final PermissionRepository permissionRepository;

    private CompanyAuthenticationManager companyAuthenticationManager;

    @BeforeEach
    public void init() {
        companyAuthenticationManager = new CompanyAuthenticationManager(companyCachingService,
                configurationProviderAdapter);
    }

    @Test
    public void authenticationManagerAuthoritiesTest() {
        given(configurationProviderAdapter.isBooleanEnabled(COMPANY_PERMISSION_ENABLED)).willReturn(true);
        //given
        var companyWithPermissions = transactionTemplate.execute(action -> {
            var company = testUserHelper.findOrCreateSuperCompany();
            var companyRole = companyRoleRepository.findByName(CompanyRoleEnum.SUPER)
                    .orElseThrow();
            var permissions = new HashSet<Permission>();
            var permission1 = permissionRepository.findByName(PermissionEnum.PARTNER_COMPANY_VIEW).orElseThrow();
            var permission2 = permissionRepository.findByName(PermissionEnum.PARTNER_COMPANY_CREATE).orElseThrow();
            var permission3 = permissionRepository.findByName(PermissionEnum.PARTNER_COMPANY_UPDATE).orElseThrow();
            permissions.add(permission1);
            permissions.add(permission2);
            permissions.add(permission3);
            permissionRepository.saveAll(permissions);
            companyRole.setPermissions(permissions);
            company.setCompanyRole(companyRole);

            return company;
        });
        assertThat(companyWithPermissions).isNotNull();
        var authenticationToken = new CompanyAuthenticationToken(companyWithPermissions.getCampaignId());

        //when
        var authentication = companyAuthenticationManager.authenticate(authenticationToken);

        //then
        var authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        assertThat(authorities).containsExactlyInAnyOrder(
                PermissionEnum.PARTNER_COMPANY_VIEW.toString(),
                PermissionEnum.PARTNER_COMPANY_CREATE.toString(),
                PermissionEnum.PARTNER_COMPANY_UPDATE.toString()
        );
    }

}
