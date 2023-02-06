import com.ataccama.dqc.commons.properties.DefaultPerformanceSettings;
import com.ataccama.dqc.model.elements.data.AccessMode;
import com.ataccama.dqc.model.elements.data.ColumnInfo;
import com.ataccama.dqc.model.elements.data.ColumnType;
import com.ataccama.dqc.model.elements.data.StandardRecordFormat;
import com.ataccama.dqc.model.elements.data.flow.RecordQueue;
import com.ataccama.dqc.model.elements.steps.EndPointMapping;
import com.ataccama.dqc.model.environment.IAlgorithmContext;
import com.ataccama.dqc.model.internal.modelimpl.DefaultAlgorithmContext;
import com.ataccama.dqc.model.internal.modelimpl.DefaultProcessContextImpl;
import com.ataccama.dqc.model.memory.DefaultMemoryManager;
import com.ataccama.dqc.model.memory.DefaultStrategy;
import com.ataccama.dqc.model.monitoring.CounterGroup;
import com.ataccama.dqc.model.monitoring.CounterId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.mdm.dqc.reader.YtReaderColumn;
import ru.yandex.market.mdm.dqc.yql.YqlRunner;
import ru.yandex.market.mdm.dqc.yql.YqlRunnerImpl;

/**
 * @author albina-gima
 * @date 2/14/22
 */
public class YqlRunnerTest {
    @Test
    @Ignore
    public void testProcessYqlQueryThenReadQueryResult() throws Exception {
        String stepId = "yql_runner";
        YtReaderColumn[] ytReaderColumns = new YtReaderColumn[]{
                new YtReaderColumn("day", ColumnType.STRING),
                new YtReaderColumn("time", ColumnType.STRING),
        };
        ColumnInfo[] ataccamaColumns = new ColumnInfo[]{
                new ColumnInfo(ColumnType.STRING, "day", AccessMode.RW),
                new ColumnInfo(ColumnType.STRING, "time", AccessMode.RW),
        };
        emulateAtaccamaStep(stepId, ytReaderColumns, ataccamaColumns);
    }

    private void emulateAtaccamaStep(String stepId,
                                     YtReaderColumn[] ytReaderColumns,
                                     ColumnInfo[] ataccamaColumns) throws Exception {
        String query = "USE hahn;\n" +
                "SELECT\n" +
                "    `time`,\n" +
                "    `day`\n" +
                "FROM hahn.`home/market/users/belkinmike/test_dyn`\n" +
                "LIMIT 100;";

        // TODO нужно заполнить проперти в DqYtConfig (см. проперти в файле с кинофигами для тестинга + свой токен)
        YqlRunner yqlRunner = new YqlRunner();
        yqlRunner.setId(stepId);
        yqlRunner.setColumns(ytReaderColumns);
        yqlRunner.setYqlQuery(query);

        StandardRecordFormat recordFormat = new StandardRecordFormat();
        for (ColumnInfo column : ataccamaColumns) {
            recordFormat.addColumn(column);
        }
        recordFormat.lock();

        IAlgorithmContext algorithmContext = new DefaultAlgorithmContext(
                stepId,
                new DefaultProcessContextImpl(),
                new CounterGroup(new CounterId("Counter")),
                new DefaultMemoryManager(new DefaultStrategy(1024)),
                new DefaultPerformanceSettings(1)
        );

        RecordQueue queue = new RecordQueue(recordFormat, new String[0], new DefaultProcessContextImpl());
        EndPointMapping[] endpointMappings = new EndPointMapping[]{
                new EndPointMapping(
                        yqlRunner.outputEndPoint(),
                        queue.createInputEndpoint(),
                        queue.createOutputEndpoint()
                )
        };

        yqlRunner.outputEndPoint().setRecordFormat(recordFormat);
        YqlRunnerImpl yqlRunnerImpl = (YqlRunnerImpl) yqlRunner.createComplexStep(endpointMappings, algorithmContext);
        yqlRunnerImpl.run();

        Assert.assertNotNull("Instance should not be null", yqlRunnerImpl);
    }
}
