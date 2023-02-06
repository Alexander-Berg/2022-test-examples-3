package ru.yandex.market.api.computervision;

import org.junit.Test;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.computervision.ComputerVisionEntityIdParser;
import ru.yandex.market.api.internal.computervision.EntityId;
import ru.yandex.market.api.util.ResourceHelpers;

import static ru.yandex.market.api.computervision.ComputerVisionTestHelper.assertEmpty;
import static ru.yandex.market.api.computervision.ComputerVisionTestHelper.assertModel;
import static ru.yandex.market.api.computervision.ComputerVisionTestHelper.assertOffer;

/**
 * @author dimkarp93
 */
public class ComputerVisionEntityIdParserTest extends UnitTestBase {
    private static final ComputerVisionEntityIdParser PARSER
        = new ComputerVisionEntityIdParser();

    @Test
    public void entityIsModel() {
        assertModel(10L, parse("entity-model-10.json"));
    }

    @Test
    public void entityIsCluster() {
        assertModel(10L, parse("entity-cluster-10.json"));
    }

    @Test
    public void entityModelPreferredThanCluster() {
        assertModel(10L, parse("entity-model-10-cluster-1.json"));
    }

    @Test
    public void entityModelPreferredThanOffer() {
        assertModel(10L, parse("entity-model-10-offer-1.json"));
    }

    @Test
    public void ignoreEntityModelIfNotValid() {
        assertOffer("1", parse("entity-model-ab-offer-1.json"));
    }

    @Test
    public void emptyEntityIfNotValid() {
        assertEmpty(parse("not-valid-entity.json"));
    }

    private EntityId parse(String filename) {
        return PARSER.parse(ResourceHelpers.getResource(filename));
    }

}
