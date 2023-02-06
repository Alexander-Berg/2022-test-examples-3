package ru.yandex.chemodan.balanceclient;

//import static org.junit.Assert.*;

import java.net.URI;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.balanceclient.model.request.BalanceClientRequstFactory;
import ru.yandex.chemodan.balanceclient.model.request.FindClientRequest;
import ru.yandex.devtools.test.annotations.YaExternal;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.inside.passport.tvm2.TvmClientCredentials;

public class BalanceClientTest {
    private BalanceClient balanceClient;
    private Tvm2 tvm2;

    @After
    public void destroy() {
        tvm2.stop();
    }

    @Before
    public void setUp() throws Exception {
        tvm2 = new Tvm2(new TvmClientCredentials(2017571, "tXyzKJcAUcdB3QAVRjHBfg")); // ps-billing-web.testing
        tvm2.addDstClientIds(Cf.list(2000601)); // balance.testing
        tvm2.refresh();
        BalanceXmlRpcClientConfig xmlRpcClientConfig = new BalanceXmlRpcClientConfig(
                null, URI.create("https://balance-xmlrpc-tvm-ts.paysys.yandex.net:8004/xmlrpctvm").toURL(),
                TimeZone.getTimeZone("Europe/Moscow"),
                Option.empty(), tvm2, s -> Option.of(2000601)
        );
        balanceClient = new BalanceClient(new BalanceXmlRpcClient(xmlRpcClientConfig),
                new BalanceClientRequstFactory());
    }

    @Test
    @YaExternal
    @Ignore
    public void testFindClient() {
        balanceClient.findClient(new FindClientRequest().withUid(1111L));
    }
}
