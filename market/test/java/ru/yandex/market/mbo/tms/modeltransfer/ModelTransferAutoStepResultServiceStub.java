package ru.yandex.market.mbo.tms.modeltransfer;

import ru.yandex.market.mbo.db.transfer.step.result.ModelTransferStepResultService;
import ru.yandex.market.mbo.gwt.models.User;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.TextResult;

import java.util.List;

/**
 * @author dmserebr
 * @date 20.08.18
 */
public class ModelTransferAutoStepResultServiceStub implements ModelTransferStepResultService<TextResult> {
    @Override
    public ResultInfo doAction(long stepId, ResultInfo.Action action, String resultText, User user) {
        return null;
    }

    @Override
    public List<TextResult> getResults(long stepId) {
        return null;
    }

    @Override
    public TextResult getResult(long resultId) {
        return null;
    }

    @Override
    public ResultInfo addResult(TextResult result, User user) {
        return null;
    }
}
