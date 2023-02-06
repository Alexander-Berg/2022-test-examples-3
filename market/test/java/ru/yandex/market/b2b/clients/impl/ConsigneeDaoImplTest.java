package ru.yandex.market.b2b.clients.impl;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.b2b.clients.AbstractFunctionalTest;
import ru.yandex.mj.generated.server.model.ConsigneeDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.b2b.clients.ConsigneeDtos.assertEqualsConsigneeDto;
import static ru.yandex.market.b2b.clients.ConsigneeDtos.getNewConsignee;

public class ConsigneeDaoImplTest extends AbstractFunctionalTest {
    @Autowired
    ConsigneeDaoImpl dao;

    @Test
    public void insertAndGet() {
        ConsigneeDto consigneeDto = getNewConsignee();

        BigDecimal id = dao.insert(consigneeDto);
        assertNotEquals(consigneeDto.getConsigneeId(), id);

        consigneeDto.setConsigneeId(id);

        // получаем все. добавленный должен быть последним
        List<ConsigneeDto> consignees = dao.listAllCustomerConsignee(consigneeDto.getCustomerId());
        assertEqualsConsigneeDto(consigneeDto, consignees.get(consignees.size() - 1));

        // получаем по ид
        ConsigneeDto g = dao.getCustomerConsignee(consigneeDto.getCustomerId(), consigneeDto.getConsigneeId());
        assertEquals(g, consigneeDto);
    }

    @Test
    public void update() {
        ConsigneeDto consigneeDto = getNewConsignee();

        BigDecimal id = dao.insert(consigneeDto);
        assertNotEquals(consigneeDto.getConsigneeId(), id);
        consigneeDto.setConsigneeId(id);

        // обновляем
        consigneeDto.setName("новое название");
        dao.update(consigneeDto);

        // получаем назад
        ConsigneeDto g = dao.getCustomerConsignee(consigneeDto.getCustomerId(), consigneeDto.getConsigneeId());
        assertEquals(g, consigneeDto);

        // обновление несуществующего вызывает исключение
        consigneeDto.setConsigneeId(consigneeDto.getConsigneeId().add(BigDecimal.ONE));
        assertThrows(ConsigneeException.class, () -> dao.update(consigneeDto));
    }

    @Test
    public void delete() {
        ConsigneeDto consigneeDto = getNewConsignee();

        BigDecimal id = dao.insert(consigneeDto);
        assertNotEquals(consigneeDto.getConsigneeId(), id);
        consigneeDto.setConsigneeId(id);

        // получаем все. добавленный должен быть последним
        List<ConsigneeDto> consignees = dao.listAllCustomerConsignee(consigneeDto.getCustomerId());
        assertEqualsConsigneeDto(consigneeDto, consignees.get(consignees.size() - 1));
        int conigneesSize = consignees.size();

        // удаляем
        dao.deleteCustomerConsignee(consigneeDto.getCustomerId(), consigneeDto.getConsigneeId());

        consignees = dao.listAllCustomerConsignee(consigneeDto.getCustomerId());
        assertEquals(conigneesSize - 1, consignees.size());

        // удаление удаленного вызывает исключение
        assertThrows(ConsigneeException.class,
                () -> dao.deleteCustomerConsignee(consigneeDto.getCustomerId(), consigneeDto.getConsigneeId()));

    }
}
