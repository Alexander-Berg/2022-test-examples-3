package ru.yandex.direct.grid.processing.service.operator;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableSet;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.grid.model.campaign.GdCampaignAccess;
import ru.yandex.direct.grid.model.campaign.GdiBaseCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaignAction;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.defaultCampaign;
import static ru.yandex.direct.grid.processing.service.operator.OperatorAccessServiceTest.operator;
import static ru.yandex.direct.grid.processing.service.operator.OperatorClientRelations.CAMPAIGN_ACCESS_HELPER;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CalculateCanTransferMoneyTest {

    public static Object[] parametersData() {
        long someWalletId = RandomNumberUtils.nextPositiveLong();

        return new Object[][]{
                {
                        "Кампаний нет, поэтому ожидаем получить false",
                        operator()
                                .withRole(RbacRole.SUPER),
                        false,
                        Collections.emptyList(),
                        false
                },
                {
                        "Кампаний должно быть больше одного, поэтому ожидаем получить false",
                        operator()
                                .withRole(RbacRole.SUPER),
                        false,
                        Collections.singleton(defaultCampaign()),
                        false
                },
                {
                        "Кампаний достаточно, поэтому ожидаем получить true",
                        operator()
                                .withRole(RbacRole.CLIENT),
                        false,
                        Arrays.asList(defaultCampaign(), defaultCampaign()),
                        true
                },
                {
                        "Для суперридера ожидаем получить false",
                        operator()
                                .withRole(RbacRole.SUPERREADER),
                        false,
                        Arrays.asList(defaultCampaign(), defaultCampaign()),
                        false
                },
                {
                        "Для агентства с запретом на трансфер ожидаем получить false",
                        operator()
                                .withRole(RbacRole.AGENCY)
                                .withRepType(RbacRepType.LIMITED),
                        true,
                        Arrays.asList(defaultCampaign(), defaultCampaign()),
                        false
                },
                {
                        "У кампании нет денег, поэтому ожидаем получить false",
                        operator()
                                .withRole(RbacRole.CLIENT),
                        false,
                        Arrays.asList(defaultCampaign(),
                                defaultCampaign().withSum(BigDecimal.ZERO)
                        ),
                        false
                },
                {
                        "У оператора нет доступа к кампаниям, поэтому ожидаем получить false",
                        operator()
                                .withRole(RbacRole.CLIENT),
                        false,
                        Arrays.asList(defaultCampaign()
                                        .withActions(defaultCampaign().getActions()
                                                .withActions(Collections.emptySet())
                                        ),
                                defaultCampaign()
                                        .withActions(defaultCampaign().getActions()
                                                .withActions(Collections.emptySet())
                                        )
                        ),
                        false
                },
                {
                        "Кампании под общим счетом, поэтому ожидаем получить false",
                        operator()
                                .withRole(RbacRole.CLIENT),
                        false,
                        Arrays.asList(defaultCampaign().withWalletId(someWalletId),
                                defaultCampaign().withWalletId(someWalletId)
                        ),
                        false
                },
                {
                        "У субклиента есть право на трансфер денег, поэтому ожидаем получить true",
                        operator()
                                .withRole(RbacRole.CLIENT),
                        false,
                        Arrays.asList(defaultCampaign()
                                        .withActions(defaultCampaign().getActions()
                                                .withHasAgency(true)
                                                .withActions(ImmutableSet.of(
                                                        GdiCampaignAction.ALLOW_TRANSFER_MONEY_SUBCLIENT,
                                                        GdiCampaignAction.EDIT_CAMP))
                                        ),
                                defaultCampaign()
                                        .withActions(defaultCampaign().getActions()
                                                .withHasAgency(true)
                                                .withActions(ImmutableSet.of(
                                                        GdiCampaignAction.ALLOW_TRANSFER_MONEY_SUBCLIENT,
                                                        GdiCampaignAction.EDIT_CAMP))
                                        )
                        ),
                        true
                },
                {
                        "У субклиента нет прав на трансфер денег, поэтому ожидаем получить false",
                        operator()
                                .withRole(RbacRole.CLIENT),
                        false,
                        Arrays.asList(defaultCampaign()
                                        .withActions(defaultCampaign().getActions()
                                                .withHasAgency(true)
                                        ),
                                defaultCampaign()
                                        .withActions(defaultCampaign().getActions()
                                                .withHasAgency(true)
                                        )
                        ),
                        false
                },
        };
    }

    @Test
    @Parameters(method = "parametersData")
    @TestCaseName("{0}")
    public void checkCalculateCanTransferMoney(String description, User operator,
                                               boolean operatorDisallowMoneyTransfer,
                                               Collection<GdiBaseCampaign> campaigns, boolean expectedResult) {
        Map<Long, GdCampaignAccess> gdCampaignAccessById = listToMap(campaigns, GdiBaseCampaign::getId,
                campaign -> CAMPAIGN_ACCESS_HELPER.getCampaignAccess(operator, campaign, null, false, false));
        boolean result = OperatorClientRelationsHelper
                .calculateCanTransferMoney(operator, operatorDisallowMoneyTransfer, campaigns, gdCampaignAccessById);

        assertThat(result).isEqualTo(expectedResult);
    }

}
