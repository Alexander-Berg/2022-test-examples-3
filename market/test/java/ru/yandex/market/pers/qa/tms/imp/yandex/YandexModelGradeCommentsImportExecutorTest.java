package ru.yandex.market.pers.qa.tms.imp.yandex;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.market.pers.grade.client.GradeClient;
import ru.yandex.market.pers.grade.client.model.GradeType;
import ru.yandex.market.pers.qa.PersQaServiceMockFactory;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.model.Comment;
import ru.yandex.market.pers.qa.model.CommentFilter;
import ru.yandex.market.pers.qa.service.CommentService;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtClientProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

public class YandexModelGradeCommentsImportExecutorTest extends PersQaTmsTest {
    @Autowired
    YtClientProvider ytClientProvider;
    @Autowired
    @Qualifier("yqlJdbcTemplate")
    JdbcTemplate yqlJdbcTemplate;
    @Autowired
    @Qualifier("pgJdbcTemplate")
    JdbcTemplate pgJdbcTemplate;
    @Autowired
    YandexModelGradeCommentsImportExecutor executor;
    @Autowired
    CommentService commentService;
    @Autowired
    private GradeClient gradeClient;

    private static final long UID = 12345;
    private static final long GRADE_ID1 = 12345;
    private static final long GRADE_ID2 = 123456;
    private static final long GRADE_ID3 = 123450;
    private static final long GRADE_ID4 = 1234560;
    private static final String TEXT1 = "родительский комментарий";
    private static final String TEXT2 = "комментарий-ребёнок";
    private static final String TEXT3 = "родительский комментарий 2";
    private static final String TEXT4 = "комментарий-ребёнок 2";


    private List<YandexComment> prepareYandexVendorComments(long parentId) {
        return Arrays.asList(
            new YandexComment(null, TEXT1, GRADE_ID1),
            new YandexComment(parentId, TEXT2, GRADE_ID2)
        );
    }

    private List<YandexComment> prepareYandexVendorComments2(long parentId) {
        return Arrays.asList(
            new YandexComment(null, TEXT3, GRADE_ID3),
            new YandexComment(parentId, TEXT4, GRADE_ID4)
        );
    }

    @Test
    void testImportYandexComments() {
        YtClient ytClient = ytClientProvider.getDefaultClient();
        mockYtClient(ytClient);

        Long parentId = commentService.createComment(CommentProject.GRADE, 12345L, "комментарий", GRADE_ID2);
        Long parentId2 = commentService.createComment(CommentProject.GRADE, 123456L, "комментарий 2", GRADE_ID4);
        mockJdbcTemplateWithComments(prepareYandexVendorComments(parentId), prepareYandexVendorComments2(parentId2));

        executor.importYandexComments();

        Mockito.verify(ytClient).remove(argThat(argument -> argument.name().equals("1")));
        Mockito.verify(ytClient).remove(argThat(argument -> argument.name().equals("2")));
        Mockito.verify(ytClient).remove(argThat(argument -> argument.name().equals("1_media")));
        Mockito.verify(ytClient).remove(argThat(argument -> argument.name().equals("2_media")));

        List<Comment> vendorComments = commentService.getComments(new CommentFilter().userId(YandexVendorRepliesImportExecutor.FAKE_USER))
            .stream()
            .sorted(Comparator.comparingLong(Comment::getRootId))
            .collect(Collectors.toList());;

        Assertions.assertEquals(4, vendorComments.size());

        Assertions.assertNull(vendorComments.get(0).getParentId());
        Assertions.assertEquals(GRADE_ID1, vendorComments.get(0).getRootId());
        Assertions.assertEquals(TEXT1, vendorComments.get(0).getText());

        Assertions.assertNull(vendorComments.get(1).getParentId());
        Assertions.assertEquals(GRADE_ID3, vendorComments.get(1).getRootId());
        Assertions.assertEquals(TEXT3, vendorComments.get(1).getText());

        Assertions.assertEquals(parentId, vendorComments.get(2).getParentId());
        Assertions.assertEquals(GRADE_ID2, vendorComments.get(2).getRootId());
        Assertions.assertEquals(TEXT2, vendorComments.get(2).getText());

        Assertions.assertEquals(parentId2, vendorComments.get(3).getParentId());
        Assertions.assertEquals(GRADE_ID4, vendorComments.get(3).getRootId());
        Assertions.assertEquals(TEXT4, vendorComments.get(3).getText());

        Assertions.assertTrue(pgJdbcTemplate.queryForObject("select true from qa.yandex_vendor_tables where type = ? and table_name = ?", Boolean.class, YandexImportType.COMMENT.getValue(), "3"));
    }

