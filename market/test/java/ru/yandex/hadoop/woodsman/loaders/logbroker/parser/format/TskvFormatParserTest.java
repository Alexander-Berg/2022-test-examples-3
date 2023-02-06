package ru.yandex.hadoop.woodsman.loaders.logbroker.parser.format;

import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.market.stat.parsers.formats.TskvFormatParser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Denis Khurtin
 */
@RunWith(DataProviderRunner.class)
public class TskvFormatParserTest {

    private TskvFormatParser tskvParser;

    @DataProvider
    public static Object[][] parseDataProvider() {
        return new Object[][]{
                new Object[]{"", ImmutableMap.of()},
                new Object[]{"tskv\ta=b", ImmutableMap.of("a", "b")},
                new Object[]{"aaa", ImmutableMap.of()},
                new Object[]{"a=b", ImmutableMap.of("a", "b")},
                new Object[]{"a=", ImmutableMap.of()},
                new Object[]{"=b", ImmutableMap.of()},
                new Object[]{"kk=vv\ttimestamp=12345", ImmutableMap.of("kk", "vv", "timestamp", "12345")},
                new Object[]{
                        "kkkkkkkkkkkkkkkkkkkk11111111111111=vvvvvvvvvvvv1111111111\t" +
                                "kkkkkkkkk22222222222222=vvv22222222222222\t" +
                                "k3333333333333333333333=v33333333333\t" +
                                "k4=a\\\\b\\\\c\\\\\\\\\\\t" +
                                "k5=\\t\\\\a\\\\b\\\\c\\\\\\\\\\\t" +
                                "k6=\\\\t\\\\a\\\\b\\\\c\\\\\\\\\\\t" +
                                "k7=v7\\\\\t" +
                                "k8=v8",
                        new HashMap<String, String>() {
                            {
                                put("kkkkkkkkkkkkkkkkkkkk11111111111111", "vvvvvvvvvvvv1111111111");
                                put("kkkkkkkkk22222222222222", "vvv22222222222222");
                                put("k3333333333333333333333", "v33333333333");
                                put("k4", "a\\\\b\\\\c\\\\\\\\\\");
                                put("k5", "\\t\\\\a\\\\b\\\\c\\\\\\\\\\");
                                put("k6", "\\\\t\\\\a\\\\b\\\\c\\\\\\\\\\");
                                put("k7", "v7\\\\");
                                put("k8", "v8");
                            }
                        }
                },
                new Object[]{"key=value\tkeylessvalue1\tkeylessvalue2",
                        ImmutableMap.of("key", "value")}
        };
    }

    @Test
    @UseDataProvider("parseDataProvider")
    public void parse(String line, Map<String, String> expectedResult) {
        tskvParser = new TskvFormatParser();

        //When
        Map<String, String> result = tskvParser.parse(line);

        // Then
        assertThat(result, equalTo(expectedResult));
//        assertThat(tskvParser.getDirtyLines(), equalTo(0L));
    }

    @DataProvider
    public static Object[][] parseRedirDataProvider() {
        return new Object[][]{
                new Object[]{"", ImmutableMap.of()},
                new Object[]{"tskv", ImmutableMap.of()},
                new Object[]{"tskv@@a=b", ImmutableMap.of("a", "b")},
                new Object[]{"aaa", ImmutableMap.of()},
                new Object[]{"a=b", ImmutableMap.of("a", "b")},
                new Object[]{"a=", ImmutableMap.of()},
                new Object[]{"=b", ImmutableMap.of()},
                new Object[]{"kk=vv@@timestamp=12345", ImmutableMap.of("kk", "vv", "timestamp", "12345")},
                new Object[]{
                        "kkkkkkkkkkkkkkkkkkkk11111111111111=vvvvvvvvvvvv1111111111@@" +
                                "kkkkkkkkk22222222222222=vvv22222222222222@@" +
                                "k33333333333@33333333333@=@v33@333333333@@" +
                                "k4=a\\\\b\\\\c\\\\\\\\\\@@" +
                                "k5=\\t\\\\a\\\\b\\\\c\\\\\\\\\\@@" +
                                "k6=\\\\t\\\\a\\\\b\\\\c\\\\\\\\\\@@" +
                                "k7=v7\\\\@@" +
                                "k8=v8",
                        new HashMap<String, String>() {
                            {
                                put("kkkkkkkkkkkkkkkkkkkk11111111111111", "vvvvvvvvvvvv1111111111");
                                put("kkkkkkkkk22222222222222", "vvv22222222222222");
                                put("k33333333333@33333333333@", "@v33@333333333");
                                put("k4", "a\\\\b\\\\c\\\\\\\\\\");
                                put("k5", "\\t\\\\a\\\\b\\\\c\\\\\\\\\\");
                                put("k6", "\\\\t\\\\a\\\\b\\\\c\\\\\\\\\\");
                                put("k7", "v7\\\\");
                                put("k8", "v8");
                            }
                        }
                },
                new Object[]{"key=value@@keylessvalue1@@keylessvalue2",
                        ImmutableMap.of("key", "value")}
        };
    }

