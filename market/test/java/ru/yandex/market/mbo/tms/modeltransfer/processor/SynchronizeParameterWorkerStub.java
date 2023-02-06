package ru.yandex.market.mbo.tms.modeltransfer.processor;

import ru.yandex.market.mbo.gwt.models.transfer.step.ListOfModelParameterLandingConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.ParameterResultEntry;
import ru.yandex.market.mbo.tms.modeltransfer.worker.ResultEntryWorker;

import java.util.List;

/**
 * @author danfertev
 * @since 05.10.2018
 */
public class SynchronizeParameterWorkerStub implements ResultEntryWorker<
    ParameterResultEntry, ListOfModelParameterLandingConfig, Void> {

    @Override
    public List<ParameterResultEntry> doWork(ListOfModelParameterLandingConfig config, Void options) {
        return null;
    }
}
