package ru.yandex.market.pers.grade.admin.base;

import java.util.List;

import ru.yandex.common.framework.corba.DefaultServRequest;
import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.pager.Pager;
import ru.yandex.common.framework.pager.PagerFactory;
import ru.yandex.common.framework.pager.SimplePagerFactory;
import ru.yandex.common.util.xml.XmlConvertable;
import ru.yandex.market.pers.grade.admin.MockedPersGradeAdminTest;

/**
 * @author Vladimir Gorovoy vgorovoy@yandex-team.ru
 */
public abstract class BaseGradeAdminDbTest extends MockedPersGradeAdminTest {

    protected ServRequest getServRequest() {
        return new DefaultServRequest("", 0l, "");
    }

    protected Pager getTestPager() {
        PagerFactory pagerFactory = new SimplePagerFactory();
        return pagerFactory.createPager(new DefaultServRequest("test", -1L, "test"));
    }

    protected void printXmlConvertableList(List<? extends XmlConvertable> list) {
        for (XmlConvertable model : list) {
            printXmlConvertable(model);
        }
    }

    public static void printXmlConvertable(XmlConvertable model) {
        StringBuilder buf = new StringBuilder();
        model.toXml(buf);
        System.out.println(buf.toString());
    }

}
