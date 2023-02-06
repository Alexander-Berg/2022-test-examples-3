package ru.yandex.market.logistics.iris.repository;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.entity.item.ItemChangeForYtEntity;

import static org.junit.Assert.assertTrue;

public class ItemChangeForYtRepositoryTest extends AbstractContextualTest {

    @Autowired
    private ItemChangeForYtRepository repository;

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item_change_to_yt/1.xml")
    public void findByIdentifiers() {
        List<ItemChangeForYtEntity> entities = repository.findAll(2);
        assertTrue(entities.size() == 2);
    }
}
