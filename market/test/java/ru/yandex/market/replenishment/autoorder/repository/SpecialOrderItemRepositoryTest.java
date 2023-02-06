package ru.yandex.market.replenishment.autoorder.repository;

import java.time.LocalDate;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.SpecialOrderItem;
import ru.yandex.market.replenishment.autoorder.repository.postgres.SpecialOrderItemRepository;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
public class SpecialOrderItemRepositoryTest extends FunctionalTest {
    @Autowired
    private SpecialOrderItemRepository repository;

    private SpecialOrderItem getSpecialOrderItem() {
        SpecialOrderItem item = new SpecialOrderItem();
        item.setId(1L);
        item.setWeekStartDate(LocalDate.parse("2020-04-20"));
        item.setQuantity(69);
        item.setSpecialOrderId(1L);
        return item;
    }

    @Test
    @DbUnitDataSet(
        before = "SpecialOrderItemRepository.before-add.csv",
        after = "SpecialOrderItemRepository.after-add.csv"
    )
    public void saveSpecialOrderItem() {
        repository.saveSpecialOrderItem(getSpecialOrderItem());
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderItemRepository.before-select.csv")
    public void getSpecialOrderItemById() {
        SpecialOrderItem item = repository.getSpecialOrderItemById(1L);
        assertThat(item.getId(), is(1L));
        assertThat(item.getWeekStartDate(), is(LocalDate.parse("2020-04-20")));
        assertThat(item.getQuantity(), is(69));
    }
}
