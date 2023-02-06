package ru.yandex.market.mbo.export.modelstorage.pipe.statistics;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.mapreduce.utils.YtStatisticsUtils;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class PipeStatisticsTest {

    @Test
    public void statsAreSerializable() throws IOException, ClassNotFoundException {
        PipeStatistics pipeStatistics = new PipeStatistics();
        pipeStatistics.saveStates(new PipeStatisticsKey(1, 0, "step 1"), 1, 1, 1, 100);
        pipeStatistics.saveStates(new PipeStatisticsKey(2, 0, "step 2_0"), 1, 2, 3, 200);
        pipeStatistics.saveStates(new PipeStatisticsKey(11, 1, "step 11_1"), 0, 0, 0, 10);
        pipeStatistics.mergeTimings(PipeStatisticsKey.systemKey("BEFORE"), 15);
        ByteArrayOutputStream written = new ByteArrayOutputStream();
        new ObjectOutputStream(written).writeObject(pipeStatistics);
        PipeStatistics p = (PipeStatistics) new ObjectInputStream(
            new ByteArrayInputStream(written.toByteArray())
        ).readObject();
        System.out.println(p.getLogStatistics());
    }

    @Test
    public void testWriteToString() {
        PipeStatistics pipeStatistics = new PipeStatistics();

        pipeStatistics.saveStates(new PipeStatisticsKey(1, 0, "step 1"), 1, 1, 1, 100);
        pipeStatistics.saveStates(new PipeStatisticsKey(2, 0, "step 2_0"), 1, 2, 3, 200);
        pipeStatistics.saveStates(new PipeStatisticsKey(11, 1, "step 11_1"), 0, 0, 0, 10);
        pipeStatistics.mergeTimings(PipeStatisticsKey.systemKey("BEFORE"), 15);

        String logStatistics = pipeStatistics.getLogStatistics();
        Assertions.assertThat(logStatistics)
            .isEqualTo("Models (count)\n" +
                "     01_00 : step 1 : 1\n" +
                "     02_00 : step 2_0 : 1\n" +
                "     11_01 : step 11_1 : 0\n" +
                "Modifications (count)\n" +
                "     01_00 : step 1 : 1\n" +
                "     02_00 : step 2_0 : 2\n" +
                "     11_01 : step 11_1 : 0\n" +
                "Skus (count)\n" +
                "     01_00 : step 1 : 1\n" +
                "     02_00 : step 2_0 : 3\n" +
                "     11_01 : step 11_1 : 0\n" +
                "Timings (ms)\n" +
                "     01_00 : step 1 : 100\n" +
                "     02_00 : step 2_0 : 200\n" +
                "     11_01 : step 11_1 : 10\n" +
                "     System : BEFORE : 15\n");
    }

    @Test
    public void testWriteToYt() {
        PipeStatistics pipeStatistics = new PipeStatistics();

        pipeStatistics.saveStates(new PipeStatisticsKey(1, 0, "step 1"), 1, 1, 1, 100);
        pipeStatistics.saveStates(new PipeStatisticsKey(2, 0, "step 2_0"), 1, 2, 3, 200);
        pipeStatistics.saveStates(new PipeStatisticsKey(11, 1, "step 11_1"), 0, 0, 0, 10);
        pipeStatistics.mergeTimings(PipeStatisticsKey.systemKey("BEFORE"), 15);

        YTreeMapNode ytStatistics = YtStatisticsUtils.convert(pipeStatistics);
        YTreeMapNode treeNode = YTree.mapBuilder()
            .key("models_count")
            .beginMap()
            .key("01_00_step_1").value(1)
            .key("02_00_step_2_0").value(1)
            .key("11_01_step_11_1").value(0)
            .endMap()
            .key("modifications_count")
            .beginMap()
            .key("01_00_step_1").value(1)
            .key("02_00_step_2_0").value(2)
            .key("11_01_step_11_1").value(0)
            .endMap()
            .key("skus_count")
            .beginMap()
            .key("01_00_step_1").value(1)
            .key("02_00_step_2_0").value(3)
            .key("11_01_step_11_1").value(0)
            .endMap()
            .key("timings_ms")
            .beginMap()
            .key("01_00_step_1").value(100)
            .key("02_00_step_2_0").value(200)
            .key("11_01_step_11_1").value(10)
            .key("system_BEFORE").value(15)
            .endMap()
            .buildMap();

        Assertions.assertThat(ytStatistics).isEqualTo(treeNode);
    }
}
