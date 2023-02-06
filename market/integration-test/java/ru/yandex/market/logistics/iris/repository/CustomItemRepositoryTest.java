package ru.yandex.market.logistics.iris.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.converter.ItemIdentifierConverter;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.entity.EmbeddableSource;
import ru.yandex.market.logistics.iris.entity.item.EmbeddableItemIdentifier;
import ru.yandex.market.logistics.iris.entity.item.ItemEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomItemRepositoryTest extends AbstractContextualTest {

    @Autowired
    private CustomItemRepository customItemRepository;

    @Autowired
    private JdbcItemRepository jdbcItemRepository;

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item/4.xml")
    public void forceUpdateContentTimestamp() {
        Set<ItemIdentifier> itemIdentifiers = Sets.newHashSet(
                ItemIdentifier.of("partner_id", "partner_sku"));

        Collection<ItemEntity> result = customItemRepository
                .findAllWithIdentifiers(ItemIdentifierConverter.toEmbeddableIdentifierSet(itemIdentifiers));

        Set<Long> ids = result.stream()
                .map(ItemEntity::getId)
                .collect(Collectors.toSet());
        assertions().assertThat(ids).containsExactlyInAnyOrder(2L, 3L, 1L);
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item/3.xml")
    public void findByIdentifiersAndSources() {
        List<ItemEntity> items = customItemRepository.findByIdentifiersAndSources(
                Set.of(new EmbeddableItemIdentifier("partner_id", "partner_sku")),
                Set.of(
                        new EmbeddableSource("1", SourceType.CONTENT),
                        new EmbeddableSource("1", SourceType.MDM)
                ));

        Set<Long> ids = items.stream()
                .map(ItemEntity::getId)
                .collect(Collectors.toSet());

        assertions().assertThat(ids).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item/3.xml")
    public void findByIdentifiersAndSource() {
        List<ItemEntity> items = customItemRepository.findByIdentifiersAndSource(
                Set.of(new EmbeddableItemIdentifier("partner_id", "partner_sku")),
                new EmbeddableSource("1", SourceType.CONTENT));
        Set<Long> ids = items.stream()
                .map(ItemEntity::getId)
                .collect(Collectors.toSet());

        assertions().assertThat(ids).containsExactlyInAnyOrder(2L);
    }
}