    @Test
    @UseDataProvider("parseRedirDataProvider")
    public void parseRedir(String line, Map<String, String> expectedResult) {
        tskvParser = new TskvFormatParser("@@");

        //When
        Map<String, String> result = tskvParser.parse(line);

        // Then
        assertThat(result, equalTo(expectedResult));
//        assertThat(tskvParser.getDirtyLines(), equalTo(0L));
    }

    @DataProvider
    public static Object[][] parseRedirWithSystemFieldsDataProvider() {
        return new Object[][]{
                new Object[]{"key=value@@keylessvalue1@@keylessvalue2",
                        ImmutableMap.of("key", "value", "systemField1", "keylessvalue1", "systemField2", "keylessvalue2")},
                new Object[]{"key=value@@keylessvalue1@@keylessvalue2@@keylessvalue3@@",
                        ImmutableMap.of("key", "value", "systemField1", "keylessvalue1", "systemField2", "keylessvalue2")},
                new Object[]{"key=value",
                        ImmutableMap.of("key", "value")},
                new Object[]{"key1=value1@@falsekeyless@@key2=value2@@keylessvalue1@@keylessvalue2",
                        ImmutableMap.of("key1", "value1@@falsekeyless", "key2", "value2", "systemField1", "keylessvalue1", "systemField2", "keylessvalue2")},
        };
    }

    @Test
    @UseDataProvider("parseRedirWithSystemFieldsDataProvider")
    public void parseRedirWithSystemFields(String line, Map<String, String> expectedResult) {
        tskvParser = new TskvFormatParser("@@", asList("systemField1", "systemField2"));

        //When
        Map<String, String> result = tskvParser.parse(line);

        // Then
        assertThat(result, equalTo(expectedResult));
//        assertThat(tskvParser.getDirtyLines(), equalTo(0L));
    }

    @Test
    public void parseInvalidUtf8Line() {
        tskvParser = new TskvFormatParser();

        // Given
        String invalidUtf8Line = "a=b\tinvalid_text=������������������������������������";

        // When
        Map<String, String> result = tskvParser.parse(invalidUtf8Line);

        // Then
        Map<String, String> expectedResult = new HashMap<String, String>() {
            {
                put("a", "b");
                put("invalid_text", "������������������������������������");
            }
        };

        assertThat(result, equalTo(expectedResult));
//        assertThat(tskvParser.getDirtyLines(), equalTo(1L));
    }

//    @DataProvider
//    public static Object[][] parseBinaryDataProvider() {
//        return new Object[][] {
//                new Object[] { new byte[] {}, ImmutableMap.of() },
//                new Object[] {
//                        new byte[] { 't', 's', 'k', 'v' },
//                        ImmutableMap.of("unparsed_records", new byte[] { 't', 's', 'k', 'v' })
//                },
//                new Object[] {
//                        new byte[] { 't', 's', 'k', 'v', '\t', 'a', '=', 'b' },
//                        ImmutableMap.of(
//                                        "unparsed_records", new byte[] { 't', 's', 'k', 'v' },
//                                        "a", new byte[] { 'b' }
//                                )
//                },
//                new Object[] {
//                        new byte[] { 'a', 'a', '\t', 'b', 'b' },
//                        ImmutableMap.of("unparsed_records", new byte[] { 'a', 'a', ',', 'b', 'b' })
//                },
//                new Object[] {
//                        new byte[] { 'a', '=', 'b' },
//                        ImmutableMap.of("a", new byte[] { 'b' })
//                },
//                new Object[] {
//                        new byte[] { 'a', '=' },
//                        ImmutableMap.of("a", new byte[0])
//                },
//                new Object[] {
//                        new byte[] { '=', 'b' },
//                        ImmutableMap.of()
//                },
//                new Object[] {
//                        new byte[] { '\t', 'k', '1', '=', '1', '2', '3', '\t', '\t', 'k', '2', '=', '0', '\t', '\t' },
//                        ImmutableMap.of(
//                                        "k1", new byte[] { '1', '2', '3' },
//                                        "k2", new byte[] { '0' }
//                                )
//                },
//                new Object[] {
//                        new byte[] { 'k', '1', '=', '\\', 't', '&', '\\', '0', '\t', 'k', '2', '=', '\\', '\\', '\\',
//                                '=' },
//                        ImmutableMap.of(
//                                        "k1", new byte[] { '\t', '&', 0 },
//                                        "k2", new byte[] { '\\', '=' }
//                                )
//                },
//        };
//    }
//
//    @Test(dataProvider = "parseBinaryDataProvider")
//    public void parseBinary(byte[] bytes, Map<String, byte[]> expectedResult) {
//        //When
//        Map<String, byte[]> result = tskvParser.parse(bytes);
//
//        // Then
//        assertThat(result, equalToBinaryMap(expectedResult));
////        assertThat(tskvParser.getDirtyLines(), equalTo(0L));
//    }

//    @DataProvider
//    public static Object[][] unescapemeDataProvider() {
//        return new Object[][] {
//                new Object[] { new byte[] {}, new byte[] {} },
//                new Object[] { new byte[] { '\\', 'n' }, new byte[] { '\n' } },
//                new Object[] { new byte[] { '\\', 't' }, new byte[] { '\t' } },
//                new Object[] { new byte[] { '\\', '0' }, new byte[] { '\0' } },
//                new Object[] { new byte[] { '\\', 'r' }, new byte[] { '\r' } },
//                new Object[] { new byte[] { '\\', '=' }, new byte[] { '=' } },
//                new Object[] { new byte[] { '\\', '\"' }, new byte[] { '\"' } },
//                new Object[] { new byte[] { '\\', '\\' }, new byte[] { '\\' } },
//                new Object[] { new byte[] { '\\', 'a' }, new byte[] { '\\', 'a' } },
//                new Object[] {
//                        new byte[] { '\\', 'n', '\\', 'n', '\\', '\\', '\\', '\t', '\\', 'n', '\\', '\\', '\0' },
//                        new byte[] { '\n', '\n', '\\', '\\', '\t', '\n', '\\', '\0' }
//                },
//        };
//    }

//    @Test(dataProvider = "unescapemeDataProvider")
//    public void unescapeme(byte[] bytes, byte[] expectedResult) {
//        // Where
//        byte[] result = tskvParser.unescapeme(bytes);
//
//        // Then
//        assertThat(result, equalTo(expectedResult));
//        assertThat(tskvParser.getDirtyLines(), equalTo(0L));
//    }

