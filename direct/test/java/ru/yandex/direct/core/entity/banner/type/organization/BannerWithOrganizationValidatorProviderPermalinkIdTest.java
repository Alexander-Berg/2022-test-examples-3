package ru.yandex.direct.core.entity.banner.type.organization;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.organization.model.Organization;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.organization.model.OrganizationStatusPublish.CLOSED;
import static ru.yandex.direct.core.entity.organization.model.OrganizationStatusPublish.MOVED;
import static ru.yandex.direct.core.entity.organization.model.OrganizationStatusPublish.PUBLISHED;
import static ru.yandex.direct.core.entity.organizations.validation.OrganizationDefects.invalidOrganizationStatus;
import static ru.yandex.direct.core.entity.organizations.validation.OrganizationDefects.organizationNotFound;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class BannerWithOrganizationValidatorProviderPermalinkIdTest {

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public Long bannerId;

    @Parameterized.Parameter(2)
    public Set<Long> bannerIdsWithUnchangedOrganizations;

    @Parameterized.Parameter(3)
    public Map<Long, Organization> clientOrganizations;

    @Parameterized.Parameter(4)
    public Boolean required;

    @Parameterized.Parameter(5)
    public boolean isCopy;

    @Parameterized.Parameter(6)
    public Long permalinkIdToValidate;

    @Parameterized.Parameter(7)
    public Defect expectedDefect;

    private static final Organization PUBLISHED_ORGANIZATION = new Organization().withStatusPublish(PUBLISHED);
    private static final Organization MOVED_ORGANIZATION = new Organization().withStatusPublish(MOVED);
    private static final Organization CLOSED_ORGANIZATION = new Organization().withStatusPublish(CLOSED);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "null permalinkIdToValidate и required = false - ok",
                        1L, // bannerId
                        Set.of(), // bannerIdsWithUnchangedOrganizations
                        Map.of(), // clientOrganizations
                        false, // required
                        false, // isCopy
                        null, // permalinkIdToValidate
                        null // expectedDefect
                },
                {
                        "null permalinkIdToValidate и required = true - error",
                        1L, // bannerId
                        Set.of(), // bannerIdsWithUnchangedOrganizations
                        Map.of(), // clientOrganizations
                        true, // required
                        false, // isCopy
                        null, // permalinkIdToValidate
                        notNull() // expectedDefect
                },
                {
                        "баннер в списке unchanged - ok",
                        1L, // bannerId
                        Set.of(1L), // bannerIdsWithUnchangedOrganizations
                        Map.of(), // clientOrganizations
                        false, // required
                        false, // isCopy
                        5L, // permalinkIdToValidate
                        null // expectedDefect
                },
                {
                        "баннера нет в списке unchanged и permalinkIdToValidate нет в available - error",
                        1L, // bannerId
                        Set.of(), // bannerIdsWithUnchangedOrganizations
                        Map.of(), // clientOrganizations
                        false, // required
                        false, // isCopy
                        5L, // permalinkIdToValidate
                        organizationNotFound() // expectedDefect
                },
                {
                        "баннера нет в списке unchanged, permalinkIdToValidate есть в available, " +
                                "статус организации publish - ok",
                        1L, // bannerId
                        Set.of(), // bannerIdsWithUnchangedOrganizations
                        Map.of(5L, PUBLISHED_ORGANIZATION), // clientOrganizations
                        false, // required
                        false, // isCopy
                        5L, // permalinkIdToValidate
                        null // expectedDefect
                },
                {
                        "баннера нет в списке unchanged, permalinkIdToValidate есть в available, " +
                                "статус организации moved - error",
                        1L, // bannerId
                        Set.of(), // bannerIdsWithUnchangedOrganizations
                        Map.of(5L, MOVED_ORGANIZATION), // clientOrganizations
                        false, // required
                        false, // isCopy
                        5L, // permalinkIdToValidate
                        invalidOrganizationStatus(MOVED) // expectedDefect
                },
                {
                        "баннера нет в списке unchanged, permalinkIdToValidate есть в available, " +
                                "статус копируемой организации moved - ok",
                        1L, // bannerId
                        Set.of(), // bannerIdsWithUnchangedOrganizations
                        Map.of(5L, MOVED_ORGANIZATION), // clientOrganizations
                        false, // required
                        true, // isCopy
                        5L, // permalinkIdToValidate
                        null // expectedDefect
                },
                {
                        "баннера нет в списке unchanged, permalinkIdToValidate есть в available, " +
                                "статус организации closed - error",
                        1L, // bannerId
                        Set.of(), // bannerIdsWithUnchangedOrganizations
                        Map.of(5L, CLOSED_ORGANIZATION), // clientOrganizations
                        false, // required
                        false, // isCopy
                        5L, // permalinkIdToValidate
                        organizationNotFound() // expectedDefect
                },
        });
    }

    @Test
    public void testValidationProviderPermalinkIdValidator() {
        ValidationResult<Long, Defect> vr = new PermalinkIdValidator(
                bannerId, bannerIdsWithUnchangedOrganizations, clientOrganizations, required, isCopy).apply(permalinkIdToValidate);
        if (expectedDefect != null) {
            assertThat(vr, hasDefectWithDefinition(validationError(path(), expectedDefect)));
        } else {
            assertThat(vr, hasNoDefectsDefinitions());
        }
    }

}
