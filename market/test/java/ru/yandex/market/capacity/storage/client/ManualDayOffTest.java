package ru.yandex.market.capacity.storage.client;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@DisplayName("Тестирование проставления ручного выходного")
public class ManualDayOffTest extends AbstractClientTest {

    @Test
    @DisplayName("Проставляем выходной успешно")
    public void testSetSuccessfully() {
        mockServer.expect(requestTo(
            getUriBuilder("/admin/day-off/manual")
                .queryParam("capacity-value-id", "1")
                .queryParam("day", "1991-08-18")
                .toUriString()
        ))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.OK));

        softly.assertThatCode(() -> client.setManualDayOff(1L, LocalDate.of(1991, 8, 18)))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Проставляем выходной, но значение капасити не найден")
    public void testSetButSomethingNotFound() {
        mockServer.expect(requestTo(
            getUriBuilder("/admin/day-off/manual")
                .queryParam("capacity-value-id", "1")
                .queryParam("day", "1991-08-18")
                .toUriString()
        ))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body("OMG, I haven't found anything")
            );

        softly.assertThatThrownBy(() -> client.setManualDayOff(1L, LocalDate.of(1991, 8, 18)))
            .isInstanceOf(HttpTemplateException.class)
            .hasMessage("Http request exception: status <404>, response body <OMG, I haven't found anything>.");
    }

    @Test
    @DisplayName("Проставляем выходной, но день в значении капасити и день в параметре отличаются")
    public void testSetButSomethingBadRequest() {
        mockServer.expect(requestTo(
            getUriBuilder("/admin/day-off/manual")
                .queryParam("capacity-value-id", "1")
                .queryParam("day", "1991-08-18")
                .toUriString()
        ))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("Something's definitely wrong")
            );

        softly.assertThatThrownBy(() -> client.setManualDayOff(1L, LocalDate.of(1991, 8, 18)))
            .isInstanceOf(HttpTemplateException.class)
            .hasMessage("Http request exception: status <400>, response body <Something's definitely wrong>.");
    }

    @Test
    @DisplayName("Проставляем выходной, но что-то пошло не так")
    public void testSetButInternalServerError() {
        mockServer.expect(requestTo(
            getUriBuilder("/admin/day-off/manual")
                .queryParam("capacity-value-id", "1")
                .queryParam("day", "1991-08-18")
                .toUriString()
        ))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("I'm an actual description but I'll not appear as a message in an exception")
            );

        softly.assertThatThrownBy(() -> client.setManualDayOff(1L, LocalDate.of(1991, 8, 18)))
            .isInstanceOf(HttpServerErrorException.class)
            .hasMessage("500 Internal Server Error");
    }

    @Test
    @DisplayName("Снимаем выходной успешно")
    public void testUnetSuccessfully() {
        mockServer.expect(requestTo(
            getUriBuilder("/admin/day-off/manual")
                .queryParam("capacity-value-id", "1")
                .queryParam("day", "1991-08-18")
                .toUriString()
        ))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(withStatus(HttpStatus.OK));

        softly.assertThatCode(() -> client.unsetManualDayOff(1L, LocalDate.of(1991, 8, 18)))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Снимаем выходной, но счётчик не найден")
    public void testUnetButSomethingNotFound() {
        mockServer.expect(requestTo(
            getUriBuilder("/admin/day-off/manual")
                .queryParam("capacity-value-id", "1")
                .queryParam("day", "1991-08-18")
                .toUriString()
        ))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(
                withStatus(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("OMG, I haven't found anything")
            );

        softly.assertThatThrownBy(() -> client.unsetManualDayOff(1L, LocalDate.of(1991, 8, 18)))
            .isInstanceOf(HttpTemplateException.class)
            .hasMessage("Http request exception: status <404>, response body <OMG, I haven't found anything>.");
    }

    @Test
    @DisplayName("Снимаем выходной, но по найденному счётчику выходной стоит технический")
    public void testUnetButSomethingBadRequest() {
        mockServer.expect(requestTo(
            getUriBuilder("/admin/day-off/manual")
                .queryParam("capacity-value-id", "1")
                .queryParam("day", "1991-08-18")
                .toUriString()
        ))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(
                withStatus(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("Something's definitely wrong")
            );

        softly.assertThatThrownBy(() -> client.unsetManualDayOff(1L, LocalDate.of(1991, 8, 18)))
            .isInstanceOf(HttpTemplateException.class)
            .hasMessage("Http request exception: status <400>, response body <Something's definitely wrong>.");
    }

    @Test
    @DisplayName("Снимаем выходной, но что-то пошло не так")
    public void testUnetButInternalServerError() {
        mockServer.expect(requestTo(
            getUriBuilder("/admin/day-off/manual")
                .queryParam("capacity-value-id", "1")
                .queryParam("day", "1991-08-18")
                .toUriString()
        ))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(
                withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("I'm an actual description but I'll not appear as a message in an exception")
            );

        softly.assertThatThrownBy(() -> client.unsetManualDayOff(1L, LocalDate.of(1991, 8, 18)))
            .isInstanceOf(HttpServerErrorException.class)
            .hasMessage("500 Internal Server Error");
    }

    @Test
    @DisplayName("Снимаем выходной, но чайник вскипел")
    public void testUnsetButTeapotBoiled() {
        mockServer.expect(requestTo(
            getUriBuilder("/admin/day-off/manual")
                .queryParam("capacity-value-id", "1")
                .queryParam("day", "1991-08-18")
                .toUriString()
        ))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(
                withStatus(HttpStatus.I_AM_A_TEAPOT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("'It is important to draw wisdom from different places.' - Uncle Iroh")
            );

        softly.assertThatThrownBy(() -> client.unsetManualDayOff(1L, LocalDate.of(1991, 8, 18)))
            .isInstanceOf(HttpTemplateException.class)
            .hasMessageContaining("Http request exception: status <418>");
    }

}
