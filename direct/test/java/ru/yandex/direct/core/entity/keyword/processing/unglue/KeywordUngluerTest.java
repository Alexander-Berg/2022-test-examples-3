package ru.yandex.direct.core.entity.keyword.processing.unglue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import one.util.streamex.EntryStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.keyword.processing.NormalizedWord;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.libs.keywordutils.model.Keyword;
import ru.yandex.direct.libs.keywordutils.model.SingleKeyword;
import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@SuppressWarnings("unchecked")
@CoreTest
@RunWith(Parameterized.class)
public class KeywordUngluerTest {

    private static final Integer ADGROUP_INDEX = 1;
    private static final Integer ADGROUP_INDEX2 = 2;

    @Autowired
    private KeywordUngluer keywordUngluer;

    @Parameterized.Parameter()
    public List<UnglueContainerWrapper> newInputWrappers;

    @Parameterized.Parameter(1)
    public List<UnglueContainerWrapper> existingInputWrappers;

    @Parameterized.Parameter(2)
    public List<UnglueResultWrapper> expectedResultWrappers;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Parameterized.Parameters(name = "{0} + {1} => {2}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                //добавление минус слова с конца и середины
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона дешево")),
                        singletonList(new UnglueResultWrapper(0, singletonList("дешево"), emptyMap()))
                },
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить большого слона")),
                        singletonList(new UnglueResultWrapper(0, singletonList("большого"), emptyMap()))
                },
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона дешево")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона")),
                        singletonList(new UnglueResultWrapper(0, emptyList(), ImmutableMap.of(0, "дешево")))
                },
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить большого слона")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона")),
                        singletonList(new UnglueResultWrapper(0, emptyList(), ImmutableMap.of(0, "большого")))
                },
                //не добавляем минус слово, если оно уже есть среди минус слов
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона дешево")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона -дешево")),
                        emptyList()
                },
                //не добавляем минус слово, если оно хоть в какой-то форме уже имеется среди минус слов
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона дешево")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона -!подешевле")),
                        emptyList()
                },
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона +когда")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона -!когда")),
                        emptyList()
                },
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона +когда")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона -!на")),
                        singletonList(new UnglueResultWrapper(0, emptyList(), ImmutableMap.of(0, "!когда")))
                },
                //проверка извлечения слов из оператора []
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить [слона]")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "[купить] слона [дешево]")),
                        singletonList(new UnglueResultWrapper(0, singletonList("дешево"), emptyMap()))
                },
                //проверка извлечения слов с оператором "!" из оператора []
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "!купить [!слона]")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "[!купить] !слона [!дешево]")),
                        singletonList(new UnglueResultWrapper(0, singletonList("!дешево"), emptyMap()))
                },
                //проверка групп слов
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "[купить слона]")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "[купить слона] дешево")),
                        singletonList(new UnglueResultWrapper(0, singletonList("дешево"), emptyMap()))
                },
                //проверка групп слов с оператором "!"
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "[!купить !слона]")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "[!купить !слона] дешево")),
                        singletonList(new UnglueResultWrapper(0, singletonList("дешево"), emptyMap()))
                },
                //фразы в кавычках не расклеиваются
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "\"купить слона\"")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "\"купить слона дешево\"")),
                        emptyList()
                },
                //количество одиночных слов отличается на 1
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона дешево москва")),
                        emptyList()
                },
                //у каждой группы слов (в []) есть эквивалентная ей в другой фразе
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "[в москве] [купить слона]")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "[в москве] [слона дешево] купить")),
                        emptyList()
                },
                //эквивалентность строгих строгим, обычных обычным и тд
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "!куплю слона")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "!куплю слона дешево")),
                        singletonList(new UnglueResultWrapper(0, singletonList("дешево"), emptyMap()))
                },
                //со стоп-словами
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "!куплю слона +в магазине")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "!куплю слона +в магазине дешево")),
                        singletonList(new UnglueResultWrapper(0, singletonList("дешево"), emptyMap()))
                },
                //минусуем стоп-слово
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "+когда купить слона")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона")),
                        singletonList(new UnglueResultWrapper(0, emptyList(), ImmutableMap.of(0, "!когда")))
                },
                //из разных групп не расклеиваем
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона дешево")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX2, "купить слона")),
                        emptyList()
                },
                //новые с новыми
                {
                        asList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона дешево"),
                                new UnglueContainerWrapper(1, ADGROUP_INDEX, "купить слона")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX2, "купить слона")),
                        singletonList(new UnglueResultWrapper(1, singletonList("дешево"), emptyMap()))
                },
                //несколько минус слов к одной новой фразе
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона")),
                        asList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона дешево"),
                                new UnglueContainerWrapper(1, ADGROUP_INDEX, "купить слона москва")),
                        singletonList(new UnglueResultWrapper(0, asList("дешево", "москва"), emptyMap()))
                },
                //несколько минус слов к существующей от разных новых фраз
                {
                        asList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона дешево"),
                                new UnglueContainerWrapper(1, ADGROUP_INDEX, "купить слона москва")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона")),
                        asList(new UnglueResultWrapper(0, emptyList(), ImmutableMap.of(0, "дешево")),
                                new UnglueResultWrapper(1, emptyList(), ImmutableMap.of(0, "москва")))
                },
                //несколько минус слов к существующим фразам от одной новой фразы
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона дешево")),
                        asList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона"),
                                new UnglueContainerWrapper(1, ADGROUP_INDEX, "купить дешево")),
                        singletonList(new UnglueResultWrapper(0, emptyList(),
                                ImmutableMap.of(0, "дешево", 1, "слона")))
                },
                //несколько групп
                {
                        asList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона дешево"),
                                new UnglueContainerWrapper(1, ADGROUP_INDEX, "купить слона москва"),
                                new UnglueContainerWrapper(2, ADGROUP_INDEX2, "продать коня дорого")),
                        asList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона дешево москва"),
                                new UnglueContainerWrapper(1, ADGROUP_INDEX2, "продать коня"),
                                new UnglueContainerWrapper(2, ADGROUP_INDEX2, "хочу продать коня дорого"),
                                new UnglueContainerWrapper(3, ADGROUP_INDEX, "купить слона срочно")),
                        asList(new UnglueResultWrapper(0, singletonList("москва"), emptyMap()),
                                new UnglueResultWrapper(1, singletonList("дешево"), emptyMap()),
                                new UnglueResultWrapper(2, singletonList("хочу"), ImmutableMap.of(1, "дорого")))
                },
                //Слово с оператором "!" добавляется с "!" в исходной форме
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "слон")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "слон !купил")),
                        singletonList(new UnglueResultWrapper(0, singletonList("!купил"), emptyMap()))
                },
                // Добавленные в процессе минус-слова с оператором "!" сравниваются с не
                // зафиксированными минус-словами по нормализованной форме, а не исходной
                {
                        asList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "слон"),
                                new UnglueContainerWrapper(1, ADGROUP_INDEX, "слон !купил"),
                                new UnglueContainerWrapper(2, ADGROUP_INDEX, "слон купить")),
                        emptyList(),
                        singletonList(new UnglueResultWrapper(0, singletonList("!купил"), emptyMap()))
                },
                // Добавляемые минус-слова с оператором "!" сравниваются с имеющимися
                // зафиксированными минус-словами по исходной форме, а не нормализованной
                {
                        asList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "слон !купили"),
                                new UnglueContainerWrapper(1, ADGROUP_INDEX, "слон -!купила")),
                        emptyList(),
                        singletonList(new UnglueResultWrapper(1, singletonList("!купили"), emptyMap()))
                },
                {
                        asList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "!москва !квартиры"),
                                new UnglueContainerWrapper(1, ADGROUP_INDEX, "!москва !новые !квартиры"),
                                new UnglueContainerWrapper(2, ADGROUP_INDEX, "!новая !москва !квартиры")),
                        emptyList(),
                        singletonList(new UnglueResultWrapper(0, List.of("!новые", "!новая"), emptyMap()))
                },
                // Существующие слова с оператором "!" сравниваются по исходной форме
                {
                        asList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "!новая москва дома"),
                                new UnglueContainerWrapper(1, ADGROUP_INDEX, "москва !новые квартиры"),
                                new UnglueContainerWrapper(2, ADGROUP_INDEX, "москва !новые")),
                        emptyList(),
                        singletonList(new UnglueResultWrapper(2, List.of("квартиры"), emptyMap()))
                },
                //Индексы учитываются правильно
                {
                        singletonList(new UnglueContainerWrapper(2, ADGROUP_INDEX, "купить слона")),
                        singletonList(new UnglueContainerWrapper(7, ADGROUP_INDEX, "купить слона дешево")),
                        singletonList(new UnglueResultWrapper(2, singletonList("дешево"), emptyMap()))
                },
                {
                        singletonList(new UnglueContainerWrapper(2, ADGROUP_INDEX, "купить слона дешево")),
                        singletonList(new UnglueContainerWrapper(7, ADGROUP_INDEX, "купить слона")),
                        singletonList(new UnglueResultWrapper(2, emptyList(), ImmutableMap.of(7, "дешево")))
                },
                //Пустые списки не ломают утилиту
                {
                        emptyList(),
                        singletonList(new UnglueContainerWrapper(7, ADGROUP_INDEX, "купить слона")),
                        emptyList()
                },
                {
                        singletonList(new UnglueContainerWrapper(7, ADGROUP_INDEX, "купить слона")),
                        emptyList(),
                        emptyList()
                },
                {
                        emptyList(),
                        emptyList(),
                        emptyList()
                },
                //В ответе присутствуют только результаты с выполненной расклейкой
                {
                        singletonList(new UnglueContainerWrapper(2, ADGROUP_INDEX, "купить слона")),
                        singletonList(new UnglueContainerWrapper(7, ADGROUP_INDEX, "купить слона")),
                        emptyList()
                },
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "!купить !слона")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "[!купить !слона]")),
                        emptyList()
                },
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить слона")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "[купить слона]")),
                        emptyList()
                },
                {
                        asList(new UnglueContainerWrapper(2, ADGROUP_INDEX, "купить слона"),
                                new UnglueContainerWrapper(4, ADGROUP_INDEX, "купить слона")),
                        singletonList(new UnglueContainerWrapper(7, ADGROUP_INDEX, "купить слона")),
                        emptyList()
                },
                {
                        singletonList(new UnglueContainerWrapper(2, ADGROUP_INDEX2, "купить слона дешево")),
                        singletonList(new UnglueContainerWrapper(7, ADGROUP_INDEX, "купить слона")),
                        emptyList()
                },
                {
                        asList(new UnglueContainerWrapper(2, ADGROUP_INDEX, "купить слона дешево"),
                                new UnglueContainerWrapper(4, ADGROUP_INDEX2, "купить слона дешево")),
                        singletonList(
                                new UnglueContainerWrapper(7, ADGROUP_INDEX, "купить слона +в максимальной комплектации")),
                        emptyList()
                },
                {
                        asList(new UnglueContainerWrapper(2, ADGROUP_INDEX, "купить коня"),
                                new UnglueContainerWrapper(4, ADGROUP_INDEX, "купить слона")),
                        singletonList(new UnglueContainerWrapper(7, ADGROUP_INDEX, "купить слона дешево")),
                        singletonList(new UnglueResultWrapper(4, singletonList("дешево"), emptyMap()))
                },
                {
                        asList(new UnglueContainerWrapper(2, ADGROUP_INDEX, "купить коня"),
                                new UnglueContainerWrapper(4, ADGROUP_INDEX, "купить слона дешево")),
                        singletonList(new UnglueContainerWrapper(7, ADGROUP_INDEX, "купить слона")),
                        singletonList(new UnglueResultWrapper(4, emptyList(), ImmutableMap.of(7, "дешево")))
                },

                // Ключевые фразы со словами, имеющими больше одной леммы
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить ухо")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить вкусную уху")),
                        singletonList(new UnglueResultWrapper(0, singletonList("вкусную"), emptyMap()))
                },
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить уху")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить вкусное ухо")),
                        // нельзя расклеивать, т.к. например поисковый запрос "купить вкусной ухи"
                        // не матчится с фразой "купить вкусное ухо"
                        emptyList()
                },
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить вкусную уху")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить ухо")),
                        singletonList(new UnglueResultWrapper(0, emptyList(), Map.of(0, "вкусную")))
                },
                {
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить вкусное ухо")),
                        singletonList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить уху")),
                        // нельзя расклеивать, т.к. например поисковый запрос "купить вкусной ухи"
                        // не матчится с фразой "купить вкусное ухо"
                        emptyList()
                },
                {
                        asList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить уху"),
                                new UnglueContainerWrapper(1, ADGROUP_INDEX, "купить вкусную уху")),
                        emptyList(),
                        singletonList(new UnglueResultWrapper(0, singletonList("вкусную"), emptyMap()))
                },
                {
                        asList(new UnglueContainerWrapper(0, ADGROUP_INDEX, "купить ухо"),
                                new UnglueContainerWrapper(1, ADGROUP_INDEX, "купить вкусную уху")),
                        emptyList(),
                        singletonList(new UnglueResultWrapper(0, singletonList("вкусную"), emptyMap()))
                },
        });
    }

    @Test
    public void unglue() {
        List<UnglueContainer> newUnglueContainers =
                mapList(newInputWrappers, UnglueContainerWrapper::getUnglueContainer);
        List<UnglueContainer> existingUnglueContainers =
                mapList(existingInputWrappers, UnglueContainerWrapper::getUnglueContainer);

        List<UnglueResult> actualResults = keywordUngluer.unglue(newUnglueContainers, existingUnglueContainers);

        List<UnglueResult> expectedResults = mapList(expectedResultWrappers, UnglueResultWrapper::getUnglueResult);
        assertThat(actualResults, beanDiffer(expectedResults));
    }

    private static class UnglueContainerWrapper {

        UnglueContainer unglueContainer;

        UnglueContainerWrapper(Integer index, Integer adGroupIndex, String keywordWithMinuses) {
            this.unglueContainer =
                    new UnglueContainer(index, adGroupIndex, KeywordParser.parseWithMinuses(keywordWithMinuses));
        }

        UnglueContainer getUnglueContainer() {
            return unglueContainer;
        }

        @Override
        public String toString() {
            return String.format("%s: %s", unglueContainer.getIndex(), unglueContainer.getNormalKeywordWithMinuses());
        }
    }


    /**
     * UnglueResult с добавление toString(), чтобы можно было выводить в названии тестов
     */
    private static class UnglueResultWrapper {

        UnglueResult unglueResult;

        UnglueResultWrapper(Integer index, List<String> addedMinusWords,
                            Map<Integer, String> addedMinusWordsToExisting) {
            Function<String, NormalizedWord<SingleKeyword>> singleKeywordParser = keywordStr -> {
                Keyword keyword = KeywordParser.parse(keywordStr);
                checkState(keyword.getAllKeywords().size() == 1);
                checkState(keyword.getAllKeywords().get(0) instanceof SingleKeyword);
                return new NormalizedWord<>((SingleKeyword) keyword.getAllKeywords().get(0));
            };

            List<NormalizedWord<SingleKeyword>> parsedAddedMinusWords = mapList(addedMinusWords, singleKeywordParser);
            Map<Integer, NormalizedWord<SingleKeyword>> parsedAddedMinusWordsToExisting =
                    EntryStream.of(addedMinusWordsToExisting)
                            .mapValues(singleKeywordParser)
                            .toMap();

            unglueResult = new UnglueResult(index, parsedAddedMinusWords, parsedAddedMinusWordsToExisting);
        }

        public UnglueResult getUnglueResult() {
            return unglueResult;
        }

        @Override
        public String toString() {
            return String.format("%s: to this: %s; to existing: %s",
                    unglueResult.getIndex(),
                    unglueResult.getNormalizedAddedMinusWords(),
                    unglueResult.getNormalizedAddedMinusWordsToExistingMap());
        }
    }
}