    private static org.hamcrest.Matcher<Map<String, byte[]>> equalToBinaryMap(Map<String, byte[]> operand) {
        return new IsMapEqual(operand);
    }

    @SuppressWarnings("unchecked")
    private static class IsMapEqual extends BaseMatcher<Map<String, byte[]>> {

        private final Map<String, byte[]> expectedMap;

        public IsMapEqual(Map<String, byte[]> operand) {
            this.expectedMap = operand;
        }

        @Override
        public boolean matches(Object item) {
            if (item == null) {
                return false;
            }

            if (!(item instanceof Map)) {
                throw new IllegalArgumentException();
            }

            Map<String, byte[]> realMap = (Map<String, byte[]>) item;

            if (realMap.size() != expectedMap.size()) {
                return false;
            }

            for (String key : realMap.keySet()) {
                if (!expectedMap.containsKey(key)) {
                    return false;
                }

                byte[] realValue = realMap.get(key);
                byte[] expectedValue = expectedMap.get(key);

                if (!Arrays.equals(realValue, expectedValue)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("<").appendText(toString(expectedMap)).appendText(">");
        }

        @Override
        public void describeMismatch(Object item, Description description) {
            description.appendText("was ").appendText("<").appendText(toString(item)).appendText(">");
        }

        private String toString(Object item) {
            if (item == null) {
                return "null";
            }

            if (!(item instanceof Map)) {
                return item.toString();
            }

            Map<String, byte[]> map = (Map<String, byte[]>) item;

            StringBuilder stringBuilder = new StringBuilder("{");

            for (String key : map.keySet()) {
                byte[] value = map.get(key);

                if (stringBuilder.length() > 1) {
                    stringBuilder.append(",");
                }

                stringBuilder.append(key).append('=').append(Arrays.toString(value));
            }

            return stringBuilder.append("}").toString();
        }
    }

//    @Test
//    public void testThroughtput(){
//        tskvParser = new TskvLogFormatParser("@@");
//
//        Random random = new Random();
//        int num = 1000000;
//        String[] lines = new String[num];
//        for (int i =0; i < num; i++) {
//            StringBuilder sb =new StringBuilder();
//            for (int j = 0; j < 30; j++) {
//                sb.append(random.ne)
//            }
//            lines[i] =
//        }
//
//    }
}
