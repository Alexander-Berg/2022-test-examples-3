package ru.yandex.market.pricelabs.integration.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.api.api.PublicUsersApi;
import ru.yandex.market.pricelabs.api.api.PublicUsersApiInterfaces;
import ru.yandex.market.pricelabs.generated.server.pub.model.IsAgencyResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PublicUsersApiTest extends AbstractApiTests {

    @Autowired
    private PublicUsersApi publicApiBean;
    private PublicUsersApiInterfaces publicApi;

    @BeforeEach
    void init() {
        publicApi = buildProxy(PublicUsersApiInterfaces.class, publicApiBean);
        super.init();
    }

    @Test
    void isAgency() {
        var ret = publicApi.usersIsAgencyGet(123L);
        assertEquals(new IsAgencyResponse()
                .uid(123L)
                .userExists(false), checkResponse(ret));
    }
}
