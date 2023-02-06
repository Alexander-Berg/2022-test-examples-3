package ru.yandex.direct.jobs.urlmonitoring;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Provider;

import one.util.streamex.EntryStream;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.urlmonitoring.service.UrlMonitoringService;
import ru.yandex.direct.jobs.urlmonitoring.model.UrlMonitoringEvent;
import ru.yandex.direct.solomon.SolomonPushClient;
import ru.yandex.kikimr.persqueue.consumer.SyncConsumer;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.jobs.urlmonitoring.UrlMonitoringEventReceivingJob.ALIVE_DOMAINS_KEY;
import static ru.yandex.direct.jobs.urlmonitoring.UrlMonitoringEventReceivingJob.DEAD_DOMAINS_KEY;

@ParametersAreNonnullByDefault
class UrlMonitoringEventReceivingJobApplyUnitTest {

    @Spy
    @InjectMocks
    private UrlMonitoringService urlMonitoringService;

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Mock
    private Provider<SyncConsumer> syncConsumerProvider;
    @Mock
    private SolomonPushClient solomonPushClient;

    private UrlMonitoringEventReceivingJob job;

    static Collection<Object[]> params() {
        return asList(new Object[][]{
                {emptyList(), emptyList(), emptyList()},
                {asList(aliveToDeadEvent("aaaa"), deadToAliveEvent("blabla")), emptyList(), emptyList()},
                {/* считаем "хостингом", т.е. www - значащий и не отрезаем */
                        singletonList(aliveToDeadEvent("http://www.ya.ru?yclid=7710547526903176844")),
                        singletonList(Pair.of("http", "www.ya.ru")), emptyList(),},
                {singletonList(aliveToDeadEvent("http://ya.ru/")), singletonList(Pair.of("http", "ya.ru")), emptyList(),},
                {singletonList(deadToAliveEvent("http://www.ya.ru")), emptyList(), singletonList(Pair.of("http", "www.ya.ru")),},
                {singletonList(offMonitoringToDeadEvent("http://www.ya.ru")), singletonList(Pair.of("http", "www.ya.ru")), emptyList()},
                {singletonList(deadToAliveEvent("https://ya.ru")), emptyList(), singletonList(Pair.of("https", "ya.ru")),},
                {singletonList(aliveToDeadEvent("http://www.supersite.ru/?yclid=7710547526903176844")),
                        singletonList(Pair.of("http", "www.supersite.ru")), emptyList(),},
                {singletonList(deadToAliveEvent("http://www.supersite.ru")), emptyList(), singletonList(Pair.of("http", "www.supersite.ru")),},
                {singletonList(aliveToDeadEvent("http://supersite.ru/")), singletonList(Pair.of("http", "supersite.ru")), emptyList(),},
                {singletonList(deadToAliveEvent("http://supersite.ru/#")), emptyList(), singletonList(Pair.of("http", "supersite.ru")),},
                {asList(deadToAliveEvent("http://ABCDE.com"),
                        aliveToDeadEvent("http://fFfF.net/sdfgssdfg?gdg=gg")),
                        singletonList(Pair.of("http", "fFfF.net")), singletonList(Pair.of("http", "ABCDE.com")),},
                {asList(
                        deadToAliveEvent("http://антикортехснаб-дв.рф#"),
                        aliveToDeadEvent("http://ABC.добрыйдомъ.рф")),
                        asList(Pair.of("http", "ABC.xn--90afbtqidn8gf.xn--p1ai"), Pair.of("http", "ABC.добрыйдомъ.рф")),
                        asList(Pair.of("http", "xn----7sbaegldxs2adk0apje6c.xn--p1ai"), Pair.of("http", "антикортехснаб-дв.рф")),},
                {asList(
                        deadToAliveEvent("http://xn----7sbaegldxs2adk0apje6c.xn--p1ai"),
                        aliveToDeadEvent("http://xn--90afbtqidn8gf.xn--p1ai")),
                        asList(Pair.of("http", "xn--90afbtqidn8gf.xn--p1ai"), Pair.of("http", "добрыйдомъ.рф")),
                        asList(Pair.of("http", "xn----7sbaegldxs2adk0apje6c.xn--p1ai"), Pair.of("http", "антикортехснаб-дв.рф")),},
                {/* URL-ы без схемы */
                        asList(
                                deadToAliveEvent("антикортехснаб-дв.рф"),
                                aliveToDeadEvent("xn--90afbtqidn8gf.xn--p1ai")),
                        emptyList(), emptyList(),},
                {/* кейс с дублирующимися данными - все должно попадать в недоступные */
                        asList(
                                deadToAliveEvent("http://антикортехснаб-дв.рф"),
                                aliveToDeadEvent("http://xn----7sbaegldxs2adk0apje6c.xn--p1ai").withUpdated(Long.MAX_VALUE)),
                        asList(Pair.of("http", "xn----7sbaegldxs2adk0apje6c.xn--p1ai"), Pair.of("http", "антикортехснаб-дв.рф")), emptyList(),},
                {/* и даже такое дублирование (один с www, другой без) - без www должно попадать в недоступные, с www в доступные */
                        asList(
                                deadToAliveEvent("http://www.антикортехснаб-дв.рф"),
                                aliveToDeadEvent("http://xn----7sbaegldxs2adk0apje6c.xn--p1ai")),
                        asList(Pair.of("http", "xn----7sbaegldxs2adk0apje6c.xn--p1ai"), Pair.of("http", "антикортехснаб-дв.рф")),
                        asList(Pair.of("http", "www.xn----7sbaegldxs2adk0apje6c.xn--p1ai"), Pair.of("http", "www.антикортехснаб-дв.рф")),},
                {/* и даже такое дублирование (один с www, другой без) - с www должно попадать в недоступные, без www в дооступные */
                        asList(
                                deadToAliveEvent("http://антикортехснаб-дв.рф"),
                                aliveToDeadEvent("http://www.xn----7sbaegldxs2adk0apje6c.xn--p1ai")),
                        asList(Pair.of("http", "www.xn----7sbaegldxs2adk0apje6c.xn--p1ai"), Pair.of("http", "www.антикортехснаб-дв.рф")),
                        asList(Pair.of("http", "xn----7sbaegldxs2adk0apje6c.xn--p1ai"), Pair.of("http", "антикортехснаб-дв.рф"))}});
    }

