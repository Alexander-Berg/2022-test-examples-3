package ru.yandex.direct.grid.processing.service.campaign.uc;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.Request;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.moderationdiag.service.ModerationDiagService;
import ru.yandex.direct.core.entity.moderationreason.service.ModerationReasonService;
import ru.yandex.direct.core.entity.moderationreason.service.ModerationReasonTextService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.i18n.Language;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class GdUcModReasonsServiceTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private AsyncHttpClient asyncHttpClient;

    private GdUcModReasonsService gdUcModReasonsService;

    @Before
    public void before() {
        Mockito.reset();
        AsyncHttpClient asyncHttpClientLocal  = mock(AsyncHttpClient.class);
        when(asyncHttpClientLocal.getConfig()).thenReturn(asyncHttpClient.getConfig());
        BoundRequestBuilder requestBuilder = mock(BoundRequestBuilder.class);
        when(requestBuilder.setRequestTimeout(anyInt())).thenCallRealMethod();
        when(asyncHttpClientLocal.prepareRequest(any(Request.class)))
                .thenAnswer(invocation -> asyncHttpClient.prepareRequest((Request) invocation.getArguments()[0]));
        ModerationReasonTextService moderationReasonTextService = new ModerationReasonTextService(
                "http://dsljlksadvlkznblkcv.com", asyncHttpClientLocal, mock(CreativeRepository.class),
                mock(ShardHelper.class), mock(ModerationDiagService.class)
        );
        gdUcModReasonsService = new GdUcModReasonsService(
                mock(ModerationReasonService.class), moderationReasonTextService, mock(TranslationService.class));
    }

    @Test
    @Parameters(method = "tokens")
    @TestCaseName("converting {0}")
    public void convertTokenTest(String tokenInDb, String convertedToken) {
        assertThat(GdUcModReasonsService.convertToken(tokenInDb), is(convertedToken));
    }

    Iterable<Object[]> tokens() {
        return asList(new Object[][]{
                {"TM Dhref", "tmDhref"},
                {" Russian law QL  ", "russianLawQl"},
                {"Kazakh law", "kazakhLaw"},
                {"TM", "tm"},
                {" TM", "tm"},
        });
    }

    @Test
    @Parameters(method = "allLanguages")
    @TestCaseName("retrieve tooltips lang {0}")
    public void retrieveAsTooltipsSmokeTest(Language lang) {
        //тест просто проверяет, что код не сломался по дороге
        //что-то конкретное сложно: куча приватных методов, запрос строится в ModerationReasonTextService::request явным построением сроки
        gdUcModReasonsService.retrieveAsTooltips(Set.of(1L), lang);
    }

    Iterable<Object[]> allLanguages() {
        return Arrays.stream(Language.values())
                .map(t-> Arrays.asList(t).toArray())
                .collect(Collectors.toList());
    }
}
