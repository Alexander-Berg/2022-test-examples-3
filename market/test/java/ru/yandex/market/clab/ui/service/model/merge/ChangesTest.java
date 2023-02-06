package ru.yandex.market.clab.ui.service.model.merge;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.clab.common.mbo.EqualityWrapper;
import ru.yandex.market.clab.common.merge.Change;
import ru.yandex.market.clab.common.merge.Changes;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 11.02.2019
 */
public class ChangesTest {

    private Changes<String> changes;

    @Before
    public void before() {
        changes = new Changes<>();
    }

    @Test
    public void registerAbsent() {
        changes.registerAbsentAsRemoved(
            Arrays.asList("one", "two", "three", "four"),
            Arrays.asList("THRee", "tWo"),
            ChangesTest::ignoreCase);

        assertThat(changes.asList())
            .filteredOn(Change::isRemove)
            .extracting(Change::getBefore)
            .containsExactly("one", "four");

        assertThat(changes.asList()).hasSize(2);
    }

    @Test
    public void registerDifference() {
        changes.registerDifference(
            Arrays.asList("one", "two", "three", "four"),
            Arrays.asList("seven", "THRee", "tWo", "ten"),
            ChangesTest::ignoreCase);

        assertThat(changes.asList())
            .filteredOn(Change::isRemove)
            .extracting(Change::getBefore)
            .containsExactly("one", "four");

        assertThat(changes.asList())
            .filteredOn(Change::isAdd)
            .extracting(Change::getAfter)
            .containsExactly("seven", "ten");
    }

    private static EqualityWrapper<String> ignoreCase(String value) {
        return new EqualityWrapper<>(value, s -> s.toLowerCase().hashCode(), String::equalsIgnoreCase);
    }
}
