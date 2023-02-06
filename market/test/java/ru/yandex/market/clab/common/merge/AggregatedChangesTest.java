package ru.yandex.market.clab.common.merge;

import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 15.04.2019
 */
public class AggregatedChangesTest {
    private AggrigatedChangesVoidMeta<String> changes;

    @Before
    public void setUp() {
        changes = new AggrigatedChangesVoidMeta<>();
    }

    @Test
    public void doubleAdd() {
        changes.registerAdded("one");
        changes.registerAdded("one");

        assertThat(changes)
            .hasSize(2)
            .extracting(Change::getAfter)
            .containsExactly("one", "one");
    }

    @Test
    public void addRemove() {
        changes.registerAdded("one");
        changes.registerRemoved("one");

        assertThat(changes).isEmpty();
    }

    @Test
    public void removeAdd() {
        changes.registerRemoved("two");
        changes.registerAdded("two");

        assertThat(changes).isEmpty();
    }

    @Test
    public void addAddRemove() {
        changes.registerAdded("one");
        changes.registerAdded("one");
        changes.registerRemoved("one");

        assertThat(changes).containsExactly(ChangeMeta.addedNoMeta("one"));
    }

    @Test
    public void addChange() {
        changes.registerAdded("one");
        changes.registerChanged("one", "four");

        assertThat(changes).containsExactly(ChangeMeta.addedNoMeta("four"));
    }

    @Test
    public void changeAddChanged() {
        changes.registerChanged("two", "four");
        changes.registerAdded("two");

        assertThat(changes).containsExactly(ChangeMeta.addedNoMeta("four"));
    }

    @Test
    public void changeAddCopy() {
        changes.registerChanged("two", "four");
        changes.registerAdded("four");

        assertThat(changes)
            .containsExactly(ChangeMeta.updatedNoMeta("two", "four"), ChangeMeta.addedNoMeta("four"));
    }

    @Test
    public void addChangeRemove() {
        changes.registerAdded("one");
        changes.registerChanged("one", "two");
        changes.registerRemoved("two");

        assertThat(changes).isEmpty();
    }

    @Test
    public void doubleChange() {
        changes.registerChanged("one", "two");
        changes.registerChanged("two", "one");

        assertThat(changes).isEmpty();
    }

    @Test
    public void changeRemove() {
        changes.registerChanged("one", "two");
        changes.registerRemoved("two");

        assertThat(changes).containsExactly(ChangeMeta.removedNoMeta("one"));
    }

    @Test
    public void multipleValues() {
        changes.registerChanged("one", "two");
        changes.registerAdded("blue");
        changes.registerChanged("two", "four");
        changes.registerAdded("cow");
        changes.registerRemoved("blue");
        changes.registerChanged("cow", "horse");
        changes.registerRemoved("four");

        assertThat(changes).containsExactly(
            ChangeMeta.addedNoMeta("horse"),
            ChangeMeta.removedNoMeta("one")
        );
    }

    private static class AggrigatedChangesVoidMeta<T> extends AggregatedChanges<T, Void> {
        public void registerAdded(T value) {
            registerAdded(value, null);
        }

        public void registerRemoved(T value) {
            registerRemoved(value, null);
        }

        public void registerChanged(@Nonnull T before, @Nonnull T after) {
            registerChanged(before, after, null);
        }
    }
}
