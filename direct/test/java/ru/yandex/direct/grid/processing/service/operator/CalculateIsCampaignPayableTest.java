package ru.yandex.direct.grid.processing.service.operator;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.defaultCampaign;
import static ru.yandex.direct.grid.processing.service.operator.OperatorAccessServiceTest.operator;
import static ru.yandex.direct.grid.processing.service.operator.OperatorClientRelations.CAMPAIGN_ACCESS_HELPER;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class CalculateIsCampaignPayableTest {
    public static Object[] parametersData() {
        return new Object[][]{
                {0L, CampaignStatusModerate.YES, 0L, true},
                {100L, CampaignStatusModerate.YES, 0L, false},
                {0L, CampaignStatusModerate.YES, 20L, true},
                {0L, CampaignStatusModerate.NO, 20L, true}
        };
    }

    @Test
    @Parameters(method = "parametersData")
    @TestCaseName("WalletId {0}, statusModerate {1}, managerUid {2}, expected {3}")
    public void checkCalculateIsCampaignPayable(Long walletId, CampaignStatusModerate statusModerate,
                                                Long managerUserId, boolean expectedResult) {
        GdiCampaign campaign = defaultCampaign().withWalletId(walletId)
                .withStatusModerate(statusModerate).withManagerUserId(managerUserId);
        var result = CAMPAIGN_ACCESS_HELPER.isCampaignPayable(operator().withRole(RbacRole.CLIENT), campaign);
        assertThat(result).isEqualTo(expectedResult);
    }
}
