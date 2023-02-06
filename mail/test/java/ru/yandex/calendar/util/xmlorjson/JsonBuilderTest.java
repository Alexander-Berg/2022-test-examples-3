package ru.yandex.calendar.util.xmlorjson;

import java.io.StringWriter;

import org.junit.Test;

import ru.yandex.commune.json.write.JsonWriter;
import ru.yandex.commune.json.write.JsonWriterFactory;
import ru.yandex.misc.test.Assert;

/**
 * @author gutman
 */
public class JsonBuilderTest {

    @Test
    public void simple() {
        StringWriter w = new StringWriter();
        JsonWriter jw = JsonWriterFactory.defaultFactory().createJsonWriter(w);
        JsonBuilder b = new JsonBuilder(jw);

        b.startObject("");
        b.addTextField("one", 1);
        b.addTextField("two", 2);
        b.addTextField("three", 3);
        b.endObject();

        jw.close();
        Assert.equals("{\"one\":\"1\",\"two\":\"2\",\"three\":\"3\"}", w.toString());
    }

    @Test
    public void array() {
        StringWriter w = new StringWriter();
        JsonWriter jw = JsonWriterFactory.defaultFactory().createJsonWriter(w);
        JsonBuilder b = new JsonBuilder(jw);

        b.startObject("");
        b.startArray("digits");
        b.addTextField("digit", 1);
        b.addTextField("digit", 2);
        b.addTextField("digit", 3);
        b.endArray();
        b.endObject();

        jw.close();
        Assert.equals("{\"digits\":[\"1\",\"2\",\"3\"]}", w.toString());
    }

    @Test
    public void nested() {
        StringWriter w = new StringWriter();
        JsonWriter jw = JsonWriterFactory.defaultFactory().createJsonWriter(w);
        JsonBuilder b = new JsonBuilder(jw);

        b.startObject("");
        b.startObject("1");

        b.startObject("1.1");
        b.endObject();

        b.startObject("1.2");
        b.addTextField("1.2.1", "text");
        b.endObject();

        b.endObject();
        b.endObject();

        jw.close();
        Assert.equals("{\"1\":{\"1.1\":{},\"1.2\":{\"1.2.1\":\"text\"}}}", w.toString());
    }

    @Test
    public void complex() {
        StringWriter w = new StringWriter();
        JsonWriter jw = JsonWriterFactory.defaultFactory().createJsonWriter(w);
        JsonBuilder b = new JsonBuilder(jw);

        b.startObject("");
        b.startArray("digits");
        b.startObject("digit");
        b.addTextField("sign", 1);
        b.addTextField("spelling", "one");
        b.endObject();
        b.startObject("digit");
        b.addTextField("sign", 2);
        b.addTextField("spelling", "two");
        b.endObject();
        b.endArray();
        b.endObject();

        jw.close();
        Assert.equals("{\"digits\":[{\"sign\":\"1\",\"spelling\":\"one\"},{\"sign\":\"2\",\"spelling\":\"two\"}]}",
                w.toString());
    }

    @Test
    public void jsonTypes() {
        StringWriter w = new StringWriter();
        JsonWriter jw = JsonWriterFactory.defaultFactory().createJsonWriter(w);
        JsonBuilder b = new JsonBuilder(jw);

        b.startArray("types");

        b.addTextField("text", "text");

        b.addNumberField("int", 25);
        b.addNumberField("long", 36l);
        b.addNumberField("float", 39.42f);
        b.addNumberField("double", 42.459);

        b.addBooleanField("boolean", true);
        b.addNullField("null");

        b.endArray();

        jw.close();
        Assert.equals("[\"text\",25,36,39.42,42.459,true,null]", w.toString());
    }
}
