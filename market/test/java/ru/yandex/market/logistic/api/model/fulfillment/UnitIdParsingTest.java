package ru.yandex.market.logistic.api.model.fulfillment;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class UnitIdParsingTest extends ParsingTest<UnitId> {
    public UnitIdParsingTest() {
        super(UnitId.class, "fixture/entities/unit_id.xml");
    }

    @Test
    public void testThatObjectIsFilledCorrectly() throws Exception {
        UnitId unitId = mapper.readValue(getFileContent(fileName), UnitId.class);

        assertions().assertThat(unitId.getId())
            .as("Asserting unitId id value")
            .isEqualTo("1");

        assertions().assertThat(unitId.getVendorId())
            .as("Asserting unitId vendor id value")
            .isEqualTo(2);

        assertions().assertThat(unitId.getArticle())
            .as("Asserting unitId article value")
            .isEqualTo("AAA");
    }
}
