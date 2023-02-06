package ru.yandex.market.logistics.iris.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.core.domain.target.TargetType;
import ru.yandex.market.logistics.iris.entity.EmbeddableItemNaturalKey;
import ru.yandex.market.logistics.iris.entity.EmbeddableSource;
import ru.yandex.market.logistics.iris.entity.EmbeddableTarget;
import ru.yandex.market.logistics.iris.entity.item.EmbeddableItemIdentifier;
import ru.yandex.market.logistics.iris.entity.item.ItemEntity;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class ItemRepositoryTest extends AbstractContextualTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private JdbcItemRepository jdbcItemRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;


    @Test
    @DatabaseSetup("classpath:fixtures/setup/item/1.xml")
    public void acquire() {
        ItemEntity item = itemRepository.findById(1L).get();
        assertItemEntityEquals(item);
    }

    @Test
    @ExpectedDatabase(value = "classpath:fixtures/expected/item/2.xml", assertionMode = NON_STRICT_UNORDERED)
    public void persist() {
        ItemEntity entity = new ItemEntity()
                .setNaturalKey(
                        new EmbeddableItemNaturalKey()
                                .setIdentifier(new EmbeddableItemIdentifier("partner_id", "partner_sku"))
                                .setSource(new EmbeddableSource("1", SourceType.ADMIN))
                                .setTarget(new EmbeddableTarget("147", TargetType.WAREHOUSE))
                ).setReferenceIndex("{}");

        itemRepository.saveAndFlush(entity);
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item/1.xml")
    public void findByIdentifierAndSource() {
        ItemEntity itemEntity = transactionTemplate.execute(
                status -> itemRepository.findByIdentifierAndSource(
                        new EmbeddableItemIdentifier("partner_id", "partner_sku"),
                        new EmbeddableSource("145", SourceType.WAREHOUSE)
                ).get());

        assertItemEntityEquals(itemEntity);
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item/2.xml")
    public void findByNaturalKey() {
        Set<EmbeddableItemNaturalKey> naturalKeys = Sets.newHashSet(
                new EmbeddableItemNaturalKey(
                        new EmbeddableItemIdentifier("partner_id", "partner_sku"),
                        new EmbeddableSource("2", SourceType.ADMIN)
                ),
                new EmbeddableItemNaturalKey(
                        new EmbeddableItemIdentifier("partner_id", "partner_sku"),
                        new EmbeddableSource("1", SourceType.ADMIN),
                        new EmbeddableTarget("145", TargetType.WAREHOUSE)
                ),
                new EmbeddableItemNaturalKey(
                        new EmbeddableItemIdentifier("partner_id", "partner_sku"),
                        new EmbeddableSource("1", SourceType.ADMIN),
                        new EmbeddableTarget("147", TargetType.WAREHOUSE)
                )
        );

        Collection<Long> ids = transactionTemplate.execute(
                status -> jdbcItemRepository.findIdsByNaturalKeys(naturalKeys)
        );

        assertions()
                .assertThat(ids)
                .containsExactlyInAnyOrder(1L, 2L);
    }




    private void assertItemEntityEquals(ItemEntity item) {
        assertSoftly(assertions -> {
            assertions.assertThat(item.getItemIdentifier().getPartnerId()).isEqualTo("partner_id");
            assertions.assertThat(item.getItemIdentifier().getPartnerSku()).isEqualTo("partner_sku");
            assertions.assertThat(item.getReferenceIndex()).isEqualTo("{}");
            assertions.assertThat(item.getSource().getSourceId()).isEqualTo("145");
            assertions.assertThat(item.getSource().getSourceType()).isEqualTo(SourceType.WAREHOUSE);

            assertions.assertThat(item.getId()).isEqualTo(1);
            assertions.assertThat(item.getOptLock()).isEqualTo(0);
            assertions.assertThat(item.getUpdated()).isEqualTo(LocalDate.of(1970, 1, 1).atStartOfDay());
            assertions.assertThat(item.getCreated()).isEqualTo(LocalDate.of(1970, 1, 1).atStartOfDay());
        });
    }
}
