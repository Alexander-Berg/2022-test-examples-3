package ru.yandex.direct.logviewercore.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.EntryStream;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.feature.service.FeatureManagingService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapperProvider;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class LogViewerServiceEnhanceConditionsTest {

    private static final String SERVICE_KEY = "service";
    private static final String SERVICE_VALUE = "some-service";
    private static final String METHOD_KEY = "method";
    private static final String METHOD_VALUE = "some-method";
    private static final String UID_KEY = "uid";
    private static final String UID_VALUE_LOGIN = "some-login";
    private static final String UID_VALUE_UID = "12345";

    private LogViewerService testingService;

    private ShardHelper shardHelper;

    private Map<String, String> conditionsWithLogin;
    private Map<String, String> conditionsWithUid;

    @Before
    public void prepare() {
        shardHelper = mock(ShardHelper.class);

        testingService = new LogViewerService(
                mock(DatabaseWrapperProvider.class),
                shardHelper,
                Collections.emptyList(),
                mock(FeatureService.class),
                mock(FeatureManagingService.class)
        );
        testingService.shardHelper = shardHelper;

        conditionsWithLogin = new HashMap<>();
        conditionsWithLogin.put(SERVICE_KEY, SERVICE_VALUE);
        conditionsWithLogin.put(METHOD_KEY, METHOD_VALUE);
        conditionsWithLogin.put(UID_KEY, UID_VALUE_LOGIN);

        conditionsWithUid = new HashMap<>();
        conditionsWithUid.put(SERVICE_KEY, SERVICE_VALUE);
        conditionsWithUid.put(METHOD_KEY, METHOD_VALUE);
        conditionsWithUid.put(UID_KEY, UID_VALUE_UID);
    }

    static Map<String, List<String>> convertToEnhanced(Map<String, String> cond) {
        return EntryStream.of(cond).mapValues(Collections::singletonList).toMap();
    }

    @Test
    public void enhanceConditions_LoginPresentsAsUidAndUidFound_ReplacesOnlyLoginWithUid() {
        when(shardHelper.getUidByLogin(UID_VALUE_LOGIN)).thenReturn(Long.valueOf(UID_VALUE_UID));
        Map<String, List<String>> expectedEnhancedConditions = convertToEnhanced(conditionsWithLogin);
        expectedEnhancedConditions.put(UID_KEY, Collections.singletonList(UID_VALUE_UID));
        Map<String, List<String>> enhancedConditions = testingService.enhanceConditions(conditionsWithLogin);
        assertThat(enhancedConditions, beanDiffer(expectedEnhancedConditions));
    }

    @Test
    public void enhanceConditions_LoginPresentsAsUidAndUidNotFound_ReplacesOnlyLoginWithSpecialValue() {
        when(shardHelper.getUidByLogin(UID_VALUE_LOGIN)).thenReturn(null);
        Map<String, List<String>> expectedEnhancedConditions = convertToEnhanced(conditionsWithLogin);
        expectedEnhancedConditions.put(UID_KEY, Collections.singletonList("-1"));
        Map<String, List<String>> enhancedConditions = testingService.enhanceConditions(conditionsWithLogin);
        assertThat(enhancedConditions, beanDiffer(expectedEnhancedConditions));
    }

    @Test
    public void enhanceConditions_UidPresentsAsUid_NoModifications() {
        Map<String, List<String>> expectedEnhancedConditions = convertToEnhanced(conditionsWithUid);
        Map<String, List<String>> enhancedConditions = testingService.enhanceConditions(conditionsWithUid);
        assertThat(enhancedConditions, beanDiffer(expectedEnhancedConditions));
    }

    @Test
    public void enhanceConditions_UidPresentsAsUid_ShardingIsNotCalled() {
        testingService.enhanceConditions(conditionsWithUid);
        verify(shardHelper, never()).getUidByLogin(any());
    }

    @Test
    public void enhanceConditions_UidDoesNotPresent_NoModifications() {
        conditionsWithLogin.remove(UID_KEY);
        Map<String, List<String>> expectedEnhancedConditions = convertToEnhanced(conditionsWithLogin);
        Map<String, List<String>> enhancedConditions = testingService.enhanceConditions(conditionsWithLogin);
        assertThat(enhancedConditions, beanDiffer(expectedEnhancedConditions));
    }

    @Test
    public void enhanceConditions_UidDoesNotPresent_ShardSupportIsNotCalled() {
        conditionsWithLogin.remove(UID_KEY);
        testingService.enhanceConditions(conditionsWithLogin);
        verify(shardHelper, never()).getUidByLogin(any());
    }
}
