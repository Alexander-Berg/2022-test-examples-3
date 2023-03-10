package ru.yandex.market.jmf.module.comment.test;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.CrmCollections;
import ru.yandex.market.crm.util.CrmStrings;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.RequiredAttributesValidationException;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.logic.def.Attachment;
import ru.yandex.market.jmf.logic.def.Bo;
import ru.yandex.market.jmf.logic.def.EntityHistory;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.InternalComment;
import ru.yandex.market.jmf.module.comment.PublicComment;
import ru.yandex.market.jmf.script.ScriptService;
import ru.yandex.market.jmf.utils.Maps;

@Transactional
@SpringJUnitConfig(InternalModuleCommentTestConfiguration.class)
public class CommentTest {

    private static final Fqn FQN = Fqn.of("simpleCommentTest");

    @Inject
    BcpService bcpService;
    @Inject
    DbService dbService;
    @Inject
    ScriptService scriptService;

    @Test
    public void addCommentOnCreate() {
        String value = Randoms.string();
        Map<String, Object> attributes = Map.of(
                "title", Randoms.string(),
                "@comment", Map.of(
                        Comment.METACLASS, PublicComment.FQN.toString(),
                        Comment.BODY, value
                )
        );
        Bo entity = bcpService.create(FQN, attributes);

        dbService.flush();

        assertHasHistory(entity, "addComment", 1);
    }

