package ru.yandex.market.jmf.logic.wf.test;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.jmf.logic.wf.WfService;
import ru.yandex.market.jmf.logic.wf.conf.Attribute;
import ru.yandex.market.jmf.logic.wf.conf.AttributeCondition;
import ru.yandex.market.jmf.logic.wf.conf.Status;
import ru.yandex.market.jmf.logic.wf.conf.StatusConf;
import ru.yandex.market.jmf.logic.wf.conf.Transition;
import ru.yandex.market.jmf.logic.wf.conf.TransitionConf;
import ru.yandex.market.jmf.logic.wf.conf.WfAttribute;
import ru.yandex.market.jmf.logic.wf.conf.Workflow;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metainfo.LStringUtils;

/**
 * Проверка сервиса изменения Жизненного цила.
 * Проверяется работа методов сервиса.
 */
@SuppressWarnings("ConstantConditions")
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = InternalLogicWfTestConfiguration.class)
public class WfServiceTest {
    private final Fqn FQN_ROOT = Fqn.of("root");
    private final Fqn FQN_CHILD = Fqn.of("root$entityChild");
    private final Fqn FQN_GTANDCHILD = Fqn.of("root$entityGrandchild");

    @Inject
    private WfService wfService;

    /**
     * Добавляет новый статус.
     * Проверяет что новый статус появился.
     */
    @Test
    void addStatus() {
        Workflow workflow = wfService.get(FQN_ROOT);
        Assertions.assertNotNull(workflow);
        int beforeStatusesCount = workflow.getStatuses().size();

        StatusConf expectedStatus = StatusConf.builder()
                .withCode("statusNew")
                .withTitle(LStringUtils.of("Status New"))
                .withAttributes(StatusConf.Attributes.builder()
                        .withAttribute(Attribute.builder()
                                .withCode("attr1")
                                .withPreCondition(AttributeCondition.FORCE_REQUIRE)
                                .withPostCondition(AttributeCondition.REQUIRE)
                                .build())
                        .build())
                .build();

        Assertions.assertNull(workflow.getStatus(expectedStatus.getCode()));
        wfService.addStatus(FQN_ROOT, expectedStatus, 1);

        workflow = wfService.get(FQN_ROOT);
        Assertions.assertNotNull(workflow);
        Assertions.assertEquals(beforeStatusesCount + 1, workflow.getStatuses().size());

        Status actualStatus = workflow.getStatus(expectedStatus.getCode());
        Assertions.assertNotNull(actualStatus);

        assertEqualsStatus(expectedStatus, actualStatus);
    }

    /**
     * Добавляет новый статус.
     * Проверяет что новый статус появился в потомке.
     */
    @Test
    void addStatus_childHas() {
        Workflow workflow = wfService.get(FQN_ROOT);
        Assertions.assertNotNull(workflow);

        StatusConf expectedStatus = StatusConf.builder()
                .withCode("statusNew")
                .withTitle(LStringUtils.of("Status New"))
                .withAttributes(StatusConf.Attributes.builder()
                        .withAttribute(Attribute.builder()
                                .withCode("attr1")
                                .withPreCondition(AttributeCondition.FORCE_REQUIRE)
                                .withPostCondition(AttributeCondition.REQUIRE)
                                .build())
                        .build())
                .build();

        Assertions.assertNull(workflow.getStatus(expectedStatus.getCode()));
        wfService.addStatus(FQN_ROOT, expectedStatus, 1);

        Workflow actualWorkflowChild = wfService.get(FQN_CHILD);
        Assertions.assertNotNull(actualWorkflowChild);

        Status actualStatus = actualWorkflowChild.getStatus(expectedStatus.getCode());
        Assertions.assertNotNull(actualStatus);

        assertEqualsStatus(expectedStatus, actualStatus);
    }

