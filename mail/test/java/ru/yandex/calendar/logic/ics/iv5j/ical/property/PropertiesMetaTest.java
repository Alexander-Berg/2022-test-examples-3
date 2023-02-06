package ru.yandex.calendar.logic.ics.iv5j.ical.property;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.XProperty;
import org.junit.Test;

import ru.yandex.calendar.logic.ics.iv5j.ical.meta.IcssMeta;
import ru.yandex.calendar.logic.ics.iv5j.ical.meta.IcssMetaTestSupport;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class PropertiesMetaTest extends IcssMetaTestSupport<IcsProperty, Property> {

    @Override
    protected String packageSuffix() {
        return "property";
    }

    @Override
    protected Class<Property> dataClass() {
        return Property.class;
    }

    @Override
    protected Class<? extends Property> xDataClass() {
        return XProperty.class;
    }

    @Override
    protected IcssMeta<IcsProperty, ?, Property, ?, ?> meta() {
        return PropertiesMeta.M;
    }


    @Test
    public void test() {
        Assert.assertTrue(PropertiesMeta.M.newTheir("ATTENDEE") instanceof Attendee);
        Assert.assertTrue(PropertiesMeta.M.newTheir("SDFSFD") instanceof XProperty);
    }

    @Test
    public void xWrTimezone() {
        Assert.assertTrue(PropertiesMeta.M.newOur("X-WR-TIMEZONE", "Europe/Moscow") instanceof IcsXWrTimezone);
    }

} //~
