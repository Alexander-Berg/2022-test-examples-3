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

import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.inside.yt.kosher.impl.operations.utils.YtSerializable;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeField;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;
import ru.yandex.inside.yt.kosher.operations.Statistics;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.misc.ExceptionUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class YtSerializationTest {

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
    public void testMultiEntityReducerWithParams() throws Exception {
        TestReducer testReducer = new TestReducer(42);

        TestReducer restoredReducer = serializeRound(testReducer);

        assertEquals(42, restoredReducer.someParam);

        SimpleYsonModel origEntity = new SimpleYsonModel("aaa");
        YTreeMapNode rec = testReducer.serialize(origEntity);
        SimpleYsonModel parsedEntity = restoredReducer.parse(rec, SimpleYsonModel.class);

        assertNotNull(parsedEntity);
        assertEquals(origEntity.someField, parsedEntity.someField);

    }


    @YTreeObject
    public static class SimpleYsonModel {
        @YTreeField
        private String someField;

        public SimpleYsonModel(String someField) {
            this.someField = someField;
        }
    }

    public static class TestReducer extends YsonMultiEntityReducerWithKey<SimpleYsonModel> implements YtSerializable {

        private int someParam;

        public TestReducer(int someParam) {
            this.someParam = someParam;
        }

        @Override
        public SimpleYsonModel key(YTreeMapNode entry) {
            return null;
        }

        @Override
        public void reduce(SimpleYsonModel jsonNode, IteratorF<YTreeMapNode> entries, Yield<YTreeMapNode> yield, Statistics statistics) {

        }
    }
}
