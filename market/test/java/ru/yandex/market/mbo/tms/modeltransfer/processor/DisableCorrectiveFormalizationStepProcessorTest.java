package ru.yandex.market.mbo.tms.modeltransfer.processor;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.TovarTreeServiceMock;
import ru.yandex.market.mbo.db.params.GLRulesServiceMock;
import ru.yandex.market.mbo.db.transfer.ModelTransferBuilder;
import ru.yandex.market.mbo.gurulight.template.conditional.GLRuleBuilder;
import ru.yandex.market.mbo.gwt.models.User;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRule;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRulePredicate;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleType;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.CategoryChangeRuleEntry;
import ru.yandex.market.mbo.gwt.models.transfer.step.DisableCorrectiveFormalizationResult;
import ru.yandex.market.mbo.gwt.models.transfer.step.EmptyStepConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.GLRuleInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.ResultEntry;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryBuilder;
import ru.yandex.market.mbo.tms.modeltransfer.ModelTransferJobContext;
import ru.yandex.market.mbo.tms.modeltransfer.ResultInfoBuilder;

import java.util.ArrayList;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * @author danfertev
 * @since 06.11.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class DisableCorrectiveFormalizationStepProcessorTest {
    private static final long USER_UID = 9999L;
    private static final long RULE_CATEGORY_ID = 9L;
    private static final int RULE_TOVAR_ID = 19;
    private static final long CATEGORY_ID = 1L;
    private static final long PARENT_CATEGORY_ID = 4L;
    private static final int TOVAR_ID = 10;
    private static final int PARENT_TOVAR_ID = 40;
    private static final long RULE_ID = 100L;
    private static final long ANOTHER_RULE_ID = 101L;
    private static final String RULE_NAME = "rule_name";
    private static final String ANOTHER_RULE_NAME = "another_rule_name";
    private static final long ANOTHER_CATEGORY_ID = 2L;
    private static final long TARGET_CATEGORY_ID = 3L;

    private DisableCorrectiveFormalizationStepProcessor processor;
    private GLRulesServiceMock glRulesServiceMock;
    private TovarTreeServiceMock tovarTreeServiceMock;
    private ModelTransferJobContext<EmptyStepConfig> context;
    private ResultInfo resultInfo;

    @Before
    public void setUp() {
        glRulesServiceMock = mock(GLRulesServiceMock.class, CALLS_REAL_METHODS);
        glRulesServiceMock.setRules(new ArrayList<>());
        tovarTreeServiceMock = new TovarTreeServiceMock();
        processor = new DisableCorrectiveFormalizationStepProcessor(glRulesServiceMock, tovarTreeServiceMock,
            new User(USER_UID));
        tovarTreeServiceMock.addCategory(TovarCategoryBuilder.newBuilder(TOVAR_ID, CATEGORY_ID)
            .setTovarId(TOVAR_ID).setParentHid(PARENT_CATEGORY_ID).create());
        tovarTreeServiceMock.addCategory(TovarCategoryBuilder.newBuilder(RULE_TOVAR_ID, RULE_CATEGORY_ID)
            .setTovarId(RULE_TOVAR_ID).create());
        tovarTreeServiceMock.addCategory(TovarCategoryBuilder.newBuilder(PARENT_TOVAR_ID, PARENT_CATEGORY_ID)
            .setTovarId(PARENT_TOVAR_ID).create());
        context = jobContext(ModelTransferBuilder.newBuilder()
            .sourceCategory(CATEGORY_ID)
            .destinationCategory(TARGET_CATEGORY_ID));
        resultInfo = ResultInfoBuilder.newBuilder(ResultInfo.Status.IN_PROGRESS).build();
    }

    @Test
    public void noRule() {
        DisableCorrectiveFormalizationResult result = processor.executeStep(resultInfo, context);

        assertResult(result, ResultInfo.Status.COMPLETED);
        assertEntry(result, 0, ResultEntry.Status.SUCCESS,
            "В исходную категорию и всех ее родителей не ведут правила смены категории", CATEGORY_ID);
    }

    @Test
    public void sourceCategoryNotFound() {
        context = jobContext(ModelTransferBuilder.newBuilder()
            .sourceCategory(ANOTHER_CATEGORY_ID)
            .destinationCategory(TARGET_CATEGORY_ID));
        DisableCorrectiveFormalizationResult result = processor.executeStep(resultInfo, context);

        assertResult(result, ResultInfo.Status.FAILED);
        assertEntry(result, 0, ResultEntry.Status.FAILURE,
            "Исходная категория не найдена", ANOTHER_CATEGORY_ID);
    }

    @Test
    public void unpublishedRule() {
        GLRule rule = categoryChangeRule(RULE_CATEGORY_ID, CATEGORY_ID, RULE_ID, RULE_NAME, false);
        glRulesServiceMock.addRule(rule, USER_UID, false);

        DisableCorrectiveFormalizationResult result = processor.executeStep(resultInfo, context);

        assertResult(result, ResultInfo.Status.COMPLETED);
        assertEntry(result, 0, ResultEntry.Status.SUCCESS,
            "Правило не опубликовано", CATEGORY_ID,
            glRuleInfo(RULE_CATEGORY_ID, RULE_TOVAR_ID, RULE_ID, RULE_NAME));
    }

    @Test
    public void unpublishSuccess() {
        GLRule rule = categoryChangeRule(RULE_CATEGORY_ID, CATEGORY_ID, RULE_ID, RULE_NAME, true);
        glRulesServiceMock.addRule(rule, USER_UID, false);

        DisableCorrectiveFormalizationResult result = processor.executeStep(resultInfo, context);

        assertResult(result, ResultInfo.Status.COMPLETED);
        assertEntry(result, 0, ResultEntry.Status.SUCCESS,
            "Правило успешно распубликовано", CATEGORY_ID,
            glRuleInfo(RULE_CATEGORY_ID, RULE_TOVAR_ID, RULE_ID, RULE_NAME));
    }

    @Test
    public void unpublishFailure() {
        GLRule rule = categoryChangeRule(RULE_CATEGORY_ID, CATEGORY_ID, RULE_ID, RULE_NAME, true);
        glRulesServiceMock.addRule(rule, USER_UID, false);

        doThrow(new RuntimeException("FAILURE")).when(glRulesServiceMock).saveRules(anyCollection(), anyLong());
        DisableCorrectiveFormalizationResult result = processor.executeStep(resultInfo, context);

        assertResult(result, ResultInfo.Status.FAILED);
        assertEntry(result, 0, ResultEntry.Status.FAILURE,
            "Невозможно распубликовать правило: FAILURE", CATEGORY_ID,
            glRuleInfo(RULE_CATEGORY_ID, RULE_TOVAR_ID, RULE_ID, RULE_NAME));
    }

    @Test
    public void anyEntryFailureAllResultFailed() {
        context = jobContext(ModelTransferBuilder.newBuilder()
            .sourceCategory(CATEGORY_ID)
            .sourceCategory(ANOTHER_CATEGORY_ID)
            .destinationCategory(TARGET_CATEGORY_ID));

        DisableCorrectiveFormalizationResult result = processor.executeStep(resultInfo, context);

        assertResult(result, ResultInfo.Status.FAILED);
        assertEntry(result, 0, ResultEntry.Status.SUCCESS,
            "В исходную категорию и всех ее родителей не ведут правила смены категории", CATEGORY_ID);
        assertEntry(result, 1, ResultEntry.Status.FAILURE,
            "Исходная категория не найдена", ANOTHER_CATEGORY_ID);

    }

    @Test
    public void processAllCategoriesToRoot() {
        GLRule ruleToParent = categoryChangeRule(RULE_CATEGORY_ID, CATEGORY_ID, RULE_ID, RULE_NAME, true);
        GLRule ruleToChild = categoryChangeRule(RULE_CATEGORY_ID, PARENT_CATEGORY_ID,
            ANOTHER_RULE_ID, ANOTHER_RULE_NAME, true);
        glRulesServiceMock.addRule(ruleToParent, USER_UID, false);
        glRulesServiceMock.addRule(ruleToChild, USER_UID, false);

        DisableCorrectiveFormalizationResult result = processor.executeStep(resultInfo, context);

        assertResult(result, ResultInfo.Status.COMPLETED);
        assertEntry(result, 0, ResultEntry.Status.SUCCESS,
            "Правило успешно распубликовано", CATEGORY_ID,
            glRuleInfo(RULE_CATEGORY_ID, RULE_TOVAR_ID, RULE_ID, RULE_NAME));
        assertEntry(result, 1, ResultEntry.Status.SUCCESS,
            "Правило успешно распубликовано", PARENT_CATEGORY_ID,
            glRuleInfo(RULE_CATEGORY_ID, RULE_TOVAR_ID, ANOTHER_RULE_ID, ANOTHER_RULE_NAME));
    }

    private void assertResult(DisableCorrectiveFormalizationResult result,
                              ResultInfo.Status status) {
        ResultInfo ri = result.getResultInfo();
        assertThat(ri.getStatus()).isEqualTo(status);
        String expectedResultText;
        switch (status) {
            case COMPLETED:
                expectedResultText = "Уточняющая формализация успешно отключена";
                break;
            case FAILED:
                expectedResultText = "Ошибка при отключении уточняющей формализации";
                break;
            default:
                expectedResultText = "";
        }
        assertThat(ri.getResultText()).isEqualTo(expectedResultText);
    }

    private void assertEntry(DisableCorrectiveFormalizationResult result,
                             int index,
                             ResultEntry.Status status, String statusMessage,
                             long sourceCategoryId,
                             GLRuleInfo glRuleInfo) {
        CategoryChangeRuleEntry entry = result.getResultEntries().get(index);
        assertThat(entry.getStatus()).isEqualTo(status);
        assertThat(entry.getStatusMessage()).isEqualTo(statusMessage);
        assertThat(entry.getSourceCategoryId()).isEqualTo(sourceCategoryId);
        if (glRuleInfo != null) {
            assertThat(entry.getRuleInfo()).isEqualTo(glRuleInfo);
        }
    }

    private void assertEntry(DisableCorrectiveFormalizationResult result,
                             int index,
                             ResultEntry.Status status, String statusMessage,
                             long sourceCategoryId) {
        assertEntry(result, index, status, statusMessage, sourceCategoryId, null);
    }

    private GLRule categoryChangeRule(long sourceCategoryId, long targetCategoryId,
                                      long ruleId, String name, boolean published) {
        GLRuleBuilder glRuleBuilder = new GLRuleBuilder(GLRuleType.MANUAL, 0);
        glRuleBuilder.setId(ruleId);
        glRuleBuilder.setHid(sourceCategoryId);
        glRuleBuilder.setPublished(published);
        glRuleBuilder.setName(name);
        glRuleBuilder.addThen(targetCategoryId, GLRulePredicate.Subject.CATEGORY_CHANGE);
        return glRuleBuilder.build();
    }

    private GLRuleInfo glRuleInfo(long ruleCategoryId, long ruleTovarId,
                                  long ruleId, String ruleName) {
        GLRuleInfo glRuleInfo = new GLRuleInfo();
        glRuleInfo.setCategoryId(ruleCategoryId);
        glRuleInfo.setTovarId(ruleTovarId);
        glRuleInfo.setRuleId(ruleId);
        glRuleInfo.setRuleName(ruleName);
        return glRuleInfo;
    }

    private ModelTransferJobContext<EmptyStepConfig> jobContext(ModelTransferBuilder modelTransferBuilder) {
        return new ModelTransferJobContext<>(modelTransferBuilder.build(), null, null,
            new EmptyStepConfig(), Collections.emptyList());
    }
}
