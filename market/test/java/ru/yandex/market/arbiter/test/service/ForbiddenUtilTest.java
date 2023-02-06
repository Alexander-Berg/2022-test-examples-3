package ru.yandex.market.arbiter.test.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.arbiter.workflow.ArbiterException;
import ru.yandex.market.arbiter.workflow.ForbiddenUtil;

/**
 * @author moskovkin@yandex-team.ru
 * @since 11.06.2020
 */
public class ForbiddenUtilTest {
    private final TestServiceImpl testServiceImpl = new TestServiceImpl();

    @Test
    public void testIncludeSomeMethod() {
        TestService impl = ForbiddenUtil.createIncludeImpl(TestService.class, testServiceImpl, "inc");
        Assertions.assertThat(impl.inc(1L))
                .isEqualTo(2L);
        Assertions.assertThatThrownBy(() -> impl.dec(1L))
                .isInstanceOf(ArbiterException.class);
    }

    @Test
    public void testExcludeSomeMethod() {
        TestService impl = ForbiddenUtil.createExcludeImpl(TestService.class, testServiceImpl, "inc");
        Assertions.assertThat(impl.dec(1L))
                .isEqualTo(0L);
        Assertions.assertThatThrownBy(() -> impl.inc(1L))
                .isInstanceOf(ArbiterException.class);
    }

    public interface TestService {
        Long inc(Long param);
        Long dec(Long param);
    }

    public static class TestServiceImpl implements TestService {
        @Override
        public Long inc(Long param) {
            return param + 1;
        }

        @Override
        public Long dec(Long param) {
            return param - 1;
        }
    }
}
