package ru.yandex.market.logistics.iris.repository;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.entity.item.EmbeddableItemIdentifier;
import ru.yandex.market.logistics.iris.entity.item.ItemWithoutContentDataEntity;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class ItemWithoutContentDataRepositoryTest extends AbstractContextualTest {

    @Autowired
    private ItemWithoutContentDataRepository itemWithoutContentDataRepository;

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item_without_content_mapping/one-item.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_without_content_mapping/two-items.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void insertIfNotExists() {
        itemWithoutContentDataRepository.insertKey("partner_sku_1", "partner_id_1");
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item_without_content_mapping/one-item.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_without_content_mapping/one-item.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void insertIfExists() {
        itemWithoutContentDataRepository.insertKey("partner_sku", "partner_id");
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item_without_content_mapping/one-item.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_without_content_mapping/one-item.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteIfNotExists() {
        itemWithoutContentDataRepository.deleteByKey("partner_sku_1", "partner_id_1");
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item_without_content_mapping/one-item.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_without_content_mapping/no-items.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteIfExists() {
        itemWithoutContentDataRepository.deleteByKey("partner_sku", "partner_id");
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item_without_content_mapping/two-items-with-content-data.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_without_content_mapping/one-item-and-content-data.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteItemsHavingContentData() {
        itemWithoutContentDataRepository.deleteItemsHavingContentData();
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item_without_content_mapping/five-items.xml")
    @ExpectedDatabase(value = "classpath:fixtures/setup/item_without_content_mapping/five-items.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void findAllHavingIdGreaterWithLimit() {
        Map<Long, ItemWithoutContentDataEntity> itemsById =
                itemWithoutContentDataRepository.findAllHavingIdGreaterWithLimit(1, 2).stream()
                        .collect(Collectors.toMap(ItemWithoutContentDataEntity::getId, Function.identity()));
        assertions().assertThat(itemsById).hasSize(2);
        assertions().assertThat(itemsById).containsKeys(2L, 3L);
        ItemWithoutContentDataEntity secondItem = itemsById.get(2L);
        assertions().assertThat(secondItem.getIdentifier())
                .isEqualTo(new EmbeddableItemIdentifier("partner_id_2", "partner_sku_2"));
        ItemWithoutContentDataEntity thirdItem = itemsById.get(3L);
        assertions().assertThat(thirdItem.getIdentifier())
                .isEqualTo(new EmbeddableItemIdentifier("partner_id_3", "partner_sku_3"));
    }
}
