package ru.yandex.direct.juggler.check;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.juggler.JugglerEvent;
import ru.yandex.direct.juggler.JugglerStatus;

@RunWith(Parameterized.class)
public class DirectNumericCheckTest {
    private static final List<String> RAW_NAMES = Arrays.asList("name1", "name2");
    private static final String SUBMESSAGE = "sm";
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

    @Parameterized.Parameter(6)
    public Long value;

    @Parameterized.Parameter(7)
    public JugglerStatus status;

    @Parameterized.Parameters(name = "{0}, {1}, {2}, {3}, {4} -> {6} -> {7}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {SERVICE_NAME, DESCRIPTION, 1L, 10L, true, null, 0L, JugglerStatus.OK},
                {SERVICE_NAME, DESCRIPTION, 1L, 10L, true, null, 6L, JugglerStatus.WARN},
                {SERVICE_NAME, DESCRIPTION, 1L, 10L, true, null, 10L, JugglerStatus.CRIT},
                {SERVICE_NAME, DESCRIPTION, 1L, 10L, true, null, 11L, JugglerStatus.CRIT},
                {SERVICE_NAME, DESCRIPTION, null, 10L, true, null, 0L, JugglerStatus.OK},
                {SERVICE_NAME, DESCRIPTION, null, 10L, true, null, 6L, JugglerStatus.OK},
                {SERVICE_NAME, DESCRIPTION, null, 10L, true, null, 11L, JugglerStatus.CRIT},
                {SERVICE_NAME, DESCRIPTION, 1L, null, true, null, 0L, JugglerStatus.OK},
                {SERVICE_NAME, DESCRIPTION, 1L, null, true, null, 6L, JugglerStatus.WARN},
                {SERVICE_NAME, DESCRIPTION, 1L, null, true, null, 11L, JugglerStatus.WARN},
                {SERVICE_NAME, DESCRIPTION, 100L, 10L, false, null, 101L, JugglerStatus.OK},
                {SERVICE_NAME, DESCRIPTION, 100L, 10L, false, null, 53L, JugglerStatus.WARN},
                {SERVICE_NAME, DESCRIPTION, 100L, 10L, false, null, 10L, JugglerStatus.CRIT},
                {SERVICE_NAME, DESCRIPTION, 100L, 10L, false, null, 9L, JugglerStatus.CRIT},
                {SERVICE_NAME, DESCRIPTION, 100L, null, false, null, 101L, JugglerStatus.OK},
                {SERVICE_NAME, DESCRIPTION, 100L, null, false, null, 53L, JugglerStatus.WARN},
                {SERVICE_NAME, DESCRIPTION, 100L, null, false, null, 9L, JugglerStatus.WARN},
                {SERVICE_NAME, DESCRIPTION, null, 10L, false, null, 101L, JugglerStatus.OK},
                {SERVICE_NAME, DESCRIPTION, null, 10L, false, null, 53L, JugglerStatus.OK},
                {SERVICE_NAME, DESCRIPTION, null, 10L, false, null, 9L, JugglerStatus.CRIT},
                {SERVICE_NAME, DESCRIPTION, null, 10L, false, null, null, JugglerStatus.CRIT},
        });
    }

    private DirectNumericCheck getCheck() {
        return new TestCheck(serviceName, warnBorder, critBorder, description, ascending);
    }

    @Test
    public void testEvent() {
        DirectNumericCheck check = getCheck();
        JugglerEvent event = check.getEvent(value, RAW_NAMES.get(0), SUBMESSAGE);


        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(event.getStatus())
                .isEqualTo(status);
        soft.assertThat(event.getService())
                .isEqualTo(RAW_NAMES.get(0));

        soft.assertAll();
    }
}
