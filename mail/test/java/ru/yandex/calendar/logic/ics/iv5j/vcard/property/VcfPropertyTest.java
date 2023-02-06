package ru.yandex.calendar.logic.ics.iv5j.vcard.property;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.logic.ics.iv5j.vcard.parameter.VcfParameter;
import ru.yandex.calendar.logic.ics.iv5j.vcard.parameter.VcfTypeParameter;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class VcfPropertyTest {

    @Test
    public void serialize() {
        VcfEmail p = new VcfEmail("conf_rr_7_8@yandex-team.ru", Cf.<VcfParameter>list(new VcfTypeParameter("internet")));
        Assert.A.equals("EMAIL;TYPE=internet:conf_rr_7_8@yandex-team.ru", p.serialize());
    }

} //~
