package ru.yandex.market.mbo.tms.erp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.serialization.YTreeBinarySerializer;
import ru.yandex.inside.yt.kosher.operations.OperationContext;
import ru.yandex.inside.yt.kosher.operations.Statistics;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.erp.dao.ErpSkuMapper;
import ru.yandex.market.mbo.http.ModelStorage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * @author amaslak
 */
public class ExportErpDiffReaderTest {

    private static final String MODELS_FILE = "sku_512743.pb.gz";

    private List<ModelStorage.Model> models;

    @Before
    public void setUp() {
        models = loadModels();
    }

    @Test
    public void test() {
        Assert.assertNotNull(models);
        Assert.assertFalse(models.isEmpty());
    }

    @Test
    public void testErpSkuMapper() {
        ErpSkuMapper mapper = new ErpSkuMapper();

        List<YTreeMapNode> nodeList = models.stream()
            .map(m -> YTree.mapBuilder()
                .key("data").value(m.toByteArray())
                .key("model_id").value(m.getId())
                .key("category_id").value(m.getCategoryId())
                .buildMap())
            .collect(Collectors.toList());

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        //noinspection unchecked
        Yield<YTreeMapNode> yield = (Yield<YTreeMapNode>)
            ((Yield) YTreeBinarySerializer.yield(new OutputStream[] {stream}));

        Statistics statistics = Mockito.mock(Statistics.class);
        OperationContext context = new OperationContext();

        for (YTreeMapNode node : nodeList) {
            mapper.map(node, yield, statistics, context);
        }
    }

    private static List<ModelStorage.Model> loadModels() {
        List<ModelStorage.Model> models = new ArrayList<>();

        try (InputStream inputStream = ExportErpDiffReaderTest.class.getResourceAsStream(MODELS_FILE);
             GZIPInputStream is = new GZIPInputStream(inputStream)) {
            ModelStorage.Model model;
            while ((model = ModelStorage.Model.PARSER.parseDelimitedFrom(is)) != null) {
                models.add(model);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return models;
    }

}
