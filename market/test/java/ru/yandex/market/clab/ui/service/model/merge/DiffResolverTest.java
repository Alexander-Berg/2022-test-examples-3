package ru.yandex.market.clab.ui.service.model.merge;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 08.02.2019
 */
public class DiffResolverTest {

    private MockDiffResolver resolver;

    @Before
    public void before() {
        resolver = spy(MockDiffResolver.class);
    }

    @Test
    public void noChanges() {
        resolver.apply("BASE", "base", "bAsE");

        resolver.unchanged("BASE", "base", "bAsE");
    }

    @Test
    public void localAdd() {
        resolver.apply(null, null, "new");

        verify(resolver).localAdd("new");
    }

    @Test
    public void localRemove() {
        resolver.apply("ORIGINAL", "original", null);

        verify(resolver).localRemove("ORIGINAL", "original");
    }


    @Test
    public void localEdit() {
        resolver.apply("ORIGINAL", "original", "edited");

        verify(resolver).localEdit("original", "edited");
    }


    @Test
    public void remoteAdd() {
        resolver.apply("added", null, null);

        verify(resolver).remoteAdd("added");
    }


    @Test
    public void remoteRemove() {
        resolver.apply(null, "base", "BASE");

        verify(resolver).remoteRemove("base", "BASE");
    }


    @Test
    public void remoteEdit() {
        resolver.apply("edited", "base", "BASE");

        verify(resolver).remoteEdit("edited", "base");
    }

    @Test
    public void wrongCall() {
        assertThatThrownBy(() -> resolver.apply(null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void conflictAddAdd() {
        resolver.apply("storage", null, "local");

        verify(resolver).conflict("storage", null, "local", ConflictType.ADD_ADD);
    }

    @Test
    public void conflictChangeChange() {
        resolver.apply("storage", "base", "local");

        verify(resolver).conflict("storage", "base", "local", ConflictType.CHANGE_CHANGE);
    }

    @Test
    public void conflictRemoveChange() {
        resolver.apply(null, "base", "local");

        verify(resolver).conflict(null, "base", "local", ConflictType.REMOVE_CHANGE);
    }

    @Test
    public void conflictChangeRemove() {
        resolver.apply("storage", "base", null);

        verify(resolver).conflict("storage", "base", null, ConflictType.CHANGE_REMOVE);
    }


    @Test
    public void conflictRemoveRemove() {
        resolver.apply(null, "base", null);

        verify(resolver).conflict(null, "base", null, ConflictType.REMOVE_REMOVE);
    }

    private static class MockDiffResolver extends DiffResolver<String, Void> {
        MockDiffResolver() {
            super(String::equalsIgnoreCase);
        }

        @Override
        protected void localRemove(String storage, String base) {
        }

        @Override
        protected void remoteRemove(String base, String edited) {
        }

        @Override
        protected void localEdit(String base, String edited) {
        }

        @Override
        protected void localAdd(String edited) {
        }

        @Override
        protected void remoteAdd(String storage) {
        }

        @Override
        protected void remoteEdit(String storage, String base) {
        }

        @Override
        protected void unchanged(String storage, String base, String edited) {
        }

        @Override
        protected void conflict(String storage, String base, String edited, ConflictType type) {
        }
    }

}
