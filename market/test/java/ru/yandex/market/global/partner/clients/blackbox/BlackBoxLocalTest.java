package ru.yandex.market.global.partner.clients.blackbox;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.global.partner.BaseLocalTest;
import ru.yandex.mj.generated.client.blackbox.api.BlackboxApiClient;
import ru.yandex.mj.generated.client.blackbox.model.BlackboxResponse;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Disabled
public class BlackBoxLocalTest extends BaseLocalTest {

    //put your user_ticket here
    private final static String xYaUserTicket = null;

    private final BlackboxApiClient blackboxClient;

    @Test
    public void testCreateClient() {

        ExecuteCall<BlackboxResponse, RetryStrategy> userTicketCall = blackboxClient.blackboxGet("user_ticket",
                null,
                "4083468771",
                xYaUserTicket,
                "27,28",
                "json",
                "bound",
                "102",
                null);
        BlackboxResponse result = userTicketCall.schedule().join();

        Assertions.assertThat(result.getUsers()).isNotNull().hasSize(1);
        System.out.println(result.getUsers().get(0));
    }

}
