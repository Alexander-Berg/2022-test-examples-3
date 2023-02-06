package ru.yandex.market.mbo.tms.modeltransfer;

import ru.yandex.market.mbo.gwt.models.User;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStep;
import ru.yandex.market.mbo.gwt.models.transfer.ModelTransferStepInfo;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author dmserebr
 * @date 01.11.18
 */
public final class ModelTransferStepInfoBuilder {
    private long id;
    private long transferId;
    private ModelTransferStep.Type stepType;
    private ModelTransferStep.ExecutionType stepExecutionType;
    private int stepIndex;
    private String description;
    private long configStepId;
    private Date deadline;
    private User responsibleUser;
    private Date modified;
    private User userModified;
    private List<ResultInfo> executionResultInfos = new ArrayList<>();
    private List<ResultInfo> validationResultInfos = new ArrayList<>();
    private boolean readyToExecute;
    private boolean hasConfig;
    private boolean configIsEmpty;
    private boolean stepIsValidatable;

    private ModelTransferStepInfoBuilder() {
    }

    public static ModelTransferStepInfoBuilder newBuilder() {
        return new ModelTransferStepInfoBuilder();
    }

    public ModelTransferStepInfoBuilder withId(long id) {
        this.id = id;
        return this;
    }

    public ModelTransferStepInfoBuilder withTransferId(long transferId) {
        this.transferId = transferId;
        return this;
    }

    public ModelTransferStepInfoBuilder withStepType(ModelTransferStep.Type stepType) {
        this.stepType = stepType;
        return this;
    }

    public ModelTransferStepInfoBuilder withStepExecutionType(ModelTransferStep.ExecutionType stepExecutionType) {
        this.stepExecutionType = stepExecutionType;
        return this;
    }

    public ModelTransferStepInfoBuilder withStepIndex(int stepIndex) {
        this.stepIndex = stepIndex;
        return this;
    }

    public ModelTransferStepInfoBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ModelTransferStepInfoBuilder withConfigStepId(long configStepId) {
        this.configStepId = configStepId;
        return this;
    }

    public ModelTransferStepInfoBuilder withDeadline(Date deadline) {
        this.deadline = deadline;
        return this;
    }

    public ModelTransferStepInfoBuilder withResponsibleUser(User responsibleUser) {
        this.responsibleUser = responsibleUser;
        return this;
    }

    public ModelTransferStepInfoBuilder withModified(Date modified) {
        this.modified = modified;
        return this;
    }

    public ModelTransferStepInfoBuilder withUserModified(User userModified) {
        this.userModified = userModified;
        return this;
    }

    public ModelTransferStepInfoBuilder withExecutionResultInfos(List<ResultInfo> executionResultInfos) {
        this.executionResultInfos = executionResultInfos;
        return this;
    }

    public ModelTransferStepInfoBuilder withValidationResultInfos(List<ResultInfo> validationResultInfos) {
        this.validationResultInfos = validationResultInfos;
        return this;
    }

    public ModelTransferStepInfoBuilder withReadyToExecute(boolean readyToExecute) {
        this.readyToExecute = readyToExecute;
        return this;
    }

    public ModelTransferStepInfoBuilder withHasConfig(boolean hasConfig) {
        this.hasConfig = hasConfig;
        return this;
    }

    public ModelTransferStepInfoBuilder withConfigIsEmpty(boolean configIsEmpty) {
        this.configIsEmpty = configIsEmpty;
        return this;
    }

    public ModelTransferStepInfoBuilder withStepIsValidatable(boolean stepIsValidatable) {
        this.stepIsValidatable = stepIsValidatable;
        return this;
    }

    public ModelTransferStepInfo build() {
        ModelTransferStepInfo modelTransferStepInfo = new ModelTransferStepInfo();
        modelTransferStepInfo.setId(id);
        modelTransferStepInfo.setTransferId(transferId);
        modelTransferStepInfo.setStepType(stepType);
        modelTransferStepInfo.setStepExecutionType(stepExecutionType);
        modelTransferStepInfo.setStepIndex(stepIndex);
        modelTransferStepInfo.setDescription(description);
        modelTransferStepInfo.setConfigStepId(configStepId);
        modelTransferStepInfo.setDeadline(deadline);
        modelTransferStepInfo.setResponsibleUser(responsibleUser);
        modelTransferStepInfo.setModified(modified);
        modelTransferStepInfo.setUserModified(userModified);
        modelTransferStepInfo.setExecutionResultInfos(executionResultInfos);
        modelTransferStepInfo.setValidationResultInfos(validationResultInfos);
        modelTransferStepInfo.setReadyToExecute(readyToExecute);
        modelTransferStepInfo.setHasConfig(hasConfig);
        modelTransferStepInfo.setConfigIsEmpty(configIsEmpty);
        modelTransferStepInfo.setStepIsValidatable(stepIsValidatable);
        return modelTransferStepInfo;
    }
}
