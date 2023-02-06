package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

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
import ru.yandex.direct.model.ModelChanges;
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
public class CampaignWithRecommendationsManagementUpdateValidationTypeSupportTest {

    @SuppressWarnings("java:S3252")
    private static final PathNode.Field PRICE_REC_FIELD =
            field(CampaignWithRecommendationsManagement.IS_PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED);

    @SuppressWarnings("java:S3252")
    private static final PathNode.Field REC_FIELD =
            field(CampaignWithRecommendationsManagement.IS_RECOMMENDATIONS_MANAGEMENT_ENABLED);

    private final CampaignWithRecommendationsManagementUpdateValidationTypeSupport support =
            new CampaignWithRecommendationsManagementUpdateValidationTypeSupport();

    private final ClientId clientId = ClientId.fromLong(RandomUtils.nextLong());
    private final long operatorUid = RandomNumberUtils.nextPositiveLong();

    @Parameterized.Parameters(name = "recommendationsManagementBefore = {0}," +
            " priceRecommendationsManagementBefore = {1}," +
            " recommendationsManagement = {2}, priceRecommendationsManagement = {3}," +
            " metatype = {4}" +
            " expect defect = {5}, defect field = {6}")
    public static Object[][] params() {
        return new Object[][]{
                {false, false, false, false, DEFAULT_, false, null}, // одного кейса без изменений хватит
                {false, false, true, true, DEFAULT_, false, null},
                {false, false, true, false, DEFAULT_, false, null},
                {false, false, false, true, DEFAULT_, true, PRICE_REC_FIELD},
                {true, true, true, false, DEFAULT_, false, null},
                {true, true, false, false, DEFAULT_, false, null},
                {true, true, false, true, DEFAULT_, true, REC_FIELD},
                {true, false, false, false, DEFAULT_, false, null},
                {true, false, true, true, DEFAULT_, false, null},
                {true, false, false, true, DEFAULT_, true, REC_FIELD},

                {false, false, false, false, ECOM, false, null},
                {false, false, true, false, ECOM, true, REC_FIELD},
                {false, false, true, true, ECOM, true, REC_FIELD},
                {false, false, false, true, ECOM, true, PRICE_REC_FIELD},
        };
    }

    @Test
    @Parameters(method = "params")
    @SuppressWarnings("rawtypes")
    public void preValidateTextCampaign(boolean recommendationsManagementBefore,
                                        boolean priceRecommendationsManagementBefore,
                                        boolean recommendationsManagement,
                                        boolean priceRecommendationsManagement,
                                        CampaignMetatype metatype,
                                        boolean expectDefect,
                                        @Nullable PathNode.Field field) {
        var campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withIsRecommendationsManagementEnabled(recommendationsManagementBefore)
                .withIsPriceRecommendationsManagementEnabled(priceRecommendationsManagementBefore)
                .withMetatype(metatype)
                .withClientId(clientId.asLong())
                .withName("valid_campaign_name")
                .withUid(operatorUid);

        var container = new CampaignValidationContainerImpl(
                0, 0L, clientId, null, new CampaignOptions(), null, Map.of()
        );
        ModelChanges<CampaignWithRecommendationsManagement> changes = null;
        if (recommendationsManagementBefore != recommendationsManagement) {
            changes = ModelChanges.build(campaign,
                    CampaignWithRecommendationsManagement.IS_RECOMMENDATIONS_MANAGEMENT_ENABLED,
                    recommendationsManagement);
        }
        if (priceRecommendationsManagementBefore != priceRecommendationsManagement) {
            if (changes == null) {
                changes = ModelChanges.build(campaign,
                        CampaignWithRecommendationsManagement.IS_PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED,
                        priceRecommendationsManagement);
            } else {
                changes.process(priceRecommendationsManagement,
                        CampaignWithRecommendationsManagement.IS_PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED);
            }
        }
        ValidationResult<List<ModelChanges<CampaignWithRecommendationsManagement>>, Defect> vr =
                new ValidationResult<>(changes == null ? List.of() : List.of(changes));

        var result = support.validateBeforeApply(container, vr, Map.of(campaign.getId(), campaign));

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
