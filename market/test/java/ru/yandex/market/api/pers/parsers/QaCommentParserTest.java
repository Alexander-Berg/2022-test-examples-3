package ru.yandex.market.api.pers.parsers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import ru.yandex.market.api.comment.Comment;
import ru.yandex.market.api.util.ApiCollections;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 * @author dimkarp93
 */
public class QaCommentParserTest {
    private static final Comparator<Comment> COMMENT_CMP = Comparator.comparing(Comment::getId);

    private static final Comparator<Entry<String, String>> PARAM_CMP =
        Comparator.<Entry<String, String>, String>comparing(Entry::getKey)
            .thenComparing(Entry::getValue);

    @Test
    public void shouldParseTree() {
        Collection<Comment> comments = new QaCommentsParser().parse(
            ResourceHelpers.getResource("QaCommentsParserTest_comment-tree.json")
        );

        assertEquals(3, comments.size());

        String root = "root-9-0-58021335";

        long uid = 292382595;

        assertTree(
            comments(
                comment(
                    "child-0-10000027",
                    root,
                    body("АПИ отзыв на магазин"),
                    user(uid),
                    date(1505041413),
                    params(param("свойство", "значение"), param("STATUS", "READY")),
                    children(
                        comment(
                            "child-0-10000028",
                            root,
                            body("АПИ отзыв на магазин. Ответ"),
                            user(uid),
                            date(1505041556)
                        ),
                        comment(
                            "child-0-10000029",
                            root,
                            body("АПИ отзыв на магазин. Для удаления"),
                            user(uid),
                            date(1505041601),
                            deleted()
                        )
                    )
                ),
                comment(
                    "child-0-3746107",
                    root,
                    body("Хоронили тещу. Порвали два баяна... =)"),
                    user(0),
                    date(1440423081),
                    params(
                        param("name", "Test comment"),
                        param("email", "kray-zemli@yandex-team.ru"),
                        param("STATUS", "READY")
                    )
                ),
                comment(
                    "child-0-34134152",
                    "root-9-0-524524524",
                    body("Ещё один коммент для другого отзыва"),
                    user(12312414),
                    date(1440423082)
                )
            ),
            comments
        );
    }

    private static void assertCommentInfo(@NotNull Comment expected, @NotNull Comment actual) {
        assertEquals(messageComment("title"), expected.getTitle(), actual.getTitle());
        assertEquals(messageComment("body"), expected.getBody(), actual.getBody());
        assertParams(expected.getParams(), actual.getParams());

        assertEquals(messageComment("id"), expected.getId(), actual.getId());
        assertEquals(messageComment("parent"), expected.getParentId(), actual.getParentId());
        assertEquals(messageComment("root"), expected.getRootId(), actual.getRootId());

        assertEquals(messageComment("date"), expected.getUpdateTimestamp(), actual.getUpdateTimestamp());

        assertEquals(messageComment("deleted"), expected.isDeleted(), actual.isDeleted());

        assertFalse("Blocked in comment must be false", actual.isBlocked());
        assertFalse("Sticky in comment must be false", actual.isSticky());
        assertTrue("Valid in comment must be true", actual.isValid());

        assertEquals(messageComment("user"), expected.getUser().getId(), actual.getUser().getId());
    }

    private static void assertTree(@NotNull Collection<Comment> expected,
                                   @NotNull Collection<Comment> actual) {
        List<Comment> e = Lists.newArrayList(expected);
        Collections.sort(e, COMMENT_CMP);
        List<Comment> a = Lists.newArrayList(actual);
        Collections.sort(a, COMMENT_CMP);

        ApiCollections.zip(e, a, QaCommentParserTest::assertCommentInfo);
        ApiCollections.zip(e, a, (x, y) -> assertTree(x.getChildren(), y.getChildren()));
    }

    private static void assertParams(@NotNull Map<String, String> expected,
                                     @NotNull Map<String, String> actual) {
        assertEquals(expected.size(), actual.size());

        List<Entry<String, String>> e = Lists.newArrayList(expected.entrySet());
        Collections.sort(e, PARAM_CMP);
        List<Entry<String, String>> a = Lists.newArrayList(actual.entrySet());
        Collections.sort(a, PARAM_CMP);

        ApiCollections.zip(e, a, QaCommentParserTest::assertParam);

    }

    private static void assertParam(Entry<String, String> expected, Entry<String, String> actual) {
        assertEquals(messageParam("name"), expected.getKey(), actual.getKey());
        assertEquals(messageParam("value"), expected.getValue(), actual.getValue());
    }

    private static String messageComment(String msg) {
        return "Comment must have same " + msg;
    }

    private static String messageParam(String msg) {
        return "Param must have same " + msg;
    }

    private static Collection<Comment> comments(Comment... comments) {
        return Arrays.asList(comments);
    }

    private static Comment comment(String id, String root, Consumer<Comment>... processors) {
        Comment comment = new Comment();
        comment.setValid(true);
        comment.setBlocked(false);
        comment.setSticky(false);

        comment.setId(id);
        comment.setRootId(root);
        comment.setParentId(root);
        comment.setTitle("");

        for (Consumer<Comment> processor : processors) {
            processor.accept(comment);
        }

        return comment;
    }

    private static Consumer<Comment> date(long unixtime) {
        return c -> c.setUpdateTimestamp(new Date(unixtime * 1000));
    }

    private static Consumer<Comment> deleted() {
        return c -> c.setDeleted(true);
    }

    private static Consumer<Comment> body(String body) {
        return c -> c.setBody(body);
    }

    private static Consumer<Comment> params(Entry<String, String>... params) {
        return c -> c.setParams(makeParams(params));
    }

    private static Entry<String, String> param(String key, String value) {
        return Pair.of(key, value);
    }

    private static Map<String, String> makeParams(Entry<String, String>... params) {
        Map<String, String> map = Maps.newHashMap();
        for (Entry<String, String> param : params) {
            map.put(param.getKey(), param.getValue());
        }
        return map;
    }


    private static Consumer<Comment> user(long id) {
        return c -> {
            Comment.User user = new Comment.User();
            user.setId(id);
            c.setUser(user);
        };
    }

    private static Consumer<Comment> children(Comment... comments) {
        return c -> {
            for (Comment comment : comments) {
                comment.setParentId(c.getId());
                c.addChild(comment);
            }
        };
    }

}
