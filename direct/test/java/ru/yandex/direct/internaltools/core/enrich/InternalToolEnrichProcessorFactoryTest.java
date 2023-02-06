package ru.yandex.direct.internaltools.core.enrich;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.internaltools.core.annotations.enrich.EnrichFetcher;
import ru.yandex.direct.internaltools.core.annotations.output.Enrich;
import ru.yandex.direct.internaltools.core.container.InternalToolDetailsData;
import ru.yandex.direct.internaltools.core.enums.InternalToolDetailKey;
import ru.yandex.direct.internaltools.core.exception.InternalToolInitialisationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.internaltools.core.bootstrap.InternalToolEnrichProcessorFactoryBootstrap.factoryFromFetchers;

public class InternalToolEnrichProcessorFactoryTest {
    public static class TestClass {
        @Enrich(value = InternalToolDetailKey.AGENCY_LOGIN)
        public String login;
        @Enrich(value = InternalToolDetailKey.CAMPAIGN_ID)
        public Long campaignId;
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Mock
    private InternalToolEnrichDataFetcher fetcher;
    private InternalToolEnrichProcessorFactory factory;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        factory = new InternalToolEnrichProcessorFactory(
                Collections.singletonMap(InternalToolDetailKey.AGENCY_LOGIN, fetcher));
    }

    @Test
    public void testForFieldHasKey() throws NoSuchFieldException {
        InternalToolEnrichProcessor processor =
                factory.forField(TestClass.class.getDeclaredField("login"), TestClass.class);

        assertThat(processor)
                .isNotNull();

        TestClass tc = new TestClass();
        tc.login = "some_login";

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(processor.getCategory())
                .isEqualTo(InternalToolDetailKey.AGENCY_LOGIN.getCategory());
        soft.assertThat(processor.getFieldName())
                .isEqualTo("login");
        soft.assertThat(processor.getExtractor().getValue(tc))
                .isEqualTo("some_login");
        soft.assertThat(processor.getFetcher())
                .isEqualTo(fetcher);
        soft.assertAll();
    }

    @Test
    public void testProcessor() throws NoSuchFieldException {
        InternalToolEnrichProcessor processor =
                factory.forField(TestClass.class.getDeclaredField("login"), TestClass.class);

        assertThat(processor)
                .isNotNull();

        TestClass tc = new TestClass();
        tc.login = "some_login";
        TestClass tc2 = new TestClass();
        tc.login = "some_login";
        TestClass tc3 = new TestClass();
        tc.login = "another_login";

        Map<Object, Object> result = ImmutableMap.builder()
                .put("some_login", "string1")
                .put("another_login", "string2")
                .build();
        doReturn(result)
                .when(fetcher).fetch(anyCollection());

        InternalToolDetailsData data = processor.fetchDetails(Arrays.asList(tc, tc2, tc3));
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(data.getCategory())
                .isEqualTo(InternalToolDetailKey.AGENCY_LOGIN.getCategory());
        soft.assertThat(data.getData())
                .isEqualTo(result);
        soft.assertAll();
    }

    @Test
    public void testForFieldHasNoKey() throws NoSuchFieldException {
        InternalToolEnrichProcessor processor =
                factory.forField(TestClass.class.getDeclaredField("campaignId"), TestClass.class);

        assertThat(processor)
                .isNull();
    }

    private EnrichFetcher getKeyProvider(InternalToolDetailKey key) {
        EnrichFetcher annotation = mock(EnrichFetcher.class);
        when(annotation.value()).thenReturn(key);
        return annotation;
    }

    @Test
    public void testFromFetchers() {
        @EnrichFetcher(InternalToolDetailKey.BANNER_ID)
        class FetcherOne implements InternalToolEnrichDataFetcher {
            @Override
            public Map fetch(Collection keys) {
                return null;
            }
        }

        @EnrichFetcher(InternalToolDetailKey.LOGIN)
        class FetcherTwo implements InternalToolEnrichDataFetcher {
            @Override
            public Map fetch(Collection keys) {
                return null;
            }
        }

        FetcherOne fetcherOne = new FetcherOne();
        FetcherTwo fetcherTwo = new FetcherTwo();
        InternalToolEnrichProcessorFactory factory = factoryFromFetchers(
                Arrays.asList(fetcherOne, fetcherTwo));

        assertThat(factory.getKeysToFetchers())
                .containsOnly(
                        entry(InternalToolDetailKey.BANNER_ID, fetcherOne),
                        entry(InternalToolDetailKey.LOGIN, fetcherTwo)
                );
    }

    @Test
    public void testFromFetchersKeysCollapse() {
        @EnrichFetcher(InternalToolDetailKey.BANNER_ID)
        class FetcherOne implements InternalToolEnrichDataFetcher {
            @Override
            public Map fetch(Collection keys) {
                return null;
            }
        }

        @EnrichFetcher(InternalToolDetailKey.BANNER_ID)
        class FetcherTwo implements InternalToolEnrichDataFetcher {
            @Override
            public Map fetch(Collection keys) {
                return null;
            }
        }

        FetcherOne fetcherOne = new FetcherOne();
        FetcherTwo fetcherTwo = new FetcherTwo();
        exception.expect(InternalToolInitialisationException.class);
        factoryFromFetchers(Arrays.asList(fetcherOne, fetcherTwo));
    }
}
