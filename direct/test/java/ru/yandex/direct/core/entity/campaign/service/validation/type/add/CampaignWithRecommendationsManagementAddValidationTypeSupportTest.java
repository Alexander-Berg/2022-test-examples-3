package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang.math.RandomUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.CampaignMetatype;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithRecommendationsManagement;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainerImpl;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.PathNode;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.campaign.model.CampaignMetatype.DEFAULT_;
import static ru.yandex.direct.core.entity.campaign.model.CampaignMetatype.ECOM;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class CampaignWithRecommendationsManagementAddValidationTypeSupportTest {

    @SuppressWarnings("java:S3252")
    private static final PathNode.Field PRICE_REC_FIELD =
            field(CampaignWithRecommendationsManagement.IS_PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED);

    @SuppressWarnings("java:S3252")
    private static final PathNode.Field REC_FIELD =
            field(CampaignWithRecommendationsManagement.IS_RECOMMENDATIONS_MANAGEMENT_ENABLED);

    private final CampaignWithRecommendationsManagementAddValidationTypeSupport support =
            new CampaignWithRecommendationsManagementAddValidationTypeSupport();

    private final ClientId clientId = ClientId.fromLong(RandomUtils.nextLong());
    private final long operatorUid = RandomNumberUtils.nextPositiveLong();

    @Parameterized.Parameters(name = "recommendationsManagement = {0}," +
            " priceRecommendationsManagement = {1}, metatype = {2}, expect defect = {3}, defect field = {4}")
    public static Object[][] params() {
        return new Object[][]{
                {null, null, DEFAULT_, false, null},
                {false, null, DEFAULT_, false, null},
                {null, false, DEFAULT_, false, null},
                {false, false, DEFAULT_, false, null},
                {true, null, DEFAULT_, false, null},
                {true, false, DEFAULT_, false, null},
                {true, true, DEFAULT_, false, null},
                {false, true, DEFAULT_, true, PRICE_REC_FIELD},
                {null, true, DEFAULT_, true, PRICE_REC_FIELD},

                {null, null, ECOM, false, null},
                {false, false, ECOM, false, REC_FIELD},
                {true, false, ECOM, true, REC_FIELD},
                {true, true, ECOM, true, REC_FIELD},
                {false, true, ECOM, true, PRICE_REC_FIELD},
        };
    }

    @Test
    @Parameters(method = "params")
    @SuppressWarnings("rawtypes")
    public void preValidateTextCampaign(Boolean recommendationsManagement,
                                        Boolean priceRecommendationsManagement,
                                        CampaignMetatype metatype,
                                        boolean expectDefect,
                                        @Nullable PathNode.Field field) {
        var campaign = new TextCampaign()
                .withIsRecommendationsManagementEnabled(recommendationsManagement)
                .withIsPriceRecommendationsManagementEnabled(priceRecommendationsManagement)
                .withMetatype(metatype)
                .withClientId(clientId.asLong())
                .withName("valid_campaign_name")
                .withUid(operatorUid);

        ValidationResult<List<CampaignWithRecommendationsManagement>, Defect> vr =
                new ValidationResult<>(List.of(campaign));
        var container = new CampaignValidationContainerImpl(
                0, 0L, clientId, null, new CampaignOptions(), null, Map.of()
        );
        var result = support.preValidate(container, vr);

        if (expectDefect) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.flattenErrors().size()).isEqualTo(1);
                softly.assertThat(result).is(matchedBy(hasDefectWithDefinition(validationError(
                        path(index(0), field),
                        CommonDefects.inconsistentState())
                )));
            });
        } else {
            assertThat(result, hasNoDefectsDefinitions());
        }
    }
}
