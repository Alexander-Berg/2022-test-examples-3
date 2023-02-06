package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.domain.repository.DomainRepository;
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DomainFilterServiceTest {

    private HostingsHandler hostingsHandler;
    private DomainRepository domainRepository;
    private DomainFilterService domainFilterService;

    @BeforeEach
    void before() {
        hostingsHandler = mock(HostingsHandler.class);
        domainRepository = mock(DomainRepository.class);
        domainFilterService = new DomainFilterService(domainRepository, hostingsHandler);
    }

    @Test
    void test() {
        var domain = "aaaa.bbbb.ccccc.site.ru";
        var hosting = "site.ru";
        var domainWithMirror = "ccccc.site.ru";
        var mirror = "dddd.site.ru";
        when(hostingsHandler.getHosting(eq(domain))).thenReturn(hosting);
        when(hostingsHandler.getHosting(mirror)).thenReturn(mirror);
        when(hostingsHandler.stripWww(mirror)).thenReturn(mirror);

        when(domainRepository.getMainMirrors(argThat(d -> d.contains(domainWithMirror)))).thenReturn(Map.of(domainWithMirror, mirror));

        var expectedDomainsFilters = Map.of(domain, mirror);
        var gotDomainsFilters = domainFilterService.getDomainsFilters(List.of(domain));
        assertThat(gotDomainsFilters).isEqualTo(expectedDomainsFilters);
    }

    @Test
    void yandexMirrorTest() {
        var domain = "aaaa.bbbb.ccccc.site.ru";
        var hosting = "site.ru";
        var domainWithMirror = "ccccc.site.ru";
        var mirror = "yandex.ru";
        when(hostingsHandler.getHosting(eq(domain))).thenReturn(hosting);
        when(hostingsHandler.getHosting(mirror)).thenReturn(mirror);
        when(hostingsHandler.stripWww(mirror)).thenReturn(mirror);

        when(domainRepository.getMainMirrors(argThat(d -> d.contains(domainWithMirror)))).thenReturn(Map.of(domainWithMirror, mirror));

        var expectedDomainsFilters = Map.of(domain, domain);
        var gotDomainsFilters = domainFilterService.getDomainsFilters(List.of(domain));
        assertThat(gotDomainsFilters).isEqualTo(expectedDomainsFilters);
    }

    @Test
    void noMainMirrorsTest() {
        var domain = "aaaa.bbbb.ccccc.site.ru";
        var hosting = "site.ru";
        when(hostingsHandler.getHosting(eq(domain))).thenReturn(hosting);
        when(hostingsHandler.stripWww(hosting)).thenReturn(hosting);
        var expectedDomainsFilters = Map.of(domain, hosting);
        var gotDomainsFilters = domainFilterService.getDomainsFilters(List.of(domain));
        assertThat(gotDomainsFilters).isEqualTo(expectedDomainsFilters);
    }

    @Test
    void noMainMirrors_DomainWithWwwTest() {
        var domain = "www.site.ru";
        var hosting = "www.site.ru";
        var hostingWithoutWWW = "site.ru";
        when(hostingsHandler.getHosting(eq(domain))).thenReturn(hosting);
        when(hostingsHandler.stripWww(hosting)).thenReturn(hostingWithoutWWW);
        var expectedDomainsFilters = Map.of(domain, hostingWithoutWWW);
        var gotDomainsFilters = domainFilterService.getDomainsFilters(List.of(domain));
        assertThat(gotDomainsFilters).isEqualTo(expectedDomainsFilters);
    }

    /**
     * Если зеракло имеет только тот домен, который на уровень меньше, чем хостинг, то фильтром будет сам домен
     */
    @Test
    void mirrorOnlyForSecondLevelDomainTest() {
        var domain = "aaaa.bbbb.ccccc.site.ru";
        var hosting = "ccccc.site.ru";
        when(hostingsHandler.getHosting(eq(domain))).thenReturn(hosting);
        when(hostingsHandler.getHosting(hosting)).thenReturn(hosting);
        when(hostingsHandler.stripWww(hosting)).thenReturn(hosting);
        var expectedDomainsFilters = Map.of(domain, hosting);
        var gotDomainsFilters = domainFilterService.getDomainsFilters(List.of(domain));
        assertThat(gotDomainsFilters).isEqualTo(expectedDomainsFilters);
    }

    @Test
    void hostingHasMirrorTest() {
        var domain = "aaaa.bbbb.ccccc.site.ru";
        var hosting = "ccccc.site.ru";
        var domainWithMirror = "ccccc.site.ru";
        var mirror = "ddd.site.com";
        when(hostingsHandler.getHosting(eq(domain))).thenReturn(hosting);
        when(hostingsHandler.getHosting(mirror)).thenReturn(mirror);
        when(hostingsHandler.stripWww(mirror)).thenReturn(mirror);

        when(domainRepository.getMainMirrors(argThat(d -> d.contains(domainWithMirror)))).thenReturn(Map.of(domainWithMirror, mirror));

        var expectedDomainsFilters = Map.of(domain, mirror);
        var gotDomainsFilters = domainFilterService.getDomainsFilters(List.of(domain));
        assertThat(gotDomainsFilters).isEqualTo(expectedDomainsFilters);
    }

    /**
     * Тест проверяет, что если у полученного зеркала хостинг не равен зеркалу, то фильтром будет хостинг зеркала
     */
    @Test
    void mirrorHasHostingTest() {
        var domain = "aaaa.bbbb.ccccc.site.ru";
        var hosting = "ccccc.site.ru";
        var domainWithMirror = "ccccc.site.ru";
        var mirror = "ddd.site.com";
        var mirrorHosting = "ddd.site.com";
        when(hostingsHandler.getHosting(eq(domain))).thenReturn(hosting);
        when(hostingsHandler.getHosting(mirror)).thenReturn(mirrorHosting);
        when(hostingsHandler.stripWww(mirrorHosting)).thenReturn(mirror);

        when(domainRepository.getMainMirrors(argThat(d -> d.contains(domainWithMirror)))).thenReturn(Map.of(domainWithMirror, mirror));

        var expectedDomainsFilters = Map.of(domain, mirrorHosting);
        var gotDomainsFilters = domainFilterService.getDomainsFilters(List.of(domain));
        assertThat(gotDomainsFilters).isEqualTo(expectedDomainsFilters);
    }

    /**
     * Тест проверяет, что если у полученное зеркало начинается с www, то оно удалится
     */
    @Test
    void stripWwwFromMirrorTest() {
        var domain = "aaaa.bbbb.ccccc.site.ru";
        var hosting = "ccccc.site.ru";
        var domainWithMirror = "ccccc.site.ru";
        var mirror = "www.ddd.site.com";
        var mirrorWithoutWWW = "ddd.site.com";
        when(hostingsHandler.getHosting(eq(domain))).thenReturn(hosting);
        when(hostingsHandler.getHosting(mirror)).thenReturn(mirror);
        when(hostingsHandler.stripWww(mirror)).thenReturn(mirrorWithoutWWW);

        when(domainRepository.getMainMirrors(argThat(d -> d.contains(domainWithMirror)))).thenReturn(Map.of(domainWithMirror, mirror));

        var expectedDomainsFilters = Map.of(domain, mirrorWithoutWWW);
        var gotDomainsFilters = domainFilterService.getDomainsFilters(List.of(domain));
        assertThat(gotDomainsFilters).isEqualTo(expectedDomainsFilters);
    }

    @Test
    void emptyDomainListTest() {
        var gotDomainsFilters = domainFilterService.getDomainsFilters(List.of());
        assertThat(gotDomainsFilters).isEmpty();
    }

    @Test
    void oneDomainHasHostingForHigherLevelThanAnotherTest() {
        var domain1 = "aaaa.bbbb.ccccc.site.ru";
        var domain2 = "ffff.eeee.kkkk.url.ru";
        var hosting1 = "ccccc.site.ru";
        var hosting2 = "kkkk.url.ru";
        var domain1WithMirror = "aaaa.bbbb.ccccc.site.ru";
        var domain2WithMirror = "kkkk.url.ru";
        var mirror1 = "ddd.site.com";
        var mirror2 = "nnnn.site.com";
        when(hostingsHandler.getHosting(eq(domain1))).thenReturn(hosting1);
        when(hostingsHandler.getHosting(eq(domain2))).thenReturn(hosting2);
        when(hostingsHandler.getHosting(mirror1)).thenReturn(mirror1);
        when(hostingsHandler.getHosting(mirror2)).thenReturn(mirror2);
        when(hostingsHandler.stripWww(mirror1)).thenReturn(mirror1);
        when(hostingsHandler.stripWww(mirror2)).thenReturn(mirror2);

        doReturn(Map.of(domain1WithMirror, mirror1)).when(domainRepository)
                .getMainMirrors(argThat(d -> d.contains(domain1WithMirror)));
        doReturn(Map.of(domain2WithMirror, mirror2)).when(domainRepository)
                .getMainMirrors(argThat(d -> d.contains(domain2WithMirror)));


        var expectedDomainsFilters = Map.of(domain1, mirror1, domain2, mirror2);
        var gotDomainsFilters = domainFilterService.getDomainsFilters(List.of(domain1, domain2));
        assertThat(gotDomainsFilters).isEqualTo(expectedDomainsFilters);
    }
}
