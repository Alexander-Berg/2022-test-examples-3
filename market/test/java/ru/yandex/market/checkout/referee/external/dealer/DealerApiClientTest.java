package ru.yandex.market.checkout.referee.external.dealer;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.checkout.entity.Note;
import ru.yandex.market.checkout.referee.EmptyTest;
import ru.yandex.market.checkout.referee.entity.CheckoutRefereeHelper;
import ru.yandex.market.request.trace.RequestContextHolder;

/**
 * @author kukabara
 */
@ContextConfiguration("classpath:context/external-services.xml")
@Disabled
public class DealerApiClientTest extends EmptyTest {
    @Autowired
    private DealerApiClient dealerApiClient;

    @Test
    public void saveRefereeNotification() {
        RequestContextHolder.createNewContext();
        Note note = CheckoutRefereeHelper.getNote();
        dealerApiClient.saveRefereeNotification(note);
    }
}
