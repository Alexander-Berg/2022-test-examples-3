package ru.yandex.direct.grid.processing.service.operator;

import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.grid.processing.model.client.GdOperatorAction;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacSubrole;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class OperatorAllowedActionsTest {

    @SuppressWarnings("unused")
    private Object[] parametrizedTestData() {
        return new Object[][]{
                {"EMPTY", getOperator(RbacRole.EMPTY), emptySet()},
                {"SUPERREADER", getOperator(RbacRole.SUPERREADER),
                        Set.of(
                                GdOperatorAction.CAN_OPEN_PRICE_PACKAGE_GRID,
                                GdOperatorAction.CAN_VIEW_CLIENT_COUNTERS,
                                GdOperatorAction.SEND_TO_BS
                        )},
                {"MEDIA", getOperator(RbacRole.MEDIA),
                        Set.of(
                                GdOperatorAction.CAN_VIEW_CLIENT_COUNTERS
                        )},
                {"INTERNAL_AD_ADMIN", getOperator(RbacRole.INTERNAL_AD_ADMIN),
                        Set.of(
                                GdOperatorAction.CAN_VIEW_CLIENT_COUNTERS,
                                GdOperatorAction.CAN_IMPORT_EXCEL_FOR_INTERNAL_ADS,
                                GdOperatorAction.COPY
                        )},
                {"INTERNAL_AD_MANAGER", getOperator(RbacRole.INTERNAL_AD_MANAGER),
                        Set.of(
                                GdOperatorAction.CAN_VIEW_CLIENT_COUNTERS,
                                GdOperatorAction.CAN_IMPORT_EXCEL_FOR_INTERNAL_ADS,
                                GdOperatorAction.COPY
                        )},
                {"INTERNAL_AD_MANAGER", getOperator(RbacRole.INTERNAL_AD_SUPERREADER),
                        Set.of(
                                GdOperatorAction.CAN_VIEW_CLIENT_COUNTERS,
                                GdOperatorAction.CAN_IMPORT_EXCEL_FOR_INTERNAL_ADS
                        )},
                {"CLIENT", getOperator(RbacRole.CLIENT),
                        Set.of(
                                GdOperatorAction.COPY,
                                GdOperatorAction.SEND_TO_MODERATION_BY_CLIENT
                        )},
                {"AGENCY", getOperator(RbacRole.AGENCY),
                        Set.of(
                                GdOperatorAction.COPY,
                                GdOperatorAction.CAN_VIEW_CLIENT_COUNTERS,
                                GdOperatorAction.CAN_VIEW_ADD_COUNTERS_CONTROL,
                                GdOperatorAction.SEND_TO_MODERATION
                        )
                },
                {"MANAGER", getOperator(RbacRole.MANAGER),
                        Set.of(
                                GdOperatorAction.REMODERATE_CAMPAIGN,
                                GdOperatorAction.COPY,
                                GdOperatorAction.SEND_TO_MODERATION,
                                GdOperatorAction.CAN_VIEW_CLIENT_COUNTERS,
                                GdOperatorAction.SEND_TO_REMODERATION
                        )},
                {"PLACER", getOperator(RbacRole.PLACER),
                        Set.of(
                                GdOperatorAction.REMODERATE_CAMPAIGN,
                                GdOperatorAction.COPY,
                                GdOperatorAction.SEND_TO_BS,
                                GdOperatorAction.SEND_TO_MODERATION,
                                GdOperatorAction.SEND_TO_REMODERATION,
                                GdOperatorAction.REMODERATE_ADS_CALLOUTS,
                                GdOperatorAction.CAN_VIEW_CLIENT_COUNTERS
                        )},
                {"DEVELOPER", getOperator(RbacRole.SUPERREADER).withDeveloper(true),
                        Set.of(

                                GdOperatorAction.SEND_TO_BS,
                                GdOperatorAction.SEND_TO_MODERATION,
                                GdOperatorAction.SEND_TO_REMODERATION,
                                GdOperatorAction.ACCEPT_MODERATION,
                                GdOperatorAction.REMODERATE_ADS_CALLOUTS,
                                GdOperatorAction.ACCEPT_ADS_CALLOUTS_MODERATION,
                                GdOperatorAction.CAN_VIEW_CLIENT_COUNTERS,
                                GdOperatorAction.CAN_OPEN_PRICE_PACKAGE_GRID
                        )},
                {"SUPERPLACER", getOperator(RbacRole.PLACER)
                        .withSubRole(RbacSubrole.SUPERPLACER),
                        Set.of(
                                GdOperatorAction.REMODERATE_CAMPAIGN,
                                GdOperatorAction.COPY,
                                GdOperatorAction.SEND_TO_BS,
                                GdOperatorAction.SEND_TO_MODERATION,
                                GdOperatorAction.SEND_TO_REMODERATION,
                                GdOperatorAction.ACCEPT_MODERATION,
                                GdOperatorAction.REMODERATE_ADS_CALLOUTS,
                                GdOperatorAction.CAN_VIEW_CLIENT_COUNTERS,
                                GdOperatorAction.ACCEPT_ADS_CALLOUTS_MODERATION
                        )},
                {"SUPPORT", getOperator(RbacRole.SUPPORT),
                        Set.of(
                                GdOperatorAction.REMODERATE_CAMPAIGN,
                                GdOperatorAction.SEND_TO_BS,
                                GdOperatorAction.SEND_TO_MODERATION,
                                GdOperatorAction.SEND_TO_REMODERATION,
                                GdOperatorAction.ACCEPT_MODERATION,
                                GdOperatorAction.REMODERATE_ADS_CALLOUTS,
                                GdOperatorAction.CAN_OPEN_PRICE_PACKAGE_GRID,
                                GdOperatorAction.MANAGE_PRICE_PACKAGE_CLIENTS,
                                GdOperatorAction.APPROVE_PRICE_PACKAGES,
                                GdOperatorAction.CAN_VIEW_CLIENT_COUNTERS,
                                GdOperatorAction.ACCEPT_ADS_CALLOUTS_MODERATION
                        )},
                {"LIMITED_SUPPORT", getOperator(RbacRole.LIMITED_SUPPORT),
                        Set.of(
                                GdOperatorAction.REMODERATE_CAMPAIGN,
                                GdOperatorAction.SEND_TO_REMODERATION,
                                GdOperatorAction.REMODERATE_ADS_CALLOUTS,
                                GdOperatorAction.CAN_VIEW_ADD_COUNTERS_CONTROL,
                                GdOperatorAction.CAN_VIEW_CLIENT_COUNTERS,
                                GdOperatorAction.SEND_TO_BS
                        )},
                {"SUPER", getOperator(RbacRole.SUPER),
                        Set.of(
                                GdOperatorAction.REMODERATE_CAMPAIGN,
                                GdOperatorAction.COPY,
                                GdOperatorAction.SEND_TO_BS,
                                GdOperatorAction.SEND_TO_MODERATION,
                                GdOperatorAction.SEND_TO_REMODERATION,
                                GdOperatorAction.ACCEPT_MODERATION,
                                GdOperatorAction.REMODERATE_ADS_CALLOUTS,
                                GdOperatorAction.ACCEPT_ADS_CALLOUTS_MODERATION,
                                GdOperatorAction.CAN_OPEN_PRICE_PACKAGE_GRID,
                                GdOperatorAction.MANAGE_PRICE_PACKAGES,
                                GdOperatorAction.MANAGE_PRICE_PACKAGE_CLIENTS,
                                GdOperatorAction.APPROVE_PRICE_PACKAGES,
                                GdOperatorAction.APPROVE_PRICE_SALES_CAMPAIGNS,
                                GdOperatorAction.CAN_VIEW_CLIENT_COUNTERS,
                                GdOperatorAction.MODIFY_DB,
                                GdOperatorAction.CAN_IMPORT_EXCEL_FOR_INTERNAL_ADS
                        )},
        };
    }

    private static User getOperator(RbacRole role) {
        return new User()
                .withRole(role)
                .withIsReadonlyRep(false);
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("check allowed actions for {0}")
    public void checkOperatorAllowedActions(@SuppressWarnings("unused") String operatorName, User operator,
                                            Set<GdOperatorAction> expectedAllowedActions) {
        Set<GdOperatorAction> actions = OperatorAllowedActionsUtils.getActions(operator);

        assertThat(actions)
                .isEqualTo(expectedAllowedActions);
    }

}
