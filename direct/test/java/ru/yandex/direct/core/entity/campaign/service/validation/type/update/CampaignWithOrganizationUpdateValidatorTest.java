package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithOrganization;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.organization.model.Organization;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.organization.model.OrganizationStatusPublish.CLOSED;
import static ru.yandex.direct.core.entity.organization.model.OrganizationStatusPublish.MOVED;
import static ru.yandex.direct.core.entity.organization.model.OrganizationStatusPublish.PUBLISHED;
import static ru.yandex.direct.core.entity.organizations.validation.OrganizationDefects.invalidOrganizationStatus;
import static ru.yandex.direct.core.entity.organizations.validation.OrganizationDefects.organizationNotFound;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class CampaignWithOrganizationUpdateValidatorTest {

    private static final Organization PUBLISHED_ORGANIZATION = new Organization().withStatusPublish(PUBLISHED);
    private static final Organization MOVED_ORGANIZATION = new Organization().withStatusPublish(MOVED);
    private static final Organization CLOSED_ORGANIZATION = new Organization().withStatusPublish(CLOSED);

    private static final CampaignWithOrganization campaign = new TextCampaign().withId(1L)
            .withDefaultPermalinkId(2L)
            .withDefaultTrackingPhoneId(3L);

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public Map<Long, Organization> clientOrganizations;

    @Parameterized.Parameter(2)
    public Set<Long> changedOrganizations;

    @Parameterized.Parameter(3)
    public boolean isCopy;

    @Parameterized.Parameter(4)
    public Defect expectedDefect;

    @Parameterized.Parameter(5)
    public ModelProperty modelProperty;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "нет измененных организаций - ок",
                        Map.of(), // clientOrganizations
                        Set.of(), // changedOrganizations
                        false, // isCopy
                        null, // expectedDefect
                        null
                },
                {
                        "изменилась организация - ок",
                        Map.of(campaign.getDefaultPermalinkId(), PUBLISHED_ORGANIZATION), // clientOrganizations
                        Set.of(campaign.getDefaultPermalinkId()), // changedOrganizations
                        false, // isCopy
                        null, // expectedDefect
                        null
                },
                {
                        "изменилась организация на несуществующую - ошибка",
                        Map.of(), // clientOrganizations
                        Set.of(campaign.getDefaultPermalinkId()), // changedOrganizations
                        false, // isCopy
                        organizationNotFound(), // expectedDefect
                        CampaignWithOrganization.DEFAULT_PERMALINK_ID
                },
                {
                        "статус измененной организации moved - ошибка",
                        Map.of(campaign.getDefaultPermalinkId(), MOVED_ORGANIZATION), // clientOrganizations
                        Set.of(campaign.getDefaultPermalinkId()), // changedOrganizations
                        false, // isCopy
                        invalidOrganizationStatus(MOVED), // expectedDefect
                        CampaignWithOrganization.DEFAULT_PERMALINK_ID
                },
                {
                        "статус измененной (копируемой) организации moved - ок",
                        Map.of(campaign.getDefaultPermalinkId(), MOVED_ORGANIZATION), // clientOrganizations
                        Set.of(campaign.getDefaultPermalinkId()), // changedOrganizations
                        true, // isCopy
                        null, // expectedDefect
                        null
                },
                {
                        "статус измененной организации closed - ошибка",
                        Map.of(campaign.getDefaultPermalinkId(), CLOSED_ORGANIZATION), // clientOrganizations
                        Set.of(campaign.getDefaultPermalinkId()), // changedOrganizations
                        false, // isCopy
                        organizationNotFound(), // expectedDefect
                        CampaignWithOrganization.DEFAULT_PERMALINK_ID
                },
        });
    }

    @Test
    public void test() {
        var vr = CampaignWithOrganizationUpdateValidationTypeSupport
                .validator(clientOrganizations, changedOrganizations, isCopy).apply(campaign);
        if (expectedDefect != null) {
            Assert.assertThat(vr, hasDefectWithDefinition(validationError(path(field(modelProperty)),
                    expectedDefect)));
        } else {
            Assert.assertThat(vr, hasNoDefectsDefinitions());
        }
    }
}
