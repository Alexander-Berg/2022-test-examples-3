package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.keyword.container.FixStopwordsResult;
import ru.yandex.direct.core.entity.keyword.model.FixationPhrase;
import ru.yandex.direct.core.entity.keyword.repository.FixationPhraseRepository;
import ru.yandex.direct.core.entity.stopword.service.StopWordService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.repository.TestFixationPhraseRepository;
import ru.yandex.direct.libs.keywordutils.helper.ParseKeywordCache;
import ru.yandex.direct.libs.keywordutils.parser.KeywordParser;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(Parameterized.class)
public class FixStopwordsServiceTest {

    private static boolean dataLoaded = false;

    @Autowired
    private StopWordService stopWordService;

    @Autowired
    private FixationPhraseRepository fixationPhraseRepository;

    private FixStopwordsService fixStopwordsService;

    @Autowired
    private TestFixationPhraseRepository testFixationPhraseRepository;

    @Autowired
    private ParseKeywordCache parseKeywordCache;

    @Parameterized.Parameter
    public String inputKeyword;

    @Parameterized.Parameter(1)
    public String expectedKeyword;

    @Parameterized.Parameter(2)
    public List<Pair<String, String>> expectedFixations;

    @Parameterized.Parameters(name = "{0} -> {1}, {2}")
    public static Object[][] params() {
        return new Object[][]{
//                /*
//                    Фиксация по словарю с учетом регистра и точки в конце слова
//                 */
//                {"мастер на час", "мастер +на час",
//                        singletonList(Pair.of("мастер на час", "мастер +на час"))},
//                {"заказать мастер на час", "заказать мастер +на час",
//                        singletonList(Pair.of("мастер на час", "мастер +на час"))},
//                {"заказать. мастер на час", "заказать. мастер +на час",
//                        singletonList(Pair.of("мастер на час", "мастер +на час"))},
//
//                {"слоны одни из нас", "слоны +одни +из +нас",
//                        singletonList(Pair.of("одни из нас", "+одни +из +нас"))},
//                {"слоны одни +из нас", "слоны +одни +из +нас",
//                        singletonList(Pair.of("одни +из нас", "+одни +из +нас"))},
//                {"слоны одни [из нас]", "слоны одни [из нас]", emptyList()},

                //обработка регистра
                {"Мастер на час", "Мастер +на час",
                        singletonList(Pair.of("Мастер на час", "Мастер +на час"))},
                {"мастер На час", "мастер +На час",
                        singletonList(Pair.of("мастер На час", "мастер +На час"))},
                {"мастер На час без регистрации и СМС.", "мастер +На час без регистрации и СМС.",
                        singletonList(Pair.of("мастер На час", "мастер +На час"))},
                {"мастер На час без [регистрации и СМС.]", "мастер +На час без [регистрации и СМС.]",
                        singletonList(Pair.of("мастер На час", "мастер +На час"))},

                //обработка точки в конце
                {"мастер на. час", "мастер на. час", emptyList()},
                {"слоны одни из. Нас", "слоны одни из. Нас", emptyList()},

                //наличие квадратных скобок не портит фиксацию других фраз
                {"слоны одни [из нас] мастер на час", "слоны одни [из нас] мастер +на час",
                        singletonList(Pair.of("мастер на час", "мастер +на час"))},

                //в кавычках не фиксируются
                {"\"мастер на час\"", "\"мастер на час\"", emptyList()},

                //в квадраных скобках не фиксируются
                {"[мастер на час]", "[мастер на час]", emptyList()},

                //не учитываем + и !
                {"заказать !мастер на час", "заказать !мастер +на час",
                        singletonList(Pair.of("!мастер на час", "!мастер +на час"))},
                {"заказать мастер на +час", "заказать мастер +на +час",
                        singletonList(Pair.of("мастер на +час", "мастер +на +час"))},
                {"на +все бабки", "+на +все бабки",
                        singletonList(Pair.of("на +все", "+на +все"))},
                {"на !все бабки", "+на !все бабки",
                        singletonList(Pair.of("на !все", "+на !все"))},

                //не фиксируется - все и так зафиксировано
                {"+на !все бабки", "+на !все бабки", emptyList()},

                //не срабатывает - несовпадение формы слова
                {"заказать мастера на час", "заказать мастера на час", emptyList()},

                //не срабатывает - не тот порялок слов
                {"заказать на мастер час", "заказать на мастер час", emptyList()},

                //лишнее слово внутри фразы
                {"мастер на один час", "мастер на один час", emptyList()},

                /*
                    Плюс-слова и числа с учетом регистра и точки
                 */
                {"is 1673844", "+is 1673844",
                        singletonList(Pair.of("is 1673844", "+is 1673844"))},
                {"is 16f73844", "is 16f73844", emptyList()},
                {"+на !но 123", "+на !но 123", emptyList()},
                {"на !но 123", "+на !но 123", singletonList(Pair.of("на !но 123", "+на !но 123"))},
                {"на !но 123.092", "+на !но 123.092", singletonList(Pair.of("на !но 123.092", "+на !но 123.092"))},
                {"на !но 1e5", "+на !но 1e5", singletonList(Pair.of("на !но 1e5", "+на !но 1e5"))},
                {"на 1 2", "+на 1 2", singletonList(Pair.of("на 1 2", "+на 1 2"))},

                //обработка регистра
                {"Is 1673844", "+Is 1673844",
                        singletonList(Pair.of("Is 1673844", "+Is 1673844"))},
                {"на !Но 123", "+на !Но 123",
                        singletonList(Pair.of("на !Но 123", "+на !Но 123"))},
                {"на +Но 123", "+на +Но 123",
                        singletonList(Pair.of("на +Но 123", "+на +Но 123"))},
                {"На +Но 123", "+На +Но 123",
                        singletonList(Pair.of("На +Но 123", "+На +Но 123"))},

                //обработка точки в конце слова
                {"is 1673844.", "+is 1673844.",
                        singletonList(Pair.of("is 1673844.", "+is 1673844."))},
                {"1673844. is 12", "1673844. +is 12",
                        singletonList(Pair.of("1673844. is 12", "1673844. +is 12"))},
                {"is. 1673844", "+is. 1673844",
                        singletonList(Pair.of("is. 1673844", "+is. 1673844"))},
                {"Is. 1673844", "+Is. 1673844",
                        singletonList(Pair.of("Is. 1673844", "+Is. 1673844"))},
                {"на +Но. 123", "+на +Но. 123",
                        singletonList(Pair.of("на +Но. 123", "+на +Но. 123"))},
                {"на. +Но 123.", "+на. +Но 123.",
                        singletonList(Pair.of("на. +Но 123.", "+на. +Но 123."))},

                //не фиксируется - все и так зафиксировано
                {"+is 1673844", "+is 1673844", emptyList()},

                //несколько фраз, которые надо зафиксировать
                {"мастер на час на все бабки", "мастер +на час +на +все бабки",
                        asList(Pair.of("мастер на час", "мастер +на час"),
                                Pair.of("на все", "+на +все"))},

                /*
                    Фиксация минус-слов
                 */
                {"купить слона -на", "купить слона -!на", singletonList(Pair.of("-на", "-!на"))},
                {"купить слона -!на", "купить слона -!на", emptyList()},

                //комбо
                {"один +из !нас мастер на час -он -слон", "+один +из !нас мастер +на час -!он -слон",
                        asList(Pair.of("один +из !нас", "+один +из !нас"),
                                Pair.of("мастер на час", "мастер +на час"),
                                Pair.of("-он", "-!он"))},
        };
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        fixStopwordsService = new FixStopwordsService(stopWordService, fixationPhraseRepository, parseKeywordCache);
        if (!dataLoaded) {
            testFixationPhraseRepository.add(asList(new FixationPhrase().withPhrase("одни из нас"),
                    new FixationPhrase().withPhrase("один из нас"),
                    new FixationPhrase().withPhrase("мастер на час"),
                    new FixationPhrase().withPhrase("на все")));
            dataLoaded = true;
        }
    }

    @Test
    public void fixStopwords() throws Exception {
        FixStopwordsResult result = fixStopwordsService.fixStopwords(KeywordParser.parseWithMinuses(inputKeyword));
        assertEquals(expectedKeyword, result.getResult().toString());
        List<Pair<String, String>> fixationsForCheck = mapList(result.getFixations(),
                sf -> Pair.of(sf.getSourceSubstring(), sf.getDestSubstring()));
        assertThat(expectedFixations, containsInAnyOrder(fixationsForCheck.toArray()));
        assertThat(fixationsForCheck, containsInAnyOrder(expectedFixations.toArray()));
    }

}
