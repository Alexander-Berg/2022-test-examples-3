package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

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
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newCampaignByCampaignType;
import static ru.yandex.direct.testing.matchers.validation.Matchers.anyValidationErrorOnPath;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class CampaignWithMinusKeywordsAddValidationTypeSupportTest {
    private CampaignWithMinusKeywordsAddValidationTypeSupport typeSupport;
    private static ClientId clientId;
    private static Long uid;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void before() {
        typeSupport = new CampaignWithMinusKeywordsAddValidationTypeSupport();
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        uid = RandomNumberUtils.nextPositiveLong();
    }

    @Test
    public void testValidateSuccessfully() {
        var campaign = ((CampaignWithMinusKeywords) newCampaignByCampaignType(campaignType))
                .withMinusKeywords(List.of("???????? ?????? ????????", "?????? ??????????"));
        ValidationResult<List<CampaignWithMinusKeywords>, Defect> result =
                typeSupport.validate(CampaignValidationContainer.create(0, uid, clientId),
                        new ValidationResult<>(List.of(campaign)));
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void testPreValidateSuccessfully() {
        var campaign = ((CampaignWithMinusKeywords) newCampaignByCampaignType(campaignType))
                .withMinusKeywords(List.of("???????? ?????? ????????"));
        var result = typeSupport.preValidate(CampaignValidationContainer.create(0, uid, clientId),
                new ValidationResult<>(List.of(campaign)));
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void testValidationError() {
        var campaign = ((CampaignWithMinusKeywords) newCampaignByCampaignType(campaignType))
                .withMinusKeywords(List.of("?????????? ?????????? ?? ?????????????? ???????? ???????????? ?????????????????????? ????????????")); // ???????????? 7 ????????
        ValidationResult<List<CampaignWithMinusKeywords>, Defect> result =
                typeSupport.validate(CampaignValidationContainer.create(0, uid, clientId),
                        new ValidationResult<>(List.of(campaign)));
        assertThat(result, hasDefectDefinitionWith(anyValidationErrorOnPath(minusKeywordsPath())));
    }

    @Test
    public void testPreValidationError() {
        var campaign = ((CampaignWithMinusKeywords) newCampaignByCampaignType(campaignType))
                .withMinusKeywords(List.of("[[?????????????????? ???????????? ??? ???????????? ????????????]]"));
        var result = typeSupport.preValidate(CampaignValidationContainer.create(0, uid, clientId),
                new ValidationResult<>(List.of(campaign)));
        assertThat(result, hasDefectDefinitionWith(anyValidationErrorOnPath(firstMinusKeywordPath())));
    }

    private static Path minusKeywordsPath() {
        return path(index(0), field(CampaignWithMinusKeywords.MINUS_KEYWORDS));
    }

    private static Path firstMinusKeywordPath() {
        return path(index(0), field(CampaignWithMinusKeywords.MINUS_KEYWORDS), index(0));
    }
}
