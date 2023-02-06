package ru.yandex.market.pers.address.services;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.pers.address.model.identity.Identity;
import ru.yandex.market.pers.address.services.exception.DataSyncAddressNotFoundException;
import ru.yandex.market.pers.address.services.model.MarketDataSyncAddress;
import ru.yandex.market.pers.address.services.model.MarketDataSyncAddressList;
import ru.yandex.market.pers.address.tvm.TvmClient;
import ru.yandex.market.pers.address.tvm.TvmTicket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.pers.address.factories.DataSyncAddressFactory.tolstogo;
import static ru.yandex.market.pers.address.util.SamePropertyValuesAsExcept.samePropertyValuesAsExcept;

class MarketDataSyncClientTest {
    private static final String DATASYNC_URL = "http://api-stable.dst.yandex.net:8080";
    private static final String DATASYNC_TABLE = "delivery_addresses";
    private static final Identity<?> UID = Identity.Type.UID.buildIdentity("1000537205013");
    private static final String TVM_TOKEN = "test";

    private MarketDataSyncClient marketDataSyncClient;
    private RestTemplate restTemplateMock;
    private TvmClient tvmClient;

    @BeforeEach
    void init() {
        restTemplateMock = mock(RestTemplate.class);
        tvmClient = mock(TvmClient.class);
        marketDataSyncClient = new MarketDataSyncClientImpl(tvmClient, DATASYNC_URL, DATASYNC_TABLE, restTemplateMock);
    }

    @Test
    void shouldSaveNewAddress() {
        String generatedId = "123213";
        MarketDataSyncAddress address = tolstogo().build();
        when(restTemplateMock.exchange(urlMatcher(UID), eq(HttpMethod.POST), httpEntityMatcher(address), eq(MarketDataSyncAddress.class)))
            .thenReturn(new ResponseEntity<>(tolstogo().setId(generatedId).build(), HttpStatus.OK));
        when(tvmClient.getTicket(eq("data_sync_api"))).thenReturn(new TvmTicket(TVM_TOKEN, 0L));

        String returnedId = marketDataSyncClient.saveNewAddress(UID, address);
        assertEquals(generatedId, returnedId);
        verify(restTemplateMock).exchange(urlMatcher(UID), eq(HttpMethod.POST), httpEntityMatcher(address), eq(MarketDataSyncAddress.class));
        verify(tvmClient).getTicket(eq("data_sync_api"));
    }

    @Test
    void shouldFetchAddresses() {
        MarketDataSyncAddress address = tolstogo().build();
        when(restTemplateMock.exchange(urlMatcher(UID), eq(HttpMethod.GET), emptyBodyMatcher(), eq(MarketDataSyncAddressList.class)))
            .thenReturn(new ResponseEntity<>(new MarketDataSyncAddressList(Collections.singletonList(address)), HttpStatus.OK));

        List<MarketDataSyncAddress> fetchedAddresses = marketDataSyncClient.getAddresses(UID);
        assertThat(fetchedAddresses, hasSize(1));
        assertThat(fetchedAddresses.get(0), samePropertyValuesAsExcept(address, "id"));
    }

    @Test
    void shouldFindAddressById() {
        String addressId = "123456";
        MarketDataSyncAddress address = tolstogo()
            .setId(addressId)
            .build();

        when(restTemplateMock.exchange(urlMatcher(UID, addressId), eq(HttpMethod.GET), emptyBodyMatcher(), eq(MarketDataSyncAddress.class)))
            .thenReturn(new ResponseEntity<>(address, HttpStatus.OK));

        assertThat(marketDataSyncClient.getAddress(UID, addressId), samePropertyValuesAs(address));
    }

    @Test
    void shouldFetchEmptyList() {
        when(restTemplateMock.exchange(urlMatcher(UID), eq(HttpMethod.GET), emptyBodyMatcher(), eq(MarketDataSyncAddressList.class)))
            .thenReturn(new ResponseEntity<>(new MarketDataSyncAddressList(Collections.emptyList()), HttpStatus.OK));
        assertThat(marketDataSyncClient.getAddresses(UID), empty());
    }

    @Test
    void shouldHandleNotFoundException() {
        String requestedAddressId = "12345";
        when(restTemplateMock.exchange(urlMatcher(UID, requestedAddressId), eq(HttpMethod.GET), emptyBodyMatcher(), eq(MarketDataSyncAddress.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        DataSyncAddressNotFoundException notFoundException = assertThrows(
            DataSyncAddressNotFoundException.class, () -> marketDataSyncClient.getAddress(UID, requestedAddressId)
        );
        assertEquals(UID, notFoundException.getUid());
        assertEquals(requestedAddressId, notFoundException.getAddressId());
    }

    @Test
    void shouldUpdateAddress() {
        String addressId = "123456";
        MarketDataSyncAddress address = tolstogo()
            .setId(addressId)
            .build();
        when(tvmClient.getTicket(eq("data_sync_api"))).thenReturn(new TvmTicket(TVM_TOKEN, 0L));

        marketDataSyncClient.updateAddress(UID, address);
        verify(restTemplateMock).exchange(urlMatcher(UID, addressId), eq(HttpMethod.PUT), httpEntityMatcher(address), eq(Void.class));
    }

    private static HttpEntity<?> emptyBodyMatcher() {
        return argThat(
            hasProperty("body", nullValue())
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> HttpEntity<T> httpEntityMatcher(T body) {
        return argThat(allOf(
            hasProperty("body", samePropertyValuesAs(body)),
            hasProperty("headers", hasEntry("X-Ya-Service-Ticket", Collections.singletonList(TVM_TOKEN)))
        ));
    }

    private static String urlMatcher(Identity<?> uid) {
        return argThat(allOf(
            startsWith(DATASYNC_URL),
            containsString(String.valueOf(uid.getStringValue())),
            containsString("/personality/profile/market/delivery_addresses")
        ));
    }

    private static String urlMatcher(Identity<?> uid, String addressId) {
        return argThat(allOf(
            startsWith(DATASYNC_URL),
            containsString(String.valueOf(uid.getStringValue())),
            containsString("/personality/profile/market/delivery_addresses/" + addressId)
        ));
    }

}
