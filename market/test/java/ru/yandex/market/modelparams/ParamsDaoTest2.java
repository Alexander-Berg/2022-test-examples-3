package ru.yandex.market.modelparams;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import ru.yandex.common.framework.core.ErrorInfo;
import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.core.SimpleErrorInfo;
import ru.yandex.common.framework.http.HttpServRequest;
import ru.yandex.common.framework.xml.NamedCollection;
import ru.yandex.market.modelparams.model.ModelDocuments;
import ru.yandex.market.modelparams.model.ModelPage;
import ru.yandex.market.modelparams.model.PostCount;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by IntelliJ IDEA.
 * User: kirill
 * Date: Oct 1, 2008
 * Time: 7:12:45 PM
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:model-params-test-bean.xml")
@WebAppConfiguration
public class ParamsDaoTest2  {
    @Autowired
    private ParamsServantlet ps;

    public void setPs(final ParamsServantlet ps) {
        this.ps = ps;
    }

    @Test
    public void test001() throws Exception {
        final ServRequest req = new HttpServRequest(0L,"",  "");
        final MockServResponse res = new MockServResponse();
        ps.process(req, res);
        final List<ErrorInfo> list = res.getErrors();
        assertEquals(list.size(), 1);
        assertEquals(((SimpleErrorInfo) list.get(0)).getMessageCode(), "Missing model id in request");
    }

    @Test
    public void test002() throws Exception {
        final ServRequest req = new HttpServRequest(0L,"",  "");
        req.setParam("modelid", "448997");
        final MockServResponse res = new MockServResponse();
        ps.process(req, res);
        final List<ErrorInfo> list = res.getErrors();
        assertEquals(list.size(), 0);
        final List<Object> data = res.getData();
        for (final Object o : data) {
            final StringBuilder sb = new StringBuilder();
            if (o instanceof PostCount) {
                ((PostCount) o).toXml(sb);
                assertEquals(sb.toString(), "<post-count id=\"448997\" value=\"1\"/>");
            } else if (o instanceof ModelDocuments) {
                ((ModelDocuments) o).toXml(sb);
                assertEquals(sb.toString(), "<model-docs id=\"448997\"></model-docs>");
            } else if (o instanceof ModelPage) {
                fail();
            } else if (o instanceof NamedCollection) {
                ((NamedCollection) o).toXml(sb);
                assertEquals(sb.toString(), "<reviews found=\"0\"  from=\"1\"  to=\"0\" ></reviews>");
            }
        }
    }

    @Test
    public void test003() throws Exception {
        final ServRequest req = new HttpServRequest(0L,"",  "");
        req.setParam("modelid", "160297");
        final MockServResponse res = new MockServResponse();
        ps.process(req, res);
        final List<ErrorInfo> list = res.getErrors();
        assertEquals(list.size(), 0);
        final List<Object> data = res.getData();
        for (final Object o : data) {
            final StringBuilder sb = new StringBuilder();
            if (o instanceof PostCount) {
                ((PostCount) o).toXml(sb);
                assertEquals("<post-count id=\"160297\" value=\"93\"/>", sb.toString());
            } else if (o instanceof ModelDocuments) {
                ((ModelDocuments) o).toXml(sb);
                assertEquals("<model-docs id=\"160297\"></model-docs>", sb.toString());
            } else if (o instanceof ModelPage) {
                ((ModelPage) o).toXml(sb);
                assertEquals("<model-vendor-page model-id=\"160297\" ><name>Nokia 8210</name>\n" +
                        "<url>http://europe.nokia.com/A4143184</url>\n" +
                        "<host>europe.nokia.com</host>\n" +
                        "</model-vendor-page>", sb.toString());
            } else if (o instanceof NamedCollection) {
                ((NamedCollection) o).toXml(sb);
                assertEquals("<reviews found=\"4\"  from=\"1\"  to=\"4\" ><reviews-group>\n" +
                        "<resource>ixbt.com</resource>\n" +
                        "<model-review model-id=\"160297\" ><name>Nokia 8210</name>\n" +
                        "<url>http://www.ixbt.com/mobile/nokia-8210.html</url>\n" +
                        "</model-review><model-review model-id=\"160297\" ><name>&#1042;&#1087;&#1077;&#1095;&#1072;&#1090;&#1083;&#1077;&#1085;&#1080;&#1077; &#1087;&#1086;&#1083;&#1100;&#1079;&#1086;&#1074;&#1072;&#1090;&#1077;&#1083;&#1077;&#1081; &#1086;&#1090; Nokia 8210</name>\n" +
                        "<url>http://www.ixbt.com/mobile/review/nokia8210-users.html</url>\n" +
                        "</model-review></reviews-group>\n" +
                        "<reviews-group>\n" +
                        "<resource>mobile-review.com</resource>\n" +
                        "<model-review model-id=\"160297\" ><name>&#1054;&#1073;&#1079;&#1086;&#1088; GSM-&#1090;&#1077;&#1083;&#1077;&#1092;&#1086;&#1085;&#1072; Nokia 8210</name>\n" +
                        "<url>http://www.mobile-review.com/review/nokia-8210.shtml</url>\n" +
                        "</model-review></reviews-group>\n" +
                        "<reviews-group>\n" +
                        "<resource>3dnews.ru</resource>\n" +
                        "<model-review model-id=\"160297\" ><name>Ericsson T28s, Nokia 8210, Motorola v3688</name>\n" +
                        "<url>http://www.3dnews.ru/100040</url>\n" +
                        "</model-review></reviews-group>\n" +
                        "</reviews>", sb.toString());
            }
        }
    }
}
