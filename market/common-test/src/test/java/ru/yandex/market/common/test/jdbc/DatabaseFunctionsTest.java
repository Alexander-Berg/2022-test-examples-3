package ru.yandex.market.common.test.jdbc;

import org.junit.Test;
import ru.yandex.market.common.test.jdbc.functions.StringAggregateFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author otedikova
 */
public class DatabaseFunctionsTest {

    @Test
    public void testExtractXMLValue() {
        assertEquals("3",
                DatabaseFunctions.extractXMLValue("<person><age>3</age></person>",
                        "/person/age/text()"));
        assertEquals("3",
                DatabaseFunctions.extractXMLValue("<person><age>3</age></person>",
                        "/person/age"));
        assertEquals("3",
                DatabaseFunctions.extractXMLValue("<person><age>3</age></person>",
                        "/person"));
        assertEquals("2",
                DatabaseFunctions.extractXMLValue("<person id=\"2\"><age>3</age></person>",
                        "/person/@id"));
    }

    @Test
    public void testStraggFunction() {
        StringAggregateFunction stringAggregateFunction = new StringAggregateFunction();
        stringAggregateFunction.add("one;");
        stringAggregateFunction.add("");
        stringAggregateFunction.add(null);
        stringAggregateFunction.add("two");
        assertEquals("one;two", stringAggregateFunction.getResult());

        stringAggregateFunction = new StringAggregateFunction();
        stringAggregateFunction.add(1);
        stringAggregateFunction.add(2);
        assertEquals("12", stringAggregateFunction.getResult());
    }
}
