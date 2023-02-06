package ru.yandex.market.logistics.iris.repository;

import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.entity.analytics.CompleteItemEntity;
import ru.yandex.market.logistics.iris.entity.item.EmbeddableItemIdentifier;
import ru.yandex.market.logistics.iris.repository.analytics.CustomCompleteItemRepository;

public class CustomCompleteItemRepositoryTest extends AbstractContextualTest {
    @Autowired
    private CustomCompleteItemRepository customCompleteItemRepository;

    @Test
    @DatabaseSetup("classpath:fixtures/setup/complete_item_publish_service/filled_complete_item.xml")
    public void findByIdentifiers() {
        Set<CompleteItemEntity> items = customCompleteItemRepository.findAllWithIdentifiers(
                Set.of(new EmbeddableItemIdentifier("1", "1"),
                        new EmbeddableItemIdentifier("2", "1")));

        Set<Long> ids = items.stream()
                .map(CompleteItemEntity::getId)
                .collect(Collectors.toSet());

        assertions().assertThat(ids).containsExactlyInAnyOrder(100L, 200L);
    }
}
