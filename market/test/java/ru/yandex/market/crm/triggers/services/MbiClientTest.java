package ru.yandex.market.crm.triggers.services;

import java.io.IOException;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.crm.core.jackson.CustomObjectMapperFactory;
import ru.yandex.market.crm.core.services.external.mbi.MbiCLient;
import ru.yandex.market.crm.core.services.external.mbi.MbiClientImpl;
import ru.yandex.market.crm.core.services.external.mbi.ReturnContacts;
import ru.yandex.market.crm.json.serialization.JsonDeserializerImpl;
import ru.yandex.market.mcrm.http.Http;
import ru.yandex.market.mcrm.http.HttpClient;
import ru.yandex.market.mcrm.http.HttpClientFactory;
import ru.yandex.market.mcrm.http.HttpResponse;
import ru.yandex.market.mcrm.http.HttpStatus;
import ru.yandex.market.mcrm.http.ResponseMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class MbiClientTest {
    @Autowired
    private MbiCLient mbiCLient;
    @Mock
    private HttpClient shopHttpClient;
    @Mock
    private HttpClientFactory clientFactory;

    @BeforeEach
    public void setUp() {
        shopHttpClient = Mockito.mock(HttpClient.class);
        clientFactory = Mockito.mock(HttpClientFactory.class);
        when(clientFactory.create(Mockito.anyString())).thenReturn(shopHttpClient);
        when(shopHttpClient.execute(Mockito.any(Http.class))).thenReturn(new HttpResponse(new ResponseMock(
                HttpStatus.OK.value(), getShopResponse())));
        mbiCLient = new MbiClientImpl(clientFactory, new JsonDeserializerImpl(
                CustomObjectMapperFactory.INSTANCE.getJsonObjectMapper()));
    }

    @Test
    public void testMbiResponseParsing() {
        ReturnContacts contacts = mbiCLient.getReturnContactsForHotlineReturn(10668064L);
        assertEquals("+78005553535", contacts.getPhoneNumber());

    }

    private byte[] getShopResponse() {
        try {
            return IOUtils.toByteArray(Objects.requireNonNull(getClass()
                    .getResourceAsStream("mbiReturnContactsResponse.json")));
        } catch (IOException e) {
            return new byte[0];
        }
    }
}
