package ru.yandex.travel.train.partners.im;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.testing.misc.TestResources;
import ru.yandex.travel.train.partners.im.model.ReservationCreateRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestImClient {
    protected ImClient client;

    @Mock
    private AsyncHttpClientWrapper ahc;

    @Before
    public void setUp() {
        client = new DefaultImClient(ahc, "pos", Duration.ofSeconds(1), "http://mock.url", "ya", "***");
    }

    @Test
    public void testGatewayError() {
        mockResponseFromInline(502, "");
        assertThatThrownBy(() -> client.reservationCreate(new ReservationCreateRequest(), null)).isInstanceOf(ImClientIOException.class);
    }

    @Test
    public void testImCommunicationError() {
        mockResponseFromInline(500,
                "{ \"Code\": 1, \"Message\": \"Ошибка при взаимодействии с партнером\", \"MessageParams\": []}");
        assertThatThrownBy(() -> client.reservationCreate(new ReservationCreateRequest(), null))
                .isInstanceOf(ImClientIOException.class)
                .hasMessage("Ошибка при взаимодействии с партнером");
    }

    @Test
    public void testImClientRetryableException() {
        mockResponseFromInline(500,
                "{ \"Code\": 2, \"Message\": \"Ошибка при взаимодействии с партнером\", \"MessageParams\": []}");
        assertThatThrownBy(() -> client.reservationCreate(new ReservationCreateRequest(), null))
                .isInstanceOf(ImClientRetryableException.class)
                .hasMessage("Ошибка при взаимодействии с партнером");
    }

    @Test
    public void testImClientValidationException() {
        mockResponseFromInline(500,
                "{ \"Code\": 1385, \"Message\": \"Некорректное значение телефона у пассажира\", \"MessageParams\": " +
                        "[]}");
        assertThatThrownBy(() -> client.reservationCreate(new ReservationCreateRequest(), null))
                .isInstanceOf(ImClientInvalidPassengerPhoneException.class)
                .hasMessage("Некорректное значение телефона у пассажира");
    }

    @Test
    public void testImClientException() {
        mockResponseFromInline(500,
                "{ \"Code\": 3, \"Message\": \"Операция завершилась неуспешно на стороне поставщика услуг\", " +
                        "\"MessageParams\": [\"В ФИО ПАССАЖИРА BCE БУКВЫ Д.Б. ЛАТИНСКИМИ\", 0]}");
        assertThatThrownBy(() -> client.reservationCreate(new ReservationCreateRequest(), null))
                .hasMessage("Операция завершилась неуспешно на стороне поставщика услуг")
                .isInstanceOfSatisfying(ImClientNameRequiredLatinLettersException.class, x -> {
                    assertThat(x.getMessageParams().size()).isEqualTo(2);
                    assertThat(x.getMessageParams().get(0)).isEqualTo("В ФИО ПАССАЖИРА BCE БУКВЫ Д.Б. ЛАТИНСКИМИ");
                    assertThat(x.getMessageParams().get(1)).isEqualTo("0");
                });
    }

    @Test
    public void testImClientBonusCardException() {
        mockResponseFromInline(500,
                "{ \"Code\": 3, \"Message\": \"Операция завершилась неуспешно на стороне поставщика услуг\", " +
                        "\"ProviderError\": \"US331: НЕДОПУСТИМЫЙ НОМЕР БОНУСНОЙ КАРТЫ\", " +
                        "\"MessageParams\": [\"US331: НЕДОПУСТИМЫЙ НОМЕР БОНУСНОЙ КАРТЫ\"]}");
        assertThatThrownBy(() -> client.reservationCreate(new ReservationCreateRequest(), null))
                .hasMessage("Операция завершилась неуспешно на стороне поставщика услуг")
                .isInstanceOfSatisfying(ImClientBonusCardException.class, x -> {
                    assertThat(x.getMessageParams().size()).isEqualTo(1);
                    assertThat(x.getMessageParams().get(0)).isEqualTo("US331: НЕДОПУСТИМЫЙ НОМЕР БОНУСНОЙ КАРТЫ");
                });

        mockResponseFromInline(500,
                "{ \"Code\": 1461, \"Message\": \"Операция завершилась неуспешно на стороне поставщика услуг\", " +
                        "\"ProviderError\": \"US333: УКАЗАН НОМЕР ПАСПОРТА, КОТОРЫЙ НЕ ЗАРЕГИСТРИРОВАН ПО ДАННОЙ KAPTE\"}");
        assertThatThrownBy(() -> client.reservationCreate(new ReservationCreateRequest(), null))
                .hasMessage("Операция завершилась неуспешно на стороне поставщика услуг")
                .isInstanceOf(ImClientBonusCardException.class);

        mockResponseFromInline(500,
                "{ \"Code\": 367, \"Message\": \"Указанные данные пассажира не совпадают с данными в бонусной карте\"}");
        assertThatThrownBy(() -> client.reservationCreate(new ReservationCreateRequest(), null))
                .hasMessage("Указанные данные пассажира не совпадают с данными в бонусной карте")
                .isInstanceOf(ImClientBonusCardException.class);
    }

    private void mockResponseFromResource(int statusCode, String resourceName) {
        var body = TestResources.readResource(resourceName);
        mockResponseFromInline(statusCode, body);
    }

    private void mockResponseFromInline(int statusCode, String body) {
        Response mockedResponse = mock(Response.class);
        when(mockedResponse.getStatusCode()).thenReturn(statusCode);
        when(mockedResponse.getResponseBody()).thenReturn(body);
        CompletableFuture<Response> mockedFuture = CompletableFuture.completedFuture(mockedResponse);
        when(ahc.executeRequest(any(RequestBuilder.class), any())).thenReturn(mockedFuture);
    }

}
