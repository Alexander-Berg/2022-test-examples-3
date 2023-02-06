package ru.yandex.canvas.service.sandbox;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static ru.yandex.canvas.service.sandbox.ClientTagsQueryBuilder.and;
import static ru.yandex.canvas.service.sandbox.ClientTagsQueryBuilder.not;
import static ru.yandex.canvas.service.sandbox.ClientTagsQueryBuilder.or;

@RunWith(Parameterized.class)
public class ClientTagsQueryBuilderTest {

    @Parameterized.Parameter
    public String expected;

    @Parameterized.Parameter(1)
    public ClientTagsQuery query;

    @Parameterized.Parameters(name = "query {0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {"GENERIC & INTEL_E5_2650 & LINUX & ~IPV4", and(ClientTag.GENERIC, ClientTag.INTEL_E5_2650, ClientTag.LINUX, not(ClientTag.IPV4))},
                {"GENERIC & INTEL_E5_2650 & LINUX", and(ClientTag.GENERIC, ClientTag.INTEL_E5_2650, ClientTag.LINUX)},
                {"(GENERIC | LINUX) & INTEL_E5_2650", and(or(ClientTag.GENERIC, ClientTag.LINUX), ClientTag.INTEL_E5_2650)},
        };
        return Arrays.asList(data);
    }

    @Test
    public void test() {
        assertEquals("matches expected", expected, query.build());
    }
}
