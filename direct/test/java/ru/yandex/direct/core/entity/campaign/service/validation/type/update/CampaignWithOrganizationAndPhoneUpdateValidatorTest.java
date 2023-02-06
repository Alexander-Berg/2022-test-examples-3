package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.Collection;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithOrganizationAndPhone;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class CampaignWithOrganizationAndPhoneUpdateValidatorTest {

    private static final CampaignWithOrganizationAndPhone campaign = new TextCampaign().withId(1L)
            .withDefaultPermalinkId(2L)
            .withDefaultTrackingPhoneId(3L);

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public Set<Long> clientPhones;

    @Parameterized.Parameter(2)
    public Set<Long> changedPhoneIds;

    @Parameterized.Parameter(3)
    public Defect expectedDefect;

    @Parameterized.Parameter(4)
    public ModelProperty modelProperty;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "нет изменений телефона - ок",
                        Set.of(), // clientPhones
                        Set.of(), // changedPhoneIds
                        null, // expectedDefect
                        null
                },
                {
                        "телефон изменился на существующий - ок",
                        Set.of(campaign.getDefaultTrackingPhoneId()), // clientPhones
                        Set.of(campaign.getDefaultTrackingPhoneId()), // changedPhoneIds
                        null, // expectedDefect
                        null
                },
                {
                        "телефон изменился на несуществующий - ошибка",
                        Set.of(), // clientPhones
                        Set.of(campaign.getDefaultTrackingPhoneId()), // changedPhoneIds
                        objectNotFound(), // expectedDefect
                        CampaignWithOrganizationAndPhone.DEFAULT_TRACKING_PHONE_ID
                }
        });
    }

    @Test
    public void test() {
        var vr = CampaignWithOrganizationAndPhoneUpdateValidationTypeSupport
                .validator(clientPhones, changedPhoneIds).apply(campaign);
        if (expectedDefect != null) {
            Assert.assertThat(vr, hasDefectWithDefinition(validationError(path(field(modelProperty)),
                    expectedDefect)));
        } else {
            Assert.assertThat(vr, hasNoDefectsDefinitions());
        }
    }

}
