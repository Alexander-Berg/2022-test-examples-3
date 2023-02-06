package ru.yandex.market.mboc.tms.service.uee;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import ru.yandex.market.ir.uee.model.UserRun;
import ru.yandex.market.ir.uee.model.UserRunReq;

public class UeeServiceMock implements UeeService {
    private final AtomicInteger sequence = new AtomicInteger(0);
    private final Map<Integer, UserRunReq> requests = new ConcurrentHashMap<>();
    private final Map<Integer, UserRun> userRuns = new ConcurrentHashMap<>();


    @Override
    public UserRun createUserRun(UserRunReq userRunReq) {
        int userRunId = sequence.incrementAndGet();
        requests.put(userRunId, userRunReq);
        return createDefaultUserRun(userRunId, userRunReq);
    }

    @Override
    public UserRun getUserRun(Integer userRunId) {
        return userRuns.get(userRunId);
    }

    public void putUserRun(UserRun userRun) {
        userRuns.put(userRun.getId(), userRun);
    }

    private UserRun createDefaultUserRun(int userRunId, UserRunReq userRunReq) {
        UserRun userRun = new UserRun();
        userRun.setId(userRunId);
        userRun.setAccountId(userRunReq.getAccountId());
        userRun.setFieldMappings(userRunReq.getFieldMappings());
        userRun.setNotificationRecipients(userRunReq.getNotificationRecipients());
        userRuns.put(userRunId, userRun);
        return userRun;
    }

    @Override
    public void stopAndCleanUserRun(Integer userRunId) {
        Objects.requireNonNull(userRunId);
        userRuns.remove(userRunId);
    }
}
