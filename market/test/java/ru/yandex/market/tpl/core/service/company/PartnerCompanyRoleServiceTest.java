package ru.yandex.market.tpl.core.service.company;

import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.company.CompanyPermissionsProjection;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.company.Company.DEFAULT_COMPANY_NAME;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.IS_PARTNER_COMPANY_ROLE_ENABLED;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PartnerCompanyRoleServiceTest {

    private final TestUserHelper testHelper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final PartnerCompanyRoleService partnerCompanyRoleService;

    private CompanyPermissionsProjection company1;
    private CompanyPermissionsProjection company2;
    private User courier1;
    private User courier2;

    @BeforeEach
    void init() {
        courier1 = testHelper.findOrCreateUser(1L);
        courier2 = testHelper.findOrCreateUser(2L);
        Company company1 = testHelper.findOrCreateCompany(DEFAULT_COMPANY_NAME);
        Company company2 = testHelper.findOrCreateCompany("company2", "company2");
        this.company1 =
                CompanyPermissionsProjection.builder().isSuperCompany(company1.isSuperCompany()).id(company1.getId()).build();
        this.company2 =
                CompanyPermissionsProjection.builder().isSuperCompany(company2.isSuperCompany()).id(company2.getId()).build();
        configurationServiceAdapter.insertValue(IS_PARTNER_COMPANY_ROLE_ENABLED, true);
    }

    @Test
    void getCompanyUserIds() {
        var couriersId = Set.of(courier1.getId(), courier2.getId());
        var company1UserIds = partnerCompanyRoleService.getCompanyUserIds(company1);
        var company2UserIds = partnerCompanyRoleService.getCompanyUserIds(company2);
        assertThat(company1UserIds).isEqualTo(couriersId);
        assertThat(company2UserIds).isEmpty();
    }
}
