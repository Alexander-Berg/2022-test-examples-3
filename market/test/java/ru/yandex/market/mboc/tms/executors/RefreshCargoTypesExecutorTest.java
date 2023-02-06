package ru.yandex.market.mboc.tms.executors;

import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.CargoTypeSnapshot;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.repository.CargoTypeSnapshotRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

/**
 * @author straight@yandex-team.ru
 * @created 05.03.2022
 * copying executor test from Deepmind as part of the separation from MBOC
 */
@SuppressWarnings("checkstyle:magicnumber")
public class RefreshCargoTypesExecutorTest extends BaseDbTestClass {
    private static final int SEED = 33;
    private static final EnhancedRandom RANDOM = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .seed(SEED).build();

    private static final int TEST_DATA_COUNT = 100;

    private RefreshCargoTypesExecutor refreshCargoTypesExecutor;

    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CargoTypeSnapshotRepository cargoTypeSnapshotRepository;

    @Before
    public void setup() {
        refreshCargoTypesExecutor = new RefreshCargoTypesExecutor(
            Mockito.mock(JdbcTemplate.class),
            transactionHelper,
            jdbcTemplate,
            YPath.simple("//test/path2")
        );
    }

    @Test
    public void testTableSwap() {
        List<CargoTypeSnapshot> newData =
            RANDOM.objects(CargoTypeSnapshot.class, TEST_DATA_COUNT)
                .collect(Collectors.toList());

        refreshCargoTypesExecutor.replacePrices(newData);
        refreshCargoTypesExecutor.replacePrices(newData);
        refreshCargoTypesExecutor.replacePrices(newData);
    }

    @Test
    public void testReplaceCargoTypes() {
        List<CargoTypeSnapshot> oldData =
            RANDOM.objects(CargoTypeSnapshot.class, TEST_DATA_COUNT)
                .collect(Collectors.toList());
        cargoTypeSnapshotRepository.save(oldData);

        List<CargoTypeSnapshot> newData =
            RANDOM.objects(CargoTypeSnapshot.class, TEST_DATA_COUNT)
                .collect(Collectors.toList());

        refreshCargoTypesExecutor.replacePrices(newData);

        List<CargoTypeSnapshot> repositoryData =
            cargoTypeSnapshotRepository.find(CargoTypeSnapshotRepository.Filter.ALL);

        Assertions.assertThat(repositoryData).usingElementComparatorIgnoringFields("id")
            .containsOnlyElementsOf(newData);
    }
}
