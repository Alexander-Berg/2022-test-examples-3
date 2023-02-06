package ru.yandex.iex.proxy;

import java.io.File;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.test.util.TestBase;

public class IexProxyGetRefundMailInfoTest extends TestBase {
    private static final String HTTP_LOCALHOST = "http://localhost:";

    @Test
    public void testGetMailInfo() throws Exception {
        try (IexProxyCluster cluster =
                new IexProxyCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();

            HttpGet getRequest = new HttpGet(
                HTTP_LOCALHOST + cluster.iexproxy().port()
                + "/get_refund_mail_info?dt_from=555555&dt_to=777777&mail_from=alfa@alfabank.ru");

            FileEntity cokeFraudMails = new FileEntity(
                new File(getClass().getResource("coke_fraud_mails_info.json").
                    toURI()), ContentType.APPLICATION_JSON);
            FileEntity cokeDisputeMails = new FileEntity(
                new File(getClass().getResource("coke_dispute_mails_info.json").
                    toURI()), ContentType.APPLICATION_JSON);
            FileEntity cokeCbkRegisterMails = new FileEntity(
                new File(getClass().getResource("coke_cbk_register_mails_info.json").
                    toURI()), ContentType.APPLICATION_JSON);

            cluster.gettext().add(
                "/sequential/search?service=iex&prefix=1120000000002254*",
                new StaticHttpItem(HttpStatus.SC_OK, cokeFraudMails));
            cluster.gettext().add(
                "/sequential/search?service=iex&prefix=1120000000321224*",
                new StaticHttpItem(HttpStatus.SC_OK, cokeDisputeMails));
            cluster.gettext().add(
                "/sequential/search?service=iex&prefix=1120000000397662*",
                new StaticHttpItem(HttpStatus.SC_OK, cokeCbkRegisterMails));

            cluster.gettext().start();

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, getRequest);
        }
    }
}
