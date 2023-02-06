package ru.yandex.market.notification.service.composer;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.notification.common.service.composer.NoChangeAddressComposer;
import ru.yandex.market.notification.model.transport.NotificationAddress;
import ru.yandex.market.notification.service.composer.NotificationAddressComposer;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit-тесты для {@link NoChangeAddressComposer}.
 *
 * @author Vladislav Bauer
 */
public class NoChangeAddressComposerTest {

    @Test
    public void testCompose() {
        checkIt(Collections.emptySet());
        checkIt(Collections.emptyList());
        checkIt(Collections.singleton(mock(NotificationAddress.class)));
        checkIt(Collections.singletonList(mock(NotificationAddress.class)));
    }


    private void checkIt(final Collection<NotificationAddress> addresses) {
        final NotificationAddressComposer composer = new NoChangeAddressComposer();
        final Collection<NotificationAddress> composed = composer.compose(addresses);

        assertThat(composed, equalTo(addresses));
    }

}
