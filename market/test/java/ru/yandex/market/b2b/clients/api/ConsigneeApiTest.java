package ru.yandex.market.b2b.clients.api;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.b2b.clients.AbstractFunctionalTest;
import ru.yandex.market.b2b.clients.impl.ConsigneeException;
import ru.yandex.mj.generated.server.model.ConsigneeDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.b2b.clients.ConsigneeDtos.assertEqualsConsigneeDto;
import static ru.yandex.market.b2b.clients.ConsigneeDtos.getNewConsignee;

public class ConsigneeApiTest extends AbstractFunctionalTest {
    @Autowired
    ConsigneesApiService api;

    @Test
    public void consigneeAddTest() {
        ConsigneeDto nc = getNewConsignee();

        ResponseEntity<List<ConsigneeDto>> consigneesResponse = api.getCustomerConsignees(nc.getCustomerId(), null);
        assertEquals(HttpStatus.OK, consigneesResponse.getStatusCode());
        int currentConsigneesCnt = consigneesResponse.getBody().size();

        // добавили
        ResponseEntity<ConsigneeDto> response = api.addCustomerConsignee(nc.getCustomerId(), nc, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotEquals(0, response.getBody().getConsigneeId().intValue());

        // стало на одного больше
        consigneesResponse = api.getCustomerConsignees(nc.getCustomerId(), null);
        assertEquals(HttpStatus.OK, consigneesResponse.getStatusCode());
        assertEquals(currentConsigneesCnt + 1, consigneesResponse.getBody().size());

        nc = response.getBody();
        nc.setName("другое название");

        // обновили
        response = api.addCustomerConsignee(nc.getCustomerId(), nc, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("другое название", response.getBody().getName());
        assertEqualsConsigneeDto(nc, response.getBody());

        // некорректный инн и кпп (длина/не цифры) вызывают исключения
        nc.setInn("123456789012");
        ConsigneeDto finalNc = nc;
        assertThrows(ConsigneeException.class, () -> api.addCustomerConsignee(finalNc.getCustomerId(), finalNc, null));

        nc.setInn("1234567890");
        nc.setKpp("1234567ab");
        ConsigneeDto finalNc1 = nc;
        assertThrows(ConsigneeException.class, () -> api.addCustomerConsignee(finalNc1.getCustomerId(), finalNc1, null));

        // превратим юр лицо в ип
        nc.setInn("123456789012");
        nc.setKpp("");
        response = api.addCustomerConsignee(nc.getCustomerId(), nc, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void changeCustomerTest() {
        ConsigneeDto nc = getNewConsignee();

        ResponseEntity<List<ConsigneeDto>> consigneesResponse = api.getCustomerConsignees(nc.getCustomerId(), null);
        assertEquals(HttpStatus.OK, consigneesResponse.getStatusCode());
        int currentConsigneesCnt = consigneesResponse.getBody().size();

        // добавили
        ResponseEntity<ConsigneeDto> response = api.addCustomerConsignee(nc.getCustomerId(), nc, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotEquals(0, response.getBody().getConsigneeId().intValue());

        nc = response.getBody();

        // стало на одного больше
        consigneesResponse = api.getCustomerConsignees(nc.getCustomerId(), null);
        assertEquals(HttpStatus.OK, consigneesResponse.getStatusCode());
        assertEquals(currentConsigneesCnt + 1, consigneesResponse.getBody().size());

        // перенесли к другому покупателю
        BigDecimal prevCustomerId = nc.getCustomerId();
        nc.setCustomerId(prevCustomerId.add(BigDecimal.ONE));

        response = api.addCustomerConsignee(nc.getCustomerId(), nc, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // у первого стало на одного меньше
        consigneesResponse = api.getCustomerConsignees(prevCustomerId, null);
        assertEquals(HttpStatus.OK, consigneesResponse.getStatusCode());
        assertEquals(currentConsigneesCnt, consigneesResponse.getBody().size());
    }

    @Test
    public void getCustomerConsigneeTest() {
        ConsigneeDto nc = getNewConsignee();

        // добавили
        ResponseEntity<ConsigneeDto> response = api.addCustomerConsignee(nc.getCustomerId(), nc, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotEquals(0, response.getBody().getConsigneeId().intValue());

        nc = response.getBody();

        // получаем грузополучателя по ид
        ResponseEntity<ConsigneeDto> getresponse = api.getCustomerConsignee(nc.getCustomerId(), nc.getConsigneeId(), null);
        assertEquals(HttpStatus.OK, getresponse.getStatusCode());
        assertEqualsConsigneeDto(nc, getresponse.getBody());

        // при некорректном юр лице ошибка
        ConsigneeDto finalNc = nc;
        assertThrows(ConsigneeException.class,
                () -> api.getCustomerConsignee(finalNc.getCustomerId().add(BigDecimal.ONE), finalNc.getConsigneeId(), null));
    }

    @Test
    public void getCustomerConsigneesTest() {
        ConsigneeDto nc = getNewConsignee();

        ResponseEntity<List<ConsigneeDto>> consigneesResponse = api.getCustomerConsignees(nc.getCustomerId(), null);
        assertEquals(HttpStatus.OK, consigneesResponse.getStatusCode());
        int currentConsigneesCnt = consigneesResponse.getBody().size();

        // добавили
        ResponseEntity<ConsigneeDto> response = api.addCustomerConsignee(nc.getCustomerId(), nc, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotEquals(0, response.getBody().getConsigneeId().intValue());
        nc = response.getBody();

        // стало на одного больше
        consigneesResponse = api.getCustomerConsignees(nc.getCustomerId(), null);
        assertEquals(HttpStatus.OK, consigneesResponse.getStatusCode());
        assertEquals(currentConsigneesCnt + 1, consigneesResponse.getBody().size());
        // список возвращается отсортированный по ид, т.е. добавленный должен быть в конце
        assertEqualsConsigneeDto(nc, consigneesResponse.getBody().get(currentConsigneesCnt));
    }

    @Test
    public void deleteCustomerConsigneeTest() {
        ConsigneeDto nc = getNewConsignee();

        ResponseEntity<List<ConsigneeDto>> consigneesResponse = api.getCustomerConsignees(nc.getCustomerId(), null);
        assertEquals(HttpStatus.OK, consigneesResponse.getStatusCode());
        int currentConsigneesCnt = consigneesResponse.getBody().size();

        // добавили
        ResponseEntity<ConsigneeDto> response = api.addCustomerConsignee(nc.getCustomerId(), nc, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        nc = response.getBody();

        // стало на одного больше
        consigneesResponse = api.getCustomerConsignees(nc.getCustomerId(), null);
        assertEquals(HttpStatus.OK, consigneesResponse.getStatusCode());
        assertEquals(currentConsigneesCnt + 1, consigneesResponse.getBody().size());

        // удалили
        ResponseEntity<Void> responseDel = api.deleteCustomerConsignee(nc.getCustomerId(), nc.getConsigneeId(), null);
        assertEquals(HttpStatus.OK, responseDel.getStatusCode());

        // стало как было
        consigneesResponse = api.getCustomerConsignees(nc.getCustomerId(), null);
        assertEquals(HttpStatus.OK, consigneesResponse.getStatusCode());
        assertEquals(currentConsigneesCnt, consigneesResponse.getBody().size());

        // повторное удаление вызывает исключение
        ConsigneeDto finalNc = nc;
        assertThrows(ConsigneeException.class,
                () -> api.deleteCustomerConsignee(finalNc.getCustomerId(), finalNc.getConsigneeId(), null));
    }
}
