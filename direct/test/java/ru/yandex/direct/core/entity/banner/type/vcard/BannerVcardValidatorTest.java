package ru.yandex.direct.core.entity.banner.type.vcard;

import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.vcardNotFound;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.vcardOfAnotherCampaign;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class BannerVcardValidatorTest {

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public Long vcardId;

    @Parameterized.Parameter(2)
    public Map<Long, Long> existingVcardIdToCampaignId;

    @Parameterized.Parameter(3)
    public Long bannerCampaignId;

    @Parameterized.Parameter(4)
    public Defect<Long> expectedDefect;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "vcardId валиден и соответствует кампании баннера",
                        1L,
                        singletonMap(1L, 101L),
                        101L,
                        null
                },
                {
                        "vcardId == null",
                        null,
                        emptyMap(),
                        101L,
                        null
                },
                {
                        "несуществующий vcardId",
                        1L,
                        emptyMap(),
                        101L,
                        vcardNotFound()
                },
                {
                        "vcardId привязан к другой кампании",
                        1L,
                        singletonMap(1L, 101L),
                        102L,
                        vcardOfAnotherCampaign()
                },
                {
                        "bannerCampaignId == null",
                        1L,
                        singletonMap(1L, 101L),
                        null,
                        null
                },
        });
    }

    @Test
    public void testValidator() {
        ValidationResult<Long, Defect> validationResult =
                new BannerVcardValidator(existingVcardIdToCampaignId, bannerCampaignId).apply(vcardId);

        if (expectedDefect != null) {
            assertThat(validationResult, hasDefectWithDefinition(validationError(path(), expectedDefect)));
        } else {
            assertThat(validationResult, hasNoDefectsDefinitions());
        }
    }
}
