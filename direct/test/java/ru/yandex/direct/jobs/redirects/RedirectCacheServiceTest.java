package ru.yandex.direct.jobs.redirects;

import java.util.function.Function;

import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import ru.yandex.direct.common.lettuce.LettuceConnectionProvider;
import ru.yandex.direct.common.lettuce.LettuceExecuteException;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.jobs.redirects.RedirectCacheService.EXPIRATION_TIME;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

class RedirectCacheServiceTest {

    private static final String HREF = "http://domain-before-redirect.ru/some/path/";
    private static final String HREF_WITH_UTM = "http://domain-before-redirect.ru/some/path/?utm_campaign=1";
    private static final String REDIRECT_URL = "http://domain-after-redirect.ru/another/path/";
    private static final String REDIRECT_DOMAIN = "domain-after-redirect.ru";

    private static final String KEY = "redirect-cache-d5fb30ef0fca443f3eaa152e3a86b7e5";
    private static final String JSON_VALUE = String.format(
            "{\"href\":\"%s\",\"redirectUrl\":\"%s\",\"redirectDomain\":\"%s\"}",
            HREF, REDIRECT_URL, REDIRECT_DOMAIN);

    @Mock
    private RedisAdvancedClusterCommands<String, String> commandsLettuce;

    private RedirectCacheService redirectCacheService;

    @BeforeEach
    void before() {
        initMocks(this);
        LettuceConnectionProvider lettuce = mock(LettuceConnectionProvider.class);

        when(lettuce.call(anyString(), any()))
                .then(invocation -> {
                    Function<RedisAdvancedClusterCommands<String, String>, ?> cmd = invocation.getArgument(1);
                    return cmd.apply(commandsLettuce);
                });
        redirectCacheService = new RedirectCacheService(lettuce);
    }

    @Test
    void saveToCache_successful() {
        RedirectCacheRecord recordToCache = new RedirectCacheRecord()
                .withHref(HREF_WITH_UTM)
                .withRedirectUrl(REDIRECT_URL)
                .withRedirectDomain(REDIRECT_DOMAIN);

        redirectCacheService.saveToCache(recordToCache);

        verify(commandsLettuce).setex(eq(KEY), eq(EXPIRATION_TIME.getSeconds()), eq(JSON_VALUE));
    }

    @Test
    void getFromCache_whenKeyExists() {
        doReturn(JSON_VALUE).when(commandsLettuce).get(eq(KEY));

        RedirectCacheRecord recordFromCache = redirectCacheService.getFromCache(HREF_WITH_UTM);
        assumeThat(recordFromCache, notNullValue());

        SoftAssertions.assertSoftly(softly -> {
            // utm параметры вырезается перед чтением/сохранением в кеш
            softly.assertThat(recordFromCache.getHref()).isEqualTo(HREF);
            softly.assertThat(recordFromCache.getRedirectUrl()).isEqualTo(REDIRECT_URL);
            softly.assertThat(recordFromCache.getRedirectDomain()).isEqualTo(REDIRECT_DOMAIN);
        });
    }

    @Test
    void getFromCache_whenKeyDoesNotExist() {
        doReturn(null).when(commandsLettuce).get(eq(KEY));

        RedirectCacheRecord recordFromCache = redirectCacheService.getFromCache(HREF_WITH_UTM);
        MatcherAssert.assertThat(recordFromCache, nullValue());
    }

    @Test
    void getFromCache_whenRedisGetThrowException() {
        doThrow(new LettuceExecuteException()).when(commandsLettuce).get(eq(KEY));

        RedirectCacheRecord recordFromCache = redirectCacheService.getFromCache(HREF_WITH_UTM);
        MatcherAssert.assertThat(recordFromCache, nullValue());
    }
}
