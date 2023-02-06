package ru.yandex.market.mbo.db.modelstorage.http.generated;

import org.apache.log4j.Logger;
import org.junit.Assert;
import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.db.modelstorage.http.utils.ProtobufHelper;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Вспомогательный класс для контроля корректности сохранения/удаления/изменения моделей.
 * <p>
 * Created by astafurovme@yandex-team.ru on 03.05.16.
 */
public class GeneratedCardsHistory {

    private static final Logger log = Logger.getLogger(GeneratedCardsHistory.class);

    // мы не можем хранить мапу по айдишнику модели
    // так как на момент создания еще не знаем их айдишники
    private Map<String, HistoryEntity> modelNameToHistoryEntityMap = new LinkedHashMap<>();

    public void setExpectedStatus(List<ModelStorage.Model> models, ModelStorage.OperationType operationType,
                                  ModelStorage.OperationStatusType expectedStatus) {
        models.forEach(model -> setExpectedStatus(model, operationType, expectedStatus));
    }

    /**
     * Указываем, что мы собираемся делать с моделькой и какой результат ожидаем увидеть.
     *
     * @param model          - модель
     * @param operationType  - операция
     * @param expectedStatus - ожидаемый статус выполнения
     */
    public void setExpectedStatus(ModelStorage.Model model, ModelStorage.OperationType operationType,
                                  ModelStorage.OperationStatusType expectedStatus) {
        setExpectedStatus(ModelProtoConverter.convert(model), operationType, expectedStatus);
    }

    /**
     * Указываем, что мы собираемся делать с моделькой и какой результат ожидаем увидеть.
     *
     * @param model          - модель
     * @param operationType  - операция
     * @param expectedStatus - ожидаемый статус выполнения
     */
    public void setExpectedStatus(CommonModel model, ModelStorage.OperationType operationType,
                                  ModelStorage.OperationStatusType expectedStatus) {
        modelNameToHistoryEntityMap.put(model.getTitle(), new HistoryEntity(operationType, expectedStatus, model));
    }

    public void setDeletedModels(List<ModelStorage.Model> models) {
        ModelProtoConverter.reverseConvertAll(models).forEach(this::setDeletedModel);
    }

    public void setDeletedModel(CommonModel commonModel) {
        HistoryEntity historyEntity = modelNameToHistoryEntityMap.get(commonModel.getTitle());
        historyEntity.setDeleted(true);
    }

    /**
     * Добавляем в мапу статусы выполнения операций.
     * Порядок данных в history должен совпадать с порядком actualStatuses.
     */
    public void writeOperationStatuses(List<ModelStorage.OperationStatus> actualStatuses) {
        Iterator<ModelStorage.OperationStatus> responseIt = actualStatuses.iterator();
        for (String modelName : modelNameToHistoryEntityMap.keySet()) {
            ModelStorage.OperationStatus operationStatus = responseIt.next();
            HistoryEntity historyEntity = modelNameToHistoryEntityMap.get(modelName);

            ModelStorage.OperationType type = operationStatus.getType();
            ModelStorage.OperationStatusType statusType = operationStatus.getStatus();

            historyEntity.setActualOperationType(type);
            historyEntity.setActualOperationStatusType(statusType);
        }
    }

    /**
     * Загружаем в мапу реальные модели из базы.
     */
    public void writeActualModels(List<ModelStorage.Model> actualModels) {
        witeActualModels(ModelProtoConverter.reverseConvertAll(actualModels));
    }

    public void witeActualModels(List<CommonModel> actualModels) {
        for (CommonModel actualModel : actualModels) {
            HistoryEntity historyEntity = modelNameToHistoryEntityMap.get(actualModel.getTitle());
            if (historyEntity != null) {
                historyEntity.setActualModel(actualModel);
            }
        }
    }

