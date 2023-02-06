package ru.yandex.market.mbo.tms.modeltransfer.processor;

import ru.yandex.market.mbo.gwt.models.transfer.step.ListOfModelParameterLandingConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.ParameterResultEntry;
import ru.yandex.market.mbo.tms.modeltransfer.worker.ResultEntryWorker;

/**
 * @author danfertev
 * @since 05.10.2018
 */
public class SynchronizeParameterBaseStepProcessorStub extends SynchronizeParameterBaseStepProcessor {
    public static final String NO_SYNC_MESSAGE = "NO_SYNC";
    public static final String SUCCESS_MESSAGE = "SUCCESS";
    public static final String FAILURE_MESSAGE = "FAILURE";

    public SynchronizeParameterBaseStepProcessorStub(
        ResultEntryWorker<ParameterResultEntry, ListOfModelParameterLandingConfig, Void> worker) {

        super(worker);
    }

    @Override
    protected String getNoSyncSuccessMessage() {
        return NO_SYNC_MESSAGE;
    }

    @Override
    protected String getSuccessMessage(int synced) {
        return SUCCESS_MESSAGE;
    }

    @Override
    protected String getFailureMessage(int failed) {
        return FAILURE_MESSAGE;
    }
}
