package ru.yandex.market.deepmind.tms.executors;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryManager;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.yql_query_service.service.QueryService;
import ru.yandex.market.yql_test.annotation.YqlTest;

public class ImportCategoryManagerExecutorTest extends DeepmindBaseDbTestClass {
    private static final String TABLE = "//tmp/category_managers/latest";

    private ImportCategoryManagerExecutor executor;
    @Resource
    private NamedParameterJdbcTemplate namedYqlJdbcTemplate;
    @Resource
    private DeepmindCategoryManagerRepository deepmindCategoryManagerRepository;
    @Resource
    private TransactionTemplate deepmindSqlTransactionTemplate;
    @Resource
    private QueryService queryService;

    @Before
    public void setUp() throws Exception {
        executor = new ImportCategoryManagerExecutor(
            YPath.simple(TABLE),
            namedYqlJdbcTemplate,
            queryService,
            deepmindCategoryManagerRepository,
            deepmindSqlTransactionTemplate
        );
    }

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = {
            TABLE,
        },
        csv = "ImportCategoryManagerExecutorTest.yql.before.csv",
        yqlMock = "ImportCategoryManagerExecutorTest.yql.mock"
    )
    public void insert() throws Exception {
        executor.doRealJob(null);

        var all = deepmindCategoryManagerRepository.findAll();
        Assertions.assertThat(all).containsExactlyInAnyOrder(
            categoryManager(278353L, "OTHER", "user-1", "Сергей", "Ермаков"),
            categoryManager(90452L, "SALESMAN", "user-1", "Петр", "Ермаков"),
            categoryManager(16011677L, "CATMAN", "user-2", "Василий", "Ермаков"),
            categoryManager(16224108L, "CATDIR", "user-3", "Генадий", "Ермаков"),
            categoryManager(15826025L, "OTHER", "user-4", "Павел", "Арманов"),
            categoryManager(15826025L, "OTHER", "user-1", "Золото", "Арманов")
        );
    }

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = {
            TABLE,
        },
        csv = "ImportCategoryManagerExecutorTest.yql.before.csv",
        yqlMock = "ImportCategoryManagerExecutorTest.yql.mock"
    )
    public void insertDeleteUpdate() throws Exception {
        deepmindCategoryManagerRepository.save(
            categoryManager(1L, "FIRST", "user-1", "Сергей", "Ермаков"),
            categoryManager(2L, "SECOND", "user-1", "Сергей", "Ермаков"),
            categoryManager(278353L, "OTHER", "user-1", "Сергей", "Ермаков"),
            categoryManager(278353L, "CATMAN", "user-1", "Сергей", "Ермаков"),
            categoryManager(15826025L, "OTHER", "user-1", "----", "----")
        );

        executor.doRealJob(null);

        var all = deepmindCategoryManagerRepository.findAll();
        Assertions.assertThat(all).containsExactlyInAnyOrder(
            categoryManager(278353L, "OTHER", "user-1", "Сергей", "Ермаков"),
            categoryManager(90452L, "SALESMAN", "user-1", "Петр", "Ермаков"),
            categoryManager(16011677L, "CATMAN", "user-2", "Василий", "Ермаков"),
            categoryManager(16224108L, "CATDIR", "user-3", "Генадий", "Ермаков"),
            categoryManager(15826025L, "OTHER", "user-4", "Павел", "Арманов"),
            categoryManager(15826025L, "OTHER", "user-1", "Золото", "Арманов")
        );
    }

    private CategoryManager categoryManager(long categoryId, String role, String staffLogin, String name,
                                            String lastName) {
        return new CategoryManager()
            .setCategoryId(categoryId)
            .setRole(role)
            .setStaffLogin(staffLogin)
            .setFirstName(name)
            .setLastName(lastName);
    }
}
