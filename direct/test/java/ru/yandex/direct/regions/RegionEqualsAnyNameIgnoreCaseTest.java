package ru.yandex.direct.regions;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class RegionEqualsAnyNameIgnoreCaseTest {
    private static final Region REGION = new Region(
            1, 0, "a", "b", "c", "d", false);

    @Parameterized.Parameter
    public String name;
    @Parameterized.Parameter(value = 1)
    public boolean expectedResult;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{"a", true,},
                new Object[]{"A", true,},
                new Object[]{"b", true,},
                new Object[]{"B", true,},
                new Object[]{"c", true,},
                new Object[]{"C", true,},
                new Object[]{"d", true,},
                new Object[]{"D", true,},
                new Object[]{"e", false});
    }

    @Test
    public void test() {
        assertThat(REGION.equalsAnyNameIgnoreCase(name), equalTo(expectedResult));
    }
}
