package ru.yandex.market.logistics.iris.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.index.change.ChangeType;
import ru.yandex.market.logistics.iris.entity.item.ItemChangeEntity;
import ru.yandex.market.logistics.iris.entity.item.MetaInformationField;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class ItemChangeRepositoryTest extends AbstractContextualTest {

    @Autowired
    private ItemChangeRepository itemChangeRepository;

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item_change/1.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_change/1.xml", assertionMode = NON_STRICT_UNORDERED)
    public void persist() {
        ItemChangeEntity change = new ItemChangeEntity()
            .setGenerationUUID("abc")
            .setItemId(1L)
            .setChangeType(ChangeType.SET)
            .setField("lifetime")
            .setChangePayload("12")
            .setMetaInformation(ImmutableMap.of(
                MetaInformationField.REQUEST_ID, "123",
                MetaInformationField.DUMMY_FIELD, "321"));

        itemChangeRepository.saveAndFlush(change);
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/item_change/2.xml")
    public void acquire() {
        Optional<ItemChangeEntity> itemChange = itemChangeRepository.findById(1L);

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(itemChange).isPresent();

            ItemChangeEntity entity = itemChange.get();

            assertions.assertThat(entity.getId()).isEqualTo(1L);
            assertions.assertThat(entity.getGenerationUUID()).isEqualTo("abc");
            assertions.assertThat(entity.getItemId()).isEqualTo(1L);
            assertions.assertThat(entity.getField()).isEqualTo("lifetime");
            assertions.assertThat(entity.getChangeType()).isEqualTo(ChangeType.SET);
            assertions.assertThat(entity.getChangePayload()).isEqualTo("12");
            assertions.assertThat(entity.getMetaInformation()).containsAllEntriesOf(
                ImmutableMap.of(
                    MetaInformationField.REQUEST_ID, "123",
                    MetaInformationField.DUMMY_FIELD, "321"
                )
            );

            LocalDateTime created = LocalDate.of(1970, 1, 1).atStartOfDay();
            assertions.assertThat(entity.getCreated()).isEqualTo(created);
        });
    }

}
