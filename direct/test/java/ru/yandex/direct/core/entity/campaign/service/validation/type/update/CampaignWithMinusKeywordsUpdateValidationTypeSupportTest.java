package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMinusKeywords;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.anyValidationErrorOnPath;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class CampaignWithMinusKeywordsUpdateValidationTypeSupportTest {
    private CampaignWithMinusKeywordsUpdateValidationTypeSupport typeSupport;
    private static ClientId clientId;
    private static Long uid;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.MCBANNER},
        });
    }

    @Before
    public void before() {
        typeSupport = new CampaignWithMinusKeywordsUpdateValidationTypeSupport();
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        uid = RandomNumberUtils.nextPositiveLong();
    }

    @Test
    public void testValidateSuccessfully() {
        var campaign = ((CampaignWithMinusKeywords) TestCampaigns.newCampaignByCampaignType(campaignType))
                .withMinusKeywords(List.of("слон как слон", "ещё фраза"));
        ValidationResult<List<CampaignWithMinusKeywords>, Defect> result = typeSupport
                .validate(CampaignValidationContainer.create(0, uid, clientId),
                        new ValidationResult<>(List.of(campaign)));
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void testPreValidateSuccessfully() {
        var campaign = (CampaignWithMinusKeywords) TestCampaigns.newCampaignByCampaignType(campaignType);
        ModelChanges<CampaignWithMinusKeywords> mc =
                ModelChanges.build(campaign, CampaignWithMinusKeywords.MINUS_KEYWORDS, List.of("слон как слон"));
        var result = typeSupport.preValidate(CampaignValidationContainer.create(0, uid, clientId),
                new ValidationResult<>(List.of(mc)));
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void testValidationError() {
        var campaign = ((CampaignWithMinusKeywords) TestCampaigns.newCampaignByCampaignType(campaignType))
                .withMinusKeywords(List.of("минус фраза в которой слов больше допустимого лимита")); // Больше 7 слов
        ValidationResult<List<CampaignWithMinusKeywords>, Defect> result = typeSupport
                .validate(CampaignValidationContainer.create(0, uid, clientId),
                        new ValidationResult<>(List.of(campaign)));
        assertThat(result, hasDefectDefinitionWith(anyValidationErrorOnPath(minusKeywordsPath())));
    }

    @Test
    public void testPreValidationError() {
        var campaign = (CampaignWithMinusKeywords) TestCampaigns.newCampaignByCampaignType(campaignType);
        ModelChanges<CampaignWithMinusKeywords> mc = ModelChanges.build(campaign,
                CampaignWithMinusKeywords.MINUS_KEYWORDS, List.of("[[вложенные скобки — против правил]]"));
        var result = typeSupport.preValidate(CampaignValidationContainer.create(0, uid, clientId),
                new ValidationResult<>(List.of(mc)));
        assertThat(result, hasDefectDefinitionWith(anyValidationErrorOnPath(firstMinusKeywordPath())));
    }

    private static Path minusKeywordsPath() {
        return path(index(0), field(CampaignWithMinusKeywords.MINUS_KEYWORDS));
    }

    private static Path firstMinusKeywordPath() {
        return path(index(0), field(CampaignWithMinusKeywords.MINUS_KEYWORDS), index(0));
    }
}
