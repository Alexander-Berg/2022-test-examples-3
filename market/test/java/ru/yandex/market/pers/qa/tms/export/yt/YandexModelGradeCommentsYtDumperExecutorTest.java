package ru.yandex.market.pers.qa.tms.export.yt;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.service.CommentService;
import ru.yandex.market.pers.qa.tms.export.yt.model.YandexModelGradeCommentYt;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtClientProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class YandexModelGradeCommentsYtDumperExecutorTest extends PersQaTmsTest {
    public static final long GRADE_ID = 123456L;
    public static final long AUTHOR_ID = 123;

    @Autowired
    YandexModelGradeCommentsYtDumperExecutor executor;
    @Autowired
    @Qualifier("yqlJdbcTemplate")
    JdbcTemplate yqlJdbcTemplate;
    @Autowired
    CommentService commentService;
    @Autowired
    YtClientProvider ytClientProvider;
    @Captor
    ArgumentCaptor<List<?>> itemsCaptor;

    @Test
    void testDump() throws Exception {
        Long noParent = null;
        when(yqlJdbcTemplate.queryForList(anyString(), any(Class.class))).thenReturn(Collections.singletonList(GRADE_ID));
        commentService.createVendorComment(CommentProject.GRADE, AUTHOR_ID, "ya text", GRADE_ID, YandexModelGradeCommentsYtDao.YANDEX_VENDOR_ID);
        commentService.createVendorComment(CommentProject.GRADE, AUTHOR_ID + 1, "vendor text", GRADE_ID, YandexModelGradeCommentsYtDao.YANDEX_VENDOR_ID + 1);
        commentService.createComment(CommentProject.GRADE, AUTHOR_ID + 2, "user text", GRADE_ID, noParent);

        executor.dump();

        YtClient ytClient = ytClientProvider.getDefaultClient();
        verify(ytClient).append(isNull(), any(YPath.class), itemsCaptor.capture());
        List<YandexModelGradeCommentYt> items = (List<YandexModelGradeCommentYt>) itemsCaptor.getValue();
        items.sort(Comparator.comparingLong(YandexModelGradeCommentYt::getAuthorId));

        Assertions.assertEquals(3, items.size());
        checkCommentYt(items, 0, true, true);
        checkCommentYt(items, 1, false, true);
        checkCommentYt(items, 2, false, false); // user
    }

    private void checkCommentYt(List<YandexModelGradeCommentYt> items, int index, boolean isYandex, boolean isVendor) {
        Assertions.assertEquals(GRADE_ID, items.get(index).getGradeId().longValue());
        Assertions.assertEquals(AUTHOR_ID + index, items.get(index).getAuthorId().longValue());
        Assertions.assertEquals(isYandex, items.get(index).isYandexComment());
        Assertions.assertEquals(isVendor, items.get(index).isVendorComment());
    }
}
