package ru.yandex.market.checkout.common.web;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.common.ping.CheckResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author : poluektov
 * date: 28.03.2019.
 */
public class HealthStateCachedProviderTest extends AbstractWebTestBase {

    @Autowired
    public HealthStateCachedProvider healthStateProvider;

    @Mock
    public PingCheckHelper mockHelper;

    @Test
    public void testOK() {
        HealthInfo healthInfo = new HealthInfo(Lists.newArrayList(EntityHelper.getServicePingResult()));
        Mockito.when(mockHelper.makeChecks()).thenReturn(healthInfo);
        healthStateProvider.setCheckHelper(mockHelper);

        HealthInfo result = healthStateProvider.getCurrentState();
        assertThat(result.getMaxLevel(), equalTo(CheckResult.Level.OK));
    }

    @Test
    public void testCrit() {
        HealthInfo healthInfo = new HealthInfo(Lists.newArrayList(EntityHelper.getServicePingResultCrit()));
        Mockito.when(mockHelper.makeChecks()).thenReturn(healthInfo);
        healthStateProvider.setCheckHelper(mockHelper);

        HealthInfo result = healthStateProvider.getCurrentState();
        assertThat(result.getMaxLevel(), equalTo(CheckResult.Level.CRITICAL));
    }
}
