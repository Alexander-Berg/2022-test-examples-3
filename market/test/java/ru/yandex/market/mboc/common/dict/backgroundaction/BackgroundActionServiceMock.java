package ru.yandex.market.mboc.common.dict.backgroundaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import ru.yandex.market.mboc.common.utils.DateTimeUtils;

public class BackgroundActionServiceMock implements BackgroundActionService {
    private final Map<Integer, BackgroundAction> repository = new HashMap<>();
    private int actionId = 1;
    private final List<BackgroundActionStatus> midStatuses = new ArrayList<>();
    private boolean cancelAll;

    @Override
    public <T> int startAction(Context context, Consumer<ActionHandle<T>> task) {
        int nextActionId = actionId++;

        BackgroundAction backgroundAction = new BackgroundAction();
        backgroundAction.setId(nextActionId);
        backgroundAction.setUserLogin(context.getLoginOrDefault("unit-test-user"));
        backgroundAction.setDescription(context.getDescription());
        backgroundAction.setStarted(DateTimeUtils.instantNow());
        backgroundAction.setResult(context.getInitialState());
        repository.put(nextActionId, backgroundAction);
        midStatuses.clear();

        ActionHandle<T> actionHandle = new ActionHandle<>() {

            @Override
            public int getActionId() {
                return nextActionId;
            }

            @Override
            public void init(String threadName, String marketReqId) {
                backgroundAction.setThreadName(threadName);
                backgroundAction.setMarketReqId(marketReqId);
                addMidStatus(backgroundAction);
            }

            @Override
            public void updateState(String state, T stateObj) {
                if (backgroundAction.isCancelRequested() || cancelAll) {
                    throw new IllegalStateException(String.format("Action: #%d cancel requested", actionId));
                }
                backgroundAction.setState(state);
                backgroundAction.setResult(stateObj);
                backgroundAction.setLastUpdated(DateTimeUtils.instantNow());
                addMidStatus(backgroundAction);
            }

            @Override
            public void finish(String state, T stateObj) {
                backgroundAction.setState(state);
                backgroundAction.setResult(stateObj);
                backgroundAction.setFinished(DateTimeUtils.instantNow());
                backgroundAction.setLastUpdated(DateTimeUtils.instantNow());
                addMidStatus(backgroundAction);
            }
        };

        task.accept(actionHandle);

        return nextActionId;
    }

    public BackgroundActionStatus getMidStatus(int offset) {
        return midStatuses.get(midStatuses.size() - offset - 1);
    }

    private void addMidStatus(BackgroundAction backgroundAction) {
        if (backgroundAction.getResult() instanceof BackgroundActionStatus) {
            midStatuses.add((BackgroundActionStatus) backgroundAction.getResult());
        }
    }

    @Nullable
    @Override
    public BackgroundAction getAction(int actionId) {
        return repository.get(actionId);
    }

    @Override
    public void cancelAsync(int actionId) {
        var action = getAction(actionId);
        action.setCancelRequested(true);
    }

    @Override
    public void cancel(int actionId) {
        var action = getAction(actionId);
        action.setCancelRequested(true);
    }

    public void cancelAll(boolean cancelAll) {
        this.cancelAll = cancelAll;
    }
}