    /**
     * Добавляет новый статус.
     * Проверяет что новый статус не появился в предке.
     */
    @Test
    void addStatus_parentHasNot() {
        Workflow workflowRootBefore = wfService.get(FQN_ROOT);
        Integer workflowRootBeforeVersion = workflowRootBefore.getVersion();

        Workflow workflowBefore = wfService.get(FQN_CHILD);
        Assertions.assertNotNull(workflowBefore);

        StatusConf expectedStatus = StatusConf.builder()
                .withCode("statusNew")
                .withTitle(LStringUtils.of("Status New"))
                .withAttributes(StatusConf.Attributes.builder()
                        .withAttribute(Attribute.builder()
                                .withCode("attr1")
                                .withPreCondition(AttributeCondition.FORCE_REQUIRE)
                                .withPostCondition(AttributeCondition.REQUIRE)
                                .build())
                        .build())
                .build();

        Assertions.assertNull(workflowBefore.getStatus(expectedStatus.getCode()));
        wfService.addStatus(FQN_CHILD, expectedStatus, 1);

        Workflow actualWorkflowRoot = wfService.get(FQN_ROOT);
        Assertions.assertNotNull(actualWorkflowRoot);
        Assertions.assertEquals(workflowRootBeforeVersion, actualWorkflowRoot.getVersion());

        Status actualStatus = actualWorkflowRoot.getStatus(expectedStatus.getCode());
        Assertions.assertNull(actualStatus);
    }

    /**
     * Редактирует существующий статус.
     * Проверят что статус изменился.
     */
    @Test
    void editStatus() {
        Workflow workflow = wfService.get(FQN_ROOT);
        Assertions.assertNotNull(workflow);
        int beforeStatusesCount = workflow.getStatuses().size();

        StatusConf expectedStatus = StatusConf.builder()
                .withCode("statusRoot1")
                .withTitle(LStringUtils.of("statusRoot1 New"))
                .withArchived(true)
                .withAttributes(StatusConf.Attributes.builder()
                        .withAttribute(Attribute.builder()
                                .withCode("attr1")
                                .withPreCondition(AttributeCondition.FORCE_REQUIRE)
                                .withPostCondition(AttributeCondition.REQUIRE)
                                .build())
                        .build())
                .build();

        Assertions.assertNotNull(workflow.getStatus(expectedStatus.getCode()));
        wfService.editStatus(FQN_ROOT, expectedStatus, 1);

        workflow = wfService.get(FQN_ROOT);
        Assertions.assertEquals(beforeStatusesCount, workflow.getStatuses().size());

        Status actualStatus = workflow.getStatus(expectedStatus.getCode());
        Assertions.assertNotNull(actualStatus);

        assertEqualsStatus(expectedStatus, actualStatus);
    }

    /**
     * Редактирует существующий статус.
     * Проверят что статус изменился в потомке.
     */
    @Test
    void editStatus_childHas() {
        StatusConf expectedStatus = StatusConf.builder()
                .withCode("statusRoot1")
                .withTitle(LStringUtils.of("statusRoot1 New"))
                .withArchived(true)
                .withAttributes(StatusConf.Attributes.builder()
                        .withAttribute(Attribute.builder()
                                .withCode("attr1")
                                .withPreCondition(AttributeCondition.FORCE_REQUIRE)
                                .withPostCondition(AttributeCondition.REQUIRE)
                                .build())
                        .build())
                .build();

        wfService.editStatus(FQN_ROOT, expectedStatus, 1);

        Workflow actualWorkflowChild = wfService.get(FQN_CHILD);
        Status actualStatus = actualWorkflowChild.getStatus(expectedStatus.getCode());

        assertEqualsStatus(expectedStatus, actualStatus);
    }

