package ru.yandex.market.rg.outlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.h2.util.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.outlet.ShopOutletOuterClass;
import ru.yandex.market.protobuf.readers.LEDelimerMessageReader;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class ExportOutletIdsExecutorTest extends FunctionalTest {

    @Autowired
    ExportOutletIdsExecutor executor;
    @Autowired
    NamedHistoryMdsS3Client historyMdsS3Client;

    @DbUnitDataSet(before = "ExportOutletIdsExecutorTest.csv")
    @Test
    void test() throws IOException {
        var baos = new ByteArrayOutputStream();
        when(historyMdsS3Client.upload(anyString(), any()))
                .thenAnswer(invocation -> {
                    ContentProvider cp = invocation.getArgument(1);
                    IOUtils.copy(cp.getInputStream(), baos);
                    return ResourceLocation.create("bucket", "file");
                });
        executor.doJob(null);
        assertTrue(baos.size() > 0);
        var result = readOutlets(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals(1, result.size());
        assertEquals(ShopOutletOuterClass.ShopOutlet.newBuilder()
                .setOutletType(ShopOutletOuterClass.OutletType.MIXED)
                .setIds(ShopOutletOuterClass.OutletIds.newBuilder()
                        .setShopId(10)
                        .setMarketOutletId(1)
                        .setShopOutletId("100")
                ).build(),
                result.get(0));
    }

    private List<ShopOutletOuterClass.ShopOutlet> readOutlets(InputStream in) throws IOException {
        try (in) {
            var result = new ArrayList<ShopOutletOuterClass.ShopOutlet>();
            var reader = new LEDelimerMessageReader<>(ShopOutletOuterClass.ShopOutlet.parser(), in);

            var outlet = reader.read();
            while (outlet != null) {
                result.add(outlet);
                outlet = reader.read();
            }
            return result;
        }
    }

}
