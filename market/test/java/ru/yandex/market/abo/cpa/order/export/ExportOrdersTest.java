package ru.yandex.market.abo.cpa.order.export;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.common.framework.user.UserInfo;
import ru.yandex.market.abo.core.user.BulkBlackBoxUserService;
import ru.yandex.market.abo.cpa.order.model.CpaOrderReport;
import ru.yandex.market.abo.cpa.order.service.CpaOrderReportRepo;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.cpa.order.export.ExportOrdersService.REPORT_ENCODING;

public class ExportOrdersTest extends EmptyTest {

    @Autowired
    private ExportOrdersService exportOrdersService;
    @Autowired
    private CpaOrderReportRepo cpaOrderReportRepo;

    @Test
    public void testExport() throws Exception {
        UserInfo ui = mock(UserInfo.class);
        when(ui.getLogin()).thenReturn("\"Ka; ;\n , ,te\"");
        BulkBlackBoxUserService service = mock(BulkBlackBoxUserService.class);
        when(service.getUserInfo(anyLong())).thenReturn(ui);
        exportOrdersService.setUserService(service);

        OrderSearchRequest request = new OrderSearchRequest();
        request.orderIds = null;
        request.userId = null;
        request.shopId = 774L;
        request.setStatuses(Arrays.asList(OrderStatus.CANCELLED, OrderStatus.DELIVERY, OrderStatus.PROCESSING).toArray(new OrderStatus[0]));
        request.fake = null;
        request.fromDate = new Date(0);
        request.toDate = new Date();

        StringWriter writer = new StringWriter();
        exportOrdersService.writeCsvReport(request, writer);
        String report = writer.toString();
        assertNotNull(report);

        Path filePath = createFile(report);
        Files.deleteIfExists(filePath);
    }

    @Test
    public void testLoad() throws Exception {
        String text = "TESTTESTTEST";
        byte[] content = text.getBytes(REPORT_ENCODING);
        cpaOrderReportRepo.save(new CpaOrderReport(1L, "1.csv", content, 100));
        OrdersReport report = exportOrdersService.getReport(1L);
        assertNotNull(report);
        assertEquals(text, new String(report.getContent(), REPORT_ENCODING));
    }

    @Nonnull
    public static Path createFile(String report) throws IOException {
        Charset charset = Charset.forName(REPORT_ENCODING);

        File file = new File("file.csv");
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            os.write(report.getBytes(charset));
            System.out.println(file.getAbsolutePath());
        }

        Path filePath = Paths.get("file.csv");
        assertTrue(Files.exists(filePath), "Export file wasn't created!");
        assertTrue(Files.size(filePath) > 0, "Export file is empty!");
        return filePath;
    }
}