    /**
     * Редактирует существующий статус.
     * Проверят что статус не изменился в предке.
     */
    @Test
    void editStatus_parentHasNot() {
        Status expectedStatus = wfService.get(FQN_CHILD).getStatus("statusRoot1");

        StatusConf editStatus = StatusConf.builder()
                .withCode("statusRoot1")
                .withTitle(LStringUtils.of("statusRoot1 New"))
                .withArchived(true)
                .withAttributes(StatusConf.Attributes.builder()
                        .withAttribute(Attribute.builder()
                                .withCode("attr1")
                                .withPreCondition(AttributeCondition.FORCE_REQUIRE)
                                .withPostCondition(AttributeCondition.REQUIRE)
                                .build())
                        .build())
                .build();

        wfService.editStatus(FQN_CHILD, editStatus, 1);

        Workflow actualWorkflowChild = wfService.get(FQN_ROOT);
        Status actualStatus = actualWorkflowChild.getStatus(expectedStatus.getCode());

        assertEqualsStatus(expectedStatus, actualStatus);
    }

    /**
     * Добавляет статус и редактирует в потомке.
     * Проверяет что новый статус появился.
     * Проверяет что статус изменился в потомке.
     */
    @Test
    void addEditStatus_addRootEditChild() {
        StatusConf newStatus = StatusConf.builder()
                .withCode("statusNew")
                .withTitle(LStringUtils.of("Status New"))
                .withAttributes(StatusConf.Attributes.builder()
                        .withAttribute(Attribute.builder()
                                .withCode("attr1")
                                .withPreCondition(AttributeCondition.FORCE_REQUIRE)
                                .withPostCondition(AttributeCondition.REQUIRE)
                                .build())
                        .build())
                .build();

        StatusConf editNewStatus = StatusConf.builder()
                .withCode("statusNew")
                .withTitle(LStringUtils.of("Status New Edit"))
                .withAttributes(StatusConf.Attributes.builder()
                        .withAttribute(Attribute.builder()
                                .withCode("attr1")
                                .withPreCondition(AttributeCondition.NONE)
                                .withPostCondition(AttributeCondition.NONE)
                                .build())
                        .build())
                .build();

        wfService.addStatus(FQN_ROOT, newStatus, 1);
        wfService.editStatus(FQN_CHILD, editNewStatus, 2);

        Workflow actualWorkflowRoot = wfService.get(FQN_ROOT);
        Workflow actualWorkflowChild = wfService.get(FQN_CHILD);

        Status actualStatusRoot = actualWorkflowRoot.getStatus(newStatus.getCode());
        assertEqualsStatus(newStatus, actualStatusRoot);

        Status actualStatusChild = actualWorkflowChild.getStatus(editNewStatus.getCode());
        assertEqualsStatus(editNewStatus, actualStatusChild);
    }

    /**
     * Добавляет новый переход статусов.
     * Проверяет что новый переход появился и соответствует тому что добавили.
     */
    @Test
    void addTransition() {
        Workflow workflow = wfService.get(FQN_ROOT);
        Assertions.assertNotNull(workflow);

        TransitionConf expectedTransition = TransitionConf.builder()
                .withFrom("statusRoot1")
                .withTo("statusRoot2")
                .withTitle(LStringUtils.of("1 to 2"))
                .withCommentTitle(LStringUtils.of("1 to 2 comment"))
                .withEnabled(true)
                .build();

        Assertions.assertNull(workflow.getStatus(expectedTransition.getFrom()).getTransition(expectedTransition.getTo()));
        wfService.addTransition(FQN_ROOT, expectedTransition, 1);

        workflow = wfService.get(FQN_ROOT);
        Transition actualTransition =
                workflow.getStatus(expectedTransition.getFrom()).getTransition(expectedTransition.getTo());
        Assertions.assertNotNull(actualTransition);

        assertEqualsTransition(expectedTransition, actualTransition);
    }

