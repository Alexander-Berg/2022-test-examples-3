package ru.yandex.crypta.graph2.testlib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.crypta.graph2.dao.yt.bendable.YsonMultiEntitySupport;
import ru.yandex.crypta.graph2.dao.yt.local.StatisticsSlf4jLoggingImpl;
import ru.yandex.inside.yt.kosher.impl.operations.mains.MapMain;
import ru.yandex.inside.yt.kosher.impl.operations.mains.ReduceMain;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeEntityNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.serialization.YTreeBinarySerializer;
import ru.yandex.inside.yt.kosher.operations.MapperOrReducer;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.operations.map.Mapper;
import ru.yandex.inside.yt.kosher.operations.reduce.Reducer;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.misc.ExceptionUtils;
import ru.yandex.misc.io.devnull.DevNullOutputStream;
import ru.yandex.misc.time.TimeUtils;

public class PerformanceTestHelper {

    private YsonMultiEntitySupport toYsonSerializer = new YsonMultiEntitySupport();

    public <I, O> void runPerfTest(MapperOrReducer<I, O> mapper,
                                   byte[] tablesBlobs,
                                   int iters) throws Exception {

        System.out.println("START");
        long start = System.currentTimeMillis();

        for (int i = 0; i < iters; i++) {
            System.out.println(i);

            DevNullOutputStream[] out = Cf.repeat(DevNullOutputStream::new, 100)
                    .toArray(DevNullOutputStream.class);

            ByteArrayInputStream in = new ByteArrayInputStream(tablesBlobs);
            if (mapper instanceof Mapper) {
                MapMain.applyMapper((Mapper<I, O>) mapper, in, out, new StatisticsSlf4jLoggingImpl());
            } else if (mapper instanceof Reducer) {
                ReduceMain.applyReducer((Reducer<I, O>) mapper, in, out, new StatisticsSlf4jLoggingImpl());
            }


        }

        long end = System.currentTimeMillis();

        System.out.println("TOTAL: " + TimeUtils.millisecondsToSecondsString(end - start));
    }

    public <T> void prepareTableDataFromEntity(T rec, int tableIndex, int recsN, Yield<YTreeMapNode> out) {
        prepareTableDataFromYson(Cf.list(toYsonSerializer.serialize(rec)), tableIndex, recsN, out);
    }

    public <T> void prepareTableDataFromEntity(ListF<T> recs, int tableIndex, int recsN, Yield<YTreeMapNode> out) {
        prepareTableDataFromYson(recs.map(toYsonSerializer::serialize), tableIndex, recsN, out);
    }

    public <T> void prepareTableDataFromYson(YTreeMapNode rec, int tableIndex, int recsN, Yield<YTreeMapNode> out) {
        prepareTableDataFromYson(Cf.list(rec), tableIndex, recsN, out);
    }

    public void prepareTableDataFromYson(ListF<YTreeMapNode> recs, int tableIndex, int recsN, Yield<YTreeMapNode> out) {

        for (int idx : Cf.range(0, recsN)) {
            if (idx % 10000 == 0) {
                System.out.println(idx);
            }
            for (YTreeMapNode rec : recs) {
                out.yield(tableIndex, rec);
            }
        }
    }

    public static class ToBytesYield implements Yield<YTreeMapNode> {

        private int currentTable = 0;
        private ByteArrayOutputStream out = new ByteArrayOutputStream();
        private Yield<YTreeNode> yield = YTreeBinarySerializer.yield(new OutputStream[]{out});

        @Override
        public void yield(int index, YTreeMapNode rec) {
            if (currentTable != index) {
                YTreeEntityNodeImpl separator = new YTreeEntityNodeImpl(
                        Cf.map(YsonMultiEntitySupport.TABLE_INDEX_COLUMN, YTree.integerNode(index))
                );
                yield.yield(separator);
            }
            yield.yield(rec);
        }

        @Override
        public void close() throws IOException {

        }

        public byte[] toByteArray() {
            try {
                yield.close();
            } catch (IOException e) {
                throw ExceptionUtils.translate(e);
            }
            return out.toByteArray();
        }


    }

}
