package ru.yandex.direct.tracing.data;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class TraceDataTest {
    @Parameterized.Parameter(0)
    public String name;

    @Parameterized.Parameter(1)
    public TraceData data;

    @Parameterized.Parameter(2)
    public String text;

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> params() {
        List<Object[]> ret = new ArrayList<>();
        TraceData stub;

        stub = stub();
        ret.add(new Object[]{
                "simple",
                stub,
                "[3,\"2016-05-30 09:53:08.000000\",\"myhost\",0,\"foo\",\"bar\",\"baz\",123,234,345,42,false,5.0," +
                        "1024," +
                        "{\"times\":{\"ela\":6.5,\"cu\":8.5,\"cs\":9.5,\"mem\":16.5},\"profile\":[],\"services\":[]," +
                        "\"marks\":[],\"annotations\":[]}]"
        });

        stub = stub();
        stub.getProfiles().add(new TraceDataProfile("foo", "bar", 60.0, 30.0, 3, 2048));
        ret.add(new Object[]{
                "profile",
                stub,
                "[3,\"2016-05-30 09:53:08.000000\",\"myhost\",0,\"foo\",\"bar\",\"baz\",123,234,345,42," +
                        "false,5.0,1024,{\"times\":{\"ela\":6.5,\"cu\":8.5,\"cs\":9.5,\"mem\":16.5},\"profile\":" +
                        "[[\"foo\",\"bar\",60.0,30.0,3,2048]],\"services\":[],\"marks\":[],\"annotations\":[]}]"
        });

        stub = stub();
        stub.getChildren().add(new TraceDataChild("foo", "bar", 111, 25.0, 19.0));
        ret.add(new Object[]{
                "children",
                stub,
                "[3,\"2016-05-30 09:53:08.000000\",\"myhost\",0,\"foo\",\"bar\",\"baz\",123,234,345,42,false," +
                        "5.0,1024,{\"times\":{\"ela\":6.5,\"cu\":8.5,\"cs\":9.5,\"mem\":16.5},\"profile\":[]," +
                        "\"services\":[[\"foo\",\"bar\",111,25.0,19.0]],\"marks\":[],\"annotations\":[]}]"
        });

        stub = stub();
        stub.getMarks().add(new TraceDataMark(23.0, "foobar"));
        ret.add(new Object[]{
                "marks",
                stub,
                "[3,\"2016-05-30 09:53:08.000000\",\"myhost\",0,\"foo\",\"bar\",\"baz\",123,234,345,42,false," +
                        "5.0,1024,{\"times\":{\"ela\":6.5,\"cu\":8.5,\"cs\":9.5,\"mem\":16.5},\"profile\":[]," +
                        "\"services\":[],\"marks\":[[23.0,\"foobar\"]],\"annotations\":[]}]"
        });

        stub = stub();
        stub.getAnnotations().add(new TraceDataAnnotation("foo", "bar"));
        stub.getAnnotations().add(new TraceDataAnnotation("foo", "baz"));
        ret.add(new Object[]{
                "annotations",
                stub,
                "[3,\"2016-05-30 09:53:08.000000\",\"myhost\",0,\"foo\",\"bar\",\"baz\",123,234,345,42,false," +
                        "5.0,1024,{\"times\":{\"ela\":6.5,\"cu\":8.5,\"cs\":9.5,\"mem\":16.5},\"profile\":[]," +
                        "\"services\":[],\"marks\":[],\"annotations\":[[\"foo\",\"bar\"],[\"foo\",\"baz\"]]}]"
        });


        return ret;
    }

    public static TraceData stub() {
        TraceData stub = new TraceData();
        stub.setLogTime(Instant.ofEpochSecond(1464601988));
        stub.setHost("myhost");
        stub.setService("foo");
        stub.setMethod("bar");
        stub.setTags("baz");
        stub.setTraceId(123);
        stub.setParentId(234);
        stub.setSpanId(345);
        stub.setChunkIndex(42);
        stub.setAllEla(5.0);
        stub.setSamplerate(1024);
        stub.getTimes().setEla(6.5);
        stub.getTimes().setCpuUserTime(8.5);
        stub.getTimes().setCpuSystemTime(9.5);
        stub.getTimes().setMem(16.5);
        return stub;
    }

    @Test
    public void serialize() throws IOException {
        assertThat(data.toJson(), is(text));
    }

    @Test
    public void deserialize() throws IOException {
        assertThat(TraceData.fromJson(text).toJson(), is(text));
    }


}
