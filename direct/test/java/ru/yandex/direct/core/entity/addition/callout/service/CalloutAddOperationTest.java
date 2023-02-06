package ru.yandex.direct.core.entity.addition.callout.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.addition.callout.repository.CalloutRepository;
import ru.yandex.direct.core.entity.addition.callout.service.validation.CalloutValidationService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCallouts.defaultCallout;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringRunner.class)
public class CalloutAddOperationTest {

    @Autowired
    private CalloutRepository calloutRepository;
    @Autowired
    private CalloutValidationService calloutValidationService;
    @Autowired
    private Steps steps;

    private ClientId clientId;
    private int shard;

    @Before
    public void before() {
        ClientInfo client = steps.clientSteps().createDefaultClient();
        clientId = client.getClientId();
        shard = client.getShard();
    }

    @Test
    public void prepareAndApply_OneCallout_Successful() {
        CalloutAddOperation calloutAddOperation = createCalloutAddOperation(singletonList(defaultCallout(clientId)));
        MassResult<Long> result = calloutAddOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    private CalloutAddOperation createCalloutAddOperation(List<Callout> callouts) {
        return new CalloutAddOperation(Applicability.FULL, callouts, calloutRepository, calloutValidationService,
                shard, clientId);
    }
}
