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

import ru.yandex.market.mdm.dqc.reader.YtReader;
import ru.yandex.market.mdm.dqc.reader.YtReaderColumn;
import ru.yandex.market.mdm.dqc.reader.YtReaderImpl;

public class YtReaderTest {

    @Test
    @Ignore
    public void testRpcClient() throws Exception {
        String stepId = "yt_reader_rpc";
        String ytPath = "//home/market/users/belkinmike/test_dyn";
        YtReaderColumn[] ytReaderColumns = new YtReaderColumn[] {
                    new YtReaderColumn("day", ColumnType.STRING),
                    new YtReaderColumn("time", ColumnType.STRING),
                };
        ColumnInfo[] ataccamaColumns = new ColumnInfo[] {
                    new ColumnInfo(ColumnType.STRING, "day", AccessMode.RW),
                    new ColumnInfo(ColumnType.STRING, "time", AccessMode.RW),
                };

        emulateAtaccamaStep(stepId, ytPath, ytReaderColumns, ataccamaColumns);
    }

    @Test
    @Ignore
    public void testHttpClient() throws Exception {
        String stepId = "yt_reader_http";
        String ytPath = "//home/market/production/mbo/export/recent/models/sku";
        YtReaderColumn[] ytReaderColumns = new YtReaderColumn[] {
                new YtReaderColumn("data", ColumnType.STRING),
        };
        ColumnInfo[] ataccamaColumns = new ColumnInfo[] {
                new ColumnInfo(ColumnType.STRING, "data", AccessMode.RW),
        };

        emulateAtaccamaStep(stepId, ytPath, ytReaderColumns, ataccamaColumns);

    }

    private void emulateAtaccamaStep(String stepId, String ytPath,
                                     YtReaderColumn[] ytReaderColumns, ColumnInfo[] ataccamaColumns) throws Exception {
        YtReader ytReader = new YtReader();
        ytReader.setId(stepId);
        ytReader.setYtPath(ytPath);
        ytReader.setColumns(ytReaderColumns);

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
                        ytReader.outputEndPoint(),
                        queue.createInputEndpoint(),
                        queue.createOutputEndpoint()
                )
        };

        ytReader.outputEndPoint().setRecordFormat(recordFormat);
        YtReaderImpl ytReaderImpl = (YtReaderImpl) ytReader.createComplexStep(endpointMappings, algorithmContext);
        ytReaderImpl.run();

        Assert.assertNotNull("Instance should not be null", ytReaderImpl);
    }
}
