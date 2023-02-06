package ru.yandex.market.mbo.tms.modeltransfer;

import ru.yandex.market.mbo.db.transfer.step.ModelTransferStepConfigService;
import ru.yandex.market.mbo.gwt.models.User;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransfer;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStep;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStepInfo;

/**
 * @author dmserebr
 * @date 20.08.18
 */
public class ModelTransferAutoStepConfigServiceStub implements
    ModelTransferStepConfigService<ModelTransferStepConfigStub> {

    @Override
    public ModelTransferStepConfigStub createStepConfig(ModelTransfer transfer, ModelTransferStep modelTransferStep,
                                                        ModelTransferStepInfo stepInfo, User user) {
        return null;
    }

    @Override
    public void updateStepConfig(ModelTransferStepConfigStub config, User user) {

    }

    @Override
    public ModelTransferStepConfigStub getStepConfig(long stepId) {
        return null;
    }
}