    /**
     * Редактирует существующий переход статусов.
     * Проверят что переход изменился соответствует изменениям.
     */
    @Test
    void editTransition() {
        Workflow workflow = wfService.get(FQN_ROOT);
        Assertions.assertNotNull(workflow);

        TransitionConf expectedTransition = TransitionConf.builder()
                .withFrom("statusRootInitial")
                .withTo("statusRoot1")
                .withTitle(LStringUtils.of("init to 1 edited"))
                .withCommentTitle(LStringUtils.of("init to 2 comment edited"))
                .withEnabled(true)
                .build();

        Assertions.assertNotNull(workflow.getStatus(expectedTransition.getFrom()).getTransition(expectedTransition.getTo()));
        wfService.editTransition(FQN_ROOT, expectedTransition, 1);

        workflow = wfService.get(FQN_ROOT);
        Transition actualTransition =
                workflow.getStatus(expectedTransition.getFrom()).getTransition(expectedTransition.getTo());
        Assertions.assertNotNull(actualTransition);

        assertEqualsTransition(expectedTransition, actualTransition);
    }

    private void assertEqualsStatus(StatusConf expectedStatus, Status actualStatus) {
        Assertions.assertNotNull(expectedStatus);
        Assertions.assertNotNull(actualStatus);

        Assertions.assertEquals(expectedStatus.getCode(), actualStatus.getCode());
        Assertions.assertEquals(
                LStringUtils.get(expectedStatus.getTitle(), LStringUtils.DEFAULT_LANG), actualStatus.getTitle());
        Assertions.assertEquals(expectedStatus.getArchived(), actualStatus.isArchived());
        Assertions.assertEquals(
                expectedStatus.getAttributes().getAttribute().size(), actualStatus.getAttributes().size());
        for (Attribute expectedAttribute : expectedStatus.getAttributes().getAttribute()) {
            WfAttribute actualAttribute = actualStatus.getAttributes().stream()
                    .filter(a -> a.getCode().equals(expectedAttribute.getCode()))
                    .findAny()
                    .orElse(null);

            Assertions.assertNotNull(actualAttribute);
            Assertions.assertEquals(expectedAttribute.getCode(), actualAttribute.getCode());
            Assertions.assertEquals(expectedAttribute.getPreCondition(), actualAttribute.getPreCondition());
            Assertions.assertEquals(expectedAttribute.getPostCondition(), actualAttribute.getPostCondition());
        }
    }

    private void assertEqualsStatus(Status expectedStatus, Status actualStatus) {
        Assertions.assertNotNull(expectedStatus);
        Assertions.assertNotNull(actualStatus);

        Assertions.assertEquals(expectedStatus.getCode(), actualStatus.getCode());
        Assertions.assertEquals(expectedStatus.getTitle(), actualStatus.getTitle());
        Assertions.assertEquals(expectedStatus.isArchived(), actualStatus.isArchived());
        Assertions.assertEquals(expectedStatus.getAttributes().size(), actualStatus.getAttributes().size());
        for (WfAttribute expectedAttribute : expectedStatus.getAttributes()) {
            WfAttribute actualAttribute = actualStatus.getAttributes().stream()
                    .filter(a -> a.getCode().equals(expectedAttribute.getCode()))
                    .findAny()
                    .orElse(null);

            Assertions.assertNotNull(actualAttribute);
            Assertions.assertEquals(expectedAttribute.getCode(), actualAttribute.getCode());
            Assertions.assertEquals(expectedAttribute.getPreCondition(), actualAttribute.getPreCondition());
            Assertions.assertEquals(expectedAttribute.getPostCondition(), actualAttribute.getPostCondition());
        }
    }

    private void assertEqualsTransition(TransitionConf expectedTransition, Transition actualTransition) {
        Assertions.assertNotNull(expectedTransition);
        Assertions.assertNotNull(actualTransition);

        Assertions.assertEquals(expectedTransition.getFrom(), actualTransition.getFrom().getCode());
        Assertions.assertEquals(expectedTransition.getTo(), actualTransition.getTo().getCode());
        Assertions.assertEquals(
                LStringUtils.get(expectedTransition.getTitle(), LStringUtils.DEFAULT_LANG),
                actualTransition.getTitle());
        Assertions.assertEquals(
                LStringUtils.get(expectedTransition.getCommentTitle(), LStringUtils.DEFAULT_LANG),
                actualTransition.getCommentTitle());
        Assertions.assertEquals(expectedTransition.getEnabled(), actualTransition.isEnabled());
    }
}
