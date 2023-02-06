package ru.yandex.calendar.util.data;

import java.util.Map;

import Yandex.RequestPackage.RequestData;
import ru.yandex.misc.test.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.function.Function;
import ru.yandex.calendar.util.idlent.RequestDataFactory;
import ru.yandex.calendar.util.idlent.RequestWrapper;

/**
 * @author ssytnik
 */

@RunWith(Parameterized.class)
public class DataProviderTest {
    private static DataProvider getXmlDp() {
        String xml =
            "<root>" +
                "<a>a-value</a>" +
                "<b-coll count=\"2\">" +
                    "<b><b1>b1-value</b1></b>" +
                    "<b><b2>b2-value</b2></b>" +
                "</b-coll>" +
                "<c>c1</c>" +
                "<c />" +
                "<c>c2</c>" +
                "<d><e><f>g</f></e></d>" +
                "<text-empty/>" +
                "<text-0>0</text-0>" +
                "<!-- xml: no dp-empty -->" +
                "<!-- xml: no dp-0 -->" +
                "<texts>" +
                    "<text/>" +
                    "<text>0</text>" +
                    "<text>text</text>" +
                "</texts>" +
                "<dps>" +
                    "<!-- xml: no dp (empty) -->" +
                    "<!-- xml: no dp (0) -->" +
                    "<dp/>" +
                "</dps>" +
            "</root>";
        return XmlDataProvider.createFromString(xml);
    }

    private static DataProvider getRequestDp() {
        RequestData rd = RequestDataFactory.create(
            "a=a-value&b_coll=1&" +
            "b_coll_b[0]=1&b_coll_b[0]_b1=b1-value&" +
            "b_coll_b[1]=1&b_coll_b[1]_b2=b2-value&" +
            "c[0]=c1&c[1]=&c[2]=c2&" +
            "d=1&d_e=1&d_e_f=g&" +
            "text_empty=&text_0=0&dp_empty=&dp_0=0&" +
            "texts=1&texts_text[0]=&texts_text[1]=0&texts_text[2]=text&" +
            "dps=1&dps_dp[0]=&dps_dp[1]=0&dps_dp[2]=1"
        );
        RequestWrapper rw = new RequestWrapper(rd);
        return new RequestDataProvider(rw);
    }

    private static DataProvider getMapDp() {
        Map<String, String> m = Cf.hashMap();
        m.put("a", "a-value");
        m.put("b_coll", "1");
        m.put("b_coll_b[0]", "1");
        m.put("b_coll_b[0]_b1", "b1-value");
        m.put("b_coll_b[1]", "1");
        m.put("b_coll_b[1]_b2", "b2-value");
        m.put("c[0]", "c1");
        m.put("c[1]", "");
        m.put("c[2]", "c2");
        m.put("d", "1");
        m.put("d_e", "1");
        m.put("d_e_f", "g");
        m.put("text_empty", "");
        m.put("text_0", "0");
        m.put("dp_empty", "");
        m.put("dp_0", "0");
        m.put("texts", "1");
        m.put("texts_text[0]", "");
        m.put("texts_text[1]", "0");
        m.put("texts_text[2]", "text");
        m.put("dps", "1");
        m.put("dps_dp[0]", "");
        m.put("dps_dp[1]", "0");
        m.put("dps_dp[2]", "1");
        return new MapDataProvider(m);
    }

    // TODO getAliasedRequestDp();

    // TODO getAliasedMapDp();

    @Parameters
    public static ListF<DataProvider[]> data() {
        return Cf.list(
            getXmlDp(),
            getRequestDp(),
            getMapDp()
        ).map(new Function<DataProvider, DataProvider[]>() {
            public DataProvider[] apply(DataProvider a) {
                return new DataProvider[] { a };
            }
        });
    }

    private DataProvider dp;

    public DataProviderTest(DataProvider dp) {
        this.dp = dp;
    }

    @Test
    public void getText() {
        Assert.A.equals("a-value", dp.getText("a", true));
        Assert.A.equals("g", dp.getDataProvider("d", true).getDataProvider("e", true).getText("f", true));
        Assert.A.equals(null, dp.getDataProvider("d", true).getDataProvider("non-existing", false));
        Assert.A.equals(null, dp.getDataProvider("d", true).getDataProvider("e", true).getText("no-text", false));
    }

    @Test
    public void textCollection() {
        Assert.A.equals(Cf.arrayList("c1", null, "c2"), Cf.toArrayList(dp.getTexts("c")));
    }

    @Test
    public void dpCollection() {
        DataProvider bColl = dp.getDataProvider("b-coll", true);
        ListF<DataProvider> bs = bColl.getDataProviders("b");
        Assert.A.equals("b1-value", bs.get(0).getText("b1", true));
        Assert.A.equals("b2-value", bs.get(1).getText("b2", true));
    }

    @Test
    public void optional() {
        Assert.A.equals(null, dp.getText("optional", false));
    }

    @Test
    public void missingText() {
        String missingText = dp.getText("missing", false);
        Assert.assertNull(missingText);
    }

    @Test
    public void missingDp() {
        DataProvider missingDp = dp.getDataProvider("missing", false);
        Assert.assertNull(missingDp);
    }

    @Test
    public void unsetTexts() {
        // Single
        String textEmpty = dp.getText("text-empty", false);
        Assert.assertNull(textEmpty);
        String text0 = dp.getText("text-0", true);
        Assert.A.equals("0", text0);
        // Multiple
        DataProvider textsDp = dp.getDataProvider("texts", true);
        Assert.A.equals(Cf.arrayList(null, "0", "text"), Cf.toArrayList(textsDp.getTexts("text")));
    }

    @Test
    public void unsetDps() {
        // Single
        DataProvider dpEmpty = dp.getDataProvider("dp-empty", false);
        Assert.assertNull(dpEmpty);
        DataProvider dp0 = dp.getDataProvider("dp-0", false);
        Assert.assertNull(dp0);
        // Multiple
        DataProvider dpsDp = dp.getDataProvider("dps", true);
        // NOTE: with collection of data providers, there is
        // a difference between prefixed and xml data providers
        int expectedSize = dpsDp instanceof PrefixedDataProvider ? 3 : 1;
        Assert.A.equals(expectedSize, dpsDp.getDataProviders("dp").size());
    }
}
