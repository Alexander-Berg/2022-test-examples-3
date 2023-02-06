package ru.yandex.market.ir.datagetter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.ir.datagetter.export.ExportStrategy;
import ru.yandex.market.mbo.http.ModelStorage;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AppTest {
    @Test
    public void whenReceivesSpoiledDataFromYtShouldFail() {
        Exception exception = null;
        try {
            runAndGetLocalDataGetterDir("incorrect binary protobuf message".getBytes());
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertNotNull(exception);
        Assert.assertTrue(exception instanceof ExecutionException);
        Assert.assertTrue(exception.getCause() instanceof RuntimeException);
        Assert.assertTrue(exception.getCause().getCause() instanceof InvalidProtocolBufferException);
    }

    @Test
    public void shouldNotFailNormally() throws Exception {
        int categoryId = 91491;

        byte[] modelStorageModelByteArray = ModelStorage.Model.getDefaultInstance().toByteString().toByteArray();
        File file = runAndGetLocalDataGetterDir(modelStorageModelByteArray).resolve("mbo_stuff")
                .resolve("stable")
                .resolve("models")
                .resolve(String.format("all_models_%d.pb", categoryId)).toFile();
        Assert.assertTrue(file.exists());
    }

    private Path runAndGetLocalDataGetterDir(byte[] modelStorageModelByteArray) throws Exception {
        // mock YtTables
        YtTables tables = mock(YtTables.class);
        doAnswer((Answer<Void>) invocation -> {
            int i = 0;
            YPath unusedYPath = invocation.getArgument(i++);
            YTableEntryType<YTreeMapNode> unusedType = invocation.getArgument(i++);
            Consumer<YTreeMapNode> consumer = invocation.getArgument(i++);
            consumer.accept(YTree.mapBuilder()
                    .key("data").value(modelStorageModelByteArray)
                    .buildMap());
            return null;
        })
                .when(tables)
                .read(any(), any(), Mockito.<Consumer>any());

        // mock Yt
        Yt yt = mock(Yt.class);
        when(yt.tables())
                .thenReturn(tables);

        // mock Supplier<Yt>
        Supplier<Yt> ytS = mock(Supplier.class);
        doReturn(yt)
                .when(ytS)
                .get();

        Path localDataGetterDir = Files.createTempDirectory("ir-getter-test");

        // run main logic
        App app = new App.Builder()
                .setThreads(1)
                .setCategoryIds(Collections.singletonList(91491L))
                .setYtS(ytS)
                .setExportStrategy(ExportStrategy.MATCHER)
                .setLocalDataGetterDir(localDataGetterDir.toFile())
                .setMboExportYPath(YPath.simple("//temp"))
                .setExportModelsYPath(YPath.simple("//temp"))
                .setDumpSessionId("20010203_0405")
                .setLastProcessedSessionId("nothing")
                .setMinimumModelsCount(1L)
                .build();
        try {
            app.init();
            app.run();
        } finally {
            app.destroy();
        }

        return localDataGetterDir;
    }
}
