package ru.yandex.market.jmf.module.comment.test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.logic.def.Attachment;
import ru.yandex.market.jmf.module.automation.AutomationRule;
import ru.yandex.market.jmf.module.automation.test.AbstractAutomationRuleTest;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.test.impl.CommentTestUtils;
import ru.yandex.market.jmf.utils.Maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringJUnitConfig(InternalModuleCommentTestConfiguration.class)
public class AutomationRuleCommentTest extends AbstractAutomationRuleTest {

    @Inject
    CommentTestUtils commentTestUtils;

    @Test
    public void commentRuleAction() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        createApprovedEventRule(entity2, "/automation_rules/commentRuleAction.json");
        startTrigger(entity1, entity2);

        List<Comment> comments = commentTestUtils.getComments(entity1);
        assertEquals(1, comments.size());
        assertEquals(
                String.format("comment %s", gidService.parse(entity1.getGid()).getId()),
                comments.get(0).getBody()
        );
    }

    /**
     * Добавление одного комментария с вложением в действии "Редактирование объекта"
     */
    @Test
    public void commentWithAttachmentRuleAction() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        Attachment attachment = createAttachment();

        AutomationRule rule = createApprovedEventRule(entity2,
                "/automation_rules/commentWithAttachmentRuleAction.json", attachment.getGid());
        Set<Attachment> attachments = getAttachments(rule);
        assertEquals(Set.of(attachment), attachments);

        startTrigger(entity1, entity2);

        List<Comment> comments = commentTestUtils.getComments(entity1);
        assertEquals(1, comments.size());

        attachments = getAttachments(comments.get(0));
        assertEquals(1, attachments.size());
        assertNotEquals(Set.of(attachment), attachments);
    }

    /**
     * Добавление нескольких комментариев с вложением в действии "Редактирование объекта"
     */
    @Test
    public void commentsWithAttachmentsRuleAction() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        Attachment attachment1 = createAttachment();
        Attachment attachment2 = createAttachment();
        Attachment attachment3 = createAttachment();

        AutomationRule rule = createApprovedEventRule(entity2,
                "/automation_rules/commentsWithAttachmentsRuleAction.json",
                attachment1.getGid(), attachment2.getGid(), attachment3.getGid());
        Set<Attachment> attachments = getAttachments(rule);
        assertEquals(Set.of(attachment1, attachment2, attachment3), attachments);

        startTrigger(entity1, entity2);

        List<Comment> comments = commentTestUtils.getComments(entity1);
        assertEquals(2, comments.size());

        attachments = getAttachments(comments.get(0));
        assertEquals(2, attachments.size());
        assertNotEquals(Set.of(attachment1, attachment2), attachments);

        attachments = getAttachments(comments.get(1));
        assertEquals(1, attachments.size());
        assertNotEquals(Set.of(attachment3), attachments);
    }

    /**
     * Проверка, что все вложения из вложенных условий связываются с самим правилом
     */
    @Test
    public void nestedCommentWithAttachmentRuleAction() {
        Set<Attachment> attachments = Set.of(
                createAttachment(),
                createAttachment(),
                createAttachment(),
                createAttachment(),
                createAttachment(),
                createAttachment(),
                createAttachment()
        );

        String[] attachmentGids = attachments.stream()
                .map(Attachment::getGid)
                .toArray(String[]::new);


        AutomationRule rule = createApprovedEventRule(null,
                "/automation_rules/nestedCommentWithAttachmentRuleAction.json", attachmentGids);
        Set<Attachment> actualAttachments = getAttachments(rule);
        assertEquals(attachments, actualAttachments);
    }

    private Attachment createAttachment() {
        return bcpService.create(Attachment.FQN_DEFAULT, Maps.of(
                Attachment.NAME, Randoms.string(),
                Attachment.CONTENT_TYPE, "ContentType",
                Attachment.URL, Randoms.url() + "/" + UUID.randomUUID()
        ));
    }

    private Set<Attachment> getAttachments(Entity entity) {
        return Set.copyOf(dbService.list(Query.of(Attachment.FQN_DEFAULT).withFilters(
                Filters.eq(Attachment.ENTITY, entity))
        ));
    }
}
