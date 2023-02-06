package ru.yandex.direct.core.entity.banner.service.text;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.service.text.FunctionTextExtractor.functionTextExtractor;

@RunWith(Parameterized.class)
public class CompoundTextExtractorTest {

    private static final String TEXT_1 = " text1 with spaces ";
    private static final String TEXT_1_RESULT = "text1 with spaces";
    private static final String TEXT_2 = "\t text2 with spaces\t ";
    private static final String TEXT_2_RESULT = "text2 with spaces";
    private static final String TEXT_12_RESULT = TEXT_1_RESULT + " " + TEXT_2_RESULT;

    private static final TextHolder1Impl TEXT_HOLDER_NOT_NULL = new TextHolder1Impl(TEXT_1);
    private static final TextHolder1Impl TEXT_HOLDER_NULL = new TextHolder1Impl(null);
    private static final TextHolder1Impl TEXT_HOLDER_EMPTY = new TextHolder1Impl("   ");

    private static final TextHolder2Impl TEXT_HOLDER_NOT_NULL_2 = new TextHolder2Impl(TEXT_2);

    private static final TextHolderWithStopType TEXT_HOLDER_STOP_NOT_NULL = new TextHolderWithStopType(TEXT_1);
    private static final TextHolderWithStopType TEXT_HOLDER_STOP_NULL = new TextHolderWithStopType(null);
    private static final TextHolderWithStopType TEXT_HOLDER_STOP_EMPTY = new TextHolderWithStopType("   ");

    private static final TextHolderBothImpl TEXT_HOLDER_BOTH_NOT_NULL = new TextHolderBothImpl(TEXT_1, TEXT_2);
    private static final TextHolderBothImpl TEXT_HOLDER_BOTH_NULL = new TextHolderBothImpl(null, null);
    private static final TextHolderBothImpl TEXT_HOLDER_BOTH_NOT_NULL_AND_NULL = new TextHolderBothImpl(TEXT_1, null);
    private static final TextHolderBothImpl TEXT_HOLDER_BOTH_NULL_AND_NOT_NULL = new TextHolderBothImpl(null, TEXT_2);
    private static final TextHolderBothImpl TEXT_HOLDER_BOTH_NOT_NULL_AND_EMPTY = new TextHolderBothImpl(TEXT_1, "  ");
    private static final TextHolderBothImpl TEXT_HOLDER_BOTH_NULL_AND_EMPTY = new TextHolderBothImpl(null, "  ");

    private final CompoundTextExtractor extractor = new CompoundTextExtractor(asList(
            functionTextExtractor(TextHolder1.class, TextHolder1::getText1),
            functionTextExtractor(TextHolder2.class, TextHolder2::getText2),
            functionTextExtractor(TextHolder3.class, StopType.class, TextHolder3::getText3)
    ));

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public List<Object> textHolders;

    @Parameterized.Parameter(2)
    public List<Map.Entry> expectedResult;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // пустой список на входе
                {
                        "пустой список объектов",
                        emptyList(),
                        emptyList()
                },

                // один объект на входе
                {
                        "объект с одним текстом != null",
                        singletonList(TEXT_HOLDER_NOT_NULL),
                        singletonList(Map.entry(TEXT_HOLDER_NOT_NULL, TEXT_1_RESULT))
                },
                {
                        "объект с одним текстом из пробельных символов",
                        singletonList(TEXT_HOLDER_EMPTY),
                        singletonList(Map.entry(TEXT_HOLDER_EMPTY, ""))
                },
                {
                        "объект с одним текстом == null",
                        singletonList(TEXT_HOLDER_NULL),
                        singletonList(Map.entry(TEXT_HOLDER_NULL, ""))
                },
                {
                        "объект с двумя текстами - оба не null",
                        singletonList(TEXT_HOLDER_BOTH_NOT_NULL),
                        singletonList(Map.entry(TEXT_HOLDER_BOTH_NOT_NULL, TEXT_12_RESULT))
                },
                {
                        "объект с двумя текстами - первый null",
                        singletonList(TEXT_HOLDER_BOTH_NULL_AND_NOT_NULL),
                        singletonList(Map.entry(TEXT_HOLDER_BOTH_NULL_AND_NOT_NULL, TEXT_2_RESULT))
                },
                {
                        "объект с двумя текстами - второй null",
                        singletonList(TEXT_HOLDER_BOTH_NOT_NULL_AND_NULL),
                        singletonList(Map.entry(TEXT_HOLDER_BOTH_NOT_NULL_AND_NULL, TEXT_1_RESULT))
                },
                {
                        "объект с двумя текстами - оба null",
                        singletonList(TEXT_HOLDER_BOTH_NULL),
                        singletonList(Map.entry(TEXT_HOLDER_BOTH_NULL, ""))
                },
                {
                        "объект с двумя текстами - второй из пробельных символов",
                        singletonList(TEXT_HOLDER_BOTH_NOT_NULL_AND_EMPTY),
                        singletonList(Map.entry(TEXT_HOLDER_BOTH_NOT_NULL_AND_EMPTY, TEXT_1_RESULT))
                },
                {
                        "объект с двумя текстами - первый == null, второй из пробельных символов",
                        singletonList(TEXT_HOLDER_BOTH_NULL_AND_EMPTY),
                        singletonList(Map.entry(TEXT_HOLDER_BOTH_NULL_AND_EMPTY, ""))
                },

