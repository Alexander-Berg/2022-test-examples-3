package ru.yandex.direct.api.v5.entity.changes.delegate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.entity.changes.service.CheckCampaignsService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.env.EnvironmentType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.entity.changes.delegate.CheckCampaignsDelegate.USE_CAMP_AGGREGATED_LAST_CHANGE_HEADER;

@Api5Test
@RunWith(JUnitParamsRunner.class)
public class CheckCampaignsLastChangePropertyDelegateTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    CheckCampaignsDelegate checkCampaignsDelegate;

    @Autowired
    ShardHelper shardHelper;

    private ApiAuthenticationSource authenticationSource;

    CheckCampaignsService checkCampaignsService;

    @Autowired
    Steps steps;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        authenticationSource = mock(ApiAuthenticationSource.class, Answers.RETURNS_DEEP_STUBS);
        when(authenticationSource.getSubclient().getClientId()).thenReturn(clientInfo.getClientId());
        checkCampaignsService = mock(CheckCampaignsService.class);
        checkCampaignsDelegate = new CheckCampaignsDelegate(authenticationSource, shardHelper,
                checkCampaignsService, ppcPropertiesSupport, EnvironmentType.DEVELOPMENT);
    }

    @After
    public void after() {
        ppcPropertiesSupport.remove(PpcPropertyNames.USE_CAMP_AGGREGATED_LASTCHANGE_PROPERTY);
    }

    @Parameterized.Parameters(name = "{0}")
    private static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {null, null, false}, //no header null property
                {null, "1", true}, //no header yes property
                {"true", null, true}, //true header null property
                {"fAlse", null, false}, //false header null property
                {"fAlse", "1", false}, //false header yes property
                {"invalid", null, false}, //invalid header null property
                {"invalid", "1", true}, //invalid header yes property
        });
    }

    @Test
    @Parameters(method = "params")
    public void testLastChangedPropertyUsage(String headerValue,
                                              String propValue,
                                              boolean expectedUseCampAggrLastChange) {
        if (propValue != null) {
            ppcPropertiesSupport.set(PpcPropertyNames.USE_CAMP_AGGREGATED_LASTCHANGE_PROPERTY, propValue);
        }
        if (headerValue != null) {
            var reqAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            ((MockHttpServletRequest) reqAttributes.getRequest())
                    .addHeader(USE_CAMP_AGGREGATED_LAST_CHANGE_HEADER, headerValue);
            RequestContextHolder.setRequestAttributes(reqAttributes);
        }
        checkCampaignsDelegate.processRequest(LocalDateTime.now().minusDays(2));
        verify(checkCampaignsService).getCampaignsChanges(any(ClientId.class), anyInt(), any(LocalDateTime.class),
                eq(expectedUseCampAggrLastChange), anySet());
    }
}
