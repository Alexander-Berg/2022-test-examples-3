package ru.yandex.market.logistics.iris.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.source.Source;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.jobs.to.CountBySource;
import ru.yandex.market.logistics.iris.jobs.to.PageableBatch;
import ru.yandex.market.logistics.iris.model.ItemIdentifierDTO;
import ru.yandex.market.logistics.iris.model.ItemIdentifierWithSourceDTO;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class JdbcItemReplicaRepositoryTest extends AbstractContextualTest {

    private static final Source FIRST_WAREHOUSE = new Source("1", SourceType.WAREHOUSE);

    @Autowired
    private JdbcItemReplicaRepository jdbcItemRepository;

    @Test
    @DatabaseSetup("classpath:fixtures/setup/jdbc_item/2.xml")
    public void getMaxItemId() {
        long maxId = jdbcItemRepository.getMaxItemId();
        Assert.assertEquals(7, maxId);
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/jdbc_item/5.xml")
    public void shouldGenerateItemIdentifiersWithIdToAllItems() {
        List<ItemIdentifierWithSourceDTO> itemIdentifiersWithId = jdbcItemRepository.generateItemIdentifiersWithIdForAllItems(3, 6);

        Assert.assertEquals(2, itemIdentifiersWithId.size());

        itemIdentifiersWithId.sort(Comparator.comparing(o -> o.getItemIdentifier().getPartnerId()));

        ItemIdentifierWithSourceDTO first = itemIdentifiersWithId.get(0);
        ItemIdentifierWithSourceDTO second = itemIdentifiersWithId.get(1);

        ItemIdentifierWithSourceDTO firstExpected = createItemIdentifierWithId("partner_id_1", "partner_sku_1");
        ItemIdentifierWithSourceDTO secondExpected = createItemIdentifierWithId("partner_id_4", "partner_sku_4");

        Assert.assertEquals(firstExpected, first);
        Assert.assertEquals(secondExpected, second);
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/jdbc_item/23.xml")
    public void shouldGenerateItemIdentifiersWithIdOfItemsWithEmptyContentReferenceIndex() {
        List<ItemIdentifierWithSourceDTO> itemIdentifiersWithId =
                jdbcItemRepository.generateItemIdentifiersWithIdOfItemsWithEmptyContentReferenceIndex(2, 10);

        itemIdentifiersWithId.sort(Comparator.comparing(o -> o.getItemIdentifier().getPartnerId()));

        Assert.assertEquals(2, itemIdentifiersWithId.size());

        ItemIdentifierWithSourceDTO first = itemIdentifiersWithId.get(0);
        ItemIdentifierWithSourceDTO second = itemIdentifiersWithId.get(1);

        ItemIdentifierWithSourceDTO firstExpected = createItemIdentifierWithId("partner_id_1", "partner_sku_1");
        ItemIdentifierWithSourceDTO secondExpected = createItemIdentifierWithId("partner_id_7", "partner_sku_7");

        Assert.assertEquals(firstExpected, first);
        Assert.assertEquals(secondExpected, second);
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/jdbc_item/20.xml")
    public void shouldFindCountBySourcesExceptFilteredSources() {
        ArrayList<Source> sources = new ArrayList<>();
        Source firstWarehous = Source.of("1", SourceType.WAREHOUSE);
        Source secondWarehouse = Source.of("2", SourceType.WAREHOUSE);
        Source thirdWarehouse = Source.of("3", SourceType.WAREHOUSE);
        Source mdm = Source.of("1", SourceType.MDM);
        Source datacampMsku = Source.of("1", SourceType.DATACAMP_MSKU);
        Source datacampSsku = Source.of("1", SourceType.DATACAMP_SSKU);
        sources.add(firstWarehous);
        sources.add(secondWarehouse);
        sources.add(Source.ADMIN_SOURCE);
        sources.add(Source.CONTENT);

        List<CountBySource> countBySources = jdbcItemRepository.findCountBySources(sources);

        assertSoftly(assertions -> {
            assertions.assertThat(countBySources.size()).isEqualTo(sources.size());

            assertions.assertThat(getCountBySource(firstWarehous, countBySources).getIntCount()).isEqualTo(3);
            assertions.assertThat(getCountBySource(secondWarehouse, countBySources).getIntCount()).isEqualTo(2);
            assertions.assertThat(getCountBySource(thirdWarehouse, countBySources)).isNull();
            assertions.assertThat(getCountBySource(Source.ADMIN_SOURCE, countBySources).getIntCount()).isEqualTo(1);
            assertions.assertThat(getCountBySource(Source.CONTENT, countBySources).getIntCount()).isEqualTo(1);
            assertions.assertThat(getCountBySource(mdm, countBySources)).isNull();
            assertions.assertThat(getCountBySource(datacampMsku, countBySources)).isNull();
            assertions.assertThat(getCountBySource(datacampSsku, countBySources)).isNull();
        });
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/jdbc_item/1.xml")
    public void acquire() {
        PageableBatch pageableBatch = new PageableBatch(FIRST_WAREHOUSE, 1L, null);

        List<PageableBatch> countBySources = jdbcItemRepository.calculateAllWithPositiveLifetime(1,
                Collections.singletonList(FIRST_WAREHOUSE));

        assertSoftly(assertions -> {
            assertions.assertThat(countBySources.size()).isEqualTo(1);
            assertions.assertThat(countBySources.get(0)).isEqualTo(pageableBatch);
        });
    }

    private ItemIdentifierWithSourceDTO createItemIdentifierWithId(String partnerId, String partnerSku) {
        return new ItemIdentifierWithSourceDTO(
                new ItemIdentifierDTO(partnerId, partnerSku),
                Source.FAKE_SOURCE
        );
    }

    private CountBySource getCountBySource(Source source, List<CountBySource> countBySources) {
        return countBySources.stream()
                .filter(countBySource -> countBySource.getSource().equals(source))
                .findFirst()
                .orElse(null);
    }
}
