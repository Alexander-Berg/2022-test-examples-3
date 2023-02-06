package ru.yandex.market.clab.common.merge;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 25.04.2019
 */
public class AggregatedChangesBreakTest {
    private AggregatedChanges<String, Integer> changes;
    private static final int USER_PABLO = 44;
    private static final int USER_DIZZY = 99;

    @Before
    public void setUp() {
        changes = new AggregatedChanges<>(this::sameUser);
    }


    @Test
    public void addButRemove() {
        changes.registerAdded("green", USER_PABLO);
        changes.registerRemoved("green", USER_DIZZY);

        assertThat(changes)
            .containsExactly(
                ChangeMeta.added("green", USER_PABLO),
                ChangeMeta.removed("green", USER_DIZZY)
            );
    }

    @Test
    public void addButAddRemove() {
        changes.registerAdded("green", USER_PABLO);
        changes.registerAdded("green", USER_DIZZY);
        changes.registerRemoved("green", USER_DIZZY);

        assertThat(changes)
            .containsExactly(
                ChangeMeta.added("green", USER_PABLO)
            );
    }

    @Test
    public void addAddButRemove() {
        changes.registerAdded("green", USER_PABLO);
        changes.registerAdded("green", USER_PABLO);
        changes.registerRemoved("green", USER_DIZZY);

        assertThat(changes)
            .containsExactly(
                ChangeMeta.added("green", USER_PABLO),
                ChangeMeta.added("green", USER_PABLO),
                ChangeMeta.removed("green", USER_DIZZY)
            );
    }

    @Test
    public void addRemoveButAdd() {
        changes.registerAdded("green", USER_PABLO);
        changes.registerRemoved("green", USER_PABLO);
        changes.registerAdded("green", USER_DIZZY);

        assertThat(changes)
            .containsExactly(
                ChangeMeta.added("green", USER_DIZZY)
            );
    }

    @Test
    public void addChangeButRemove() {
        changes.registerAdded("green", USER_PABLO);
        changes.registerChanged("green", "blue", USER_PABLO);
        changes.registerRemoved("blue", USER_DIZZY);

        assertThat(changes)
            .containsExactly(
                ChangeMeta.added("blue", USER_PABLO),
                ChangeMeta.removed("blue", USER_DIZZY)
            );
    }

    private boolean sameUser(int earlierUser, int laterUser) {
        return earlierUser == laterUser;
    }
}
