package ru.yandex.calendar.frontend.ywmi;

import lombok.val;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.util.HttpClientConfiguration;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

@Ignore("TODO - make this test non-ignored after implementing tvm")
public class YwmiTest extends AbstractConfTest {

    @Test
    public void filterSearch() {
        val ywmi = new Ywmi("https://meta-test.mail.yandex.net", HttpClientConfiguration.forTest(), registry);
        val uid = PassportUid.cons(4007747592L);
        val mids = Cf.list(163255486492180481L, 163255486492180482L);

        val envelopes = ywmi.filterSearch(uid, mids);

        Assert.equals(2, envelopes.envelopes.size());
        Assert.equals(mids.sorted(), envelopes.envelopes.map(e -> e.mid).sorted());

        Assert.equals(Cf.list("hello@yandex-team.ru", "hello@yandex.ru").sorted(),
                envelopes.envelopes.map(e -> e.from.single().getEmail().getEmail()));

        Assert.assertThrows(() -> ywmi.filterSearch(PassportUid.cons(2), 1L), Ywmi.StrangeResponseException.class);
        Assert.assertThrows(() -> ywmi.filterSearch(uid, 1L), Ywmi.EnvelopeNotFoundException.class);
    }
}
