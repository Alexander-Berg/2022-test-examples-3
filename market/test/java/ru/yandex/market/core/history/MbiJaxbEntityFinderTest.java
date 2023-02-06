package ru.yandex.market.core.history;

import java.util.UUID;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link MbiJaxbEntityFinder}.
 *
 * @author Vladislav Bauer
 */
public class MbiJaxbEntityFinderTest {

    @Test
    public void testSpecificName() {
        final String name = UUID.randomUUID().toString();
        final NamedEntityFinder entityFinder = new MbiJaxbEntityFinder(new Object(), name);
        final String entityName = entityFinder.getEntityName();

        assertThat(entityName, equalTo(name));
    }

}
