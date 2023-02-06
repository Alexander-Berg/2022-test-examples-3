package ru.yandex.market.crm.campaign.test;

import java.util.function.Consumer;

import ru.yandex.market.crm.campaign.domain.actions.status.StepStatus;
import ru.yandex.market.crm.campaign.services.actions.StepStatusUpdater;
import ru.yandex.market.crm.campaign.services.actions.StepsStatusDAO;

/**
 * @author apershukov
 */
public class StepStatusUpdaterMock implements StepStatusUpdater {

    private final StepsStatusDAO stepsStatusDAO;

    public StepStatusUpdaterMock(StepsStatusDAO stepsStatusDAO) {
        this.stepsStatusDAO = stepsStatusDAO;
    }

    @Override
    public <T extends StepStatus<T>> T update(String actionId, String stepId, boolean startNew, Consumer<T> callback) {
        T status = (T) stepsStatusDAO.get(actionId, stepId);
        callback.accept(status);
        return status;
    }
}