    /**
     * На основе собранных данных проверяем как все прошло.
     */
    public void assertResults() {
        modelNameToHistoryEntityMap.forEach((modelName, historyEntity) -> {
            CommonModel expectedModel = historyEntity.expectedModel;
            ModelStorage.OperationType expectedType = historyEntity.expectedOperationType;
            ModelStorage.OperationStatusType expectedStatusType = historyEntity.expectedOperationStatusType;

            CommonModel actualModel = historyEntity.actualModel;
            ModelStorage.OperationType actualType = historyEntity.actualOperationType;
            ModelStorage.OperationStatusType actualStatusType = historyEntity.actualOperationStatusType;

            assertEquals(expectedType, actualType);
            assertEquals(expectedStatusType, actualStatusType);

            if (expectedType == ModelStorage.OperationType.CREATE) {
                assertModelEquals(expectedModel, actualModel);
            } else if (expectedType.equals(ModelStorage.OperationType.CHANGE)) {
                if (expectedStatusType == ModelStorage.OperationStatusType.OK) {
                    assertModelEquals(expectedModel, actualModel);
                } else if (expectedStatusType == ModelStorage.OperationStatusType.MODEL_MODIFIED) {
                    // тут 2 варианта, либо модель удалена
                    // либо она изменена
                    if (historyEntity.deleted) {
                        assertNull(actualModel);
                    } else {
                        assertModelEquals(expectedModel, actualModel);
                    }
                } else if (expectedStatusType == ModelStorage.OperationStatusType.INTERNAL_ERROR) {
                    Assert.fail(String.format("Model %s %d was failed to update",
                        expectedModel.getTitle(), expectedModel.getId()));
                }
            } else if (expectedType == ModelStorage.OperationType.REMOVE) {
                assertNull(actualModel);
            }
        });
    }

    public void printResults() {
        StringBuilder message = new StringBuilder();
        message.append("-------------------------------------------------------------------------------");

        modelNameToHistoryEntityMap.forEach((modelName, historyEntity) -> {
            String expectedModelName = historyEntity.getExpectedModelName();
            String expectedComment = historyEntity.getExpectedModelComment();
            ModelStorage.OperationType expectedType = historyEntity.expectedOperationType;
            ModelStorage.OperationStatusType expectedStatusType = historyEntity.expectedOperationStatusType;

            String actualModelName = historyEntity.getActualModelName();
            String actualComment = historyEntity.getActualModelComment();
            ModelStorage.OperationType actualType = historyEntity.actualOperationType;
            ModelStorage.OperationStatusType actualStatusType = historyEntity.actualOperationStatusType;

            String format = "Model name '%s', comment: '%s'; operation: '%s', status: '%s'";

            String expectedMessage = String.format(format, expectedModelName, expectedComment,
                expectedType, expectedStatusType);
            String actualMessage = String.format(format, actualModelName, actualComment,
                actualType, actualStatusType);
            message.append(String.format("\n---EXPECTED--\n%s\n---ACTUAL---\n%s", expectedMessage, actualMessage));
        });

        log.info(message);
    }

    private static void assertModelEquals(CommonModel expectedModel, CommonModel actualModel) {
        String expectedName = getModelName(expectedModel);
        String actualName = getModelName(actualModel);

        String expectedComment = getModelComment(expectedModel);
        String actualComment = getModelComment(actualModel);

        assertEquals(expectedName, actualName);
        assertEquals(expectedComment, actualComment);
    }

    private static class HistoryEntity {
        private ModelStorage.OperationType expectedOperationType;
        private ModelStorage.OperationStatusType expectedOperationStatusType;
        private CommonModel expectedModel;

        private ModelStorage.OperationType actualOperationType;
        private ModelStorage.OperationStatusType actualOperationStatusType;
        private CommonModel actualModel;
        private boolean deleted;

        HistoryEntity(ModelStorage.OperationType expectedOperationType,
                      ModelStorage.OperationStatusType expectedOperationStatusType,
                      CommonModel model) {
            this.expectedOperationStatusType = expectedOperationStatusType;
            this.expectedModel = model;
            this.expectedOperationType = expectedOperationType;
        }

        public void setActualOperationType(ModelStorage.OperationType actualOperationType) {
            this.actualOperationType = actualOperationType;
        }

        public void setActualModel(CommonModel actualModel) {
            this.actualModel = actualModel;
        }

        public void setActualOperationStatusType(ModelStorage.OperationStatusType actualOperationStatusType) {
            this.actualOperationStatusType = actualOperationStatusType;
        }

        public String getExpectedModelName() {
            return getModelName(expectedModel);
        }

        public String getActualModelName() {
            return getModelName(actualModel);
        }

        public String getExpectedModelComment() {
            return getModelComment(expectedModel);
        }

        public String getActualModelComment() {
            return getModelComment(actualModel);
        }

        public void setDeleted(boolean deleted) {
            this.deleted = deleted;
        }
    }

    private static String getModelComment(CommonModel model) {
        if (model == null) {
            return null;
        }
        ParameterValue operatorCommentValue = model.getSingleParameterValue(ProtobufHelper.OPERATOR_COMMENT_PARAM_ID);
        return WordUtil.getDefaultWord(operatorCommentValue.getStringValue());
    }

    private static String getModelName(CommonModel model) {
        return model == null ? null : model.getTitle();
    }
}
