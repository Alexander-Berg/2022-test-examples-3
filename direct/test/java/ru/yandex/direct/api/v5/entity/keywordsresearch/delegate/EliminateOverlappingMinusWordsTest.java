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
import junitparams.naming.TestCaseName;
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
import ru.yandex.direct.libs.keywordutils.helper.ParseKeywordCache;
import ru.yandex.direct.libs.keywordutils.helper.SingleKeywordsCache;
import ru.yandex.direct.libs.keywordutils.inclusion.model.KeywordWithLemmasFactory;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;

@Api5Test
@RunWith(JUnitParamsRunner.class)
public class EliminateOverlappingMinusWordsTest {
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
        var singleKeywordsCache = new SingleKeywordsCache();
        var parseKeywordCache = new ParseKeywordCache();
        var keywordFactory = new KeywordWithLemmasFactory(singleKeywordsCache, parseKeywordCache);
        delegate = new DeduplicateDelegate(mock(ApiAuthenticationSource.class),
                resultConverter,
                keywordNormalizer,
                keywordUngluer,
                mock(StopWordService.class),
                keywordFactory,
                singleKeywordsCache);
    }

    public Object[][] eliminateOverlappingMinusWordsTest_params() {
        return new Object[][]{
                {"similar, but not equal minus and key words",
                        List.of(keyword("тест с базой", 58L, 0L),
                                keyword("тест -база", 57L, 1L)),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                },
                // non stop word
                {"key word matched with minus word",
                        List.of(keyword("субсидиарная ответственность -суть", 57L),
                                keyword("суть субсидиарной ответственности", 58L)),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                },
                {"fixed key word matched with minus word",
                        List.of(keyword("субсидиарная ответственность -суть", 57L),
                                keyword("!суть субсидиарной ответственности", 58L)),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                },
                {"plus key word matched with minus word",
                        List.of(keyword("субсидиарная ответственность -суть", 57L),
                                keyword("суть субсидиарной ответственности", 58L)),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                },
                {"key word matched with fixed minus word",
                        List.of(keyword("субсидиарная ответственность -!суть", 57L),
                                keyword("суть субсидиарной ответственности", 58L)),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                },
                {"fixed key word matched with fixed minus word",
                        List.of(keyword("субсидиарная ответственность -!суть", 57L),
                                keyword("!суть субсидиарной ответственности", 58L)),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                },
                {"plus key word matched with fixed minus word",
                        List.of(keyword("субсидиарная ответственность -!суть", 57L),
                                keyword("+суть субсидиарной ответственности", 58L)),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                },
                // stop word
                {"stop key word matched with stop minus word",
                        List.of(keyword("сумки этого сезона 2016", 57L),
                                keyword("сумки сезон 2016 -этого", 58L)),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                },
                {"fixed stop key word matched with stop minus word",
                        List.of(keyword("сумки !этого сезона 2016", 57L),
                                keyword("сумки сезон 2016 -этого", 58L)),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                },
                {"plus stop key word matched with stop minus word",
                        List.of(keyword("сумки этого сезона 2016", 57L),
                                keyword("сумки сезон 2016 -этого", 58L)),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                },
                {"stop key word matched with fixed stop minus word",
                        List.of(keyword("сумки этого сезона 2016", 57L),
                                keyword("сумки сезон 2016 -!этого", 58L)),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                },
                {"fixed stop key word matched with fixed stop minus word",
                        List.of(keyword("сумки !этого сезона 2016", 57L),
                                keyword("сумки сезон 2016 -!этого", 58L)),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                },
                {"plus stop key word matched with fixed stop minus word",
                        List.of(keyword("сумки +этого сезона 2016", 57L),
                                keyword("сумки сезон 2016 -!этого", 58L)),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                },
                {"stop key word matched with stop minus word",
                        List.of(keyword("купить 2 к квартиру -ком", 57L),
                                keyword("купить 2 ком квартиру", 58L)),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                },
                // minus words with the same normalized form
                {"two fixed overlapping key words with the same normalized form",
                        List.of(keyword("программист работа", 0L),
                                keyword("!студент работа программистом", 1L),
                                keyword("программист работа !студенту", 2L)),
                        Set.of(),
                        Set.of(Pair.of("программист работа -!студент -!студенту", 0L)),
                        Set.of(),
                },
                {"fixed and raw overlapping key words with the same normalized form",
                        List.of(keyword("программист работа", 0L),
                                keyword("работа программистом для студента", 1L),
                                keyword("программист работа !студенту", 2L)),
                        Set.of(),
                        Set.of(Pair.of("программист работа -студента", 0L)),
                        Set.of(),
                },
                {"two raw overlapping key words with the same normalized form",
                        List.of(keyword("программист работа", 0L),
                                keyword("работа программистом для студента", 1L),
                                keyword("программист работа студенту", 2L)),
                        Set.of(),
                        Set.of(Pair.of("программист работа -студента", 0L)),
                        Set.of(2L),
                }
        };
    }

    @Test
    @Parameters(method = "eliminateOverlappingMinusWordsTest_params")
    @TestCaseName("{0}")
    public void eliminateOverlappingMinusWordsTest(String description,
                                                   List<DeduplicateRequestItem> items,
                                                   Set<String> setAdd,
                                                   Set<Pair<String, Long>> setUpdate,
                                                   Set<Long> setDelete) {
        var request = delegate.convertRequest(new DeduplicateRequest()
                .withKeywords(items)
                .withOperation(DeduplicateOperationEnum.ELIMINATE_OVERLAPPING));

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
    public void eliminateOverlapping_similarMinusWordsInOnePhrase_areMerged() {
        var keywords = List.of(keyword("фраза -база -баз", null));
        var request = delegate.convertRequest(new DeduplicateRequest()
                .withKeywords(keywords)
                .withOperation(DeduplicateOperationEnum.ELIMINATE_OVERLAPPING));

        var result = delegate.processRequestWithList(request);
        var result2 = delegate.convertResponse(result);

        assertSoftly(soft -> {
            soft.assertThat(StreamEx.of(result2.getAdd()).map(DeduplicateResponseAddItem::getKeyword).toSet())
                    .as("add")
                    .isEqualTo(Set.of("фраза -база"));
            soft.assertThat(StreamEx.of(result2.getUpdate()).map(item -> Pair.of(item.getKeyword(), item.getId())).toSet())
                    .as("update")
                    .isEqualTo(Set.of());
            soft.assertThat(Optional.ofNullable(result2.getDelete()).map(ids -> Set.copyOf(ids.getIds())).orElse(Set.of()))
                    .as("delete")
                    .isEqualTo(Set.of());
        });
    }

    private static DeduplicateRequestItem keyword(String keyword, Long id) {
        return new DeduplicateRequestItem().withKeyword(keyword).withId(id);
    }

    private static DeduplicateRequestItem keyword(String keyword, Long id, Long weight) {
        return keyword(keyword, id).withWeight(weight);
    }
}
