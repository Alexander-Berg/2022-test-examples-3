package ru.yandex.direct.api.v5.entity.keywordsresearch.delegate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.yandex.direct.api.v5.keywordsresearch.DeduplicateOperationEnum;
import com.yandex.direct.api.v5.keywordsresearch.DeduplicateRequest;
import com.yandex.direct.api.v5.keywordsresearch.DeduplicateRequestItem;
import com.yandex.direct.api.v5.keywordsresearch.DeduplicateResponseAddItem;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.keyword.processing.KeywordNormalizer;
import ru.yandex.direct.core.entity.keyword.processing.unglue.KeywordUngluer;
import ru.yandex.direct.core.entity.stopword.service.StopWordService;
import ru.yandex.direct.libs.keywordutils.helper.SingleKeywordsCache;
import ru.yandex.direct.libs.keywordutils.inclusion.model.KeywordWithLemmasFactory;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;

@Api5Test
@RunWith(JUnitParamsRunner.class)
public class DeduplicateMinusWordsTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private DeduplicateDelegate delegate;

    @Autowired
    private ResultConverter resultConverter;

    @Autowired
    private KeywordNormalizer keywordNormalizer;

    @Autowired
    private KeywordUngluer keywordUngluer;

    @Before
    public void before() {
        var keywordFactory = new KeywordWithLemmasFactory();
        var singleKeywordsCache = new SingleKeywordsCache();
        delegate = new DeduplicateDelegate(mock(ApiAuthenticationSource.class),
                resultConverter,
                keywordNormalizer,
                keywordUngluer,
                mock(StopWordService.class),
                keywordFactory,
                singleKeywordsCache);
    }

    public Object[][] deduplicateMinusWordsTest_params() {
        return new Object[][]{
                {"new, new with duplicate",
                        keyword("лучшие лечебные санатории россии -база", null),
                        keyword("лучшие лечебные санатории россии -база -базой", null),
                        Set.of("лучшие лечебные санатории россии -база"),
                        Set.of(),
                        Set.of(),
                },
                {"new with duplicate, new",
                        keyword("лучшие лечебные санатории россии -база -базой", null),
                        keyword("лучшие лечебные санатории россии -база", null),
                        Set.of("лучшие лечебные санатории россии -база"),
                        Set.of(),
                        Set.of(),
                },
                {"new, old with duplicate",
                        keyword("лучшие лечебные санатории россии -база", null),
                        keyword("лучшие лечебные санатории россии -база -базой", 1L),
                        Set.of(),
                        Set.of(Pair.of("лучшие лечебные санатории россии -база", 1L)),
                        Set.of(),
                },
                {"old with duplicate, new",
                        keyword("лучшие лечебные санатории россии -база -базой", 1L),
                        keyword("лучшие лечебные санатории россии -база", null),
                        Set.of(),
                        Set.of(Pair.of("лучшие лечебные санатории россии -база", 1L)),
                        Set.of(),
                },
                {"old, new with duplicate",
                        keyword("лучшие лечебные санатории россии -база", 1L),
                        keyword("лучшие лечебные санатории россии -база -базой", null),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                },
                {"new with duplicate, old",
                        keyword("лучшие лечебные санатории россии -база -базой", null),
                        keyword("лучшие лечебные санатории россии -база", 1L),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                },
                {"old small id, old big id with duplicate",
                        keyword("лучшие лечебные санатории россии -база", 1L),
                        keyword("лучшие лечебные санатории россии -база -базой", 2L),
                        Set.of(),
                        Set.of(),
                        Set.of(2L),
                },
                {"old big id with duplicate, old small id",
                        keyword("лучшие лечебные санатории россии -база -базой", 2L),
                        keyword("лучшие лечебные санатории россии -база", 1L),
                        Set.of(),
                        Set.of(Pair.of("лучшие лечебные санатории россии -база", 2L)),
                        Set.of(1L)
                },
                {"old big id, old small id with duplicate",
                        keyword("лучшие лечебные санатории россии -база", 2L),
                        keyword("лучшие лечебные санатории россии -база -базой", 1L),
                        Set.of(),
                        Set.of(),
                        Set.of(1L),
                },
                {"old small id with duplicate, old big id",
                        keyword("лучшие лечебные санатории россии -база -базой", 1L),
                        keyword("лучшие лечебные санатории россии -база", 2L),
                        Set.of(),
                        Set.of(Pair.of("лучшие лечебные санатории россии -база", 1L)),
                        Set.of(2L)
                },
        };
    }

    @Test
    @Parameters(method = "deduplicateMinusWordsTest_params")
    public void mergeDuplicatesMinusWordsTest(String description,
                                              DeduplicateRequestItem item1,
                                              DeduplicateRequestItem item2,
                                              Set<String> setAdd,
                                              Set<Pair<String, Long>> setUpdate,
                                              Set<Long> setDelete) {
        var keywords = List.of(item1, item2);
        var request = delegate.convertRequest(new DeduplicateRequest()
                .withKeywords(keywords)
                .withOperation(DeduplicateOperationEnum.MERGE_DUPLICATES));

        var result = delegate.processRequestWithList(request);
        var result2 = delegate.convertResponse(result);

        assertSoftly(soft -> {
            soft.assertThat(StreamEx.of(result2.getAdd()).map(DeduplicateResponseAddItem::getKeyword).toSet())
                    .as("add")
                    .isEqualTo(setAdd);
            soft.assertThat(StreamEx.of(result2.getUpdate()).map(item -> Pair.of(item.getKeyword(), item.getId())).toSet())
                    .as("update")
                    .isEqualTo(setUpdate);
            soft.assertThat(Optional.ofNullable(result2.getDelete()).map(ids -> Set.copyOf(ids.getIds())).orElse(Set.of()))
                    .as("delete")
                    .isEqualTo(setDelete);
        });
    }

    @Test
    public void mergeDuplicates_similarMinusWordsInOnePhrase_areMerged() {
        var keywords = List.of(keyword("фраза -слово -слово", null));
        var request = delegate.convertRequest(new DeduplicateRequest()
                .withKeywords(keywords)
                .withOperation(DeduplicateOperationEnum.MERGE_DUPLICATES));

        var result = delegate.processRequestWithList(request);
        var result2 = delegate.convertResponse(result);

        assertSoftly(soft -> {
            soft.assertThat(StreamEx.of(result2.getAdd()).map(DeduplicateResponseAddItem::getKeyword).toSet())
                    .as("add")
                    .isEqualTo(Set.of("фраза -слово"));
            soft.assertThat(StreamEx.of(result2.getUpdate()).map(item -> Pair.of(item.getKeyword(), item.getId())).toSet())
                    .as("update")
                    .isEqualTo(Set.of());
            soft.assertThat(Optional.ofNullable(result2.getDelete()).map(ids -> Set.copyOf(ids.getIds())).orElse(Set.of()))
                    .as("delete")
                    .isEqualTo(Set.of());
        });
    }

    @Test
    public void mergeDuplicates_keywordAndMinusWordMatch_correctKeywordIsChosed() {
        var keywords = List.of(keyword("горшки +для цветов черного цвета", 100L),
                keyword("черные горшки +для цветов -купить -цвета", null));
        var request = delegate.convertRequest(new DeduplicateRequest()
                .withKeywords(keywords)
                .withOperation(DeduplicateOperationEnum.MERGE_DUPLICATES));

        var result = delegate.processRequestWithList(request);
        var result2 = delegate.convertResponse(result);

        assertSoftly(soft -> {
            soft.assertThat(StreamEx.of(result2.getAdd()).map(DeduplicateResponseAddItem::getKeyword).toSet())
                    .as("add")
                    .isEqualTo(Set.of("черные горшки +для цветов -купить -цвета"));
            soft.assertThat(StreamEx.of(result2.getUpdate()).map(item -> Pair.of(item.getKeyword(), item.getId())).toSet())
                    .as("update")
                    .isEqualTo(Set.of());
            soft.assertThat(Optional.ofNullable(result2.getDelete()).map(ids -> Set.copyOf(ids.getIds())).orElse(Set.of()))
                    .as("delete")
                    .isEqualTo(Set.of(100L));
        });
    }

    private static DeduplicateRequestItem keyword(String keyword, Long id) {
        return new DeduplicateRequestItem().withKeyword(keyword).withId(id);
    }
}
