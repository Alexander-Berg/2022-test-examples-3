package ru.yandex.market.logistics.cs.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.cs.domain.enumeration.DayOffType;
import ru.yandex.market.logistics.cs.domain.enumeration.DayOffUnsetMode;
import ru.yandex.market.logistics.cs.domain.exception.DayOffChangeException;
import ru.yandex.market.logistics.cs.repository.CapacityValueCounterTestRepository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static ru.yandex.market.logistics.cs.domain.enumeration.DayOffType.MANUAL;
import static ru.yandex.market.logistics.cs.domain.enumeration.DayOffType.PROPAGATED;
import static ru.yandex.market.logistics.cs.domain.enumeration.DayOffType.TECHNICAL;
import static ru.yandex.market.logistics.cs.domain.enumeration.DayOffType.THRESHOLD;
import static ru.yandex.market.logistics.cs.domain.enumeration.DayOffType.UNSET;

@DisplayName("Тестирование проставления и снятия выходных на счётчиках")
class CapacityValueCounterDayOffTest extends AbstractIntegrationTest {

    @Autowired
    private CapacityValueCounterTestRepository counterTestRepository;

    @Autowired
    private CapacityValueCounterService counterService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // --------------------------
    // SetDayOff Validation Tests
    // --------------------------

    @DisplayName("Ставим DayOff с запрещённым типом PROPAGATED")
    @Test
    void testSetDayOffWithPropagatedType() {
        // Action and Assertion
        softly.assertThatThrownBy(() -> counterService.setDayOff(420, PROPAGATED))
            .isInstanceOf(DayOffChangeException.class)
            .hasMessage("Unable to set a day off. Unaccepted day off type provided: PROPAGATED");
    }

    @DisplayName("Ставим DayOff с запрещённым типом UNSET")
    @Test
    void testSetDayOffWithUnsetType() {
        // Action and Assertion
        softly.assertThatThrownBy(() -> counterService.setDayOff(420, UNSET))
            .isInstanceOf(DayOffChangeException.class)
            .hasMessage("Unable to set a day off. Unaccepted day off type provided: UNSET");
    }

    @DisplayName("Ставим DayOff на несуществующий счётчик")
    @Test
    void testSetDayOffToNonExistingCounter() {
        // Action and Assertion
        softly.assertThatThrownBy(() -> counterService.setDayOff(420, TECHNICAL))
            .isInstanceOf(DayOffChangeException.class)
            .hasMessage("Unable to set a day off. There's no counter with provided id: 420");
    }

    @DisplayName("Ставим DayOff на фиктивный счётчик")
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/counters_default.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetDayOffToFictitiousCounter() {
        // Action and Assertion
        softly.assertThatThrownBy(() -> counterService.setDayOff(1004, THRESHOLD))
            .isInstanceOf(DayOffChangeException.class)
            .hasMessage("Unable to set a day off. Counter 1004 is fictitious");
    }

    @DisplayName("Ставим DayOff на счётчик, который не превысил порог")
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/counters_default.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetDayOffToNonOverflowedCounter() {
        // Action and Assertion
        softly.assertThatThrownBy(() -> counterService.setDayOff(1001, TECHNICAL))
            .isInstanceOf(DayOffChangeException.class)
            .hasMessage("Unable to set a day off in counter 1001: count=0 < threshold=4000");
    }

    @DisplayName("Ставим DayOff на счётчик, у которого уже стоит True DayOff")
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_day_off_to_already_true_day_off_counter.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetDayOffToAlreadyTrueDayOffCounter() {
        // Setup
        runInTransaction(() -> {
            counterTestRepository.updateCount(1009, 1500);
            counterService.setDayOff(1009, TECHNICAL);
        });

        // Action and Assertion
        softly.assertThatThrownBy(() -> counterService.setDayOff(1009, TECHNICAL))
            .isInstanceOf(DayOffChangeException.class)
            .hasMessage("Unable to set a day off. There's already a non propagated day off in counter 1009");
    }

    // ------------------------
    // SetDayOff Behavior Tests
    // ------------------------

