package ru.yandex.chemodan.app.eventloader.serializer.tests;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.eventlog.events.comment.CommentEvent;
import ru.yandex.chemodan.eventlog.events.comment.CommentEventType;
import ru.yandex.chemodan.eventlog.events.comment.CommentRef;
import ru.yandex.chemodan.eventlog.events.comment.CommentType;
import ru.yandex.chemodan.eventlog.events.comment.EntityRef;
import ru.yandex.chemodan.eventlog.events.comment.LikableType;
import ru.yandex.chemodan.eventlog.events.comment.LikeDislikeEvent;
import ru.yandex.chemodan.eventlog.events.comment.LikeDislikeEventType;
import ru.yandex.chemodan.eventlog.events.comment.ParentCommentRef;
import ru.yandex.chemodan.mpfs.MpfsFileInfo;
import ru.yandex.chemodan.mpfs.MpfsFileMetaDto;
import ru.yandex.chemodan.mpfs.MpfsUid;

/**
 * @author dbrylev
 */
public class SerializeCommentEventTest extends AbstractSerializeEventTest {
    private static final MpfsUid USER_UID = new MpfsUid(1001197903L);
    private static final MpfsUid USER2_UID = new MpfsUid(2001197904L);
    private static final MpfsUid OWNER_UID = new MpfsUid(3001197905L);

    private static final EntityRef ENTITY = new EntityRef("public_resource", OWNER_UID.getRawValue() + ":file")
            .withFileInfo(new MpfsFileInfo("XXX", "dir", Mockito.mock(MpfsFileMetaDto.class)));
    private static final CommentRef COMMENT = new CommentRef("comment-id", USER2_UID, "Comment text");
    private static final ParentCommentRef PARENT = new ParentCommentRef("parent-id", USER_UID);

    @Test
    public void testCommentReplyAdd() {
        new ExpectedJson()
                .withPerformer(USER_UID)
                .withEntityRef(ENTITY)
                .withCommentRef(COMMENT)
                .withParentCommentRef(PARENT)
                .with("comment_type", CommentType.REPLY.value())
                .serializeAndCheck(new CommentEvent(
                        CommentEventType.COMMENT_ADD, METADATA, USER_UID, ENTITY, COMMENT, PARENT, Cf.list()));
    }

    @Test
    public void testDislikeDelete() {
        new ExpectedJson()
                .withPerformer(USER_UID)
                .withEntityRef(ENTITY)
                .withCommentRef(COMMENT)
                .with("comment_type", LikableType.COMMENT.value())
                .serializeAndCheck(new LikeDislikeEvent(
                        LikeDislikeEventType.DISLIKE_DELETE, METADATA, USER_UID, ENTITY, COMMENT, Cf.list()));
    }
}