    @Test
    public void addInternalCommentWithoutBody() {
        Entity entity = createBO();
        Attachment attachment = createAttachment();

        Map<String, Object> properties = Maps.of(
                Comment.BODY, CrmStrings.EMPTY_STRING,
                Comment.ENTITY, entity,
                "@attachments", attachment.getGid()
        );
        Comment result = addComment(properties);

        dbService.flush();

        Assertions.assertEquals(attachment.getEntity(), result.getGid());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void addCommentsOnCreate() {
        String internalBody = Randoms.string();
        String publicBody = Randoms.string();
        String userBody = Randoms.string();
        String userName = Randoms.string();
        Map<String, Object> attributes = Map.of(
                "title", Randoms.string(),
                "@comments", List.of(
                        Map.of(
                                Comment.METACLASS, InternalComment.FQN.toString(),
                                Comment.BODY, internalBody),
                        Map.of(
                                Comment.METACLASS, PublicComment.FQN.toString(),
                                Comment.BODY, publicBody),
                        Map.of(
                                Comment.METACLASS, Comment.FQN_USER.toString(),
                                Comment.BODY, userBody,
                                "userName", userName)
                )
        );
        Bo entity = bcpService.create(FQN, attributes);

        dbService.flush();

        assertHasHistory(entity, "addComment", 3);

        Query q = Query.of(Comment.FQN)
                .withFilters(Filters.eq(Comment.ENTITY, entity));
        List<Comment> list = dbService.list(q);
        Assertions.assertEquals(3, list.size(), "???????????? ???????? ?????????????? ??????????????????????");

        for (Comment comment : list) {
            if (comment.getFqn().equals(InternalComment.FQN)) {
                Assertions.assertEquals(internalBody, comment.getBody(), "???????? ?????????????????????? ???? ??????????????");
            } else if (comment.getFqn().equals(PublicComment.FQN)) {
                Assertions.assertEquals(publicBody, comment.getBody(), "???????? ?????????????????????? ???? ??????????????");
            } else if (comment.getFqn().equals(Comment.FQN_USER)) {
                Assertions.assertEquals(userBody, comment.getBody(), "???????? ?????????????????????? ???? ??????????????");
                Assertions.assertEquals(userName, comment.getAttribute(
                        "userName"), "?????? ???????????????????????? ?????????????????????? ???? ??????????????");
            }
        }
    }

    @Test
    public void addComment() {
        Bo entity = createBO();

        String value = Randoms.string();
        Comment result = addComment(entity, value);

        dbService.flush();

        Assertions.assertEquals(entity.getGid(), result.getEntity());
        Assertions.assertEquals(value, result.getBody());
        // ?????????? ?????? ???? ?????? ???????????????? ?????????????????????? ???????????????? entityCreationTime
        // ??.??. ???? ?????????????? ?????????? ?????????????? ???????????????????? ?????????????????????????????????? ???????????????????????? ?? ???? ??????????
        Assertions.assertEquals(entity.getCreationTime(), result.getEntityCreationTime());
        assertHasHistory(entity, "addComment", 1);
    }

    @Test
    public void addCommentWithAttachment() {
        Entity entity = createBO();
        Attachment attachment = createAttachment();

        Map<String, Object> properties = Maps.of(
                Comment.BODY, Randoms.string(),
                Comment.ENTITY, entity,
                "@attachments", attachment.getGid()
        );
        Comment result = addComment(properties);

        dbService.flush();

        Assertions.assertEquals(result.getGid(), attachment.getEntity(), "attachment ???????????? ???????? ???????????? ?? " +
                "????????????????????????");
    }

    @Test
    public void addComment_script_internal() {
        Entity entity = createBO();

        String bodyPart = Randoms.string();
        Map<String, Object> variables = Maps.of(
                "e", entity,
                "body", bodyPart
        );
        HasGid result = scriptService.execute("api.comments.createInternal(e, \"$body\")", variables);

        Comment comment = dbService.get(result.getGid());
        Assertions.assertEquals(InternalComment.FQN, comment.getFqn());
        Assertions.assertEquals(bodyPart, comment.getAttribute("body"));
    }

    @Test
    public void addComment_script_internal_null() {
        Entity entity = createBO();

        Map<String, Object> variables = Maps.of(
                "e", entity
        );
        Assertions.assertThrows(RequiredAttributesValidationException.class, () -> scriptService.execute("api" +
                ".comments.createInternal(e, null)", variables));
    }

    @Test
    public void addComment_script_public() {
        Entity entity = createBO();

        String bodyPart = Randoms.string();
        Map<String, Object> variables = Maps.of(
                "e", entity,
                "body", bodyPart
        );
        HasGid result = scriptService.execute("api.comments.createPublic(e, \"$body\")", variables);

        Comment comment = dbService.get(result.getGid());
        Assertions.assertEquals(PublicComment.FQN, comment.getFqn());
        Assertions.assertEquals(bodyPart, comment.getAttribute("body"));
    }

    @Test
    public void editComment() {
        Entity entity = createBO();
        Comment result = addComment(entity, Randoms.string());

        String value = Randoms.string();
        bcpService.edit(result, Maps.of(Comment.BODY, value));

        dbService.flush();

        Assertions.assertEquals(entity.getGid(), result.getEntity());
        Assertions.assertEquals(value, result.getBody());
        assertHasHistory(entity, "editComment", 1);
    }

    @Test
    public void editComment_entity() throws Exception {
        Bo entity0 = createBO();
        Comment result = addComment(entity0, Randoms.string());

        dbService.flush();

        // ???????????????? ?????? ???? ?????????????????????????? ???????????????????? ?????????? ???????????????? entity
        Thread.sleep(1000);

        Bo entity = createBO();
        Assertions.assertNotEquals(
                entity0.getCreationTime(), entity.getCreationTime(),
                "?????? ???????????????????? ???????????? ?????????? ?????????????????? ?????? ???? ?????????? ???????????????? ???????????????? ????????????????????");
        bcpService.edit(result, Maps.of(Comment.ENTITY, entity));

        dbService.flush();

        Assertions.assertEquals(entity.getGid(), result.getEntity());
        // ?????????? ?????? ???? ?????? ?????????????????? entity ???????????????? ???????????????? entityCreationTime
        // ??.??. ???? ?????????????? ?????????? ?????????????? ???????????????????? ?????????????????????????????????? ???????????????????????? ?? ???? ??????????
        Assertions.assertEquals(entity.getCreationTime(), result.getEntityCreationTime());
    }

    @Test
    public void deleteComment() {
        Entity entity = createBO();
        Comment result = addComment(entity, Randoms.string());

        bcpService.delete(result);

        dbService.flush();

        assertHasHistory(entity, "deleteComment", 1);
    }

    @Test
    public void filterComments() {
        Entity entity = createBO();
        addComment(entity, Randoms.string());
        addComment(entity, Randoms.string());

        dbService.flush();

        List<Comment> result = getComments(entity);

        Assertions.assertEquals(2,
                result.size(), "???????????? ???????????????? ?????? ?????????????????????? ??.??. ???????? ?????????????????? 2 ?????????????????????? ?? entity");
    }

    @Test
    public void filterComments_many() {
        Entity entity0 = createBO();
        addComment(entity0, Randoms.string());

        Entity entity1 = createBO();
        addComment(entity1, Randoms.string());

        dbService.flush();

        Query q = Query.of(Comment.FQN)
                .withFilters(Filters.in(Comment.ENTITY, CrmCollections.asSet(null, entity0, entity1.getGid())));
        List<Comment> result = dbService.list(q);

        Assertions.assertEquals(2,
                result.size(), "???????????? ???????????????? 2 ?????????????????????? ??.??. ???????? ?????????????????? ???? ???????????? ?????????????????????? ?? entity");
    }

    @Test
    public void filterComments_null() {
        addComment(createBO(), Randoms.string());
        addComment(createBO(), Randoms.string());

        dbService.flush();

        List<Comment> result = getComments(null);

        Assertions.assertEquals(
                0, result.size(), "???????????? ???????????????? ???????????? ???????????? ??.??. ???????????? ?????????????? ?????????????????????? ?????????????????????????? ?? " +
                        "??????????????");
    }

    @Test
    public void addHtmlCommentWithHtmlLogic() {
        Entity entity = createBO();

        String commentText = Randoms.string();
        Map<String, Object> properties = Maps.of(
                Comment.BODY, "<mytag>" + commentText + "</mytag>",
                Comment.ENTITY, entity
        );
        Comment result = addComment(properties, InternalComment.FQN);

        dbService.flush();

        Assertions.assertEquals(result.getBody(), commentText);
    }

    @Test
    public void addPublicCommentWithoutBody() {
        Entity entity = createBO();
        Attachment attachment = createAttachment();

        Map<String, Object> properties = Maps.of(
                Comment.BODY, CrmStrings.EMPTY_STRING,
                Comment.ENTITY, entity,
                "@attachments", attachment.getGid()
        );
        Assertions.assertThrows(RequiredAttributesValidationException.class, () -> addComment(properties,
                PublicComment.FQN));
    }

    @Test
    public void commentShouldBeMarkedAsEditedAfterChangingBody() {
        Entity entity = createBO();
        Comment comment = addComment(entity, Randoms.string());
        Assertions.assertFalse(comment.isEdited());

        var newBody = Randoms.string();
        bcpService.edit(comment, Maps.of(Comment.BODY, newBody));
        Assertions.assertTrue(getComments(entity).get(0).isEdited());
    }

    @Test
    public void commentShouldNotBeMarkedAsEditedAfterChangingOtherAttributes() {
        Entity entity = createBO();
        Entity entityTwo = createBO();
        Comment comment = addComment(entity, Randoms.string());
        Assertions.assertFalse(comment.isEdited());

        bcpService.edit(comment, Maps.of(Comment.ENTITY, entityTwo));
        Assertions.assertFalse(getComments(entityTwo).get(0).isEdited());
    }


    private Bo createBO() {
        return bcpService.create(FQN, Maps.of("title", Randoms.string()));
    }

    private Attachment createAttachment() {
        return bcpService.create(Attachment.FQN_DEFAULT, Maps.of(
                Attachment.NAME, Randoms.string(),
                Attachment.CONTENT_TYPE, Randoms.string(),
                Attachment.URL, Randoms.string()
        ));
    }

    private Comment addComment(Entity bo, String body) {
        return addComment(Maps.of(Comment.BODY, body, Comment.ENTITY, bo));
    }

    private Comment addComment(Map<String, Object> properties, Fqn fqn) {
        return bcpService.create(fqn, properties);
    }

    private Comment addComment(Map<String, Object> properties) {
        return addComment(properties, InternalComment.FQN);
    }

    private void assertHasHistory(Entity entity, String process, long expectedHistoryCount) {
        Query q = Query.of(EntityHistory.FQN)
                .withFilters(
                        Filters.eq(EntityHistory.ENTITY, entity),
                        Filters.eq(EntityHistory.PROCESS, process)
                );

        List<Entity> list = dbService.list(q);
        Assertions.assertEquals(expectedHistoryCount, list.size(), "???????????? ???????? ?????????????? " + process);
    }

    private List<Comment> getComments(Entity entity) {
        Query q = Query.of(Comment.FQN)
                .withFilters(Filters.eq(Comment.ENTITY, entity));
        return dbService.list(q);
    }

}
