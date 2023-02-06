package ru.yandex.market.notification.mail.service.composer;

import java.util.Collection;

import org.junit.Test;

import ru.yandex.market.notification.exception.address.InvalidAddressException;
import ru.yandex.market.notification.exception.address.MissedAddressException;
import ru.yandex.market.notification.mail.model.address.ComposedEmailAddress;
import ru.yandex.market.notification.mail.model.address.EmailAddress;
import ru.yandex.market.notification.mail.model.address.EmailAddress.Type;
import ru.yandex.market.notification.model.transport.NotificationAddress;
import ru.yandex.market.notification.service.composer.NotificationAddressComposer;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Тесты для {@link EmailAddressComposer}.
 *
 * @author avetokhin 24/08/16.
 */
public class EmailAddressComposerTest {

    private static final String EMAIL_TO_1 = "to1@yandex.ru";
    private static final String EMAIL_TO_2 = "to2@yandex.ru";

    private static final String EMAIL_CC_1 = "cc1@yandex.ru";
    private static final String EMAIL_CC_2 = "cc2@yandex.ru";

    private static final String EMAIL_BCC_1 = "bcc1@yandex.ru";
    private static final String EMAIL_BCC_2 = "bcc2@yandex.ru";

    private static final String EMAIL_FROM_1 = "from1@yandex.ru";
    private static final String EMAIL_FROM_2 = "from2@yandex.ru";

    private static final String EMAIL_REPLY_TO_1 = "replay_to1@yandex.ru";
    private static final String EMAIL_REPLY_TO_2 = "replay_to2@yandex.ru";

    private static final String INVALID_EMAIL = "test123";

    private static final EmailAddress ADDRESS_TO_1 = EmailAddress.create(EMAIL_TO_1, Type.TO);
    private static final EmailAddress ADDRESS_TO_2 = EmailAddress.create(EMAIL_TO_2, Type.TO);

    private static final EmailAddress ADDRESS_CC_1 = EmailAddress.create(EMAIL_CC_1, Type.CC);
    private static final EmailAddress ADDRESS_CC_2 = EmailAddress.create(EMAIL_CC_2, Type.CC);

    private static final EmailAddress ADDRESS_BCC_1 = EmailAddress.create(EMAIL_BCC_1, Type.BCC);
    private static final EmailAddress ADDRESS_BCC_2 = EmailAddress.create(EMAIL_BCC_2, Type.BCC);

    private static final EmailAddress ADDRESS_REPLY_TO_1 = EmailAddress.create(EMAIL_REPLY_TO_1, Type.REPLY_TO);
    private static final EmailAddress ADDRESS_REPLY_TO_2 = EmailAddress.create(EMAIL_REPLY_TO_2, Type.REPLY_TO);

    private static final EmailAddress ADDRESS_FROM_1 = EmailAddress.create(EMAIL_FROM_1, Type.FROM);
    private static final EmailAddress ADDRESS_FROM_2 = EmailAddress.create(EMAIL_FROM_2, Type.FROM);

    private final NotificationAddressComposer strictComposer = new EmailAddressComposer(true);
    private final NotificationAddressComposer composer = new EmailAddressComposer(false);


    @Test
    public void composeTest() throws Exception {
        final Collection<NotificationAddress> addresses = asList(
            ADDRESS_FROM_1, ADDRESS_FROM_2,
            ADDRESS_TO_1, ADDRESS_TO_2,
            ADDRESS_CC_1, ADDRESS_CC_2,
            ADDRESS_BCC_1, ADDRESS_BCC_2,
            ADDRESS_REPLY_TO_1, ADDRESS_REPLY_TO_2
        );
        final Collection<NotificationAddress> composed = strictComposer.compose(addresses);

        final ComposedEmailAddress address = checkComposedAddressAndCast(composed);

        assertThat(address.getFrom(), containsInAnyOrder(EMAIL_FROM_1, EMAIL_FROM_2));
        assertThat(address.getTo(), containsInAnyOrder(EMAIL_TO_1, EMAIL_TO_2));
        assertThat(address.getCc(), containsInAnyOrder(EMAIL_CC_1, EMAIL_CC_2));
        assertThat(address.getBcc(), containsInAnyOrder(EMAIL_BCC_1, EMAIL_BCC_2));
        assertThat(address.getReplyTo(), containsInAnyOrder(EMAIL_REPLY_TO_1, EMAIL_REPLY_TO_2));
    }

