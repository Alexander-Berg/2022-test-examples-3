package ru.yandex.market.tpl.billing.service;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeListNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.tpl.billing.model.yt.YtExportTransactionDto;
import ru.yandex.market.tpl.billing.service.step.StepEventRestClient;
import ru.yandex.market.tpl.billing.service.yt.YtService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static ru.yandex.market.tpl.billing.service.yt.YtService.getSchema;

public class YtServiceTest {

    @Test
    public void export() {
        //todo сделать тест на тестовом ыте, https://st.yandex-team.ru/MARKETTPLBILL-25
    }

    @Test
    public void testGetSchema() {
        YTreeListNode schema = getSchema(YtExportTransactionDto.class);
        YTreeNode expected = getExpectedSchema();
        assertEquals("schema", expected, schema);
    }

    private YTreeListNode getExpectedSchema() {
        return YTree.listBuilder()
                .beginMap().key("name").value("billingTransactionId").key("type").value("int64").endMap()
                .beginMap().key("name").value("serviceTransactionId").key("type").value("string").endMap()
                .beginMap().key("name").value("serviceEventTime").key("type").value("string").endMap()
                .beginMap().key("name").value("billingEventTime").key("type").value("string").endMap()
                .beginMap().key("name").value("productName").key("type").value("string").endMap()
                .beginMap().key("name").value("clientId").key("type").value("int64").endMap()
                .beginMap().key("name").value("serviceId").key("type").value("int32").endMap()
                .beginMap().key("name").value("amount").key("type").value("string").endMap()
                .beginMap().key("name").value("paymentSum").key("type").value("string").endMap()
                .beginMap().key("name").value("paymentMethod").key("type").value("string").endMap()
                .beginMap().key("name").value("withVAT").key("type").value("int32").endMap()
                .beginMap().key("name").value("isCorrection").key("type").value("int32").endMap()
                .beginMap().key("name").value("payload").key("type").value("string").endMap()
                .beginMap().key("name").value("transactionScope").key("type").value("string").endMap()
                .buildList();
    }

    @Test
    @DisplayName("Тест на продолжение всей выгрузки если упала только одна выгрузка в кластер")
    public void testFailExportOnce() {

        YtService ytService = setupYtService(true, false);
        ytService.export(List.of(), Object.class, "", "", "", true);

        ytService = setupYtService(false, true);
        ytService.export(List.of(), Object.class, "", "", "", true);
    }

    @Test
    @DisplayName("Тест на падение всей выгрузки если упали все выгрузки (во все кластеры)")
    public void testFailExport() {

        YtService ytService = setupYtService(false, false);
        var exception = assertThrows(RuntimeException.class, () -> {
            ytService.export(List.of(), Object.class, "", "", "", true);
        });
        var expected = "Arnold export to yt exception";

        Assertions.assertEquals(expected, exception.getMessage());

    }

    private YtService setupYtService(boolean hahnSuccessfullyExport, boolean arnoldSuccessfullyExport) {
        Yt hahn = mock(Yt.class);
        Yt arnold = mock(Yt.class);
        StepEventRestClient stepEventRestClient = mock(StepEventRestClient.class);

        YtService yt = spy(new YtService(hahn, arnold, stepEventRestClient));
        setupYtExport("Hahn", hahn, yt, hahnSuccessfullyExport);
        setupYtExport("Arnold", arnold, yt, arnoldSuccessfullyExport);

        return yt;
    }

    private void setupYtExport(String ytClusterName, Yt ytCluster, YtService ytService, boolean isSuccessfulExport) {
        doAnswer(inv -> {
            if (!isSuccessfulExport) {
                throw new RuntimeException(ytClusterName + " export to yt exception");
            }
            return null;
        }).when(ytService).exportToCluster(eq(ytCluster), anyList(), any(Class.class), anyString(), anyString(),
                anyString(),
                anyBoolean());

        doReturn(false).when(ytService).isTableExists(anyString(), anyString(), eq(ytCluster));
    }
}
