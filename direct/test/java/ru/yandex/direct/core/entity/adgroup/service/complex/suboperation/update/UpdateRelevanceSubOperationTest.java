package ru.yandex.direct.core.entity.adgroup.service.complex.suboperation.update;

import java.math.BigDecimal;
import java.util.List;

import one.util.streamex.IntStreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchModificationBaseTest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.validation.defects.MoneyDefects;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateRelevanceSubOperationTest extends RelevanceMatchModificationBaseTest {

    public static final int CAMPAIGNS_NUMBER = 10;

    @Test
    public void addRelevanceMatch_Prepare_success() {
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withPrice(BigDecimal.TEN)
                .withPriceContext(BigDecimal.TEN.add(BigDecimal.ONE))
                .withIsSuspended(true);


        UpdateRelevanceSubOperation updateRelevanceSubOperation =
                new UpdateRelevanceSubOperation(
                        relevanceMatchService,
                        relevanceMatchRepository,
                        singletonList(relevanceMatch),
                        false,
                        null,
                        defaultUser.getUid(),
                        defaultUser.getClientInfo().getClientId(),
                        defaultUser.getUid(),
                        defaultUser.getShard());
        updateRelevanceSubOperation.setAffectedAdGroupIds(adGroupIds);

        ValidationResult<List<RelevanceMatch>, Defect> prepare = updateRelevanceSubOperation.prepare();
        assertThat(prepare.hasAnyErrors()).isFalse();
    }

    @Test
    public void addRelevanceMatchesOneInvalid_Prepare_ResultHasElementError() {
        List<CampaignInfo> campaignInfos = IntStreamEx.range(0, CAMPAIGNS_NUMBER)
                .mapToObj((index) -> defaultUser.getClientInfo())
                .map(campaignSteps::createActiveCampaign)
                .toList();

        List<AdGroupInfo> adGroupInfos =
                mapList(campaignInfos, campaignInfo -> adGroupSteps.createAdGroup(getAdGroup(), campaignInfo));

        List<RelevanceMatch> relevanceMatchList =
                mapList(adGroupInfos.subList(0, CAMPAIGNS_NUMBER - 1), adGroupInfo ->
                        new RelevanceMatch()
                                .withCampaignId(adGroupInfo.getCampaignId())
                                .withAdGroupId(adGroupInfo.getAdGroupId())
                                .withPrice(BigDecimal.TEN)
                                .withIsSuspended(true));

        relevanceMatchList.add(new RelevanceMatch()
                .withCampaignId(adGroupInfos.get(CAMPAIGNS_NUMBER - 1).getCampaignId())
                .withAdGroupId(adGroupInfos.get(CAMPAIGNS_NUMBER - 1).getAdGroupId())
                .withPrice(BigDecimal.ZERO)
                .withIsSuspended(true));


        UpdateRelevanceSubOperation updateRelevanceSubOperation =
                new UpdateRelevanceSubOperation(relevanceMatchService,
                        relevanceMatchRepository,
                        relevanceMatchList,
                        false,
                        null,
                        defaultUser.getUid(),
                        defaultUser.getClientInfo().getClientId(),
                        defaultUser.getUid(),
                        defaultUser.getShard());
        updateRelevanceSubOperation.setAffectedAdGroupIds(adGroupIds);

        ValidationResult<List<RelevanceMatch>, Defect> prepare =
                updateRelevanceSubOperation.prepare();

        Currency currency = defaultUser.getClientInfo().getClient().getWorkCurrency().getCurrency();
        assertThat(prepare.getSubResults().get(index(CAMPAIGNS_NUMBER - 1)))
                .is(matchedBy(hasDefectWithDefinition(validationError(path(field("price")),
                        MoneyDefects.invalidValueNotLessThan(
                                Money.valueOf(currency.getMinPrice(), currency.getCode()))))));
    }
}