    @Test(expected = MissedAddressException.class)
    public void composeNoFromAddressTest() throws Exception {
        final Collection<NotificationAddress> addresses = asList(
            ADDRESS_TO_1, ADDRESS_TO_2,
            ADDRESS_CC_1, ADDRESS_CC_2,
            ADDRESS_BCC_1, ADDRESS_BCC_2,
            ADDRESS_REPLY_TO_1, ADDRESS_REPLY_TO_2
        );
        strictComposer.compose(addresses);
    }

    @Test
    public void composeNoToAddressTest() throws Exception {
        final Collection<NotificationAddress> addresses = asList(
            ADDRESS_FROM_1, ADDRESS_FROM_2,
            ADDRESS_CC_1, ADDRESS_CC_2,
            ADDRESS_BCC_1, ADDRESS_BCC_2,
            ADDRESS_REPLY_TO_1, ADDRESS_REPLY_TO_2
        );
        final Collection<NotificationAddress> composed = composer.compose(addresses);
        assertThat(composed, empty());
    }

    @Test(expected = InvalidAddressException.class)
    public void composeInvalidEmailStrictTest() throws Exception {
        final Collection<NotificationAddress> addresses = asList(
            ADDRESS_FROM_1, ADDRESS_FROM_2,
            ADDRESS_TO_1, ADDRESS_TO_2,
            ADDRESS_CC_1, ADDRESS_CC_2,
            ADDRESS_BCC_1, ADDRESS_BCC_2,
            ADDRESS_REPLY_TO_1, ADDRESS_REPLY_TO_2,
            EmailAddress.create(INVALID_EMAIL, Type.TO)
        );
        strictComposer.compose(addresses);
    }

    @Test
    public void composeInvalidEmailTest() throws Exception {
        final Collection<NotificationAddress> addresses = asList(
            ADDRESS_FROM_1,
            ADDRESS_TO_1,
            ADDRESS_CC_1,
            ADDRESS_BCC_1,
            ADDRESS_REPLY_TO_1,
            EmailAddress.create(INVALID_EMAIL, Type.FROM),
            EmailAddress.create(INVALID_EMAIL, Type.TO),
            EmailAddress.create(INVALID_EMAIL, Type.CC),
            EmailAddress.create(INVALID_EMAIL, Type.BCC),
            EmailAddress.create(INVALID_EMAIL, Type.REPLY_TO)
        );

        final Collection<NotificationAddress> composed = composer.compose(addresses);
        final ComposedEmailAddress address = checkComposedAddressAndCast(composed);

        assertThat(address.getFrom(), containsInAnyOrder(EMAIL_FROM_1));
        assertThat(address.getTo(), containsInAnyOrder(EMAIL_TO_1));
        assertThat(address.getCc(), containsInAnyOrder(EMAIL_CC_1));
        assertThat(address.getBcc(), containsInAnyOrder(EMAIL_BCC_1));
        assertThat(address.getReplyTo(), containsInAnyOrder(EMAIL_REPLY_TO_1));
    }


    private ComposedEmailAddress checkComposedAddressAndCast(final Collection<NotificationAddress> composed) {
        assertThat(composed, notNullValue());
        assertThat(composed, hasSize(1));

        final NotificationAddress notificationAddress = composed.iterator().next();
        assertThat(notificationAddress, instanceOf(ComposedEmailAddress.class));

        return notificationAddress.cast(ComposedEmailAddress.class);
    }

}
