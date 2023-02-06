package ru.yandex.direct.logviewercore.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.feature.service.FeatureManagingService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapperProvider;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class LogViewerServiceEnhanceResultsTest {

    private static final String SERVICE_KEY = "service";
    private static final String SERVICE_VALUE = "some-service";
    private static final String METHOD_KEY = "method";
    private static final String METHOD_VALUE = "some-method";
    private static final String UID_KEY = "uid";
    private static final String UID_VALUE1 = "12345";
    private static final String UID_VALUE2 = "123456789";

    private static final String LOGIN1 = "login1";
    private static final String LOGIN2 = "login2";
    private static final String LOGIN3 = "login3";

    private LogViewerService testingService;

    private ShardHelper shardHelper;

    private List<String> fieldsWithUid;
    private List<Object> resultWithUid;

    private List<String> fieldsWithoutUid;
    private List<Object> resultWithoutUid;

    @Before
    public void prepare() {
        shardHelper = mock(ShardHelper.class);
        DatabaseWrapperProvider dbProvider = mock(DatabaseWrapperProvider.class);

        testingService = new LogViewerService(
                dbProvider,
                shardHelper,
                Collections.emptyList(),
                mock(FeatureService.class),
                mock(FeatureManagingService.class)
        );

        fieldsWithUid = Arrays.asList(SERVICE_KEY, METHOD_KEY, UID_KEY);
        resultWithUid = Arrays.asList(SERVICE_VALUE, METHOD_VALUE, UID_VALUE1);

        fieldsWithoutUid = Arrays.asList(SERVICE_KEY, METHOD_KEY);
        resultWithoutUid = Arrays.asList(SERVICE_VALUE, METHOD_VALUE);
    }

    @Test
    public void enhanceResults_OneResultAndUidPresentsAndLoginsFound_UidReplaced() {
        when(shardHelper.getLoginsByUids(any())).
                thenReturn(Collections.singletonList(Arrays.asList(LOGIN1, LOGIN2)));

        List<List<Object>> resultsWithUid = Collections.singletonList(new ArrayList<>(resultWithUid));

        List<Object> resultWithReplacedUid = Arrays.asList(SERVICE_VALUE, METHOD_VALUE, LOGIN1);
        List<List<Object>> expectedEnhancedResults = Collections.singletonList(resultWithReplacedUid);

        testingService.enhanceResults(resultsWithUid, fieldsWithUid);
        assertThat("results after enhancing (replacing uid with login) are valid",
                resultsWithUid, beanDiffer(expectedEnhancedResults));
    }

    @Test
    public void enhanceResults_ManyResultsAndUidPresentsAndLoginsFound_UidsReplaced() {
        // return different logins for each uid
        when(shardHelper.getLoginsByUids(any())).
                thenReturn(Arrays.asList(Arrays.asList(LOGIN1, LOGIN2), Collections.singletonList(LOGIN3)));

        // 2 results with different uids
        List<Object> resultWithUid1 = new ArrayList<>(resultWithUid);
        List<Object> resultWithUid2 = Arrays.asList(SERVICE_VALUE, METHOD_VALUE, UID_VALUE2);
        List<List<Object>> resultsWithUid = Arrays.asList(resultWithUid1, resultWithUid2);

        // 2 enhanced results with different logins after replacing
        List<Object> resultWithReplacedUid1 = Arrays.asList(SERVICE_VALUE, METHOD_VALUE, LOGIN1);
        List<Object> resultWithReplacedUid2 = Arrays.asList(SERVICE_VALUE, METHOD_VALUE, LOGIN3);
        List<List<Object>> expectedEnhancedResults = Arrays.asList(resultWithReplacedUid1, resultWithReplacedUid2);

        testingService.enhanceResults(resultsWithUid, fieldsWithUid);
        assertThat("results after enhancing (replacing uid with login) are valid",
                resultsWithUid, beanDiffer(expectedEnhancedResults));
    }

    @Test
    public void enhanceResults_OneResultAndUidPresentsAndLoginsNotFound_UidNotReplaced() {
        when(shardHelper.getLoginsByUids(any())).
                thenReturn(Collections.singletonList(new ArrayList<>()));

        List<List<Object>> resultsWithUid = Collections.singletonList(new ArrayList<>(resultWithUid));

        List<List<Object>> expectedEnhancedResults = Collections.singletonList(new ArrayList<>(resultWithUid));

        testingService.enhanceResults(resultsWithUid, fieldsWithUid);
        assertThat("results after enhancing (no replacing uid with login because logins not found) are valid",
                resultsWithUid, beanDiffer(expectedEnhancedResults));
    }

    @Test
    public void enhanceResults_ManyResultsAndUidPresentsAndSomeLoginsFound_SomeUidsReplaced() {
        // return logins for one uid and no logins for another
        when(shardHelper.getLoginsByUids(any())).
                thenReturn(Arrays.asList(Arrays.asList(LOGIN1, LOGIN2), new ArrayList<>()));

        // 2 results with different uids
        List<Object> resultWithUid1 = new ArrayList<>(resultWithUid);
        List<Object> resultWithUid2 = Arrays.asList(SERVICE_VALUE, METHOD_VALUE, UID_VALUE2);
        List<List<Object>> resultsWithUid = Arrays.asList(resultWithUid1, resultWithUid2);

        // 2 enhanced results, one with found login and one with uid
        List<Object> resultWithReplacedUid1 = Arrays.asList(SERVICE_VALUE, METHOD_VALUE, LOGIN1);
        List<Object> resultWithReplacedUid2 = Arrays.asList(SERVICE_VALUE, METHOD_VALUE, UID_VALUE2);
        List<List<Object>> expectedEnhancedResults = Arrays.asList(resultWithReplacedUid1, resultWithReplacedUid2);

        testingService.enhanceResults(resultsWithUid, fieldsWithUid);
        assertThat("results after enhancing (replacing uid with login at one result of two) are valid",
                resultsWithUid, beanDiffer(expectedEnhancedResults));
    }

    @Test
    public void enhanceResults_ResultWithoutUid_ResultsNotChanged() {
        when(shardHelper.getLoginsByUids(any())).
                thenReturn(new ArrayList<>(new ArrayList<>()));

        List<List<Object>> resultsWithoutUid = Collections.singletonList(new ArrayList<>(resultWithoutUid));
        List<List<Object>> expectedEnhancedResults = Collections.singletonList(new ArrayList<>(resultWithoutUid));

        testingService.enhanceResults(resultsWithoutUid, fieldsWithoutUid);
        assertThat("results without uid are not changed after enhancing",
                resultsWithoutUid, beanDiffer(expectedEnhancedResults));
    }
}
