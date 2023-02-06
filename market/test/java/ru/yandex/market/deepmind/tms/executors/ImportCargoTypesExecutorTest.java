package ru.yandex.market.deepmind.tms.executors;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CargoTypeSnapshot;
import ru.yandex.market.deepmind.common.repository.DeepmindCargoTypeSnapshotRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;

/**
 * @author moskovkin@yandex-team.ru
 * @created 23.07.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ImportCargoTypesExecutorTest extends DeepmindBaseDbTestClass {
    private static final int SEED = 33;
    private static final EnhancedRandom RANDOM = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .seed(SEED).build();

    private static final int TEST_DATA_COUNT = 100;

    private ImportCargoTypesExecutor importCargoTypesExecutor;

    @Resource(name = "deepmindTransactionHelper")
    private TransactionHelper transactionHelper;
    @Resource(name = "deepmindSqlJdbcTemplate")
    private JdbcTemplate jdbcTemplate;
    @Resource
    private DeepmindCargoTypeSnapshotRepository deepmindCargoTypeSnapshotRepository;

    @Before
    public void setup() {
        importCargoTypesExecutor = new ImportCargoTypesExecutor(
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

        importCargoTypesExecutor.replacePrices(newData);
        importCargoTypesExecutor.replacePrices(newData);
        importCargoTypesExecutor.replacePrices(newData);
    }

    @Test
    public void testReplaceCargoTypes() {
        List<CargoTypeSnapshot> oldData =
            RANDOM.objects(CargoTypeSnapshot.class, TEST_DATA_COUNT)
                .collect(Collectors.toList());
        deepmindCargoTypeSnapshotRepository.save(oldData);

        List<CargoTypeSnapshot> newData =
            RANDOM.objects(CargoTypeSnapshot.class, TEST_DATA_COUNT)
                .collect(Collectors.toList());

        importCargoTypesExecutor.replacePrices(newData);

        List<CargoTypeSnapshot> repositoryData =
            deepmindCargoTypeSnapshotRepository.find(DeepmindCargoTypeSnapshotRepository.Filter.ALL);

        Assertions.assertThat(repositoryData).usingElementComparatorIgnoringFields("id")
            .containsOnlyElementsOf(newData);
    }
}
