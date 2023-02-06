package ru.yandex.market.logistics.management.service.combinator;

import java.time.LocalDate;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;

@DatabaseSetup("/data/service/combinator/db/before/service_codes.xml")
public class LogisticServiceCapacityServiceTest extends AbstractContextualTest {

    @Autowired
    private LogisticServiceCapacityService logisticServiceCapacityService;

    /**
     * Дерево капасити
     * <pre>
     * root
     *   |
     *   ---1 location
     *        |
     *        ---from 1 to 2
     *        |
     *        ---21651 location
     *              |
     *              ---from 21651 to 2
     *              |
     *              ---from 21651 to 213  ->  7 partner_capacity
     * </pre>
     */
    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/service_capacity.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/service_capacity_max_leaf_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addDayOffToMaxLeafCapacity() {
        logisticServiceCapacityService.addDisabledDate(7L, LocalDate.of(2020, 7, 3));
    }

    /**
     * Дерево капасити
     * <pre>
     * root
     *   |
     *   ---1 location
     *        |
     *        ---from 1 to 2  ->  3 partner_capacity
     *        |
     *        ---21651 location
     *              |
     *              ---from 21651 to 2
     *              |
     *              ---from 21651 to 213
     * </pre>
     */
    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/service_capacity.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/service_capacity_middle_leaf_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addDayOffToMiddleLeafCapacity() {
        logisticServiceCapacityService.addDisabledDate(3L, LocalDate.of(2020, 7, 3));
    }


    /**
     * Дерево капасити
     * <pre>
     * root
     *   |
     *   ---1 location   ->   1 partner_capacity
     *        |
     *        ---from 1 to 2
     *        |
     *        ---21651 location
     *              |
     *              ---from 21651 to 2
     *              |
     *              ---from 21651 to 213
     * </pre>
     */
    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/service_capacity.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/service_capacity_top_location_root_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addDayOffToTopLocationRootCapacity() {
        logisticServiceCapacityService.addDisabledDate(1L, LocalDate.of(2020, 7, 3));
    }


    /**
     * Дерево капасити
     * <pre>
     * root
     *   |
     *   ---1 location
     *        |
     *        ---from 1 to 2
     *        |
     *        ---21651 location   ->   5 partner_capacity
     *              |
     *              ---from 21651 to 2
     *              |
     *              ---from 21651 to 213
     * </pre>
     */
    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/service_capacity.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/service_capacity_middle_location_root_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addDayOffToMiddleLocationRootCapacity() {
        logisticServiceCapacityService.addDisabledDate(5L, LocalDate.of(2020, 7, 2));
    }

    /**
     * Дерево капасити
     * <pre>
     * root
     *   |
     *   ---1 location   ->    1 partner_capacity
     *        |
     *        ---from 1 to 2   ->   2020-07-03
     *        |
     *        ---21651 location   ->   2020-07-02
     *              |
     *              ---from 21651 to 2    ->   2020-07-02
     *              |
     *              ---from 21651 to 213
     * </pre>
     */
    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/service_capacity_with_different_dayoffs.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/service_capacity_top_location_root_with_dayoffs_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addDayOffToTopLocationRootCapacityWithDayOffs() {
        logisticServiceCapacityService.addDisabledDate(1L, LocalDate.of(2020, 7, 2));
    }

    /**
     * Дерево капасити
     * <pre>
     * root
     *   |
     *   ---1 location
     *        |
     *        ---from 1 to 2   ->   2020-07-02
     *        |
     *        ---21651 location   ->   2020-07-02
     *              |
     *              ---from 21651 to 2    ->  6 partner_capacity, 2020-07-02
     *              |
     *              ---from 21651 to 213
     * </pre>
     */
    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/service_capacity_with_dayoffs.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/service_capacity_remove_max_leaf_dayoffs_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void doNotRemoveDayOffIfAncestorHasOne() {
        logisticServiceCapacityService.removeDisabledDate(6L, LocalDate.of(2020, 7, 2));
    }


    /**
     * Дерево капасити
     * <pre>
     * root
     *   |
     *   ---1 location   ->    1 partner_capacity
     *        |
     *        ---from 1 to 2   ->   2020-07-02
     *        |
     *        ---21651 location   ->   2020-07-02 with day-off
     *              |
     *              ---from 21651 to 2    ->  2020-07-02
     *              |
     *              ---from 21651 to 213
     * </pre>
     */
    @Test
    @DatabaseSetup(
        value = "/data/service/combinator/db/before/service_capacity_with_dayoffs.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/service/combinator/db/after/service_capacity_remove_top_location_with_dayoffs_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void removeDayOffFromTopLocationCapacity() {
        logisticServiceCapacityService.removeDisabledDate(1L, LocalDate.of(2020, 7, 2));
    }
}
