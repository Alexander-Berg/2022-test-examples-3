package ru.yandex.market.deepmind.common.services.background;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundAction;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionStatus;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

@Slf4j
public class BackgroundServiceMock implements BackgroundService {

    private final Map<Integer, BackgroundAction> repository = new HashMap<>();
    private int actionId = 1;
    private final List<BackgroundActionStatus> midStatuses = new ArrayList<>();
    private boolean cancelAll;

    @Override
    public int startAction(Context context, Consumer<BackgroundHandler> task) {
        int nextActionId = actionId++;

        BackgroundAction backgroundAction = new BackgroundAction();
        backgroundAction.setId(nextActionId);
        backgroundAction.setUserLogin(context.getLoginOrDefault("unit-test-user"));
        backgroundAction.setDescription(context.getDescription());
        backgroundAction.setStarted(DateTimeUtils.instantNow());
        backgroundAction.setResult(context.getInitialState());
        repository.put(nextActionId, backgroundAction);
        midStatuses.clear();

        BackgroundHandler handler = new BackgroundHandler() {

            @Override
            public int getActionId() {
                return nextActionId;
            }

            @Override
            public void updateState(BackgroundActionStatus status) {
                if (backgroundAction.isCancelRequested() || cancelAll) {
                    throw new IllegalStateException(String.format("Action: #%d cancel requested", actionId));
                }
                backgroundAction.setState(status.getStatus().toString());
                backgroundAction.setResult(status);
                backgroundAction.setLastUpdated(DateTimeUtils.instantNow());
                addMidStatus(backgroundAction);
            }

            @Override
            public void finish(BackgroundActionStatus status) {
                backgroundAction.setState(status.getStatus().toString());
                backgroundAction.setResult(status);
                backgroundAction.setFinished(DateTimeUtils.instantNow());
                backgroundAction.setLastUpdated(DateTimeUtils.instantNow());
                addMidStatus(backgroundAction);
            }
        };

        task.accept(handler);

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
    public BackgroundAction fetchFromDb(int actionId) {
        return repository.get(actionId);
    }

    @Override
    public void cancelActionAsync(int actionId) {
        var action = fetchFromDb(actionId);
        action.setCancelRequested(true);
    }

    @Override
    public void cancelAction(int actionId) {
        var action = fetchFromDb(actionId);
        action.setCancelRequested(true);
    }

    public void cancelAll(boolean cancelAll) {
        this.cancelAll = cancelAll;
    }
}
