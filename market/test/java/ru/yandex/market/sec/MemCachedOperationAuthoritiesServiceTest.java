package ru.yandex.market.sec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.common.cache.memcached.MemCachedServiceConfig;
import ru.yandex.common.cache.memcached.MemCachingService;
import ru.yandex.market.core.config.MemCachedTestConfig;
import ru.yandex.market.security.AuthorityNamesLoader;
import ru.yandex.market.security.core.HttpAllOperationNamesLoader;
import ru.yandex.market.security.core.HttpBatchAuthoritiesLoader;
import ru.yandex.market.security.model.Authority;
import ru.yandex.market.security.model.OperationAuthorities;
import ru.yandex.market.security.model.OperationPermission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link ru.yandex.common.cache.memcached.MemCachedService} для прав из java-sec.
 */
@SpringJUnitConfig(classes = MemCachedTestConfig.class)
@ExtendWith(MockitoExtension.class)
class MemCachedOperationAuthoritiesServiceTest {

    @Autowired
    MemCachingService memCachingService;

    private MemCachedOperationAuthoritiesService instance;

    @Mock
    private HttpBatchAuthoritiesLoader batchAuthoritiesLoader;

    @Mock
    private HttpAllOperationNamesLoader operationNamesLoader;

    @Mock
    private AuthorityNamesLoader authorityNamesLoader;

    @BeforeEach
    void setUp() {
        instance = buildService(2);
    }

    /**
     * Проверить, что memCachedService получает все OperationAuthorities по передаваемым именам.
     */
    @Test
    void getAllOperations() {
        when(operationNamesLoader.loadAllNames("MBI-PARTNER")).thenReturn(allOperationNames());
        when(batchAuthoritiesLoader.loadBatch("MBI-PARTNER", indexAndToolOperationNames()))
                .thenReturn(indexAndToolOperationAuthorities());
        when(batchAuthoritiesLoader.loadBatch("MBI-PARTNER", prefsAndOfertaOperationNames()))
                .thenReturn(prefsAndOfertaOperationAuthorities());

        var actualAllOperationAuthorities = instance.getAllOperationAuthoritiesFromCache();
        var expectedAllOperationAuthorities = allOperationAuthorities();

        assertThat(actualAllOperationAuthorities).isEqualTo(expectedAllOperationAuthorities);
    }

    /**
     * Проверить, что memCachedService возвращает OperationAuthorities через метод load.
     */
    @Test
    void getIndexOperation() {
        when(operationNamesLoader.loadAllNames("MBI-PARTNER"))
                .thenReturn(allOperationNames());
        when(batchAuthoritiesLoader.loadBatch("MBI-PARTNER", indexAndToolOperationNames()))
                .thenReturn(indexAndToolOperationAuthorities());
        when(batchAuthoritiesLoader.loadBatch("MBI-PARTNER", prefsAndOfertaOperationNames()))
                .thenReturn(prefsAndOfertaOperationAuthorities());

        var actualOperationAuthorities = instance.load("MBI-PARTNER", "index");
        var expectedOperationAuthorities = indexOperationAuthorities();

        assertThat(actualOperationAuthorities).isEqualTo(expectedOperationAuthorities);
    }

    /**
     * Проверить, что memCachedService корректно переживает дубли в запросе
     */
    @Test
    void getDuplicateOperations() {
        when(operationNamesLoader.loadAllNames("MBI-PARTNER"))
                .thenReturn(allOperationNames());
        when(batchAuthoritiesLoader.loadBatch("MBI-PARTNER", indexAndToolOperationNames()))
                .thenReturn(indexAndToolOperationAuthorities());
        when(batchAuthoritiesLoader.loadBatch("MBI-PARTNER", prefsAndOfertaOperationNames()))
                .thenReturn(prefsAndOfertaOperationAuthorities());

        var actualOperationAuthorities = instance
                .loadBatch("MBI-PARTNER", List.of("index", "index"));
        var expectedOperationAuthorities = Map.of(
                "index", indexOperationAuthorities());

        assertThat(actualOperationAuthorities).isEqualTo(expectedOperationAuthorities);
    }

    /**
     * Проверить, что memCachedService получает OperationAuthorities только для существующих имен.
     */
    @Test
    void getAllOperationWithNonexistentName() {
        when(operationNamesLoader.loadAllNames("MBI-PARTNER"))
                .thenReturn(List.of("index", "wrong"));
        when(batchAuthoritiesLoader.loadBatch("MBI-PARTNER", List.of("index", "wrong")))
                .thenReturn(Map.of("index", indexOperationAuthorities()));

        var actualAllOperationAuthorities = instance.getAllOperationAuthoritiesFromCache();
        var actualOperationAuthorities = instance.load("MBI-PARTNER", "wrong");

        assertThat(actualAllOperationAuthorities).isEqualTo(Map.of("index", indexOperationAuthorities()));
        assertThat(actualOperationAuthorities).isNull();
    }

