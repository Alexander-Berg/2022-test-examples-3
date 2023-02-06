package ru.yandex.market.global.checkout.clients;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.ApplicationDefaults;
import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.market.global.checkout.domain.client.trust.TrustPlusClient;
import ru.yandex.market.global.checkout.domain.client.trust.model.TrustCreateAccountRequest;
import ru.yandex.market.global.checkout.domain.client.trust.model.TrustTopUpRequest;

@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TrustPlusLocalTest extends BaseLocalTest {

    public static final Long UID = 4092490744L;
    public static final String USER_IP = null;
    public static final String PLUS_ACCOUNT = "yandex_account-w/47ed793a-0e04-55dc-bad6-1bd49eec71bf";

    private final TrustPlusClient trustPopupClient;

    @Test
    public void addPlusPointsToAccount() {
        String plusTopUpRequest = trustPopupClient.createPlusTopUpRequest(new TrustTopUpRequest()
                .setAmount(100_00L)
                .setCurrency(ApplicationDefaults.CURRENCY)
                .setUid(String.valueOf(UID))
                .setPlusPaymethod(PLUS_ACCOUNT)
                .setUserIp(USER_IP));
        System.out.println(plusTopUpRequest);
        trustPopupClient.startTopUpRequest(plusTopUpRequest);
    }


    @Test
    public void getStatusPayment() {
        System.out.println(trustPopupClient.checkTopUpStatus("e71f87cf9562062e20ffbb081131f89f"));
    }

    @Test
    public void createAccount() {
        TrustCreateAccountRequest trustCreateAccountRequest =
                new TrustCreateAccountRequest().setCurrency(ApplicationDefaults.CURRENCY)
                        .setUid(String.valueOf(UID))
                        .setUserIp(USER_IP);
        System.out.println(trustPopupClient.createAccount(trustCreateAccountRequest));
    }

    @Test
    public void getAccounts() {
        System.out.println(trustPopupClient.getAccounts(String.valueOf(UID), USER_IP));
    }

}
