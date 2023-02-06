package ru.yandex.market.logistics.management.repository;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.domain.entity.KorobyteRestriction;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

class KorobyteRestrictionRepositoryTest extends AbstractContextualAspectValidationTest {

    @Autowired
    KorobyteRestrictionRepository korobyteRestrictionRepository;

    @Test
    @ExpectedDatabase(
        value = "/data/repository/korobyteRestriction/after_save_new.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @JpaQueriesCount(1)
    void shouldSaveNew() {
        // given:
        KorobyteRestriction newEntity = new KorobyteRestriction()
            .setKey("KEY")
            .setDescription("description")
            .setMinWeightG(0)
            .setMaxWeightG(1)
            .setMinLengthCm(2)
            .setMaxLengthCm(3)
            .setMinWidthCm(4)
            .setMaxWidthCm(5)
            .setMinHeightCm(6)
            .setMaxHeightCm(7)
            .setMinSidesSumCm(8)
            .setMaxSidesSumCm(9);

        // when:
        KorobyteRestriction actual = korobyteRestrictionRepository.save(newEntity);

        // then:
        softly.assertThat(actual.isNew()).isFalse();
    }
}
