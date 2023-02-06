package ru.yandex.market.loyalty.back.util;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.loyalty.back.controller.GdprDataDeleteController;
import ru.yandex.market.loyalty.back.controller.PingController;
import ru.yandex.market.loyalty.back.security.Actions;
import ru.yandex.market.loyalty.core.utils.CommonTestUtils;
import ru.yandex.market.loyalty.monitoring.beans.MonitorController;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.loyalty.back.security.Actions.TEST_FEATURE;
import static ru.yandex.market.loyalty.core.utils.CoreCollectionUtils.minus;
import static ru.yandex.market.loyalty.lightweight.ExceptionUtils.makeExceptionsUnchecked;

public class SecuredHandlesTest {
    @Test
    public void checkAllWebMethodsSecured() {
        CommonTestUtils.checkAllWebMethodsSecured(
                "ru.yandex.market.loyalty.back",
                ImmutableSet.of(
                        PingController.class,
                        MonitorController.class,
                        GdprDataDeleteController.class
                )
        );
    }

    @Test
    public void allActionsAndUsedRolesAllowedIsSame() {
        Set<String> allActions = allActions();

        Set<String> usedRoles = CommonTestUtils.allRolesAllowed("ru.yandex.market.loyalty.back")
                .collect(Collectors.toSet());

        assertThat(minus(allActions, usedRoles), containsInAnyOrder(TEST_FEATURE));
        assertThat(minus(usedRoles, allActions), is(empty()));
    }

    @NotNull
    private static Set<String> allActions() {
        return Arrays.stream(Actions.class.getDeclaredFields())
                .filter(field -> field.getType().equals(String.class))
                .map(makeExceptionsUnchecked(f -> (String) f.get(null)))
                .collect(Collectors.toSet());
    }

    //TODO
    @Test
    @Ignore("TODO")
    public void allActionsUsedInBackClients() {

    }

}
