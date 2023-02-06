import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.ataccama.dqc.commons.properties.DefaultPerformanceSettings;
import com.ataccama.dqc.model.elements.data.AccessMode;
import com.ataccama.dqc.model.elements.data.ColumnInfo;
import com.ataccama.dqc.model.elements.data.ColumnType;
import com.ataccama.dqc.model.elements.data.IRecord;
import com.ataccama.dqc.model.elements.data.IRecordFormat;
import com.ataccama.dqc.model.elements.data.StandardRecordFormat;
import com.ataccama.dqc.model.elements.data.bindings.Binding;
import com.ataccama.dqc.model.elements.data.flow.IQueueInputEndpoint;
import com.ataccama.dqc.model.elements.data.flow.RecordQueue;
import com.ataccama.dqc.model.elements.steps.EndPointMapping;
import com.ataccama.dqc.model.elements.steps.bean.ExpressionWrapper;
import com.ataccama.dqc.model.environment.IAlgorithmContext;
import com.ataccama.dqc.model.internal.modelimpl.DefaultAlgorithmContext;
import com.ataccama.dqc.model.internal.modelimpl.DefaultProcessContextImpl;
import com.ataccama.dqc.model.memory.DefaultMemoryManager;
import com.ataccama.dqc.model.memory.DefaultStrategy;
import com.ataccama.dqc.model.monitoring.CounterGroup;
import com.ataccama.dqc.model.monitoring.CounterId;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mdm.dqc.writer.YtWriter;
import ru.yandex.market.mdm.dqc.writer.YtWriterImpl;

/**
 * @author albina-gima
 * @date 3/18/21
 */
public class YtWriterTest {

    @Test
    public void test() throws Exception {
        String stepId = "YTWriterId";
        YtWriter ytWriter = new YtWriter();
        ytWriter.setId(stepId);
        ytWriter.setYtProxy("hahn.yt.yandex.net");
        ytWriter.setYtPath("//home/market/development/mbo/sbye/MBO-30806/yt_writer_test/v12");
        ytWriter.setWriteAllColumns(true);

        ExpressionWrapper expression = new ExpressionWrapper("\"2012-01-01\"");
        expression.setPropertyContext(ytWriter, "/");
        ytWriter.setYtTabletName(expression);

        StandardRecordFormat recordFormat = new StandardRecordFormat();
        recordFormat.addColumn(new ColumnInfo(ColumnType.STRING, "model_id", AccessMode.RW));
        recordFormat.lock();

        IAlgorithmContext algorithmContext = new DefaultAlgorithmContext(
                stepId,
                new DefaultProcessContextImpl(),
                new CounterGroup(new CounterId("Counter")),
                new DefaultMemoryManager(new DefaultStrategy(1024)),
                new DefaultPerformanceSettings(1)
        );

        RecordQueue queue = new RecordQueue(recordFormat, new String[0], new DefaultProcessContextImpl());
        IQueueInputEndpoint inputEndpoint = queue.createInputEndpoint();

        EndPointMapping[] endpointMappings = new EndPointMapping[]{
                new EndPointMapping(
                        ytWriter.getInputEndpoint(),
                        inputEndpoint,
                        queue.createOutputEndpoint()
                )
        };

        ytWriter.getInputEndpoint().setRecordFormat(recordFormat);
        YtWriterImpl ytWriterImpl = (YtWriterImpl) ytWriter.createComplexStep(endpointMappings, algorithmContext);
        Assert.assertNotNull("Instance should not be null", ytWriterImpl);

//      TODO: Чтобы запустить запись, нужно добавить токен для YT и расскомментировать строку ниже.
        int recordsTotal = 200;
        startAsyncInputQueueProcessing(inputEndpoint, recordFormat, recordsTotal);
//        ytWriterImpl.run();
    }

    private void startAsyncInputQueueProcessing(
            IQueueInputEndpoint inputEndpoint,
            IRecordFormat recordFormat,
            int recordsTotal
    ) {
        Binding fieldBinding = recordFormat.createBinding("model_id", AccessMode.RW);
        ExecutorService pool = Executors.newFixedThreadPool(1);
        pool.submit(() -> {
            try {
                inputEndpoint.putBatch(generateRecords(recordFormat, fieldBinding, recordsTotal)
                        .toArray(IRecord[]::new));

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            inputEndpoint.close();
        });
    }

    List<IRecord> generateRecords(IRecordFormat recordFormat, Binding fieldBinding, int recordsTotal) {
        List<IRecord> list = new LinkedList<>();

        for (int j = 0; j < recordsTotal; j++) {
            IRecord record = recordFormat.createNewRecord();
            fieldBinding.set(record, String.valueOf(recordsTotal + j));
            list.add(record);
        }

        return list;
    }
}
