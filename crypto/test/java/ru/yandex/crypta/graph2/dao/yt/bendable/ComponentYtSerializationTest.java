package ru.yandex.crypta.graph2.dao.yt.bendable;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.matching.human.workflow.merge.ops.ChooseMergeOrderReducer;
import ru.yandex.crypta.graph2.matching.human.workflow.merge.ops.ConfirmMergeOrderReducer;
import ru.yandex.crypta.graph2.model.matching.component.score.HumanMultiHistogramScoringStrategy;
import ru.yandex.crypta.graph2.model.matching.merge.MergeOfferPriority;
import ru.yandex.crypta.graph2.model.soup.edge.EdgeType;
import ru.yandex.crypta.graph2.model.soup.edge.EdgeTypeActivityStats;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.misc.ExceptionUtils;

public class ComponentYtSerializationTest {

    public static final String DUMP_FILE = "test.serialize";

    private <T> void serializeToFile(T obj, String fileName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();

        FileOutputStream fileOutputStream = new FileOutputStream(fileName);
        fileOutputStream.write(baos.toByteArray());

    }

    @SuppressWarnings("TypeParameterUnusedInFormals")
    private <T> T deserializeFromFile(String fileName) throws IOException, ClassNotFoundException {
        FileInputStream input = new FileInputStream(fileName);
        ObjectInputStream ois = new ObjectInputStream(input);
        return (T) ois.readObject();
    }

    private <T> T serializeRound(T entity) {
        try {
            serializeToFile(entity, DUMP_FILE);
            return deserializeFromFile(DUMP_FILE);
        } catch (IOException | ClassNotFoundException e) {
            throw ExceptionUtils.translate(e);
        }
    }

    @After
    public void tearDown() throws Exception {
        Files.delete(Paths.get(DUMP_FILE));

    }

    @Test
    public void testWeightedMultiScore() throws Exception {
        serializeRound(new HumanMultiHistogramScoringStrategy());
    }

    @Test
    public void testEdgeStats() throws Exception {
        EdgeType fakeEdgeType = new EdgeType(EIdType.EMAIL, EIdType.PHONE, ESourceType.ACCOUNT_MANAGER,
                ELogSourceType.ACCESS_LOG);
        EdgeTypeActivityStats edgeStats = new EdgeTypeActivityStats(fakeEdgeType, Cf.map(1, 1L), Cf.map(1, 1L));

        serializeRound(edgeStats);
    }

    @Test
    public void someReducers() {
        serializeRound(new ChooseMergeOrderReducer(new MergeOfferPriority()));
        serializeRound(new ConfirmMergeOrderReducer(new MergeOfferPriority()));
    }

}