    /**
     * Проверить, что memCachedService совершает только нужное число batch запросов.
     */
    @Test
    void checkCountOfBatchRequest() {
        when(operationNamesLoader.loadAllNames("MBI-PARTNER"))
                .thenReturn(allOperationNames());
        when(batchAuthoritiesLoader.loadBatch("MBI-PARTNER", indexAndToolOperationNames()))
                .thenReturn(indexAndToolOperationAuthorities());
        when(batchAuthoritiesLoader.loadBatch("MBI-PARTNER", prefsAndOfertaOperationNames()))
                .thenReturn(prefsAndOfertaOperationAuthorities());

        instance.getAllOperationAuthoritiesFromCache();

        verify(batchAuthoritiesLoader, times(1))
                .loadBatch("MBI-PARTNER", indexAndToolOperationNames());
        verify(batchAuthoritiesLoader, times(1))
                .loadBatch("MBI-PARTNER", prefsAndOfertaOperationNames());
    }

    /**
     * Проверить, что если размер батча больше числа опраций, то будет сделан только 1 запрос
     * и вернутся только существующие операции без дублей.
     */
    @Test
    void checkCountOfBatchRequestWithBigBatchSize() {
        instance = buildService(5);
        when(operationNamesLoader.loadAllNames("MBI-PARTNER")).thenReturn(allOperationNames());
        when(batchAuthoritiesLoader.loadBatch("MBI-PARTNER", allOperationNames())).
                thenReturn(allOperationAuthorities());

        var actualAllOperationAuthorities =
                instance.getAllOperationAuthoritiesFromCache();

        verify(batchAuthoritiesLoader, never()).
                loadBatch("MBI-PARTNER", indexAndToolOperationNames());
        verify(batchAuthoritiesLoader, never()).
                loadBatch("MBI-PARTNER", prefsAndOfertaOperationNames());
        verify(batchAuthoritiesLoader, times(1)).
                loadBatch("MBI-PARTNER", allOperationNames());
        var expectedAllOperationAuthorities = allOperationAuthorities();
        assertThat(actualAllOperationAuthorities).isEqualTo(expectedAllOperationAuthorities);
    }

    private Map<String, OperationAuthorities> allOperationAuthorities() {
        Map<String, OperationAuthorities> operationAuthoritiesMap = new HashMap<>();

        operationAuthoritiesMap.putAll(indexAndToolOperationAuthorities());
        operationAuthoritiesMap.putAll(prefsAndOfertaOperationAuthorities());

        return operationAuthoritiesMap;
    }

    private Map<String, OperationAuthorities> indexAndToolOperationAuthorities() {

        var superAuthority = new Authority("YA_SUPER", "",
                "staticDomainAuthorityChecker");
        superAuthority.setDomain("MBI-PARTNER");

        var toolPermission = new OperationPermission();
        toolPermission.setOperationName("tool");
        toolPermission.setAuthorities(List.of(superAuthority));

        var toolOperationAuthorities = new OperationAuthorities("tool");
        toolOperationAuthorities.setPermissions(List.of(toolPermission));

        return Map.of(
                "index", indexOperationAuthorities(),
                "tool", toolOperationAuthorities
        );
    }

    private OperationAuthorities indexOperationAuthorities() {
        var readerAuthority = new Authority("PARTNER_READER", "",
                "staticDomainAuthorityChecker");
        readerAuthority.setDomain("MBI-PARTNER");

        var indexPermission = new OperationPermission();
        indexPermission.setOperationName("index");
        indexPermission.setAuthorities(List.of(readerAuthority));

        var indexOperationAuthorities = new OperationAuthorities("index");
        indexOperationAuthorities.setPermissions(List.of(indexPermission));

        return indexOperationAuthorities;
    }

    private Map<String, OperationAuthorities> prefsAndOfertaOperationAuthorities() {
        var agencyAuthority = new Authority("AGENCY", "",
                "staticDomainAuthorityChecker");
        agencyAuthority.setDomain("MBI-PARTNER");

        var guestAuthority = new Authority("GUEST", "",
                "staticDomainAuthorityChecker");
        guestAuthority.setDomain("MBI-PARTNER");

        var prefsPermission = new OperationPermission();
        prefsPermission.setOperationName("prefs");
        prefsPermission.setAuthorities(List.of(agencyAuthority));

        var ofertaPermission = new OperationPermission();
        ofertaPermission.setOperationName("oferta");
        ofertaPermission.setAuthorities(List.of(guestAuthority));

        var prefsOperationAuthorities = new OperationAuthorities("prefs");
        prefsOperationAuthorities.setPermissions(List.of(prefsPermission));

        var ofertaOperationAuthorities = new OperationAuthorities("oferta");
        ofertaOperationAuthorities.setPermissions(List.of(ofertaPermission));

        return Map.of(
                "prefs", prefsOperationAuthorities,
                "oferta", ofertaOperationAuthorities
        );
    }


    private List<String> allOperationNames() {
        return List.of("index", "tool", "prefs", "oferta");
    }

    private List<String> indexAndToolOperationNames() {
        return List.of("index", "tool");
    }

    private List<String> prefsAndOfertaOperationNames() {
        return List.of("prefs", "oferta");
    }

    private MemCachedOperationAuthoritiesService buildService(int batchSize) {
        return new MemCachedOperationAuthoritiesService(
                memCachingService,
                new MemCachedServiceConfig(),
                batchAuthoritiesLoader,
                operationNamesLoader,
                authorityNamesLoader,
                "MBI-PARTNER",
                batchSize
        );
    }
}
