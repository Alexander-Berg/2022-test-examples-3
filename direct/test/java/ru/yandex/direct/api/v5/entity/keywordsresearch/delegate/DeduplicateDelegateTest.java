package ru.yandex.direct.api.v5.entity.keywordsresearch.delegate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.yandex.direct.api.v5.keywordsresearch.DeduplicateRequest;
import com.yandex.direct.api.v5.keywordsresearch.DeduplicateRequestItem;
import com.yandex.direct.api.v5.keywordsresearch.DeduplicateResponse;
import one.util.streamex.IntStreamEx;
import one.util.streamex.LongStreamEx;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.result.ApiResult;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.keyword.processing.KeywordNormalizer;
import ru.yandex.direct.core.entity.keyword.processing.unglue.KeywordUngluer;
import ru.yandex.direct.core.entity.stopword.service.StopWordService;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.libs.keywordutils.helper.SingleKeywordsCache;
import ru.yandex.direct.libs.keywordutils.inclusion.model.KeywordWithLemmasFactory;
import ru.yandex.direct.liveresource.LiveResourceFactory;

import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@Api5Test
@RunWith(SpringRunner.class)
public class DeduplicateDelegateTest {
    private static final String TEST_DATA_URL =
            "classpath:///ru/yandex/direct/api/v5/entity/keywordsresearch/delegate/test_data";

    private DeduplicateDelegate delegate;

    @Autowired
    private Steps steps;

    @Autowired
    private ResultConverter resultConverter;

    @Autowired
    private KeywordNormalizer keywordNormalizer;

    @Autowired
    private KeywordUngluer keywordUngluer;


    @Before
    public void before() {
        delegate = new DeduplicateDelegate(mock(ApiAuthenticationSource.class),
                resultConverter,
                keywordNormalizer,
                keywordUngluer,
                mock(StopWordService.class),
                mock(KeywordWithLemmasFactory.class),
                mock(SingleKeywordsCache.class));
    }

    @Test
    @Ignore("Сейчас тест бежит 20 минут, тестируемый код нуждается в оптимизации, это тест для замеров ее результатов")
    public void volumeTest() {
        DeduplicateRequest request = createRequest();
        time("validateRequest", () -> delegate.validateRequest(request));
        DeduplicateKeywordsRequest converted = time("convertRequest", () -> delegate.convertRequest(request));
        time("validateInternalRequest", () -> delegate.validateInternalElements(converted.getItems()));
        ApiResult<List<ApiResult<DeduplicateKeywordResponse>>> result
                = time("processList", () -> delegate.processRequestWithList(converted));
        DeduplicateResponse response = time("convertResponse", () -> delegate.convertResponse(result));
    }

    private DeduplicateRequest createRequest() {
        return new DeduplicateRequest()
                .withKeywords(LongStreamEx.range(1, 80_000).mapToObj(this::createItem).toList());
    }

    private DeduplicateRequestItem createItem(long id) {
        return new DeduplicateRequestItem()
                .withKeyword(createKeyword())
                .withId(id)
                .withWeight(createWeight());
    }

    private String createKeyword() {
        int plusWordCount = nextInt(0, 4) + 1;
        int minusWordCount = nextInt(0, 20);
        return IntStreamEx.range(plusWordCount).mapToObj(i -> createWord()).joining(" ")
                + " "
                + IntStreamEx.range(0, minusWordCount).mapToObj(i -> "-" + createWord()).joining(" ");
    }

    private String createWord() {
        return RandomStringUtils.random(nextInt(0, 5) + 3, "абвгдежзийклмнопрстуфхцчшщьыъэюя");
    }

    private long createWeight() {
        return nextInt(0, 3) == 0 ? 0L : nextInt(0, 100_000);
    }

    private static <T> T time(String name, Supplier<T> func) {
        long startTime = System.currentTimeMillis();
        T result = func.get();
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println(name + ": " + elapsedTime);
        return result;
    }

    // Тест на скорость выполнения всего цикла операций.
    // Тестовые данные получены объединением нескольких запросов,
    // взятых из логов в кликхаусе. Число кейвордов обрезано до 80000.
    @Test
    @Ignore("Тест долгий, запускать вручную")
    public void volumeTestRealData() {
        DeduplicateRequest request = createRequestFromPreparedFile();
        time("validateRequest", () -> delegate.validateRequest(request));
        DeduplicateKeywordsRequest converted = time("convertRequest", () -> delegate.convertRequest(request));
        time("validateInternalRequest", () -> delegate.validateInternalElements(converted.getItems()));
        ApiResult<List<ApiResult<DeduplicateKeywordResponse>>> result
                = time("processList", () -> delegate.processRequestWithList(converted));
        DeduplicateResponse response = time("convertResponse", () -> delegate.convertResponse(result));
    }

    private DeduplicateRequest createRequestFromPreparedFile() {
        String contents = LiveResourceFactory.get(TEST_DATA_URL).getContent();

        //Разбиваем строку из тестового файла на куски вида "keywords:[{последовательность строк}]"
        final String regex = "Keywords\":\\[(.*?)]";
        final Matcher m = Pattern.compile(regex).matcher(contents);
        List<String> matches = new ArrayList<>();
        while (m.find()) {
            matches.add(m.group(0));
        }

        //Избавляемся в полученных кусках от "keywords:" в начале и объединим
        //слова списков в один большой список
        List<String> keywordsList =
                StreamEx.of(matches).map(t -> t.split("Keywords\":")[1]).toFlatList(this::parseStringList);
        keywordsList = mapList(keywordsList, this::correctString);

        //оставляем только 80000 элементов
        keywordsList = keywordsList.subList(0, 79999);

        long keywordNum = 0;
        List<DeduplicateRequestItem> requestItems = new ArrayList<>();
        for (String curKeyword : keywordsList) {
            requestItems.add(createItemFromString(keywordNum, curKeyword));
            keywordNum += 1;
        }
        return new DeduplicateRequest().withKeywords(requestItems);
    }

    private List<String> parseStringList(String stringList) {
        final String regex = "\",\"";
        List<String> matches = StreamEx.of(stringList.split(regex)).toList();
        matches.set(0, matches.get(0).substring(2));
        String lastMatch = matches.get(matches.size() - 1);
        matches.set(matches.size() - 1, lastMatch.substring(0, lastMatch.length() - 3));
        return matches;
    }

    private String correctString(String unprepearedString) {
        return unprepearedString.replaceAll("\\\\", "");
    }


    private DeduplicateRequestItem createItemFromString(long id, String keyword) {
        return new DeduplicateRequestItem()
                .withKeyword(keyword)
                .withId(id)
                .withWeight(createWeight());
    }
}
