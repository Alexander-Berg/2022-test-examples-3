package ru.yandex.direct.teststeps.service;

import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.CalloutSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static java.util.stream.Collectors.toList;

@Service
@ParametersAreNonnullByDefault
public class CalloutStepsService {

    private final CalloutSteps calloutSteps;
    private final InfoHelper infoHelper;
    private final ShardHelper shardHelper;

    @Autowired
    public CalloutStepsService(CalloutSteps calloutSteps, InfoHelper infoHelper, ShardHelper shardHelper) {
        this.calloutSteps = calloutSteps;
        this.infoHelper = infoHelper;
        this.shardHelper = shardHelper;
    }

    public List<Long> createCallouts(String login, Collection<String> callouts) {
        UserInfo userInfo = infoHelper.getUserInfo(login);
        ClientInfo clientInfo = infoHelper.getClientInfo(login, userInfo.getUid());

        return callouts.stream()
                .map(text -> calloutSteps.createCalloutWithText(clientInfo, text).getId())
                .collect(toList());
    }

    public void deleteCallouts(String login, Collection<Long> calloutIds) {
        Long clientId = shardHelper.getClientIdByLogin(login);
        calloutSteps.deleteCallouts(ClientId.fromLong(clientId), calloutIds);
    }
}
