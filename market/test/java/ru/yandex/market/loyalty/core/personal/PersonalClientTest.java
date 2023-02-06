package ru.yandex.market.loyalty.core.personal;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.loyalty.core.service.personal.DataType;
import ru.yandex.market.loyalty.core.service.personal.PersonalClient;
import ru.yandex.market.loyalty.core.service.personal.PersonalClientImpl;
import ru.yandex.market.loyalty.core.service.personal.model.BulkStoreRequestItem;
import ru.yandex.market.loyalty.core.service.personal.model.BulkStoreResponseItemSuccess;
import ru.yandex.market.loyalty.core.service.personal.model.PersonalBulkStoreRequest;
import ru.yandex.market.loyalty.core.service.personal.model.PersonalBulkStoreResponse;
import ru.yandex.market.loyalty.core.service.personal.model.PersonalFindRequest;
import ru.yandex.market.loyalty.core.service.personal.model.PersonalFindResponse;
import ru.yandex.market.loyalty.core.service.personal.model.PersonalRetrieveRequest;
import ru.yandex.market.loyalty.core.service.personal.model.PersonalRetrieveResponse;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestFor(PersonalClient.class)
public class PersonalClientTest extends MarketLoyaltyCoreMockedDbTestBase {

    private final Tvm2 tvm2 = mock(Tvm2.class);
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final PersonalClient personalClient = new PersonalClientImpl(restTemplate,
            () -> tvm2.getServiceTicket(2034438).toOptional(),"http://personal.tst.yandex.net");

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void init() {
        when(tvm2.getServiceTicket(2034438)).thenReturn(Option.of("service ticket"));
    }

    @Test
    public void getPersonalPhone() {
        when(restTemplate.exchange(any(RequestEntity.class), any(Class.class))).thenReturn(
                ResponseEntity.ok(PersonalRetrieveResponse.builder().id("data").value("value").build()));

        PersonalRetrieveResponse response = personalClient.retrieve(DataType.PHONES,
                PersonalRetrieveRequest.builder().id("f715104a05f64750bc73f8087eee3de7").build());

        assertThat(response.getId(), equalTo("data"));
        assertThat(response.getValue(), equalTo("value"));
    }

    @Test
    public void findPersonalPhoneId() {
        final String phone = "+79000000000";
        final String personalPhoneId = "personal_phone_id";
        when(restTemplate.exchange(any(RequestEntity.class), any(Class.class))).thenReturn(
                ResponseEntity.ok(new PersonalFindResponse(personalPhoneId, phone)));

        PersonalFindResponse response = personalClient.find(DataType.PHONES,
                PersonalFindRequest.builder().value(phone).build());
        assertThat(response.getId(), equalTo(personalPhoneId));
        assertThat(response.getValue(), equalTo(phone));
    }

    @Test
    public void bulkStorePhones() {
        var expected = new PersonalBulkStoreResponse(List.of(
                new BulkStoreResponseItemSuccess("id1", "+79001112233"),
                new BulkStoreResponseItemSuccess("id2", "+79001112244")
        ));
        when(restTemplate.exchange(any(RequestEntity.class), any(Class.class))).thenReturn(ResponseEntity.ok(expected));

        PersonalBulkStoreResponse response = personalClient.bulkStore(DataType.PHONES,
                PersonalBulkStoreRequest.builder().items(
                        List.of(new BulkStoreRequestItem("+79001112233"), new BulkStoreRequestItem("+79001112244"))
                ).build());

        assertThat(response, equalTo(expected));
    }

    @Test
    public void shouldSerializeDeserialize() throws JsonProcessingException {
        var request = new PersonalBulkStoreRequest(List.of(
                new BulkStoreRequestItem("v1"),
                new BulkStoreRequestItem("v2")),
                false);
        objectMapper.writeValueAsString(request);
        var response = new PersonalBulkStoreResponse(List.of(
                new BulkStoreResponseItemSuccess("id1", "phone1")
        ));
        var respJson = objectMapper.writeValueAsString(response);
        objectMapper.readValue(respJson, PersonalBulkStoreResponse.class);
    }
}