    @Test
    void testImportYandexCommentsWithFixId() {
        YtClient ytClient = ytClientProvider.getDefaultClient();
        mockYtClient(ytClient);

        Long parentId = commentService.createComment(CommentProject.GRADE, UID, "комментарий", GRADE_ID2);
        List<YandexComment> expectedComments = prepareYandexVendorComments(parentId);
        mockJdbcTemplateWithComments(expectedComments, expectedComments);

        Map<Long, Long> gradeToFixId = Map.of(GRADE_ID1, 6724L, GRADE_ID2, 6832423L);
        gradeToFixId.forEach((gradeId, fixId) -> PersQaServiceMockFactory
            .mockGradeWithFixId(gradeClient, gradeId, UID, GradeType.MODEL_GRADE, 472823, fixId));

        executor.importYandexComments();

        Mockito.verify(ytClient).remove(argThat(argument -> argument.name().equals("1")));
        Mockito.verify(ytClient).remove(argThat(argument -> argument.name().equals("2")));

        List<Comment> vendorComments = commentService.getComments(new CommentFilter().userId(YandexVendorRepliesImportExecutor.FAKE_USER))
            .stream()
            .sorted(Comparator.comparingLong(Comment::getRootId))
            .collect(Collectors.toList());

        Assertions.assertEquals(4, vendorComments.size());

        Assertions.assertNull(vendorComments.get(0).getParentId());
        Assertions.assertEquals(gradeToFixId.get(GRADE_ID1), vendorComments.get(0).getRootId());
        Assertions.assertEquals(TEXT1, vendorComments.get(0).getText());

        Assertions.assertNull(vendorComments.get(1).getParentId());
        Assertions.assertEquals(gradeToFixId.get(GRADE_ID1), vendorComments.get(1).getRootId());
        Assertions.assertEquals(TEXT1, vendorComments.get(1).getText());

        Assertions.assertEquals(parentId, vendorComments.get(2).getParentId());
        Assertions.assertEquals(gradeToFixId.get(GRADE_ID2), vendorComments.get(2).getRootId());
        Assertions.assertEquals(TEXT2, vendorComments.get(2).getText());
    }

    private void mockYtClient(YtClient ytClient) {
        Mockito.when(ytClient.list(any())).thenReturn(Arrays.asList("1", "2", "3"), Collections.singletonList("3"),
            Arrays.asList("1_media", "2_media", "3_media"), Collections.singletonList("3_media"));
        pgJdbcTemplate.update("insert into qa.yandex_vendor_tables(type, table_name) values (?, ?), (?, ?), (?, ?), " +
                "(?, ?), (?, ?), (?, ?)",
            YandexImportType.COMMENT.getValue(), "1",
            YandexImportType.COMMENT.getValue(), "2",
            YandexImportType.ANSWER.getValue(), "3",
            YandexImportType.COMMENT.getValue(), "1_media",
            YandexImportType.COMMENT.getValue(), "2_media",
            YandexImportType.ANSWER.getValue(), "3_media");
    }

    private void mockJdbcTemplateWithComments(List<YandexComment> firstExpectedComments, List<YandexComment> secondExpectedComments) {
        Mockito.when(yqlJdbcTemplate.query(
            (String) argThat(argument -> {
                String arg = (String) argument;
                return arg.contains("3") && arg.contains(executor.getImportSubPath()) ||
                    arg.contains("3_media") && arg.contains(executor.getImportSubPath());
            }), any(RowMapper.class))).thenReturn(firstExpectedComments, secondExpectedComments);
    }

}
