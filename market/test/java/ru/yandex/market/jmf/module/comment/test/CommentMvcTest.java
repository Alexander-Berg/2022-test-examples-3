package ru.yandex.market.jmf.module.comment.test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.CrmCollections;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.logic.def.Attachment;
import ru.yandex.market.jmf.logic.def.Bo;
import ru.yandex.market.jmf.logic.def.operations.AttachmentsOperationHandler;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.InternalComment;
import ru.yandex.market.jmf.module.comment.operations.AddCommentOperationHandler;
import ru.yandex.market.jmf.ui.UiMvcConfiguration;
import ru.yandex.market.jmf.ui.controller.TableMultiActionController;
import ru.yandex.market.jmf.ui.controller.actions.MultiActionEditRequest;
import ru.yandex.market.jmf.utils.Maps;

@Transactional
@ExtendWith(SpringExtension.class)
@ContextHierarchy({
        @ContextConfiguration(classes = InternalModuleCommentTestConfiguration.class),
        @ContextConfiguration(classes = UiMvcConfiguration.class)
})
public class CommentMvcTest {
    private static final Fqn FQN = Fqn.of("simpleCommentTest");
    @Inject
    BcpService bcpService;
    @Inject
    DbService dbService;
    @Inject
    TableMultiActionController multiActionController;

    @Test
    public void multiAddCommentWithAttachments() {
        Entity entity1 = createBO();
        Entity entity2 = createBO();
        List<Attachment> attachments = List.of(createAttachment(), createAttachment());

        String commentText = Randoms.string();
        Map<String, Object> properties = Maps.of(
                Comment.BODY, commentText,
                Comment.METACLASS, InternalComment.FQN.toString(),
                AttachmentsOperationHandler.ID, CrmCollections.transform(attachments, Attachment::getGid)
        );
        MultiActionEditRequest request = new MultiActionEditRequest(
                FQN.toString(),
                List.of(entity1.getGid(), entity2.getGid()),
                Map.of(AddCommentOperationHandler.ID, properties)
        );
        multiActionController.edit(request);

        assertCommentWithAttachments(entity1, commentText, attachments);
        assertCommentWithAttachments(entity2, commentText, attachments);
    }

    @Test
    public void multiAddCommentWithoutAttachments() {
        Entity entity1 = createBO();
        Entity entity2 = createBO();

        String commentText = Randoms.string();
        Map<String, Object> properties = Maps.of(
                Comment.BODY, commentText,
                Comment.METACLASS, InternalComment.FQN.toString()
        );
        MultiActionEditRequest request = new MultiActionEditRequest(
                FQN.toString(),
                List.of(entity1.getGid(), entity2.getGid()),
                Map.of(AddCommentOperationHandler.ID, properties)
        );
        multiActionController.edit(request);

        assertCommentWithAttachments(entity1, commentText, Collections.emptyList());
        assertCommentWithAttachments(entity2, commentText, Collections.emptyList());
    }

    private Attachment createAttachment() {
        return bcpService.create(Attachment.FQN_DEFAULT, Maps.of(
                Attachment.NAME, Randoms.string(),
                Attachment.CONTENT_TYPE, Randoms.string(),
                Attachment.URL, Randoms.string()
        ));
    }

    private Comment addComment(Map<String, Object> properties) {
        return bcpService.create(InternalComment.FQN, properties);
    }

    private Bo createBO() {
        return bcpService.create(FQN, Maps.of("title", Randoms.string()));
    }

    private void assertCommentWithAttachments(Entity entity, String commentText, List<Attachment> attachments) {
        List<Comment> comments = getComments(entity);
        Assertions.assertEquals(1, comments.size());
        Comment comment = comments.get(0);
        Assertions.assertEquals(commentText, comment.getBody());
        List<Attachment> commentAttachments = getAttachments(comment);
        Assertions.assertEquals(
                Set.copyOf(CrmCollections.transform(attachments, Attachment::getUrl)),
                Set.copyOf(CrmCollections.transform(commentAttachments, Attachment::getUrl))
        );
    }

    private List<Comment> getComments(Entity entity) {
        Query q = Query.of(Comment.FQN)
                .withFilters(Filters.eq(Comment.ENTITY, entity));
        return dbService.list(q);
    }

    private List<Attachment> getAttachments(Comment comment) {
        Query q = Query.of(Attachment.FQN)
                .withFilters(Filters.eq(Attachment.ENTITY, comment));
        return dbService.list(q);
    }
}
