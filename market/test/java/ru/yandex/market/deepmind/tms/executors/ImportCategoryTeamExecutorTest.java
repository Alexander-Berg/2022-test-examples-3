package ru.yandex.market.deepmind.tms.executors;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryTeam;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.yql_query_service.service.QueryService;
import ru.yandex.market.yql_test.annotation.YqlTest;

public class ImportCategoryTeamExecutorTest extends DeepmindBaseDbTestClass {
    private static final String TABLE = "//tmp/catteam/latest";

    private ImportCategoryTeamExecutor executor;
    @Resource
    private NamedParameterJdbcTemplate namedYqlJdbcTemplate;
    @Resource
    private DeepmindCategoryTeamRepository deepmindCategoryTeamRepository;
    @Resource
    private TransactionTemplate deepmindSqlTransactionTemplate;
    @Resource
    private QueryService queryService;

    @Before
    public void setUp() throws Exception {
        executor = new ImportCategoryTeamExecutor(
            YPath.simple(TABLE),
            namedYqlJdbcTemplate,
            queryService,
            deepmindCategoryTeamRepository,
            deepmindSqlTransactionTemplate
        );
    }

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = {
            TABLE,
        },
        csv = "ImportCategoryTeamExecutorTest.yql.before.csv",
        yqlMock = "ImportCategoryTeamExecutorTest.yql.mock"
    )
    public void insert() throws Exception {
        executor.doRealJob(null);

        var all = deepmindCategoryTeamRepository.findAll();
        Assertions.assertThat(all).containsExactlyInAnyOrder(
            categoryManager(278353L, "Электроника"),
            categoryManager(90452L, "Электроника"),
            categoryManager(16011677L, "Вода"),
            categoryManager(16224108L, "Чемоданы"),
            categoryManager(15826025L, "Пуговицы")
        );
    }

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = {
            TABLE,
        },
        csv = "ImportCategoryTeamExecutorTest.yql.before.csv",
        yqlMock = "ImportCategoryTeamExecutorTest.yql.mock"
    )
    public void insertDeleteUpdate() throws Exception {
        deepmindCategoryTeamRepository.save(
            categoryManager(278353L, "Бутыли"),
            categoryManager(90452L, "Электроника")
        );

        executor.doRealJob(null);

        var all = deepmindCategoryTeamRepository.findAll();
        Assertions.assertThat(all).containsExactlyInAnyOrder(
            categoryManager(278353L, "Электроника"),
            categoryManager(90452L, "Электроника"),
            categoryManager(16011677L, "Вода"),
            categoryManager(16224108L, "Чемоданы"),
            categoryManager(15826025L, "Пуговицы")
        );
    }

    private CategoryTeam categoryManager(long categoryId, String catteam) {
        return new CategoryTeam()
            .setCategoryId(categoryId)
            .setCatteam(catteam);
    }
}
