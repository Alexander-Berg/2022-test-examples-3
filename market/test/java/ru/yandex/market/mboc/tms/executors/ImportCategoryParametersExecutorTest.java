package ru.yandex.market.mboc.tms.executors;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.storage.StorageKeyValueRepository;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mbo.storage.StorageKeyValueServiceImpl;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.parameters.Parameter;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.tms.executors.parameters.YtParametersReaderMock;
import ru.yandex.market.mboc.tms.service.YtStuffSessionReaderMock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author eremeevvo
 * @since 17.10.2019
 */
public class ImportCategoryParametersExecutorTest extends BaseDbTestClass {

    private static final int SEED = 433224;

    private static final int BATCH_SIZE = 10;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StorageKeyValueRepository storageKeyValueRepository;

    @Autowired
    private TransactionHelper transactionHelper;

    private StorageKeyValueService storageKeyValueService;

    private ImportCategoryParametersExecutor importCategoryParametersExecutor;

    private YtStuffSessionReaderMock ytStuffSessionReader;

    private YtParametersReaderMock ytParametersReaderMock;

    private EnhancedRandom random;

    @Before
    @SuppressWarnings("checkstyle:magicnumber")
    public void setUp() {
        random = TestDataUtils.defaultRandomBuilder(SEED)
            .exclude(MboParameters.ValueType.class)
            .build();

        ytStuffSessionReader = new YtStuffSessionReaderMock()
            .setLastSessionId("20190703_0457");

        ytParametersReaderMock = new YtParametersReaderMock();
        storageKeyValueService = new StorageKeyValueServiceImpl(storageKeyValueRepository, null);
        importCategoryParametersExecutor = new ImportCategoryParametersExecutor(jdbcTemplate, transactionHelper,
            ytStuffSessionReader, storageKeyValueService, ytParametersReaderMock, BATCH_SIZE);
    }

    @Test
    public void testImport() {
        Parameter parameter = getParameter();
        ytParametersReaderMock.insert(parameter);
        importCategoryParametersExecutor.execute();
        assertThat(getAllParameters()).containsExactly(parameter);
    }

    @Test
    public void testImportNew() {
        Parameter parameter1 = getParameter();
        Parameter parameter2 = getParameter();

        ytParametersReaderMock.insert(parameter1);

        importCategoryParametersExecutor.execute();
        assertThat(getAllParameters()).containsExactly(parameter1);

        ytParametersReaderMock.insert(parameter2);
        upStuffSessionId();

        importCategoryParametersExecutor.execute();
        assertThat(getAllParameters())
            .containsExactlyInAnyOrder(parameter1, parameter2);
    }

    @Test
    public void testImportBatch() {
        List<Parameter> parameters = Stream.iterate(1, i -> i + 1)
            .limit(BATCH_SIZE * 2 + 1)
            .map(id -> getParameter())
            .collect(Collectors.toList());

        ytParametersReaderMock.insert(parameters);

        importCategoryParametersExecutor.execute();
        assertThat(getAllParameters())
            .containsExactlyInAnyOrderElementsOf(parameters);
    }

    @Test
    public void testUpdateChanges() {
        Parameter parameter = getParameter().setName("test");
        ytParametersReaderMock.insert(parameter);

        importCategoryParametersExecutor.execute();
        assertThat(getAllParameters()).containsExactly(parameter);

        parameter.setName("test-new");
        upStuffSessionId();
        importCategoryParametersExecutor.execute();

        assertThat(getAllParameters())
            .containsExactly(parameter);
    }

    @Test
    public void testDeleteDeleted() {
        Parameter parameter = getParameter();
        ytParametersReaderMock.insert(parameter);

        importCategoryParametersExecutor.execute();
        assertThat(getAllParameters())
            .containsExactly(parameter);

        ytParametersReaderMock.delete(parameter);
        upStuffSessionId();
        importCategoryParametersExecutor.execute();

        assertThat(getAllParameters().size()).isEqualTo(1);
        assertThat(getAllParameters().get(0).isDeleted()).isTrue();
    }

    @Test
    public void testDoNotImportWhenStuffSessionIsLessThenTheLastOne() {
        Parameter parameter1 = getParameter();
        Parameter parameter2 = getParameter();

        ytParametersReaderMock.insert(parameter1);

        importCategoryParametersExecutor.execute();
        assertThat(getAllParameters())
            .containsExactly(parameter1);

        ytParametersReaderMock.insert(parameter2);
        importCategoryParametersExecutor.execute();

        assertThat(getAllParameters())
            .containsExactly(parameter1);
    }

    private void upStuffSessionId() {
        ytStuffSessionReader.setLastSessionId("20190703_0458");
    }

    private List<Parameter> getAllParameters() {
        List<Parameter> result = new ArrayList<>();
        jdbcTemplate.query("select * from monetization.category_parameter", rs -> {
            Parameter parameter = new Parameter()
                .setParamId(rs.getLong("param_id"))
                .setCategoryId(rs.getLong("category_id"))
                .setXslName(rs.getString("xsl_name"))
                .setDescription(rs.getString("description"))
                .setParameterValueType(MboParameters.ValueType.valueOf(rs.getString("value_type")))
                .setName(rs.getString("name"))
                .setDeleted(rs.getBoolean("deleted"));
            result.add(parameter);
        });
        return result;
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private Parameter getParameter() {
        return random.nextObject(Parameter.class)
            .setParameterValueType(MboParameters.ValueType.valueOf(random.nextInt(5) + 1)) // 1 - 5
            .setDeleted(false);
    }
}
