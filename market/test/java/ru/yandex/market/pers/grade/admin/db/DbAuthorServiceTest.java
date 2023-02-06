package ru.yandex.market.pers.grade.admin.db;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.filter.QueryFilter;
import ru.yandex.common.framework.pager.Pager;
import ru.yandex.market.pers.grade.admin.action.monitoring.author.AuthorInfoFilter;
import ru.yandex.market.pers.grade.admin.base.BaseGradeAdminDbTest;
import ru.yandex.market.pers.grade.core.author.AuthorService;

/**
 * @author Sergey Simonchik ssimonchik@yandex-team.ru
 */
public class DbAuthorServiceTest extends BaseGradeAdminDbTest {
    @Autowired
    private AuthorService authorService;

    @Test
    public void testExtraAuthorInfo() {
        printXmlConvertable(authorService.getExtraInfo(22892095L));
    }

    @Test
    public void testCriminalRecordsByAuthor() {
        ServRequest req = getServRequest();
        final long authorId = 9051607L;
        QueryFilter filter = new AuthorInfoFilter(req, authorId);
        Pager pager = getTestPager();
        printXmlConvertableList(authorService.getCriminalRecordsByAuthor(filter, pager, authorId));
        System.out.println(authorService.countCriminalRecordsByAuthor(filter));
    }
}