    @DisplayName("Ставим DayOff на счётчик в середине дерева")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_day_off_to_the_middle.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetDayOffToTheMiddle() {
        // Setup
        counterTestRepository.updateCount(1014, 3000);

        // Action
        var affectedCounterIds = counterService.setDayOff(1014, TECHNICAL);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1014L, 1020L, 1022L, 1023L));
    }

    @DisplayName("Ставим DayOff на счётчик в корне дерева")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_day_off_to_the_root.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetDayOffToTheRoot() {
        // Setup
        counterTestRepository.updateCount(1001, 4000);

        // Action
        var affectedCounterIds = counterService.setDayOff(1001, TECHNICAL);

        // Assertion
        assertSameIds(
            affectedCounterIds,
            List.of(1001L, 1002L, 1004L, 1005L, 1006L, 1007L, 1008L, 1009L, 1010L, 1011L, 1012L)
        );
    }

    @DisplayName("Ставим DayOff на счётчик в листе дерева")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_day_off_to_a_leaf.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetDayOffToALeaf() {
        // Setup
        counterTestRepository.updateCount(1010, 500);

        // Action
        var affectedCounterIds = counterService.setDayOff(1010, TECHNICAL);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1010L));
    }

    @DisplayName("Ставим DayOff на счётчик, ниже которого по дереву уже стоит DayOff")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_day_off_but_there_is_already_one_below.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetDayOffButThereIsAlreadyOneBelow() {
        // Setup
        counterTestRepository.updateCount(1002, 3000);
        counterTestRepository.updateCount(1009, 1500);
        counterService.setDayOff(1009, TECHNICAL);

        // Action
        var affectedCounterIds = counterService.setDayOff(1002, TECHNICAL);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1002L, 1005L, 1008L));
    }

    @DisplayName("Ставим DayOff на счётчик, выше которого по дереву уже стоит DayOff")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_day_off_but_there_is_already_one_above.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetDayOffButThereIsAlreadyOneAbove() {
        // Setup
        counterTestRepository.updateCount(1001, 4000);
        counterTestRepository.updateCount(1005, 2000);
        counterService.setDayOff(1001, THRESHOLD);

        // Action
        var affectedCounterIds = counterService.setDayOff(1005, TECHNICAL);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1005L, 1008L, 1009L, 1010L, 1011L, 1012L));
    }

    @DisplayName("Ставим DayOff на счётчик, находящийся в дереве между двух DayOffed счётчиков")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_day_off_between_two_closest_day_offed_counters.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetDayOffBetweenTwoClosestDayOffedCounters() {
        // Setup
        counterTestRepository.updateCount(1005, 2000);
        counterTestRepository.updateCount(1009, 1500);
        counterTestRepository.updateCount(1011, 500);
        counterService.setDayOff(1005, THRESHOLD);
        counterService.setDayOff(1011, TECHNICAL);

        // Action
        var affectedCounterIds = counterService.setDayOff(1009, TECHNICAL);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1009L, 1010L, 1012L));
    }

    @DisplayName("Ставим DayOff на все счётчики в дереве")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_day_offs_to_all_counters.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetDayOffsToAllCounters() {
        // Setup
        counterTestRepository.updateCount(1013, 4000);
        counterTestRepository.updateCount(1014, 3000);
        counterTestRepository.updateCount(1015, 500);
        counterTestRepository.updateCount(1018, 500);
        counterTestRepository.updateCount(1019, 250);
        counterTestRepository.updateCount(1020, 1500);
        counterTestRepository.updateCount(1022, 500);
        counterTestRepository.updateCount(1023, 500);

        // Action
        var affectedCounterIds1020 = counterService.setDayOff(1020, TECHNICAL);
        var affectedCounterIds1014 = counterService.setDayOff(1014, THRESHOLD);
        var affectedCounterIds1015 = counterService.setDayOff(1015, TECHNICAL);
        var affectedCounterIds1019 = counterService.setDayOff(1019, TECHNICAL);
        var affectedCounterIds1013 = counterService.setDayOff(1013, TECHNICAL);
        var affectedCounterIds1018 = counterService.setDayOff(1018, THRESHOLD);
        var affectedCounterIds1022 = counterService.setDayOff(1022, TECHNICAL);
        var affectedCounterIds1023 = counterService.setDayOff(1023, TECHNICAL);

        // Assertion
        assertSameIds(affectedCounterIds1020, List.of(1020L));
        assertSameIds(affectedCounterIds1014, List.of(1014L, 1022L, 1023L));
        assertSameIds(affectedCounterIds1015, List.of(1015L));
        assertSameIds(affectedCounterIds1019, List.of(1019L));
        assertSameIds(affectedCounterIds1013, List.of(1013L, 1018L));
        assertSameIds(affectedCounterIds1018, List.of(1018L));
        assertSameIds(affectedCounterIds1022, List.of(1022L));
        assertSameIds(affectedCounterIds1023, List.of(1023L));
    }

    // ----------------------------
    // UnsetDayOff Validation Tests
    // ----------------------------

    @DisplayName("Снимаем DayOff с несуществующего счётчика")
    @Test
    void testUnsetDayOffFromNonExistingCounter() {
        // Action and Assertion
        softly.assertThatThrownBy(() -> counterService.unsetDayOff(420))
            .isInstanceOf(DayOffChangeException.class)
            .hasMessage("Unable to unset a day off. There's no counter with provided id: 420");
    }

    @DisplayName("Снимаем DayOff с фиктивного счётчика")
    @DatabaseSetup({
        "/repository/value_counter/day_off/before/capacity_tree.xml",
        "/repository/value_counter/day_off/before/capacity_tree_with_fictious_node.xml",
    })
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/counters_fictious_unset.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUnsetDayOffFromFictitiousCounter() {
        assertSameIds(
            assertDoesNotThrow(() -> counterService.unsetDayOff(1300)),
            List.of(1300L)
        );
    }

    @DisplayName("Снимаем DayOff со счётчика без DayOff")
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/counters_default.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUnsetDayOffFromNonDayOffedCounter() {
        // Action and Assertion
        softly.assertThatThrownBy(() -> counterService.unsetDayOff(1014))
            .isInstanceOf(DayOffChangeException.class)
            .hasMessage("Unable to unset a day off. Counter 1014 doesn't have a day off to unset");
    }

    @DisplayName("Снимаем DayOff со счётчика с DayOff типа PROPAGATED")
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/unset_day_off_from_propagated_day_offed_counter.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUnsetDayOffFromPropagatedDayOffedCounter() {
        // Setup
        runInTransaction(() -> {
            counterTestRepository.updateCount(1002, 3000);
            counterService.setDayOff(1002, TECHNICAL);
        });

        // Action and Assertion
        softly.assertThatThrownBy(() -> counterService.unsetDayOff(1009))
            .isInstanceOf(DayOffChangeException.class)
            .hasMessage("Unable to unset a propagated day off form counter 1009");
    }

    @DisplayName("Снимаем DayOff со счётчика превысившего порог")
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/unset_day_off_from_overflowed_counter.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUnsetDayOffFromOverflowedCounter() {
        // Setup
        runInTransaction(() -> {
            counterTestRepository.updateCount(1014, 3000);
            counterService.setDayOff(1014, TECHNICAL);
        });

        // Action and Assertion
        softly.assertThatThrownBy(() -> counterService.unsetDayOff(1014))
            .isInstanceOf(DayOffChangeException.class)
            .hasMessage("Unable to unset a day off from counter 1014: count=3000 >= threshold=3000");
    }

    // --------------------------
    // UnsetDayOff Behavior Tests
    // --------------------------

    @DisplayName("Снимаем DayOff со счётчика в листе дерева")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/counters_default.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUnsetDayOffFromALeaf() {
        // Setup
        counterTestRepository.updateCount(1012, 500);
        counterService.setDayOff(1012, TECHNICAL);
        counterTestRepository.updateCount(1012, 499);

        // Action
        var affectedCounterIds = counterService.unsetDayOff(1012);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1012L));
    }

    @DisplayName("Снимаем DayOff со счётчика без родителя")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/counters_default.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUnsetDayOffFromCounterWithoutParent() {
        // Setup
        counterTestRepository.updateCount(1001, 4000);
        counterService.setDayOff(1001, THRESHOLD);
        counterTestRepository.updateCount(1001, 3000);

        // Action
        var affectedCounterIds = counterService.unsetDayOff(1001);

        // Assertion
        assertSameIds(
            affectedCounterIds,
            List.of(1001L, 1002L, 1004L, 1005L, 1006L, 1007L, 1008L, 1009L, 1010L, 1011L, 1012L)
        );
    }

    @DisplayName("Снимаем DayOff со счётчика, родитель которого без DayOff")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/counters_default.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUnsetDayOffFromCounterWithNonDayOffedParent() {
        // Setup
        counterTestRepository.updateCount(1014, 3000);
        counterService.setDayOff(1014, TECHNICAL);
        counterTestRepository.updateCount(1014, 2999);

        // Action
        var affectedCounterIds = counterService.unsetDayOff(1014);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1014L, 1020L, 1022L, 1023L));
    }

    @DisplayName("Снимаем DayOff со счётчика, родитель которого c True DayOff")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/unset_day_off_from_counter_with_true_day_off_parent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUnsetDayOffFromCounterWithTrueDayOffParent() {
        // Setup
        counterTestRepository.updateCount(1005, 2000);
        counterTestRepository.updateCount(1009, 1500);
        counterService.setDayOff(1005, TECHNICAL);
        counterService.setDayOff(1009, TECHNICAL);
        counterTestRepository.updateCount(1009, 1499);

        // Action
        var affectedCounterIds = counterService.unsetDayOff(1009);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1009L, 1010L, 1011L, 1012L));
    }

    @DisplayName("Снимаем DayOff со счётчика, родитель которого c Propagated DayOff")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/unset_day_off_from_counter_with_false_day_off_parent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUnsetDayOffFromCounterWithFalseDayOffParent() {
        // Setup
        counterTestRepository.updateCount(1002, 3000);
        counterTestRepository.updateCount(1009, 1500);
        counterService.setDayOff(1002, TECHNICAL);
        counterService.setDayOff(1009, TECHNICAL);
        counterTestRepository.updateCount(1009, 1499);

        // Action
        var affectedCounterIds = counterService.unsetDayOff(1009);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1009L, 1010L, 1011L, 1012L));
    }

    @DisplayName("Снимаем DayOff со счётчика, ниже которого по дереву уже стоит DayOff")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/unset_day_off_but_there_is_already_one_below.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUnsetDayOffButThereIsAlreadyOneBelow() {
        // Setup
        counterTestRepository.updateCount(1002, 3000);
        counterTestRepository.updateCount(1009, 1500);
        counterService.setDayOff(1002, TECHNICAL);
        counterService.setDayOff(1009, TECHNICAL);
        counterTestRepository.updateCount(1002, 2999);

        // Action
        var affectedCounterIds = counterService.unsetDayOff(1002);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1002L, 1005L, 1008L));
    }

    @DisplayName("Снимаем DayOff со счётчика, находящегося в дереве между двух DayOffed счётчиков")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/unset_day_off_between_two_closest_day_offed_counters.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUnsetDayOffBetweenTwoClosestDayOffedCounters() {
        // Setup
        counterTestRepository.updateCount(1005, 2000);
        counterTestRepository.updateCount(1009, 1500);
        counterTestRepository.updateCount(1011, 500);
        counterService.setDayOff(1011, TECHNICAL);
        counterService.setDayOff(1005, TECHNICAL);
        counterService.setDayOff(1009, TECHNICAL);
        counterTestRepository.updateCount(1009, 1499);

        // Action
        var affectedCounterIds = counterService.unsetDayOff(1009);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1009L, 1010L, 1012L));
    }

    @DisplayName("Снимаем DayOff со всех счётчиков в дереве")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/counters_default.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUnsetDayOffsFromAllCountersWithinTree() {
        // Setup
        counterTestRepository.updateCount(1013, 4000);
        counterTestRepository.updateCount(1014, 3000);
        counterTestRepository.updateCount(1015, 500);
        counterTestRepository.updateCount(1018, 500);
        counterTestRepository.updateCount(1019, 250);
        counterTestRepository.updateCount(1020, 1500);
        counterTestRepository.updateCount(1022, 500);
        counterTestRepository.updateCount(1023, 500);
        counterService.setDayOff(1020, THRESHOLD);
        counterService.setDayOff(1014, TECHNICAL);
        counterService.setDayOff(1015, TECHNICAL);
        counterService.setDayOff(1019, TECHNICAL);
        counterService.setDayOff(1013, THRESHOLD);
        counterService.setDayOff(1018, THRESHOLD);
        counterService.setDayOff(1022, TECHNICAL);
        counterService.setDayOff(1023, TECHNICAL);
        counterTestRepository.updateCount(1013, 3999);
        counterTestRepository.updateCount(1014, 2999);
        counterTestRepository.updateCount(1015, 499);
        counterTestRepository.updateCount(1018, 499);
        counterTestRepository.updateCount(1019, 249);
        counterTestRepository.updateCount(1020, 1499);
        counterTestRepository.updateCount(1022, 499);
        counterTestRepository.updateCount(1023, 499);

        // Action
        var affectedCounterIds1020 = counterService.unsetDayOff(1020);
        var affectedCounterIds1018 = counterService.unsetDayOff(1018);
        var affectedCounterIds1022 = counterService.unsetDayOff(1022);
        var affectedCounterIds1014 = counterService.unsetDayOff(1014);
        var affectedCounterIds1023 = counterService.unsetDayOff(1023);
        var affectedCounterIds1013 = counterService.unsetDayOff(1013);
        var affectedCounterIds1015 = counterService.unsetDayOff(1015);
        var affectedCounterIds1019 = counterService.unsetDayOff(1019);

        // Assertion
        assertSameIds(affectedCounterIds1020, List.of(1020L));
        assertSameIds(affectedCounterIds1018, List.of(1018L));
        assertSameIds(affectedCounterIds1022, List.of(1022L));
        assertSameIds(affectedCounterIds1014, List.of(1014L, 1020L, 1022L));
        assertSameIds(affectedCounterIds1023, List.of(1023L));
        assertSameIds(affectedCounterIds1013, List.of(1013L, 1014L, 1018L, 1020L, 1022L, 1023L));
        assertSameIds(affectedCounterIds1015, List.of(1015L));
        assertSameIds(affectedCounterIds1019, List.of(1019L));
    }

    // ----------------------------------------------
    // SetDayOff And UnsetDayOff Mixed Scenario Tests
    // ----------------------------------------------

    @DisplayName("Сценарный тест снятия и проставления DayOff A")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_unset_day_off_mixed_scenario_a.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetAndUnsetDayOffMixedScenarioA() {
        // Step One: Set 4 DayOffs [1001, 1005, 1006, 1011]
        counterTestRepository.updateCount(1001, 4000);
        counterTestRepository.updateCount(1005, 2000);
        counterTestRepository.updateCount(1006, 500);
        counterTestRepository.updateCount(1011, 500);
        assertSameIds(
            counterService.setDayOff(1001, TECHNICAL),
            List.of(1001L, 1002L, 1004L, 1005L, 1006L, 1007L, 1008L, 1009L, 1010L, 1011L, 1012L)
        );
        assertSameIds(counterService.setDayOff(1006, TECHNICAL), List.of(1006L));
        assertSameIds(counterService.setDayOff(1011, TECHNICAL), List.of(1011L));
        assertSameIds(counterService.setDayOff(1005, TECHNICAL), List.of(1005L, 1008L, 1009L, 1010L, 1012L));

        // Step Two: Unset DayOff 1005
        counterTestRepository.updateCount(1005, 1999);
        assertSameIds(counterService.unsetDayOff(1005), List.of(1005L, 1008L, 1009L, 1010L, 1012L));

        // Step Three: Set DayOff 1009
        counterTestRepository.updateCount(1009, 1500);
        assertSameIds(counterService.setDayOff(1009, TECHNICAL), List.of(1009L, 1010L, 1012L));

        // Step Four: Unset DayOff 1001
        counterTestRepository.updateCount(1001, 3999);
        assertSameIds(counterService.unsetDayOff(1001), List.of(1001L, 1002L, 1004L, 1005L, 1007L, 1008L));
    }

    @DisplayName("Сценарный тест снятия и проставления DayOff B")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_unset_day_off_mixed_scenario_b.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetAndUnsetDayOffMixedScenarioB() {
        // Step One: Set 4 DayOffs [1002, 1010, 1011, 1012]
        counterTestRepository.updateCount(1002, 3000);
        counterTestRepository.updateCount(1010, 500);
        counterTestRepository.updateCount(1011, 500);
        counterTestRepository.updateCount(1012, 500);
        assertSameIds(counterService.setDayOff(1010, TECHNICAL), List.of(1010L));
        assertSameIds(counterService.setDayOff(1011, THRESHOLD), List.of(1011L));
        assertSameIds(counterService.setDayOff(1012, THRESHOLD), List.of(1012L));
        assertSameIds(counterService.setDayOff(1002, TECHNICAL), List.of(1002L, 1005L, 1008L, 1009L));

        // Step Two: Unset DayOff 1012
        counterTestRepository.updateCount(1012, 499);
        assertSameIds(counterService.unsetDayOff(1012), List.of(1012L));

        // Step Three: Set DayOff 1009
        counterTestRepository.updateCount(1009, 1500);
        assertSameIds(counterService.setDayOff(1009, TECHNICAL), List.of(1009L, 1012L));

        // Step Four: Unset DayOffs [1002, 1010]
        counterTestRepository.updateCount(1002, 2999);
        counterTestRepository.updateCount(1010, 499);
        assertSameIds(counterService.unsetDayOff(1010), List.of(1010L));
        assertSameIds(counterService.unsetDayOff(1002), List.of(1002L, 1005L, 1008L));
    }

    @DisplayName("Сценарный тест снятия и проставления DayOff C")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_unset_day_off_mixed_scenario_c.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetAndUnsetDayOffMixedScenarioC() {
        // Step One: Set DayOffs [1013, 1014, 1015, 1019, 1023]
        counterTestRepository.updateCount(1013, 4000);
        counterTestRepository.updateCount(1014, 3000);
        counterTestRepository.updateCount(1015, 500);
        counterTestRepository.updateCount(1019, 250);
        counterTestRepository.updateCount(1023, 500);
        assertSameIds(counterService.setDayOff(1023, THRESHOLD), List.of(1023L));
        assertSameIds(counterService.setDayOff(1015, TECHNICAL), List.of(1015L));
        assertSameIds(counterService.setDayOff(1019, TECHNICAL), List.of(1019L));
        assertSameIds(counterService.setDayOff(1013, TECHNICAL), List.of(1013L, 1014L, 1018L, 1020L, 1022L));
        assertSameIds(counterService.setDayOff(1014, THRESHOLD), List.of(1014L, 1020L, 1022L));

        // Step Two: Unset DayOffs [1015, 1019]
        counterTestRepository.updateCount(1015, 499);
        counterTestRepository.updateCount(1019, 249);
        assertSameIds(counterService.unsetDayOff(1015), List.of(1015L));
        assertSameIds(counterService.unsetDayOff(1019), List.of(1019L));
    }

    @DisplayName("Сценарный тест снятия и проставления DayOff D")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_unset_day_off_mixed_scenario_d.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetAndUnsetDayOffMixedScenarioD() {
        // Step One: Set DayOffs [1022, 1020, 1019, 1014, 1013, 1015]
        counterTestRepository.updateCount(1013, 4000);
        counterTestRepository.updateCount(1014, 3000);
        counterTestRepository.updateCount(1015, 500);
        counterTestRepository.updateCount(1019, 250);
        counterTestRepository.updateCount(1020, 1500);
        counterTestRepository.updateCount(1022, 500);
        assertSameIds(counterService.setDayOff(1022, TECHNICAL), List.of(1022L));
        assertSameIds(counterService.setDayOff(1020, TECHNICAL), List.of(1020L));
        assertSameIds(counterService.setDayOff(1019, TECHNICAL), List.of(1019L));
        assertSameIds(counterService.setDayOff(1014, TECHNICAL), List.of(1014L, 1023L));
        assertSameIds(counterService.setDayOff(1013, TECHNICAL), List.of(1013L, 1015L, 1018L));
        assertSameIds(counterService.setDayOff(1015, TECHNICAL), List.of(1015L));

        // Step Two: Unset DayOffs [1013, 1019, 1022, 1014]
        counterTestRepository.updateCount(1013, 3999);
        counterTestRepository.updateCount(1014, 2999);
        counterTestRepository.updateCount(1019, 249);
        counterTestRepository.updateCount(1022, 499);
        assertSameIds(counterService.unsetDayOff(1013), List.of(1013L, 1018L));
        assertSameIds(counterService.unsetDayOff(1019), List.of(1019L));
        assertSameIds(counterService.unsetDayOff(1022), List.of(1022L));
        assertSameIds(counterService.unsetDayOff(1014), List.of(1014L, 1022L, 1023L));
    }

    // ---------------------------------------------
    // Setting Manual DayOff Specific Behavior Tests
    // ---------------------------------------------

    @DisplayName("Ставим MANUAL DayOff на счётчик в середине дерева")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_manual_day_off_to_the_middle.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetManualDayOffToTheMiddle() {
        // Action
        var affectedCounterIds = counterService.setDayOff(1005, MANUAL);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1005L, 1008L, 1009L, 1010L, 1011L, 1012L));
    }


    @DisplayName("Ставим MANUAL DayOff на счётчик в котором уже стоит TECHNICAL DayOff")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_manual_day_off_to_the_middle.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetManualHavingTechnical() {
        // Setup
        counterTestRepository.updateCount(1005, 2000);
        counterService.setDayOff(1005, TECHNICAL);

        // Action
        var affectedCounterIds = counterService.setDayOff(1005, MANUAL);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1005L));
    }

    @DisplayName("Ставим MANUAL DayOff на счётчик в котором уже стоит MANUAL DayOff")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_manual_day_off_to_the_middle.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetManualHavingManual() {
        // Setup
        counterService.setDayOff(1005, MANUAL);

        // Action
        var affectedCounterIds = counterService.setDayOff(1005, MANUAL);

        // Assertion
        assertSameIds(affectedCounterIds, List.of());
    }

    @DisplayName("Ставим MANUAL DayOff на счётчик выше которого уже стоит TECHNICAL DayOff")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_manual_day_off_below_technical.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetManualBelowTechnical() {
        // Setup
        counterTestRepository.updateCount(1005, 2000);
        counterService.setDayOff(1005, TECHNICAL);

        // Action
        var affectedCounterIds = counterService.setDayOff(1009, MANUAL);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1009L, 1010L, 1011L, 1012L));
    }

    @DisplayName("Ставим MANUAL DayOff на счётчик ниже которого уже стоит TECHNICAL DayOff")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_manual_day_off_above_technical.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testSetManualAboveTechnical() {
        // Setup
        counterTestRepository.updateCount(1009, 1500);
        counterService.setDayOff(1009, TECHNICAL);

        // Action
        var affectedCounterIds = counterService.setDayOff(1002, MANUAL);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1002L, 1005L, 1008L));
    }

    @DisplayName("Ставим TECHNICAL DayOff на счётчик ниже которого уже стоит MANUAL DayOff")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_technical_day_off_above_manual.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void setTechnicalHavingManual() {
        // Setup
        counterService.setDayOff(1009, MANUAL);
        counterTestRepository.updateCount(1002, 3000);

        // Action
        var affectedCounterIds = counterService.setDayOff(1002, TECHNICAL);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1002L, 1005L, 1008L));
    }

    // ---------------------------------------------
    // Unsetting Manual DayOff Specific Behavior Tests
    // ---------------------------------------------

    @DisplayName("Пытаемся снять MANUAL DayOff в TECHNICAL режиме")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/unset_manual_day_off_in_technical_mode.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUnsetManualDayOffInTechnicalMode() {
        // Setup
        counterService.setDayOff(1009, MANUAL);

        // Action
        var affectedCounterIds = counterService.unsetDayOff(1009);

        // Assertion
        assertSameIds(affectedCounterIds, List.of());
    }

    @DisplayName("Пытаемся снять MANUAL DayOff в MANUAL режиме")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/counters_default.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUnsetManualDayOffInManualMode() {
        // Setup
        counterService.setDayOff(1009, MANUAL);

        // Action
        var affectedCounterIds = counterService.unsetManualDayOff(1009);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1009L, 1010L, 1011L, 1012L));
    }

    @DisplayName("Пытаемся снять TECHNICAL DayOff в MANUAL режиме")
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/unset_technical_day_off_in_manual_mode.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUnsetTechnicalDayOffInManualMode() {
        // Setup
        runInTransaction(() -> {
            counterTestRepository.updateCount(1009, 1500);
            counterService.setDayOff(1009, TECHNICAL);
        });

        // Action and assertion
        softly.assertThatThrownBy(() -> counterService.unsetManualDayOff(1009))
            .isInstanceOf(DayOffChangeException.class)
            .hasMessage("Can't use MANUAL mode to unset a NOT MANUAL day off");
    }

    @DisplayName("Пытаемся снять несуществующий DayOff в MANUAL режиме")
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/counters_default.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUnsetDayOffInManualModeButThereIsNoDayOff() {
        // Action and assertion
        softly.assertThatThrownBy(() -> counterService.unsetManualDayOff(1009))
            .isInstanceOf(DayOffChangeException.class)
            .hasMessage("Unable to unset a day off. Counter 1009 doesn't have a day off to unset");
    }

    @DisplayName("Пытаемся снять TECHNICAL DayOff в TECHNICAL режиме имея выше MANUAL DayOff")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/unset_day_off_having_manual_above.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUnsetDayOffHavingManualAbove() {
        // Setup
        counterTestRepository.updateCount(1009, 1500);
        counterService.setDayOff(1009, TECHNICAL);
        counterService.setDayOff(1002, MANUAL);
        counterTestRepository.updateCount(1009, 0);

        // Action
        var affectedCounterIds = counterService.unsetDayOff(1009);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1009L, 1010L, 1011L, 1012L));
    }

    @DisplayName("Пытаемся снять MANUAL DayOff в MANUAL режиме имея выше TECHNICAL DayOff")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/unset_manual_day_off_having_technical_above.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUnsetDayOffHavingTechnicalAbove() {
        // Setup
        counterTestRepository.updateCount(1002, 3000);
        counterService.setDayOff(1002, TECHNICAL);
        counterService.setDayOff(1005, MANUAL);

        // Action
        var affectedCounterIds = counterService.unsetManualDayOff(1005);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1005L, 1008L, 1009L, 1010L, 1011L, 1012L));
    }

    @DisplayName("Пытаемся снять MANUAL DayOff с переполненого счётчика")
    @Transactional
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/unset_manual_from_overflowed_counter.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testUnsetManualDayOffHavingOverflowedCounter() {
        // Setup
        counterTestRepository.updateCount(1009, 1500);
        counterService.setDayOff(1009, TECHNICAL);
        counterService.setDayOff(1009, MANUAL);

        // Action
        var affectedCounterIds = counterService.unsetManualDayOff(1009);

        // Assertion
        assertSameIds(affectedCounterIds, List.of(1009L));
    }

    // -------------------------------------------
    // SetDayOff And UnsetDayOff Concurrency Tests
    // -------------------------------------------

    // If there's no SELECT FOR UPDATE, or any other explicit lock then one of the following
    // situations may appear between two simultaneously started transactions:

    //      Operations Order       | Concurrency Issues
    // ------------------------------------------------
    //  Set Upper -> Set Lower     |         -
    //  Set Lower -> Set Upper     |        YES        The second transaction's locked by the first. (1)
    // ------------------------------------------------
    //  Set Upper -> Unset Lower   |        YES        Order doesn't matter: there's no locked transaction awaiting
    //  Unset Lower -> Set Upper   |        YES        another. Counter sets selected for update are disjoint. (2)
    // ------------------------------------------------
    //  Unset Upper -> Set Lower   |         -
    //  Set Lower -> Unset Upper   |        YES        The second transaction's locked by the first. (3)
    // ------------------------------------------------
    //  Unset Upper -> Unset Lower |        YES        Order doesn't matter: there's no locked transaction awaiting
    //  Unset Lower -> Unset Upper |        YES        another. Counter sets selected for update are disjoint. (4)

    // Attention: tests (2) and (4) may sometime be false positive:
    // there's a possibility they won't show a problem.
    // But if there's a problem then tests (1) and (3) will surely highlight one.

    // Attention: f@©k!ng magic's happening there

    @DisplayName("Одновременно ставим выходные на верхнюю и нижнюю ноды. Нижняя захватывает лок первая")
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_upper_set_lower.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void setUpperSetLowerSimultaneouslyButLowerAcquiresLockFirst() throws InterruptedException {
        // Setup
        runInTransaction(() -> {
            counterTestRepository.updateCount(1002, 3000);
            counterTestRepository.updateCount(1009, 1500);
        });

        Exchanger<Long> pidExchanger = new Exchanger<>();
        CountDownLatch latch = new CountDownLatch(1);

        DayOffRoutine upper = () -> {
            var coroutine = new DayOffCoroutine();
            coroutine.beginTransaction();
            long pid = coroutine.queryProcessId();
            pidExchanger.exchange(pid);
            latch.await();
            coroutine.setDayOff(1002, TECHNICAL);
            coroutine.commitTransaction();
        };

        DayOffRoutine lower = () -> {
            var coroutine = new DayOffCoroutine();
            coroutine.beginTransaction();
            long upperPid = pidExchanger.exchange(null);
            coroutine.setDayOff(1009, TECHNICAL);
            latch.countDown();
            Awaitility.await()
                .timeout(5, TimeUnit.SECONDS)
                .until(() -> coroutine.isProcessAwaitingOrHoldingCounterLock(upperPid));
            coroutine.commitTransaction();
        };

        // Action
        performConcurrentOperations(upper, lower);
    }

    @DisplayName("Одновременно выходной ставим на верхнюю ноду и снимаем с нижней ноды")
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/set_upper_unset_lower.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void setUpperUnsetLowerSimultaneously() throws InterruptedException {
        // Setup
        runInTransaction(() -> {
            counterTestRepository.updateCount(1002, 3000);
            counterTestRepository.updateCount(1009, 1500);
            counterService.setDayOff(1009, TECHNICAL);
            counterTestRepository.updateCount(1009, 1499);
        });

        CyclicBarrier barrier = new CyclicBarrier(2);

        DayOffRoutine upper = () -> {
            var coroutine = new DayOffCoroutine();
            coroutine.beginTransaction();
            barrier.await();
            coroutine.setDayOff(1002, TECHNICAL);
            coroutine.commitTransaction();
        };

        DayOffRoutine lower = () -> {
            var coroutine = new DayOffCoroutine();
            coroutine.beginTransaction();
            barrier.await();
            coroutine.unsetDayOff(1009);
            coroutine.commitTransaction();
        };

        // Action
        performConcurrentOperations(upper, lower);
    }

    @DisplayName("Одновременно выходной снимаем с верхней ноды и ставим на нижнюю ноду. Нижняя захватывает лок первая")
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/unset_upper_set_lower.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void unsetUpperSetLowerSimultaneouslyButLowerAcquiresLockFirst() throws InterruptedException {
        // Setup
        runInTransaction(() -> {
            counterTestRepository.updateCount(1002, 3000);
            counterTestRepository.updateCount(1009, 1500);
            counterService.setDayOff(1002, TECHNICAL);
            counterTestRepository.updateCount(1002, 2999);
        });

        Exchanger<Long> pidExchanger = new Exchanger<>();
        CountDownLatch latch = new CountDownLatch(1);

        DayOffRoutine upper = () -> {
            var coroutine = new DayOffCoroutine();
            coroutine.beginTransaction();
            long pid = coroutine.queryProcessId();
            pidExchanger.exchange(pid);
            latch.await();
            coroutine.unsetDayOff(1002);
            coroutine.commitTransaction();
        };

        DayOffRoutine lower = () -> {
            var coroutine = new DayOffCoroutine();
            coroutine.beginTransaction();
            long upperPid = pidExchanger.exchange(null);
            coroutine.setDayOff(1009, TECHNICAL);
            latch.countDown();
            Awaitility.await()
                .timeout(5, TimeUnit.SECONDS)
                .until(() -> coroutine.isProcessAwaitingOrHoldingCounterLock(upperPid));
            coroutine.commitTransaction();
        };

        // Action
        performConcurrentOperations(upper, lower);
    }

    @DisplayName("Одновременно снимаем выходные с верхней и нижней нод")
    @DatabaseSetup("/repository/value_counter/day_off/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/repository/value_counter/day_off/after/counters_default.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void unsetUpperUnsetLowerSimultaneously() throws InterruptedException {
        // Setup
        runInTransaction(() -> {
            counterTestRepository.updateCount(1002, 3000);
            counterTestRepository.updateCount(1009, 1500);
            counterService.setDayOff(1002, TECHNICAL);
            counterService.setDayOff(1009, TECHNICAL);
            counterTestRepository.updateCount(1002, 2999);
            counterTestRepository.updateCount(1009, 1499);
        });

        CyclicBarrier barrier = new CyclicBarrier(2);

        DayOffRoutine upper = () -> {
            var coroutine = new DayOffCoroutine();
            coroutine.beginTransaction();
            barrier.await();
            coroutine.unsetDayOff(1002);
            coroutine.commitTransaction();
        };

        DayOffRoutine lower = () -> {
            var coroutine = new DayOffCoroutine();
            coroutine.beginTransaction();
            barrier.await();
            coroutine.unsetDayOff(1009);
            coroutine.commitTransaction();
        };

        // Action
        performConcurrentOperations(upper, lower);
    }

    private void performConcurrentOperations(
        DayOffRoutine upperRoutine,
        DayOffRoutine lowerRoutine
    ) throws InterruptedException {
        CountDownLatch completionLatch = new CountDownLatch(2);

        Runnable upperRunnable = () -> {
            try {
                upperRoutine.run();
            } catch (SQLException | InterruptedException | BrokenBarrierException e) {
                System.out.println("Something unexpected and terribly bad happened:" + e.getMessage());
                completionLatch.countDown();
            } finally {
                completionLatch.countDown();
            }
        };

        Runnable lowerRunnable = () -> {
            try {
                lowerRoutine.run();
            } catch (SQLException | InterruptedException | BrokenBarrierException e) {
                System.out.println("Something unexpected and terribly bad happened:" + e.getMessage());
                completionLatch.countDown();
            } finally {
                completionLatch.countDown();
            }
        };

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        // Action
        executorService.submit(upperRunnable);
        executorService.submit(lowerRunnable);

        completionLatch.await();

        executorService.shutdown();
    }

    private interface DayOffRoutine {
        void run() throws SQLException, InterruptedException, BrokenBarrierException;
    }

    private class DayOffCoroutine {
        protected Connection connection;

        public void beginTransaction() throws SQLException {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
        }

        public void setDayOff(long counterId, DayOffType dayOffType) throws SQLException {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM set_day_off(?, ?) AS id");
            statement.setLong(1, counterId);
            statement.setString(2, dayOffType.name());
            statement.execute();
        }

        public void unsetDayOff(long counterId) throws SQLException {
            PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM unset_day_off(?, CAST(? AS DayOffUnsetMode)) AS id"
            );
            statement.setLong(1, counterId);
            statement.setString(2, DayOffUnsetMode.TECHNICAL.name());
            statement.execute();
        }

        public long queryProcessId() throws SQLException {
            Statement statement = connection.createStatement();
            try (ResultSet rs = statement.executeQuery("SELECT pg_backend_pid() AS pid")) {
                rs.next();
                return rs.getLong("pid");
            }
        }

        public boolean isProcessAwaitingOrHoldingCounterLock(long processId) throws SQLException {
            PreparedStatement statement = connection.prepareStatement(
                "       SELECT count(*) = 1 AS result"
                    + " FROM pg_locks AS locks"
                    + " JOIN pg_class AS class ON class.oid = locks.relation"
                    + " WHERE"
                    + "     class.relname = 'capacity_value_counter'"
                    + "     AND class.relkind = 'r'"
                    + "     AND (locks.mode = 'RowShareLock' OR locks.mode = 'RowExclusiveLock')"
                    + "     AND locks.pid = ?"
            );
            statement.setLong(1, processId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getBoolean("result");
            }
        }

        public void commitTransaction() throws SQLException {
            connection.commit();
            connection.close();
        }
    }

    private void runInTransaction(Runnable runnable) {
        new TransactionTemplate(transactionManager).execute(status -> {
            runnable.run();
            return null;
        });
    }

    private void assertSameIds(List<Long> actual, List<Long> expected) {
        softly.assertThat(actual).hasSameElementsAs(expected);
    }

}
