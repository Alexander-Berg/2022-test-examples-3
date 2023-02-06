package ru.yandex.market.logistics.iris.repository;

import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.entity.EmbeddableItemNaturalKey;
import ru.yandex.market.logistics.iris.entity.EmbeddableSource;
import ru.yandex.market.logistics.iris.model.ItemIdWithPartnerIdDTO;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class JdbcItemRepositoryTest extends AbstractContextualTest {

    @Autowired
    private JdbcItemRepository jdbcItemRepository;

    @Autowired
    private JdbcItemReplicaRepository jdbcItemReplicaRepository;

    @Test
    @DatabaseSetup("classpath:fixtures/setup/jdbc_item/10.xml")
    public void shouldFindItemIdentifierReadyToPushToWarehouse() {
        List<ItemIdentifier> result = jdbcItemReplicaRepository.
                findItemIdentifiersToPutToWarehouse(5L, Long.MAX_VALUE, "173", List.of("partner_id_4"));

        assertions().assertThat(result).hasSize(1);

        ItemIdentifier actual = result.get(0);
        ItemIdentifier expected = ItemIdentifier.of("partner_id_4", "partner_sku_4");

        Assert.assertEquals(expected, actual);
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/jdbc_item/21.xml")
    public void shouldFindEmbeddableItemNaturalKeysByItemIdentifiers() {
        ItemIdentifier identifier = ItemIdentifier.of("partner_id_1", "partner_sku_1");
        List<ItemIdentifier> identifiers = ImmutableList.of(identifier);

        List<EmbeddableItemNaturalKey> results = jdbcItemRepository.findEmbeddableItemNaturalKeysByItemIdentifiers(identifiers);

        assertSoftly(assertions -> {
            assertions.assertThat(results).isNotEmpty();

            EmbeddableItemNaturalKey firstKey = getNaturalKeyBySource(new EmbeddableSource("1", SourceType.WAREHOUSE), results);
            assertions.assertThat(firstKey).isNotNull();
            assertions.assertThat(firstKey.getIdentifier().getPartnerId()).isEqualTo("partner_id_1");
            assertions.assertThat(firstKey.getIdentifier().getPartnerSku()).isEqualTo("partner_sku_1");

            EmbeddableItemNaturalKey secondKey = getNaturalKeyBySource(new EmbeddableSource("2", SourceType.WAREHOUSE), results);
            assertions.assertThat(secondKey).isNotNull();
            assertions.assertThat(secondKey.getIdentifier().getPartnerId()).isEqualTo("partner_id_1");
            assertions.assertThat(secondKey.getIdentifier().getPartnerSku()).isEqualTo("partner_sku_1");
        });
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/jdbc_item/28.xml")
    public void findIdsOfItemsReadyToPutToWarehouses() {
        ItemIdentifier identifier = ItemIdentifier.of("partner_id_1", "partner_sku_1");
        ItemIdentifier secondIdentifier = ItemIdentifier.of("partner_id_2", "partner_sku_2");
        List<ItemIdentifier> identifiers = ImmutableList.of(identifier, secondIdentifier);

        List<ItemIdWithPartnerIdDTO> itemIdsWithPartner =
                jdbcItemRepository.findIdsOfItemsReadyToPutToWarehouses(identifiers);

        assertSoftly(assertions -> {
            List<Long> ids =
                    itemIdsWithPartner.stream().map(ItemIdWithPartnerIdDTO::getItemId).collect(Collectors.toList());
            assertions.assertThat(ids).containsExactlyInAnyOrder(1L, 3L);
        });
    }

    private EmbeddableItemNaturalKey getNaturalKeyBySource(EmbeddableSource source, List<EmbeddableItemNaturalKey> identifiers) {
        return identifiers.stream()
                .filter(identifier -> identifier.getSource().equals(source))
                .findFirst()
                .orElse(null);
    }
}
