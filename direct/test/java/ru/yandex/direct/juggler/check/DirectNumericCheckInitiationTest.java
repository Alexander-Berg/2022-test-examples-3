package ru.yandex.direct.juggler.check;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(Parameterized.class)
public class DirectNumericCheckInitiationTest {
    private static final List<String> RAW_NAMES = Arrays.asList("name1", "name2");
    private static final String SERVICE_NAME = "service.name";
    private static final String DESCRIPTION = "description";
    private static final String TEST_HOST = "check.ya.ru";

    private static class TestCheck extends DirectNumericCheck {
        public TestCheck(String serviceName, @Nullable Long warnBorder,
                         @Nullable Long critBorder, String description, boolean ascending) {
            super(serviceName, warnBorder, critBorder, TEST_HOST, description, ascending);
        }

        @Override
        public List<String> getRawServiceNames() {
            return RAW_NAMES;
        }
    }

    @Parameterized.Parameter
    public String serviceName;

    @Parameterized.Parameter(1)
    public String description;

    @Parameterized.Parameter(2)
    public Long warnBorder;

    @Parameterized.Parameter(3)
    public Long critBorder;

    @Parameterized.Parameter(4)
    public boolean ascending;

    @Parameterized.Parameter(5)
    public Class<?> eClass;

    @Parameterized.Parameters(name = "{0}, {1}, {2}, {3}, {4} -> {5}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {SERVICE_NAME, DESCRIPTION, 1L, 10L, true, null},
                {SERVICE_NAME, DESCRIPTION, 1L, null, true, null},
                {SERVICE_NAME, DESCRIPTION, null, 10L, true, null},
                {SERVICE_NAME, DESCRIPTION, 1L, -10L, false, null},
                {SERVICE_NAME, DESCRIPTION, 1L, null, false, null},
                {SERVICE_NAME, DESCRIPTION, null, 10L, false, null},
                {null, DESCRIPTION, 1L, 10L, true, NullPointerException.class},
                {SERVICE_NAME, null, 1L, 10L, true, NullPointerException.class},
                {SERVICE_NAME, DESCRIPTION, null, null, true, IllegalArgumentException.class},
                {SERVICE_NAME, DESCRIPTION, null, null, false, IllegalArgumentException.class},
                {SERVICE_NAME, DESCRIPTION, 1L, 10L, false, IllegalArgumentException.class},
                {SERVICE_NAME, DESCRIPTION, 100L, 10L, true, IllegalArgumentException.class}
        });
    }

    private DirectNumericCheck getCheck() {
        return new TestCheck(serviceName, warnBorder, critBorder, description, ascending);
    }

    @Test
    public void testInitiation() {
        if (eClass == null) {
            DirectNumericCheck check = getCheck();

            SoftAssertions soft = new SoftAssertions();

            soft.assertThat(check.getServiceName())
                    .isEqualTo(serviceName);
            soft.assertThat(check.getRawServiceNames())
                    .isEqualTo(RAW_NAMES);

            soft.assertAll();
        } else {
            assertThatThrownBy(this::getCheck)
                    .isInstanceOf(eClass);
        }
    }
}