                // один объект, но с исключающим интерфейсом
                {
                        "объект с исключающим интерфейсом с одним текстом != null",
                        singletonList(TEXT_HOLDER_STOP_NOT_NULL),
                        singletonList(Map.entry(TEXT_HOLDER_STOP_NOT_NULL, ""))
                },
                {
                        "объект с исключающим интерфейсом с одним текстом из пробельных символов",
                        singletonList(TEXT_HOLDER_STOP_EMPTY),
                        singletonList(Map.entry(TEXT_HOLDER_STOP_EMPTY, ""))
                },
                {
                        "объект с исключающим интерфейсом с одним текстом == null",
                        singletonList(TEXT_HOLDER_STOP_NULL),
                        singletonList(Map.entry(TEXT_HOLDER_STOP_NULL, ""))
                },

                // несколько объектов на входе
                {
                        "два объекта с одним текстом, оба текста != null",
                        asList(TEXT_HOLDER_NOT_NULL, TEXT_HOLDER_NOT_NULL_2),
                        asList(
                                Map.entry(TEXT_HOLDER_NOT_NULL, TEXT_1_RESULT),
                                Map.entry(TEXT_HOLDER_NOT_NULL_2, TEXT_2_RESULT))
                },
                {
                        "два объекта с одним текстом, один из текстов == null",
                        asList(TEXT_HOLDER_NULL, TEXT_HOLDER_NOT_NULL_2),
                        asList(
                                Map.entry(TEXT_HOLDER_NULL, ""),
                                Map.entry(TEXT_HOLDER_NOT_NULL_2, TEXT_2_RESULT))
                },
                {
                        "три объекта, два с одним текстом, один с двумя",
                        asList(TEXT_HOLDER_NULL, TEXT_HOLDER_NOT_NULL_2, TEXT_HOLDER_BOTH_NOT_NULL),
                        asList(
                                Map.entry(TEXT_HOLDER_NULL, ""),
                                Map.entry(TEXT_HOLDER_NOT_NULL_2, TEXT_2_RESULT),
                                Map.entry(TEXT_HOLDER_BOTH_NOT_NULL, TEXT_12_RESULT))
                },
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void extractorWorksFine() {
        Map<Object, String> result = extractor.extractTexts(textHolders);

        if (!expectedResult.isEmpty()) {
            assertThat(result)
                    .containsOnly(expectedResult.toArray(new Map.Entry[0]));
        } else {
            assertThat(result).isEmpty();
        }
    }

    private interface TextHolder1 {
        String getText1();
    }

    private interface TextHolder2 {
        String getText2();
    }

    private interface TextHolder3 {
        String getText3();
    }

    private interface StopType {
    }

    private static class TextHolder1Impl implements TextHolder1 {
        private final String text1;

        public TextHolder1Impl(String text1) {
            this.text1 = text1;
        }

        @Override
        public String getText1() {
            return text1;
        }
    }

    private static class TextHolderWithStopType implements TextHolder3, StopType {
        private final String text3;

        public TextHolderWithStopType(String text3) {
            this.text3 = text3;
        }

        @Override
        public String getText3() {
            return text3;
        }
    }

    private static class TextHolder2Impl implements TextHolder2 {
        private final String text2;

        public TextHolder2Impl(String text2) {
            this.text2 = text2;
        }

        @Override
        public String getText2() {
            return text2;
        }
    }

    private static class TextHolderBothImpl extends TextHolder1Impl implements TextHolder1, TextHolder2 {

        private final String text2;

        public TextHolderBothImpl(String text1, String text2) {
            super(text1);
            this.text2 = text2;
        }

        @Override
        public String getText2() {
            return text2;
        }
    }

}
