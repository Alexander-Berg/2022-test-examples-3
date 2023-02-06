package ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.yt;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

import Market.Gumoful.TemplateRendering;
import com.google.protobuf.InvalidProtocolBufferException;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeEntityNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.operations.Statistics;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.operations.specs.ReducerSpec;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.yt.gumoful_rendering.YtGumofulRenderingOutput;
import ru.yandex.market.mbo.yt.TestYt;
import ru.yandex.market.mbo.yt.TestYtOperations;
import ru.yandex.market.yt.util.mapreduce.ReducerWithJoinByKeyAndStats;
import ru.yandex.misc.digest.Md5;
import ru.yandex.misc.io.file.FileInputStreamSource;

/**
 * Эмуляции работы
 * {@link ru.yandex.market.mbo.synchronizer.export.modelstorage.mapreduce.yt.gumoful_rendering.YtGumofulRenderingJob}
 * в тестах.
 *
 * @author s-ermakov
 */
public class YtGumofulRendererJobTestInitializer {

    private final TestYt yt;
    private final File gumofulRenderingReducerExecutablePath;
    private final Map<Long, TemplateRendering.TModelRenderingResult> modelIdToRenderingResult = new HashMap<>();
    private TemplateRendering.TModelRenderingResult defaultModelRenderingResult;

    public YtGumofulRendererJobTestInitializer(TestYt yt, File gumofulRenderingReducerExecutablePath) {
        this.yt = yt;
        this.gumofulRenderingReducerExecutablePath = gumofulRenderingReducerExecutablePath;
        init();
    }

    private void init() {
        try {
            Files.write(gumofulRenderingReducerExecutablePath.toPath(), Collections.singletonList(
                "Sorry, this is not executable file. This file is temporary and was created for unit tests only"
            ), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String ytFileName = calculateYPath(new FileInputStreamSource(gumofulRenderingReducerExecutablePath));

        TestYtOperations operations = yt.operations();
        operations.mockCommandSpec(
            "YT_USE_CLIENT_PROTOBUF=0 ./" + ytFileName + " --yt-map \"YtRendererReducer\" 1 0",
            new ReducerSpec(new RendererReducer()));
    }

    public File getExecutableFile() {
        return gumofulRenderingReducerExecutablePath;
    }

    public void setDefaultRenderingResult(TemplateRendering.TModelRenderingResult defaultModelRenderingResult) {
        this.defaultModelRenderingResult = defaultModelRenderingResult;
    }

    public void mockModelRenderingResult(long modelId, TemplateRendering.TModelRenderingResult renderingResult) {
        if (modelIdToRenderingResult.containsKey(modelId)) {
            throw new IllegalArgumentException("Already contains rendering result for model " + modelId + ", " +
                "result " + renderingResult);
        }
        modelIdToRenderingResult.put(modelId, renderingResult);
    }

    private static String calculateYPath(FileInputStreamSource fileSource) {
        String md5 = Md5.A.digest(fileSource).hex();
        String fileName = fileSource.getFile().getName();
        return fileName + "_" + md5;
    }

    /**
     * Java implemetation of reducer: /trunk/arcadia/market/gumoful/tools/yt_renderer_reducer/main.cpp.
     */
    private class RendererReducer extends ReducerWithJoinByKeyAndStats<Long, Long, MboParameters.Category> {

        @Override
        public Long joinByKey(YTreeMapNode foreignTableEntry) {
            return foreignTableEntry.getLong("category_id");
        }

        @Override
        public Long reduceByKey(Long categoryId, YTreeMapNode primaryTableEntry) {
            return primaryTableEntry.getLong("category_id");
        }

        @Override
        public MboParameters.Category mapForeignValue(Long categoryId, @Nullable YTreeMapNode foreignTableEntry) {
            if (foreignTableEntry == null) {
                throw new IllegalStateException("Expected to contain category (" + categoryId + ") at foreign table");
            }
            byte[] data = foreignTableEntry.getBytes("data");
            try {
                return MboParameters.Category.parseFrom(data);
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void reduceGroup(Long categoryId, MboParameters.Category category,
                                Long dummy, Iterator<YTreeMapNode> primaryTableEntries,
                                Yield<YTreeMapNode> yield, Statistics statistics) {
            while (primaryTableEntries.hasNext()) {
                YTreeMapNode entry = primaryTableEntries.next();
                byte[] data = entry.getBytes("data");
                ModelStorage.Model model;
                try {
                    model = ModelStorage.Model.parseFrom(data);
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }

                YTreeNode renderResult = new YTreeEntityNodeImpl(Cf.map());
                boolean isForRender = entry.getBool("render");
                if (isForRender) {
                    TemplateRendering.TModelRenderingResult result = modelIdToRenderingResult.getOrDefault(
                        model.getId(), defaultModelRenderingResult);
                    if (result == null) {
                        throw new RuntimeException("No mocked rendering result for model with id: " + model.getId());
                    }
                    renderResult = new YTreeStringNodeImpl(result.toByteArray(), null);
                }

                Map<String, YTreeNode> map = entry.asMap();
                map.put(YtGumofulRenderingOutput.RENDERING_RESULT.getField(), renderResult);
                yield.yield(YTree.node(map).mapNode());
            }
        }
    }
}
