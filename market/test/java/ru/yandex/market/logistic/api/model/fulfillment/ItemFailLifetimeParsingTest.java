package ru.yandex.market.logistic.api.model.fulfillment;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class ItemFailLifetimeParsingTest extends ParsingTest<Item> {

    public ItemFailLifetimeParsingTest() {
        super(Item.class, "fixture/entities/item_fail_lifetime.xml");
    }

    @Override
    @Test
    public void testSerializationAndDeserialization() throws Exception {
        String expected = getFileContent(fileName);

        Throwable catched = null;

        try {
            Item forward = getMapper().readValue(expected, type);
        } catch (Throwable t) {
            catched = t;
        }

        assertions().assertThat(catched)
            .as("Asserting catched throwable")
            .isNotNull();
        assertions().assertThat(catched)
            .as("Asserting catched throwable type")
            .isInstanceOf(InvalidFormatException.class);
    }

    @Override
    @Test
    public void testExtractedValues() throws Exception {
        Throwable catched = null;

        try {
            Item object = getMapper().readValue(getFileContent(fileName), type);
        } catch (Throwable t) {
            catched = t;
        }

        assertions().assertThat(catched)
            .as("Asserting catched throwable")
            .isNotNull();
        assertions().assertThat(catched)
            .as("Asserting catched throwable type")
            .isInstanceOf(InvalidFormatException.class);
    }
}
