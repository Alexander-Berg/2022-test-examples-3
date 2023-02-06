package ru.yandex.travel.orders.repository;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.orders.entities.DolphinOrderItem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class DolphinOrderItemRepositoryTests {
    @Autowired
    private DolphinOrderItemRepository repository;

    @Test
    public void testGetByCode() {
        DolphinOrderItem item = new DolphinOrderItem();
        repository.save(item);
        assertThat(repository.getByDolphinOrderCode("foo")).isNull();
        item = new DolphinOrderItem();
        item.setId(UUID.randomUUID());
        item.setDolphinOrderCode("bar");
        repository.save(item);
        DolphinOrderItem retrieved = repository.getByDolphinOrderCode("bar");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getDolphinOrderCode()).isEqualTo("bar");
        var duplicate = new DolphinOrderItem();
        duplicate.setId(UUID.randomUUID());
        duplicate.setDolphinOrderCode("bar");
        repository.save(duplicate);
        assertThatThrownBy(() -> repository.getByDolphinOrderCode("bar")).isInstanceOf(IncorrectResultSizeDataAccessException.class);
    }
}
