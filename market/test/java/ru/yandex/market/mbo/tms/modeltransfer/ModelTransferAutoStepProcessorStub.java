package ru.yandex.market.mbo.tms.modeltransfer;

import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.TextResult;
import ru.yandex.market.mbo.tms.modeltransfer.processor.ModelTransferAutoStepProcessorBase;

import java.util.Date;

/**
 * @author dmserebr
 * @date 20.08.18
 */
public class ModelTransferAutoStepProcessorStub extends ModelTransferAutoStepProcessorBase<
    TextResult, TextResult, ModelTransferStepConfigStub> {

    @Override
    public TextResult executeStep(ResultInfo resultInfo, ModelTransferJobContext<ModelTransferStepConfigStub> context) {
        resultInfo.setStatus(ResultInfo.Status.COMPLETED);
        resultInfo.setCompleted(new Date());
        return new TextResult(resultInfo);
    }

    @Override
    public TextResult validateStep(ResultInfo resultInfo,
                                   ModelTransferJobContext<ModelTransferStepConfigStub> context) {
        resultInfo.setStatus(ResultInfo.Status.COMPLETED);
        resultInfo.setCompleted(new Date());
        return new TextResult(resultInfo);
    }

    @Override
    public TextResult createExecutionResult() {
        return new TextResult();
    }

    @Override
    public TextResult createValidationResult() {
        return new TextResult();
    }
}
