package ru.yandex.market.pers.qa.controller.api.comment;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.yandex.market.pers.qa.client.dto.AuthorIdDto;
import ru.yandex.market.pers.qa.client.dto.CommentDto;
import ru.yandex.market.pers.qa.client.dto.CommentTreeDto;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.client.model.CommentState;
import ru.yandex.market.pers.qa.controller.dto.CommentResultDto;
import ru.yandex.market.pers.qa.model.CommentStatus;
import ru.yandex.market.pers.qa.model.UserInfo;
import ru.yandex.market.util.ListUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 20.11.2020
 */
public class CommentAssert {
    public static void assertForest(List<CommentTreeDto> forest, TreeChecker... checkers) {
        assertEquals(checkers.length, forest.size(), "forest size check");
        for (int idx = 0; idx < forest.size(); idx++) {
            checkers[idx].accept(forest.get(idx));
        }
    }

    public static void assertResult(List<CommentResultDto> results, ResultChecker... checkers) {
        Map<Long, CommentResultDto> resultsMap = results.stream().collect(Collectors.toMap(
            CommentResultDto::getEntityId,
            x -> x
        ));

        Map<Long, ResultChecker> checkersMap = Stream.of(checkers).collect(Collectors.toMap(
            ResultChecker::getRootId,
            x -> x
        ));

        assertEquals(checkersMap.size(), resultsMap.size(), "result size check");
        for (Long rootId : checkersMap.keySet()) {
            checkersMap.get(rootId).accept(resultsMap.get(rootId));
        }
    }

    public static void assertComments(List<CommentDto> comments, CommentChecker... checkers) {
        checkComments(checkers).accept(comments);
    }

    public static void assertCommentsSet(List<CommentDto> comments, CommentByIdChecker... checkers) {
        checkCommentsSet(checkers).accept(comments);
    }

    public static void assertComment(CommentDto comment, CommentFieldChecker... checkers) {
        checkComment(checkers).accept(comment);
    }

    public static void assertAllComments(List<CommentDto> comments, CommentChecker checker) {
        assertComments(comments, comments.stream()
            .map(x -> checker)
            .toArray(CommentChecker[]::new));
    }

    public static TreeChecker checkTree(CommentProject project, long rootId, CommentChecker... checkers) {
        return tree -> {
            assertEquals(project.getEntityName(), tree.getEntity(), "tree project check");
            assertEquals(rootId, tree.getEntityId(), "tree root check");

            // obvious checks - root is same in all tree, first-level comments have no parent
            assertAllComments(tree.getComments(), checkRoot(project, rootId));
            assertAllComments(tree.getComments(), checkNoParent());
            assertComments(tree.getComments(), checkers);
        };
    }

    public static ResultChecker checkResult(CommentProject project, long rootId, CommentChecker... checkers) {
        return new ResultChecker() {
            @Override
            public long getRootId() {
                return rootId;
            }

            @Override
            public void accept(CommentResultDto result) {
                assertEquals(rootId, result.getEntityId(), "tree root check");

                // obvious checks - root is same in all tree, first-level comments have no parent
                assertAllComments(result.getData(), checkRoot(project, rootId));
                assertAllComments(result.getData(), checkNoParent());
                assertComments(result.getData(), checkers);
            }
        };
    }

    public static TreeChecker checkFlatTree(CommentProject project, long rootId, CommentChecker... checkers) {
        return tree -> {
            assertEquals(project.getEntityName(), tree.getEntity(), "tree project check");
            assertEquals(rootId, tree.getEntityId(), "tree root check");

            // obvious checks - root is same in all tree
            assertAllComments(tree.getComments(), checkRoot(project, rootId));
            assertComments(tree.getComments(), checkers);
        };
    }

    private static Consumer<List<CommentDto>> checkComments(CommentChecker... checkers) {
        return comments -> {
            assertEquals(checkers.length, comments.size(), "comment list check");
            for (int idx = 0; idx < comments.size(); idx++) {
                checkers[idx].accept(comments.get(idx));
            }
        };
    }

    private static Consumer<List<CommentDto>> checkCommentsSet(CommentByIdChecker... checkers) {
        return comments -> {
            Set<Long> commentIds = ListUtils.toSet(comments, CommentDto::getId);
            Map<Long, CommentByIdChecker> checkersMap = ListUtils.toMap(List.of(checkers), CommentByIdChecker::getId);

            assertEquals(checkersMap.keySet(), commentIds, "comment list check");

            for (CommentDto comment : comments) {
                checkersMap.get(comment.getId()).accept(comment);
            }
        };
    }

    public static CommentByIdChecker checkComment(long id, CommentFieldChecker... checkers) {
        return new CommentByIdChecker(id, checkComment(checkers));
    }

