package ru.yandex.market.mboc.common.services.offers.processing;

import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author yuramalinov
 * @created 31.10.18
 */
public class ContentCommentProcessorTest {
    @Test
    public void testParseItems() {
        assertThat(ContentCommentProcessor.parseContentCommentItems("")).isNull();
        assertThat(ContentCommentProcessor.parseContentCommentItems("a")).containsExactly("a");
        assertThat(ContentCommentProcessor.parseContentCommentItems("a,b")).containsExactly("a", "b");
        assertThat(ContentCommentProcessor.parseContentCommentItems(" a , b ")).containsExactly("a", "b");
    }

    @Test
    public void testParseComments() {
        assertThat(ContentCommentProcessor.parseContentComments(null, null, null, null)).isEmpty();
        assertThat(ContentCommentProcessor.parseContentComments(
            ContentCommentType.NEED_CLASSIFICATION_INFORMATION, Collections.singletonList("test"), null, null))
            .containsExactly(new ContentComment(ContentCommentType.NEED_CLASSIFICATION_INFORMATION, "test"));

        assertThat(ContentCommentProcessor.parseContentComments(
            ContentCommentType.NEED_CLASSIFICATION_INFORMATION, Collections.singletonList("test"),
            ContentCommentType.CONFLICTING_INFORMATION, Collections.singletonList("test2")))
            .containsExactly(
                new ContentComment(ContentCommentType.NEED_CLASSIFICATION_INFORMATION, "test"),
                new ContentComment(ContentCommentType.CONFLICTING_INFORMATION, "test2")
            );

        assertThat(ContentCommentProcessor.parseContentComments(
            ContentCommentType.NEED_CLASSIFICATION_INFORMATION, Collections.singletonList("test"),
            ContentCommentType.NEED_CLASSIFICATION_INFORMATION, Collections.singletonList("test2")))
            .containsExactly(
                new ContentComment(ContentCommentType.NEED_CLASSIFICATION_INFORMATION, "test", "test2")
            );
    }
}
