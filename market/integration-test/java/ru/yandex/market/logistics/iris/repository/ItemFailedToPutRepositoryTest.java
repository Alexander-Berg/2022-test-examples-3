package ru.yandex.market.logistics.iris.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class ItemFailedToPutRepositoryTest extends AbstractContextualTest {

    @Autowired
    private ItemFailedToPutRepository itemFailedToPutRepository;

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item_failed_to_put/one-item.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_failed_to_put/two-items.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void insertIfNotExists() {
        itemFailedToPutRepository.insertKey("partner_sku_1", "partner_id_1");
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item_failed_to_put/one-item.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_failed_to_put/one-item.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void insertIfExists() {
        itemFailedToPutRepository.insertKey("partner_sku", "partner_id");
    }
}
