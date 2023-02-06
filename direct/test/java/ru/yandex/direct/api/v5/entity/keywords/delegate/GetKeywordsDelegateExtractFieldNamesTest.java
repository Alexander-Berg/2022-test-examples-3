package ru.yandex.direct.api.v5.entity.keywords.delegate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.keywords.GetRequest;
import com.yandex.direct.api.v5.keywords.KeywordFieldEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.entity.keywords.converter.KeywordsGetRequestConverter;
import ru.yandex.direct.api.v5.entity.keywords.converter.KeywordsGetResponseConverter;
import ru.yandex.direct.api.v5.entity.keywords.validation.KeywordsGetRequestValidator;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.common.util.PropertyFilter;
import ru.yandex.direct.core.entity.bids.service.BidService;
import ru.yandex.direct.core.entity.keyword.service.KeywordService;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchService;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class GetKeywordsDelegateExtractFieldNamesTest {

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public GetRequest request;

    @Parameterized.Parameter(2)
    public Set<KeywordFieldEnum> expectedFields;

    private GetKeywordsDelegate delegate;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {
        List<KeywordFieldEnum> keywordFieldNames = asList(KeywordFieldEnum.values());
        List<KeywordFieldEnum> keywordFieldNamesWithDuplicates = new ArrayList<>(keywordFieldNames);
        keywordFieldNamesWithDuplicates.addAll(keywordFieldNames);
        Set<KeywordFieldEnum> expectedAllFieldNames = Arrays.stream(KeywordFieldEnum.values()).collect(toSet());

        return new Object[][]{
                {"with field names", new GetRequest().withFieldNames(keywordFieldNames), expectedAllFieldNames},
                {"with field names with duplicates", new GetRequest().withFieldNames(keywordFieldNamesWithDuplicates),
                        expectedAllFieldNames},
        };
    }

    @Before
    public void prepare() {
        delegate = new GetKeywordsDelegate(mock(ApiAuthenticationSource.class),
                mock(KeywordService.class), mock(RelevanceMatchService.class), mock(BidService.class),
                mock(PropertyFilter.class), mock(KeywordsGetResponseConverter.class),
                mock(KeywordsGetRequestValidator.class), mock(
                KeywordsGetRequestConverter.class));
    }

    @Test
    public void test() {
        assertThat(delegate.extractFieldNames(request)).containsExactlyInAnyOrderElementsOf(expectedFields);
    }
}

