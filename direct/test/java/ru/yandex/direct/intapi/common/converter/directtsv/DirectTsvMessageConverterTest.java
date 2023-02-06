package ru.yandex.direct.intapi.common.converter.directtsv;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class DirectTsvMessageConverterTest {
    private DirectTsvMessageConverter converter;

    @DirectTsvFieldsOrder({"adGroupId", "cid", "bid"})
    private static class TestClass {
        private Long cid;
        private Long pid;
        private Long bid;

        private TestClass(Long cid, Long pid, Long bid) {
            this.cid = cid;
            this.pid = pid;
            this.bid = bid;
        }

        @JsonProperty("cid")
        public Long getCid() {
            return cid;
        }

        @JsonProperty("pid")
        @DirectTsvColumn("adGroupId")
        public Long getPid() {
            return pid;
        }

        @JsonProperty("bid")
        public Long getBid() {
            return bid;
        }
    }

    @DirectTsvFieldsOrder({"cid", "text"})
    private static class TestClassString {
        private Long cid;
        private String text;

        private TestClassString(Long cid, String text) {
            this.cid = cid;
            this.text = text;
        }

        @JsonProperty("cid")
        public Long getCid() {
            return cid;
        }

        @JsonProperty("text")
        public String getText() {
            return text;
        }
    }

    @Before
    public void before() {
        converter = new DirectTsvMessageConverter();
    }

    @Test
    public void testGetKeysTsvString() {
        assertThat("Строка заголовка сериализуется верно",
                converter.getKeysTsvString(Arrays.asList("cid", "pid", "bid")),
                equalTo("#cid\tpid\tbid\n"));

        assertThat("Строка заголовка сериализуется верно",
                converter.getKeysTsvString("cid", "pid", "bid"),
                equalTo("#cid\tpid\tbid\n"));
    }

    @Test
    public void testGetTsvString() {
        assertThat("Строка заголовка сериализуется верно",
                converter.getTsvString(ImmutableMap.<String, Object>builder().put("cid", 123).put("bid", 321).build(),
                        Arrays.asList("cid", "pid", "bid")),
                equalTo("123\t\t321\n"));
    }

    @Test
    public void testGetKeysOrder() {
        assertThat("Строка заголовка сериализуется верно",
                converter.getKeysOrder(Arrays.asList("cid", "pid", "bid"),
                        Collections.singletonMap("pid", "adGroupId")),
                beanDiffer(Arrays.asList("cid", "adGroupId", "bid")));
    }

    @Test
    public void testWriteTsv() throws IOException {
        List<TestClass> data = Arrays.asList(
                new TestClass(1L, 2L, 11L),
                new TestClass(3L, 4L, 12L)
        );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        converter.writeTsv(data, new HttpOutputMessage() {
            @Override
            public HttpHeaders getHeaders() {
                return null;
            }

            @Override
            public OutputStream getBody() throws IOException {
                return baos;
            }
        });

        assertThat("Данные серриализованы корректно", baos.toString(),
                equalTo("#adGroupId\tcid\tbid\n2\t1\t11\n4\t3\t12\n#End\n"));
    }

    @Test
    public void testWriteTsvWithNull() throws IOException {
        List<TestClass> data = Collections.singletonList(
                new TestClass(1L, 2L, null)
        );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        converter.writeTsv(data, new HttpOutputMessage() {
            @Override
            public HttpHeaders getHeaders() {
                return null;
            }

            @Override
            public OutputStream getBody() throws IOException {
                return baos;
            }
        });

        assertThat("Данные серриализованы корректно", baos.toString(),
                equalTo("#adGroupId\tcid\tbid\n2\t1\t\n#End\n"));
    }

    @Test
    public void testWriteTsvWithString() throws IOException {
        List<TestClassString> data = Collections.singletonList(
                new TestClassString(1L, "текстћ")
        );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        converter.writeTsv(data, new HttpOutputMessage() {
            @Override
            public HttpHeaders getHeaders() {
                return null;
            }

            @Override
            public OutputStream getBody() throws IOException {
                return baos;
            }
        });

        assertThat("Данные серриализованы корректно", baos.toString(),
                equalTo("#cid\ttext\n1\tтекстћ\n#End\n"));
    }
}
