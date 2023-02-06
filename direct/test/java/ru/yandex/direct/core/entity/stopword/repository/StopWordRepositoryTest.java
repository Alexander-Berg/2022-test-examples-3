package ru.yandex.direct.core.entity.stopword.repository;


import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class StopWordRepositoryTest {

    private static final Set<String> EXAMPLE_STOP_WORDS_1;
    private static final Set<String> EXAMPLE_STOP_WORDS_2;

    static {
        Collection<String> words1 = asList(
                "один", "одного", "для",
                "такой", "такого", "такому",
                "вот",
                "только",
                "еще",
                "нашего",
                "да",
                "о",
                "so",
                "we",
                "they",
                "what",
                "would",
                "any",
                "which"
        );
        EXAMPLE_STOP_WORDS_1 = Collections.unmodifiableSet(new HashSet<>(words1));

        Collection<String> words2 = asList(
                "can",
                "but",
                "by",
                "at",
                "an",
                "will",
                "no",
                "all",
                "was",
                "do",
                "there"
        );
        EXAMPLE_STOP_WORDS_2 = Collections.unmodifiableSet(new HashSet<>(words2));
    }

    @Autowired
    private DslContextProvider dslContextProvider;

    private StopWordRepository stopWordRepository;

    @Before
    public void before() {
        // ручное создание бина, иначе автоварийтся мок из {@code CoreTestingConfiguration}
        stopWordRepository = new StopWordRepository(dslContextProvider);
    }

    @Test
    public void getStopWords_NotEmptyCollection() {
        stopWordRepository.replaceStopWords(EXAMPLE_STOP_WORDS_1);
        assertStopWords(EXAMPLE_STOP_WORDS_1);
    }

    @Test
    public void replaceStopWords_EmptyCollection_GetEmptySet() {
        stopWordRepository.replaceStopWords(emptySet());
        assertStopWords(emptySet());
    }

    @Test
    public void replaceStopWords_Null_GetEmptySet() {
        stopWordRepository.replaceStopWords(null);
        assertStopWords(emptySet());
    }

    @Test
    public void replaceStopWords_EmptyToNotEmptyCollection_GetNewCollection() {
        insertStopWords(emptySet());

        stopWordRepository.replaceStopWords(EXAMPLE_STOP_WORDS_1);
        assertStopWords(EXAMPLE_STOP_WORDS_1);
    }

    @Test
    public void replaceStopWords_NullToNotEmptyCollection_GetNewCollection() {
        stopWordRepository.replaceStopWords(null);

        Set<String> stopWords = stopWordRepository.getStopWords();
        assumeThat(stopWords, empty());

        stopWordRepository.replaceStopWords(EXAMPLE_STOP_WORDS_1);
        assertStopWords(EXAMPLE_STOP_WORDS_1);
    }

    @Test
    public void replaceStopWords_OldToNewWords_GetNewCollection() {
        insertStopWords(EXAMPLE_STOP_WORDS_1);

        stopWordRepository.replaceStopWords(EXAMPLE_STOP_WORDS_2);
        assertStopWords(EXAMPLE_STOP_WORDS_2);
    }


    private void insertStopWords(Set<String> stopWords) {
        stopWordRepository.replaceStopWords(stopWords);
        Set<String> initialStopWords = stopWordRepository.getStopWords();
        assumeThat(initialStopWords, equalTo(stopWords));
    }

    private void assertStopWords(Set<String> expectedStopWords) {
        Set<String> resultStopWords = stopWordRepository.getStopWords();
        assertThat(resultStopWords, equalTo(expectedStopWords));
    }
}

