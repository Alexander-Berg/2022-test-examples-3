package ru.yandex.market.mbo.export.modelstorage;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.export.modelstorage.pipe.ModelPipeContext;
import ru.yandex.market.mbo.export.modelstorage.pipe.ModelPipePart;
import ru.yandex.market.mbo.export.modelstorage.pipe.Pipe;
import ru.yandex.market.mbo.http.ModelStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author york
 * @since 27.02.2018
 */
@SuppressWarnings("checkstyle:magicnumber")
public class PipesTest {

    @Test
    public void testOrdering() throws IOException {
        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setModifiedTs(1)
            .build();
        ModelPipeContext context = new ModelPipeContext(model, Collections.emptyList(), Collections.emptyList());

        Pipe pipe = Pipe.start()
            .then(ctx ->
                ctx.getModel().setModifiedTs(ctx.getModel().getModifiedTs() + 3)
            )
            .then(ctx ->
                ctx.getModel().setModifiedTs(ctx.getModel().getModifiedTs() * 2)
            )
            .then(ctx ->
                ctx.getModel().setModifiedTs(1 << ctx.getModel().getModifiedTs())
            )
            .build();
        pipe.acceptModelsGroup(context);
        Assert.assertEquals(256L, context.getModel().getModifiedTs());
    }


    @Test
    public void testFork() throws IOException {
        ModelStorage.Model model = ModelStorage.Model.getDefaultInstance();
        ModelPipeContext context = new ModelPipeContext(model, Collections.emptyList(), Collections.emptyList());

        List<Long> barcodes = new ArrayList<>();
        Set<Long> modifTimes = new HashSet<>();
        Set<Long> modifUsers = new HashSet<>();

        Pipe pipe = Pipe.start()
            .then(ctx ->
                ctx.getModel().setModifiedTs(100)
            )
            .fork(true,
                ctx -> ctx.getModel().addBarcodes(1),
                ctx -> ctx.getModel().addBarcodes(2),
                ctx -> ctx.getModel().addBarcodes(3)
            )
            .forAll(
                ctx -> ctx.getModel().setModifiedUserId(666)
            )
            .forAll(
                ctx -> {
                    barcodes.addAll(ctx.getModel().getBarcodesList());
                    modifTimes.add(ctx.getModel().getModifiedTs());
                    modifUsers.add(ctx.getModel().getModifiedUserId());
                }
            )

            .build();

        pipe.acceptModelsGroup(context);

        Assert.assertTrue(barcodes.containsAll(Arrays.asList(1L, 2L, 3L)));
        Assert.assertEquals(3, barcodes.size());
        Assert.assertEquals(1, modifTimes.size());
        Assert.assertTrue(modifTimes.contains(100L));
        Assert.assertEquals(1, modifUsers.size());
        Assert.assertTrue(modifUsers.contains(666L));
    }

    @Test
    public void testConstruction() throws IOException {
        List<Integer> ids = new ArrayList<>();

        ModelPipePart p1 = new ModelPipePartTestImpl(ids::add);
        ModelPipePart p2 = new ModelPipePartTestImpl(ids::add);
        ModelPipePart p22 = new ModelPipePartTestImpl(ids::add);

        Pipe simple = Pipe.start()
            .then(p1)
            .fork(true, p2, p22)
            .build();

        ModelPipePart p3 = new ModelPipePartTestImpl(ids::add);
        ModelPipePart p4 = new ModelPipePartTestImpl(ids::add);
        ModelPipePart p5 = new ModelPipePartTestImpl(ids::add);

        Pipe pipe2 =  Pipe.start()
            .then(p3)
            .fork(false,
                simple,
                p4)
            .forAll(p5)
            .build();
        pipe2.acceptModelsGroup(new ModelPipeContext(ModelStorage.Model.getDefaultInstance(),
            Collections.emptyList(), Collections.emptyList()));
        Assert.assertEquals(8, ids.size());

        Assert.assertEquals(6, pipe2.getAllParts().size());
    }

    private static class ModelPipePartTestImpl implements ModelPipePart {
        static int idseq = 0;
        private final Consumer<Integer> consumer;
        private final int id;
        ModelPipePartTestImpl(Consumer<Integer> consumer) {
            this.consumer = consumer;
            id = idseq++;
        }
        @Override
        public void acceptModelsGroup(ModelPipeContext context) throws IOException {
            consumer.accept(id);
        }
    }
}
