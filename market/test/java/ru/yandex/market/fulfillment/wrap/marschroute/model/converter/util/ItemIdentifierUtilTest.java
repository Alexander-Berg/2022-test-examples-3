package ru.yandex.market.fulfillment.wrap.marschroute.model.converter.util;

import org.junit.jupiter.api.Test;
import ru.yandex.market.fulfillment.wrap.marschroute.exception.ModelConversionException;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

class ItemIdentifierUtilTest extends BaseIntegrationTest {

    @Test
    void testTransformationToItemIdentifier() throws Exception {
        UnitId unitId = new UnitId(null, 10L, "AAAA");
        String identifier = ItemIdentifierUtil.toItemIdentifier(unitId);

        softly.assertThat(identifier)
                .as("Asserting identifier value was created correctly")
                .isEqualTo("AAAA.10");
    }

    @Test
    void testTransformationToUnitId() throws Exception {
        String identifier = "AAAA.10";

        UnitId unitId = ItemIdentifierUtil.toUnitId(identifier);

        softly.assertThat(unitId.getId())
                .as("Asserting that unit id yandex id is null")
                .isNull();

        softly.assertThat(unitId.getVendorId())
                .as("Asserting that vendor id is filled correctly")
                .isEqualTo(10L);

        softly.assertThat(unitId.getArticle())
                .as("Asserting that article is filled correctly")
                .isEqualTo("AAAA");
    }

    @Test
    void testTransformationWhenVendorIdIsNull() {
        UnitId unitId = new UnitId(null, null, "AAAA");
        softly.assertThatThrownBy(() -> ItemIdentifierUtil.toItemIdentifier(unitId))
                .isInstanceOf(ModelConversionException.class);
    }

    @Test
    void testTransformationWhenArticleIsNull() {
        UnitId unitId = new UnitId(null, 10L, null);
        softly.assertThatThrownBy(() -> ItemIdentifierUtil.toItemIdentifier(unitId))
                .isInstanceOf(ModelConversionException.class);
    }

    @Test
    void testTransformationWhenItemIdentifierIsNull() {
        softly.assertThatThrownBy(() -> ItemIdentifierUtil.toItemIdentifier(null))
                .isInstanceOf(ModelConversionException.class);
    }
}
