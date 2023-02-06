package ru.yandex.direct.core.entity.retargeting.service;

import java.util.List;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.metrika.repository.LalSegmentRepository;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestLalSegmentRepository;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;

import static java.util.stream.Collectors.toList;

public class BaseRetargetingConditionServiceTest {

    @Autowired
    protected RetargetingConditionOperationFactory retargetingConditionOperationFactory;

    @Autowired
    protected RetargetingConditionService retargetingConditionService;

    @Autowired
    public ClientSteps clientSteps;

    @Autowired
    protected RetargetingConditionRepository retConditionRepository;

    @Autowired
    protected LalSegmentRepository lalSegmentRepository;

    @Autowired
    protected TestLalSegmentRepository testLalSegmentRepository;

    @Autowired
    protected RbacService rbacService;

    @Autowired
    protected MetrikaClientStub metrikaClientStub;

    @Autowired
    protected MetrikaHelperStub metrikaHelperStub;

    protected ClientInfo clientInfo;
    protected int shard;
    protected long uid;
    protected ClientId clientId;

    @Before
    public void before() {
        clientInfo = clientSteps.createDefaultClient();
        shard = clientInfo.getShard();
        uid = clientInfo.getUid();
        clientId = clientInfo.getClientId();
    }

    protected List<Long> extractIdsFromResult(MassResult<Long> result) {
        return result.getResult()
                .stream()
                .filter(Result::isSuccessful)
                .map(Result::getResult)
                .collect(toList());
    }
}
