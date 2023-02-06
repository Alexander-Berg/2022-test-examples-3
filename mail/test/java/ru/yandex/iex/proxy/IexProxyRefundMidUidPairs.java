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

public class IexProxyRefundMidUidPairs extends TestBase {
    private static final String HTTP_LOCALHOST = "http://localhost:";

    @Test
    public void testRefundMidUidPairs() throws Exception {
        try (IexProxyCluster cluster =
                new IexProxyCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.iexproxy().start();

            HttpGet getRequest = new HttpGet(
                HTTP_LOCALHOST + cluster.iexproxy().port()
                + "/get_refund_mid_uid_pairs?dt_from=555555&dt_to=777777");

            FileEntity cokeFraudMids = new FileEntity(
                new File(getClass().getResource("coke_fraud_mids.json").
                    toURI()), ContentType.APPLICATION_JSON);
            FileEntity cokeDisputeMids = new FileEntity(
                new File(getClass().getResource("coke_dispute_mids.json").
                    toURI()), ContentType.APPLICATION_JSON);
            FileEntity cokeCbkRegisterMids = new FileEntity(
                new File(getClass().getResource("coke_cbk_register_mids.json").
                    toURI()), ContentType.APPLICATION_JSON);

            cluster.gettext().add(
                "/sequential/search?service=iex&prefix=1120000000002254*",
                new StaticHttpItem(HttpStatus.SC_OK, cokeFraudMids));
            cluster.gettext().add(
                "/sequential/search?service=iex&prefix=1120000000321224*",
                new StaticHttpItem(HttpStatus.SC_OK, cokeDisputeMids));
            cluster.gettext().add(
                "/sequential/search?service=iex&prefix=1120000000397662*",
                new StaticHttpItem(HttpStatus.SC_OK, cokeCbkRegisterMids));

            cluster.gettext().start();

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, getRequest);
        }
    }
}
