package ru.yandex.direct.core.entity.banner.type.phone;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithOrganizationAndPhone;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.organization.model.Organization;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.organizations.validation.OrganizationDefects.hasNoAccessToOrganization;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultActiveOrganization;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithOrganizationAndPhoneAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {

    @Autowired
    private OrganizationsClientStub organizationsClient;

    @Autowired
    private RbacService rbacService;

    @Test
    public void prepareAndApply_BannerWithOrganization_NoRightsToOrg() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        var clientId = clientInfo.getClientId();
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        ClientPhone clientPhone = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId);
        Long phoneId = clientPhone.getId();

        Organization organization = defaultActiveOrganization(clientId);
        ClientInfo clientInfo2 = steps.clientSteps().createDefaultClient();
        long chiefUid2 = rbacService.getChiefByClientId(clientInfo2.getClientId());
        organizationsClient.addUidsByPermalinkId(organization.getPermalinkId(), List.of(chiefUid2));

        var banner = clientTextBanner()
                .withPermalinkId(organization.getPermalinkId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withPhoneId(phoneId);

        ValidationResult<?, Defect> validationResult = prepareAndApplyInvalid(banner);

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(field(BannerWithOrganizationAndPhone.PHONE_ID)), hasNoAccessToOrganization())));
        assertThat(validationResult.flattenErrors(), hasSize(1));
    }


}
