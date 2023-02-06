package ru.yandex.market.admin.service.remote.agency;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.service.remote.RemoteAgencyUIService;
import ru.yandex.market.admin.ui.model.StringID;
import ru.yandex.market.admin.ui.model.agency.UIAgency;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Тесты для {@link RemoteAgencyUIService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class RemoteAgencyUIServiceTest extends FunctionalTest {

    @Autowired
    private RemoteAgencyUIService agencyUIService;

    @Test
    @DisplayName("Поиск агентств")
    @DbUnitDataSet(before = "RemoteAgencyUIService.search.before.csv")
    void testSearchAgency() {
        final List<Map<StringID, Object>> expected = Arrays.asList(
                ImmutableMap.of(
                        UIAgency.ID, 3L,
                        UIAgency.NAME, "my_test_ag",
                        UIAgency.MANAGER_ID, 46L,
                        UIAgency.EMAIL, "test2@test.ru",
                        UIAgency.REWARD_ACCESS, false
                ),
                ImmutableMap.of(
                        UIAgency.ID, 4L,
                        UIAgency.NAME, "my_test_age",
                        UIAgency.MANAGER_ID, 47L,
                        UIAgency.EMAIL, "test3@test.ru",
                        UIAgency.REWARD_ACCESS, true
                )
        );

        final List<UIAgency> actual = agencyUIService.searchAgency("test", 2, 3);

        Assertions.assertEquals(2, actual.size());

        final Map<Long, UIAgency> actualMap =
                actual.stream().collect(Collectors.toMap(e -> e.getLongField(UIAgency.ID), Function.identity()));
        expected.forEach(e -> Assertions.assertEquals(e, actualMap.get((long) e.get(UIAgency.ID)).getFields()));
    }

    @Test
    @DisplayName("Включение агентских премий")
    @DbUnitDataSet(before = "RemoteAgencyUIService.switch.before.csv",
            after = "RemoteAgencyUIService.turn_on.after.csv")
    void testTurnOnReward() {
        switchRewardAccess(true, 1L, 2L, 3L);
    }

    @Test
    @DisplayName("Выключение агентских премий")
    @DbUnitDataSet(before = "RemoteAgencyUIService.switch.before.csv",
            after = "RemoteAgencyUIService.turn_off.after.csv")
    void testTurnOffReward() {
        switchRewardAccess(false, 1L, 2L, 3L);
    }

    @MethodSource("args")
    @ParameterizedTest(name = "{0}")
    @DbUnitDataSet(before = "RemoteAgencyUIService.testIsSubclientByService.csv")
    void testIsSubclientByService(String testCaseTitle, long partnerId, boolean isSubclient) {
        Assertions.assertEquals(isSubclient, agencyUIService.isSubclientByService(partnerId));
    }


    private static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of("Не сабклиент", 100, false),
                Arguments.of("Сабклиент", 200, true),
                Arguments.of("Нет открытой кампании", 300, false));
    }


    private void switchRewardAccess(final boolean newStatus, long... agencyIds) {
        Arrays.stream(agencyIds).forEach(id -> {
            agencyUIService.updateAgencyRewardAccess(id, newStatus);
        });

    }
}
