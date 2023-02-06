package ru.yandex.market.jmf.module.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ChatMetadataTest {
    @Test
    public void chatMetadata_merge_shouldPreferOtherValues() {
        var origin = new ChatMetadata().withMemberRights(
                new ChatMemberRights()
                        .withWrite(true)
                        .withRemoveUsers(false)
        );
        var other = new ChatMetadata().withMemberRights(
                new ChatMemberRights()
                        .withWrite(false)
                        .withRemoveUsers(true)
        );

        var expected = new ChatMetadata().withMemberRights(
                new ChatMemberRights()
                        .withWrite(false)
                        .withRemoveUsers(true)
        );
        var merged = origin.merge(other);
        assertEquals(expected, merged);
    }

    @Test
    public void chatMetadata_merge_shouldPreferOriginValuesIfNotPresentInOther() {
        var origin = new ChatMetadata().withMemberRights(
                new ChatMemberRights()
                        .withWrite(true)
                        .withRemoveUsers(false)
        );
        var other = new ChatMetadata();

        var expected = new ChatMetadata().withMemberRights(
                new ChatMemberRights()
                        .withWrite(true)
                        .withRemoveUsers(false)
        );
        var merged = origin.merge(other);
        assertEquals(expected, merged);
    }

    @Test
    public void chatMetadata_merge_nonPresentValuesShouldNotBePresentInResult() {
        var origin = new ChatMetadata().withMemberRights(
                new ChatMemberRights()
                        .withWrite(true)
                        .withRemoveUsers(false)
        );
        var other = new ChatMetadata().withMemberRights(
                new ChatMemberRights()
                        .withWrite(false)
                        .withLeave(true)
        );

        var merged = origin.merge(other);
        var notExpected = new ChatMetadata().withMemberRights(
                new ChatMemberRights()
                        .withWrite(false)
                        .withLeave(true)
                        .withRemoveUsers(false)
                        .withJoin(false)
        );
        assertNotEquals(notExpected, merged);
    }

    @Test
    public void chatMetadata_merge_shouldNotModifyOriginAndOther() {
        var origin = new ChatMetadata().withMemberRights(
                new ChatMemberRights()
                        .withWrite(true)
                        .withRemoveUsers(false)
        );
        var other = new ChatMetadata().withMemberRights(
                new ChatMemberRights()
                        .withWrite(false)
                        .withLeave(true)
        );

        var merged = origin.merge(other);

        var expectedOrigin = new ChatMetadata().withMemberRights(
                new ChatMemberRights()
                        .withWrite(true)
                        .withRemoveUsers(false)
        );
        var expectedOther = new ChatMetadata().withMemberRights(
                new ChatMemberRights()
                        .withWrite(false)
                        .withLeave(true)
        );

        assertEquals(expectedOrigin, origin);
        assertEquals(expectedOther, other);
    }
}
