package ru.yandex.market.checkout.checkouter.order;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Mikhail Usachev <mailto:musachev@yandex-team.ru>
 * Date: 18/05/2017.
 */

public class ItemParameterToPrettyStringTest {

    public static Stream<Arguments> parameterizedTestData() {

        try {
            return ItemParameterTestData.getDataSet().stream().map(Arguments::of);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest(name = "{index}")
    @MethodSource("parameterizedTestData")
    public void test(ItemParameterTestData testDataSet) {
        Assertions.assertEquals(testDataSet.getItemParameter().toPrettyString(), testDataSet.getPrettyString());
    }

}
