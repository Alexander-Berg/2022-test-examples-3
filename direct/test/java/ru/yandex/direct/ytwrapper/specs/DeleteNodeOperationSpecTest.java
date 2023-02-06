package ru.yandex.direct.ytwrapper.specs;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.YPath;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DeleteNodeOperationSpecTest {
    private static final String TEST_PATH = "//tmp/test";

    private OperationSpec spec;
    private Yt yt;

    @Before
    public void before() {
        spec = new DeleteNodeOperationSpec(YPath.simple(TEST_PATH));
        yt = mock(Yt.class, RETURNS_DEEP_STUBS);
    }

    @Test
    public void testSpec() {
        spec.run(yt);
        verify(yt.cypress()).remove(eq(Optional.empty()), eq(false), eq(YPath.simple(TEST_PATH)));
    }

    @Test
    public void testSpecTransaction() {
        spec.run(yt, GUID.valueOf("1234-1234-1234-1234"));
        verify(yt.cypress())
                .remove(eq(Optional.of(GUID.valueOf("1234-1234-1234-1234"))), eq(true), eq(YPath.simple(TEST_PATH)));
    }
}
