package ru.yandex.market.replenishment.autoorder.service;

import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.client.FfwfApiClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
public class CachedSupplyShadowSskusServiceTest extends FunctionalTest {

    @Autowired
    private CachedSupplyShadowSskusService service;

    @Autowired
    private FfwfApiClient ffwfApiClient;

    @Test
    @DbUnitDataSet(before = "CachedSupplyShadowSskusService.testGetAndSave.before.csv",
            after = "CachedSupplyShadowSskusService.testGetAndSave.after.csv")
    public void testGetAndSave() {
        when(ffwfApiClient.getShadowSupplySskus(123L)).thenReturn(Set.of("100", "200", "300"));
        Set<String> sskus = service.getShadowSupplySskus(123L);
        assertThat(sskus, hasSize(3));
        assertThat(sskus, hasItems("100", "200", "300"));
    }

    @Test
    @DbUnitDataSet(before = "CachedSupplyShadowSskusService.testGetAndSave.after.csv",
            after = "CachedSupplyShadowSskusService.testGetAndSave.after.csv")
    public void testGet() {
        when(ffwfApiClient.getShadowSupplySskus(123L)).thenThrow(new RuntimeException("Never"));
        Set<String> sskus = service.getShadowSupplySskus(123L);
        assertThat(sskus, hasSize(3));
        assertThat(sskus, hasItems("100", "200", "300"));
    }
}