    public static CommentChecker checkComment(CommentFieldChecker... checkers) {
        return comment -> {
            for (CommentFieldChecker checker : checkers) {
                checker.accept(comment);
            }
        };
    }

    public static CommentFieldChecker checkId(long id) {
        return comment -> assertEquals(id, comment.getId(), details("id check", comment));
    }

    public static CommentFieldChecker checkText(String text) {
        return comment -> assertEquals(text, comment.getText(), details("text check", comment));
    }

    public static CommentFieldChecker checkRoot(CommentProject project, long rootId) {
        return comment -> {
            assertEquals(project.getId(), comment.getProjectId(), details("project check", comment));
            assertEquals(rootId, comment.getEntityId(), details("root check", comment));
        };
    }

    public static CommentFieldChecker checkParent(Long parent) {
        return comment -> assertEquals(parent, comment.getParentId(), details("parent check", comment));
    }

    public static CommentFieldChecker checkNoParent() {
        return checkParent(null);
    }

    public static CommentFieldChecker checkCanDelete(boolean check) {
        return comment -> assertEquals(check, comment.isCanDelete(), details("canDelete check", comment));
    }

    public static CommentFieldChecker checkPublished(boolean value) {
        return comment -> assertEquals(value, comment.isPublished(), details("published check", comment));
    }

    public static CommentFieldChecker checkState(CommentState state) {
        return comment -> assertEquals(state, comment.getStateEnum().orElse(null), details("state check", comment));
    }

    public static CommentFieldChecker checkStatus(CommentStatus status) {
        return comment -> assertEquals(status.getCode(), comment.getStatus(), details("status check", comment));
    }

    public static CommentFieldChecker checkUser(UserInfo user) {
        return comment -> assertAuthorDto(comment, user, comment.getUser());
    }

    public static CommentFieldChecker checkAuthor(UserInfo user) {
        return comment -> assertAuthorDto(comment, user, comment.getAuthor());
    }

    private static void assertAuthorDto(CommentDto comment, UserInfo user, AuthorIdDto dto) {
        assertEquals(user == null ? null : new AuthorIdDto(user.getType(), user.getId()),
            dto, details("user check", comment));
    }

    public static CommentFieldChecker checkParams(Map<String, String> params) {
        return comment -> assertEquals(params, comment.getParameters(), details("params check (full)", comment));
    }

    public static CommentFieldChecker checkParamsHas(Map<String, String> params) {
        return comment -> {
            Map<String, String> mapWithExpectedKeys = comment.getParameters().entrySet().stream()
                .filter(entry -> params.containsKey(entry.getKey()))
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
            assertEquals(params, mapWithExpectedKeys, details("params check (partial)", comment));
        };
    }

    public static CommentFieldChecker checkChildCount(long firstLevelChildCount,
                                                      long childCount) {
        return comment -> {
            assertEquals(childCount, comment.getChildCount(), details("child count", comment));
            assertEquals(firstLevelChildCount,
                comment.getFirstLevelChildCount(),
                details("child count (first level)", comment));
        };
    }

    public static CommentFieldChecker checkSubComments(CommentChecker... checkers) {
        return comment -> {
            // transfer obvious checks
            assertAllComments(comment.getComments(), checkParent(comment.getId()));
            assertAllComments(comment.getComments(), checkRoot(comment.getProjectEnum(), comment.getEntityId()));
            assertComments(comment.getComments(), checkers);
        };
    }

    public static CommentFieldChecker checkSubCommentsSet(CommentByIdChecker... checkers) {
        return comment -> {
            // transfer obvious checks
            assertAllComments(comment.getComments(), checkParent(comment.getId()));
            assertAllComments(comment.getComments(), checkRoot(comment.getProjectEnum(), comment.getEntityId()));
            assertCommentsSet(comment.getComments(), checkers);
        };
    }

    private static String details(String message, CommentDto comment) {
        return message + " in (" + comment.getId() + ": [" + comment.getText() + "])";
    }

    public interface ResultChecker extends Consumer<CommentResultDto> {
        long getRootId();
    }

    public interface TreeChecker extends Consumer<CommentTreeDto> {
    }

    public interface CommentChecker extends Consumer<CommentDto> {
    }

    public interface CommentFieldChecker extends CommentChecker {
    }

    public static final class CommentByIdChecker implements CommentChecker {
        private final long id;
        private final Consumer<CommentDto> consumer;

        private CommentByIdChecker(long id, Consumer<CommentDto> consumer) {
            this.id = id;
            this.consumer = consumer;
        }

        @Override
        public void accept(CommentDto comment) {
            assertEquals(id, comment.getId());
            consumer.accept(comment);
        }

        public long getId() {
            return id;
        }
    }

}
