package ru.yandex.market.global.checkout.clients;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.market.global.checkout.domain.client.blackbox.BlackboxService;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Disabled
public class BlackBoxLocalTest extends BaseLocalTest {

    //put your user_ticket here
    private final static String xYaUserTicket = "";

    private final BlackboxService blackboxService;

    @Test
    public void testCreateClient() {
        boolean plusEnabledForUser = blackboxService.isPlusEnabledForUser(4092490744L, xYaUserTicket);
        System.out.println(plusEnabledForUser);
    }

}
