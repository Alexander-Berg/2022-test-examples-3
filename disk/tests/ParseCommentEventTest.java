package ru.yandex.chemodan.eventlog.log.tests;

import org.joda.time.Instant;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.commentaries.CommentsTskvAction;
import ru.yandex.chemodan.eventlog.events.EventMetadata;
import ru.yandex.chemodan.eventlog.events.EventType;
import ru.yandex.chemodan.eventlog.events.comment.CommentEvent;
import ru.yandex.chemodan.eventlog.events.comment.CommentEventType;
import ru.yandex.chemodan.eventlog.events.comment.CommentRef;
import ru.yandex.chemodan.eventlog.events.comment.EntityRef;
import ru.yandex.chemodan.eventlog.events.comment.LikeDislikeEvent;
import ru.yandex.chemodan.eventlog.events.comment.LikeDislikeEventType;
import ru.yandex.chemodan.eventlog.events.comment.ParentCommentRef;
import ru.yandex.chemodan.mpfs.MpfsUid;

/**
 * @author dbrylev
 */
public class ParseCommentEventTest extends AbstractParseEventTest {

    private static final MpfsUid USER_UID = new MpfsUid(1001197903L);
    private static final MpfsUid USER2_UID = new MpfsUid(2001197904L);
    private static final MpfsUid OWNER_UID = new MpfsUid(3001197905L);

    private static final EventMetadata EVENT_METADATA =
            new EventMetadata(OWNER_UID, new Instant(TIME * 1000), YANDEX_CLOUD_REQUEST_ID);

    private static final EntityRef ENTITY = new EntityRef("public_resource", OWNER_UID.getRawValue() + ":file");
    private static final CommentRef COMMENT = new CommentRef("comment-id", USER2_UID, "Comment text");
    private static final CommentRef COMMENT_NO_TEXT =
            new CommentRef(Option.of("comment-id"), Option.of(USER2_UID), Option.empty());
    private static final ParentCommentRef PARENT = new ParentCommentRef("parent-id", USER_UID);

    @Test
    public void testCommentReplyAdd() {
        assertParseEquals(UID, CommentsTskvAction.COMMENT_ADD.value(), ""
                + "user_uid=" + USER_UID.getRawValue() + "\t"
                + "entity_type=" + ENTITY.type + "\t"
                + "entity_id=" + ENTITY.id + "\t"
                + "comment_id=" + COMMENT.commentId.get() + "\t"
                + "comment_author_uid=" + COMMENT.commentAuthorUid.get().getRawValue() + "\t"
                + "parent_comment_id=" + PARENT.parentCommentId.get() + "\t"
                + "parent_author_uid=" + PARENT.parentAuthorUid.get().getRawValue() + "\t"
                + "comment_text=" + COMMENT.commentText.get(),

                new CommentEvent(
                        CommentEventType.COMMENT_ADD, EVENT_METADATA, USER_UID, ENTITY, COMMENT, PARENT, Cf.list()),
                EventType.COMMENT_ADD);
    }

    @Test
    public void testDislikeDelete() {
        assertParseEquals(UID, CommentsTskvAction.DISLIKE_DELETE.value(), ""
                + "user_uid=" + USER_UID.getRawValue() + "\t"
                + "entity_type=" + ENTITY.type + "\t"
                + "entity_id=" + ENTITY.id + "\t"
                + "comment_id=" + COMMENT.commentId.get() + "\t"
                + "comment_author_uid=" + COMMENT.commentAuthorUid.get().getRawValue(),

                new LikeDislikeEvent(
                        LikeDislikeEventType.DISLIKE_DELETE,
                        EVENT_METADATA,
                        USER_UID,
                        ENTITY,
                        COMMENT_NO_TEXT,
                        Cf.list()),
                EventType.COMMENT_DISLIKE_DELETE);
    }
}
