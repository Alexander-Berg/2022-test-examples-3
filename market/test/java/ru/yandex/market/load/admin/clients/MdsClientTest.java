package ru.yandex.market.load.admin.clients;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.load.admin.AbstractFunctionalTest;
import ru.yandex.mj.generated.client.mds.api.MdsApiClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by aproskriakov on 6/30/22
 */
@Disabled
public class MdsClientTest extends AbstractFunctionalTest {

    @Autowired
    MdsApiClient mdsClient;

    @Test
    void testGet() throws ExecutionException, InterruptedException {
        String resp = mdsClient.initGet("http://s3.mds.yandex" +
                ".net/vendors/dumps/vendors_info/vendors_info_current.tsv", false).scheduleResponse().get().body();
        assertEquals(1734014, resp.length());
    }
}
