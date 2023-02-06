package ru.yandex.market.notification.mail.model.address;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import ru.yandex.market.notification.test.model.AbstractModelTest;
import ru.yandex.market.notification.test.util.DataSerializerUtils;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit-тесты для {@link ComposedEmailAddress}.
 *
 * @author Vladislav Bauer
 */
public class ComposedEmailAddressTest extends AbstractModelTest {

    private static final String FROM = "from@yandex-team.ru";
    private static final String TO = "to@yandex-team.ru";
    private static final String CC = "cc@yandex-team.ru";
    private static final String BCC = "bcc@yandex-team.ru";
    private static final String REPLY_TO = "replyTo@yandex-team.ru";


    @Test(expected = UnsupportedOperationException.class)
    public void testImmutableFrom() {
        checkImmutableField(createEmptyAddress().getFrom());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutableReplyTo() {
        checkImmutableField(createEmptyAddress().getReplyTo());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutableTo() {
        checkImmutableField(createEmptyAddress().getTo());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutableCc() {
        checkImmutableField(createEmptyAddress().getCc());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutableBcc() {
        checkImmutableField(createEmptyAddress().getBcc());
    }

    @Test
    public void testConstruction() {
        final ComposedEmailAddress composedAddress = createAddress();

        checkAddress(composedAddress);
    }

    @Test
    public void testBasicMethods() {
        final ComposedEmailAddress address = createEmptyAddress();
        final ComposedEmailAddress sameAddress = createEmptyAddress();
        final ComposedEmailAddress otherAddress =
            ComposedEmailAddress.create(singleton("vbauer@yandex-team.ru"), set(), set(), set(), set());

        checkBasicMethods(address, sameAddress, otherAddress);
    }

    @Test
    public void testSerialization() {
        final ComposedEmailAddress address = createAddress();
        final String content = DataSerializerUtils.serializeToString(address);

        assertThat(content, containsString(FROM));
        assertThat(content, containsString(TO));
        assertThat(content, containsString(CC));
        assertThat(content, containsString(BCC));
        assertThat(content, containsString(REPLY_TO));
    }

    @Test
    public void testDeserialization() throws Exception {
        final ComposedEmailAddress address = DataSerializerUtils.deserializeFromResource(ComposedEmailAddress.class);

        checkAddress(address);
    }


    private void checkAddress(final ComposedEmailAddress composedAddress) {
        checkAddress(composedAddress.getFrom(), FROM);
        checkAddress(composedAddress.getTo(), TO);
        checkAddress(composedAddress.getCc(), CC);
        checkAddress(composedAddress.getBcc(), BCC);
        checkAddress(composedAddress.getReplyTo(), REPLY_TO);
    }

    private void checkAddress(final Set<String> addresses, final String correct) {
        assertThat(addresses, hasSize(1));
        assertThat(addresses.iterator().next(), equalTo(correct));
    }

    private void checkImmutableField(final Collection<String> addresses) {
        fail(String.valueOf(addresses.add("")));
    }

    private ComposedEmailAddress createAddress() {
        return ComposedEmailAddress.create(
            singleton(FROM),
            singleton(TO),
            singleton(CC),
            singleton(BCC),
            singleton(REPLY_TO)
        );
    }

    private ComposedEmailAddress createEmptyAddress() {
        return ComposedEmailAddress.create(set(), set(), set(), set(), set());
    }

    private Set<String> set() {
        return new HashSet<>();
    }

}
