package ru.yandex.common.framework.core;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static javax.servlet.http.HttpServletResponse.SC_ACCEPTED;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.common.framework.core.ServantletManager.getErrorCode;

/**
 * Unit-тесты для {@link ServantletManager}.
 *
 * @author Vladislav Bauer
 */
public class ServantletManagerTest {

    @Test
    public void testGetErrorCodeEmpty() {
        assertThat(getErrorCode(null), equalTo(null));
        assertThat(getErrorCode(emptySet()), equalTo(null));
    }

    @Test
    public void testGetErrorCodeSimple() {
        for (final SystemError error : SystemError.values()) {
            assertThat(getErrorCode(singleton(error)), equalTo(SC_INTERNAL_SERVER_ERROR));
        }

        assertThat(getErrorCode(singleton(new SimpleErrorInfo(null, SC_ACCEPTED))), equalTo(SC_ACCEPTED));
        assertThat(getErrorCode(singleton(new SimpleErrorInfo(null, null))), equalTo(SC_INTERNAL_SERVER_ERROR));
        assertThat(
                getErrorCode(singleton(new SimpleErrorInfo(null, SC_INTERNAL_SERVER_ERROR))),
                equalTo(SC_INTERNAL_SERVER_ERROR)
        );
    }

    @Test
    public void testGetErrorCodeDifferentValues() {
        final List<ErrorInfo> errors = new ArrayList<>();
        errors.add(new SimpleErrorInfo(null, SC_ACCEPTED));
        errors.add(new SimpleErrorInfo(null, SC_BAD_REQUEST));

        assertThat(getErrorCode(errors), equalTo(SC_BAD_REQUEST));
    }

    @Test
    public void testGetErrorCodeSameValues() {
        final List<ErrorInfo> errors = new ArrayList<>();
        errors.add(new SimpleErrorInfo(null, SC_ACCEPTED));
        errors.add(new SimpleErrorInfo(null, SC_ACCEPTED));

        assertThat(getErrorCode(errors), equalTo(SC_ACCEPTED));
    }

}
