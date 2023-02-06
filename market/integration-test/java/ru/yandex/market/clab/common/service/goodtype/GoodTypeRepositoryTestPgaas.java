package ru.yandex.market.clab.common.service.goodtype;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.clab.ControlledClock;
import ru.yandex.market.clab.common.service.ConcurrentModificationException;
import ru.yandex.market.clab.common.test.RandomTestUtils;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.GoodType;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author anmalysh
 */
public class GoodTypeRepositoryTestPgaas extends BasePgaasIntegrationTest {
    @Autowired
    private GoodTypeRepository goodTypeRepository;

    @Autowired
    private ControlledClock clock;

    @Test
    public void simpleInsert() {
        GoodType goodType = createGoodType();

        GoodType saved = goodTypeRepository.saveType(goodType);
        assertThat(goodType.getId()).withFailMessage("should not affect original object").isNull();
        assertThat(goodType.getModifiedDate()).withFailMessage("should not affect original object").isNull();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDisplayName()).isEqualTo(goodType.getDisplayName());
        assertThat(saved.getModifiedDate()).isNotNull();
    }

    @Test
    public void simpleUpdate() {
        GoodType goodType = createGoodType();
        goodType.setDisplayName("Type1");

        GoodType saved = goodTypeRepository.saveType(goodType);
        long id = saved.getId();
        assertThat(saved.getDisplayName()).isEqualTo("Type1");

        saved.setDisplayName("Type2");
        clock.tickMinute();
        GoodType updated = goodTypeRepository.saveType(saved);
        assertThat(updated.getId()).isEqualTo(id);
        assertThat(updated.getModifiedDate()).isAfter(saved.getModifiedDate());

        GoodType fetched = goodTypeRepository.getById(id);
        assertThat(fetched.getDisplayName()).isEqualTo("Type2");
        assertThat(fetched.getModifiedDate()).isEqualTo(updated.getModifiedDate());
    }

    @Test
    public void optimisticLocking() {
        GoodType saved = goodTypeRepository.saveType(createGoodType());

        goodTypeRepository.saveType(saved);

        assertThatThrownBy(() -> {
            saved.setDisplayName("qwerty");
            goodTypeRepository.saveType(saved);
        }).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    public void remove() {
        GoodType saved = goodTypeRepository.saveType(createGoodType());

        assertThat(goodTypeRepository.getTypes()).containsExactly(saved);

        goodTypeRepository.removeType(saved.getId());

        assertThat(goodTypeRepository.getTypes()).isEmpty();
    }

    private GoodType createGoodType() {
        return RandomTestUtils.randomObject(GoodType.class, "id", "modifiedDate");
    }
}
