package ru.yandex.market.crm.platform.reader.export.startrek.parsers;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(Parameterized.class)
public class CustomerOrderNumberParserTest {
    private final String customerOrderNumbers;
    private final List<Long> expected;

    public CustomerOrderNumberParserTest(String customerOrderNumbers, List<Long> expected) {
        this.customerOrderNumbers = customerOrderNumbers;
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "{index}: Test with text {0}, result: {1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {"", Collections.emptyList()},
            {null, Collections.emptyList()},
            {"12345", Collections.singletonList(12345L)},
            {"1253hsh18, 2525", Arrays.asList(1253L, 18L, 2525L)},
            {"   1636,   1253hsh18 , 2525, 1999999999999999, -555  ", Arrays.asList(1636L, 1253L, 18L, 2525L, 1999999999999999L, 555L)},
            {"№4456884-№4456885", Arrays.asList(4456884L, 4456885L)},
            {"4194723;3984842;4691135", Arrays.asList(4194723L, 3984842L, 4691135L)},
            {"Заказ №4832338", Arrays.asList(4832338L)},
            {"нет", Collections.emptyList()},
            {"4378935\\4393565", Arrays.asList(4378935L, 4393565L)},
            {"4557956-", Arrays.asList(4557956L)},
            {"4378935/4393565", Arrays.asList(4378935L, 4393565L)},
            {"4573984 4573446 4573146", Arrays.asList(4573984L, 4573446L, 4573146L)},
            {"4410781м", Arrays.asList(4410781L)},
            {"4337858 и 4325841", Arrays.asList(4337858L, 4325841L)},
        });
    }

    @Test
    public void extractCustomerOrderNumbersTest() {
        List<Long> actual = CustomerOrderNumberParser.extractOrders(customerOrderNumbers);
        Assert.assertEquals(expected, actual);
    }
}