    private static UrlMonitoringEvent aliveToDeadEvent(String s) {
        return event(s).withNewStatus(0).withOldStatus(1);
    }

    private static UrlMonitoringEvent deadToAliveEvent(String s) {
        return event(s).withNewStatus(1).withOldStatus(0);
    }

    private static UrlMonitoringEvent offMonitoringToDeadEvent(String s) {
        return event(s).withNewStatus(0).withOldStatus(4);
    }

    private static UrlMonitoringEvent event(String url) {
        return new UrlMonitoringEvent().withUrl(url).withUpdated(Long.MIN_VALUE);
    }

    @BeforeEach
    void setUp() {
        initMocks(this);
        this.job = new UrlMonitoringEventReceivingJob(ppcPropertiesSupport, syncConsumerProvider, urlMonitoringService, solomonPushClient);
        when(urlMonitoringService.notifyUsersOnDomainsStateChange(anyMap(), anyBoolean())).thenReturn(emptyMap());
    }

    @ParameterizedTest(name = "события из LB: {0}, потухшие домены: {1}, ожившие домены: {2}")
    @MethodSource("params")
    void testUrlMonitoringEventFilter(List<UrlMonitoringEvent> events, List<Pair<String, String>> deadDomains, List<Pair<String, String>> aliveDomains) {
        this.job.applyEvents(events);
        Map<String, Set<Pair<String, String>>> domainsByState =
                EntryStream.of(
                        DEAD_DOMAINS_KEY, (Set<Pair<String, String>>) new HashSet<>(deadDomains),
                        ALIVE_DOMAINS_KEY, new HashSet<>(aliveDomains))
                        .filterValues(domains -> !domains.isEmpty())
                        .toMap();
        verify(urlMonitoringService).notifyUsersOnDomainsStateChange(eq(domainsByState), anyBoolean());
    }

}
