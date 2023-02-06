package ru.yandex.market.mbo.core.validators;

import org.junit.Test;
import ru.yandex.market.mbo.core.metadata.legacy.PropertyTemplate;
import ru.yandex.market.mbo.core.metadata.legacy.PropertyType;
import ru.yandex.market.mbo.core.validators.generic.kdepot.managers.KDepotXmlError;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Dmitry Tsyganov dtsyganov@yandex-team.ru
 */
public class PropertyTemplateTest {

    @Test
    public void testGetErrorsNecessary() {
        PropertyTemplate t = new PropertyTemplate("test", "test", PropertyType.BOOLEAN,
            false, false, false, true, false, false,
            Collections.<String>emptyList(), null, null, null, null, null,
            false, null, null);
        Set<KDepotXmlError> errors = t.getErrors(Collections.emptyList());
        assertEquals(1, errors.size());
        assertEquals(KDepotXmlError.NECESSARY_VALUE_NOT_SET, errors.toArray()[0]);
    }
}
