package client;

import api.DsFfMockApi;
import dto.requests.mock.GetOrderInstancesData;
import dto.requests.mock.GetOrderInstancesData.GetOrderInstancesItem;
import io.qameta.allure.Step;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import toolkit.DateUtil;
import toolkit.FileUtil;
import toolkit.Retrofits;

@Slf4j
@Resource.Classpath("delivery/dsffmock.properties")
public class DsFfMockClient {

    private final DsFfMockApi dsFfMockApi;

    @Property("dsffmock.host")
    private String host;

    public DsFfMockClient() {
        PropertyLoader.newInstance().populate(this);
        dsFfMockApi = Retrofits.RETROFIT_XML.getRetrofit(host).create(DsFfMockApi.class);
    }

    @Step("Задаем данные для мока getOrder")
    public int mockGetOrder(long yandexId, String ffTrackCode, long firstKorobyte, long secondKorobyte) {
        log.debug("Mocking getOrder data for yandexId={}, fulfillmentId={}...", yandexId, ffTrackCode);

        String reqString = FileUtil.bodyStringFromFile(
            "delivery/mock/mockGetOrder.xml",
            yandexId,
            ffTrackCode,
            firstKorobyte,
            secondKorobyte
        );

        return mockRequest(reqString, "/getOrder/mock");
    }

    @Step("Задаем данные для мока getOrder (S7 с Go)")
    public int mockGetOrderS7(long yandexId, String ffTrackCode) {
        log.debug("Mocking getOrder data for yandexId={}, fulfillmentId={}...", yandexId, ffTrackCode);

        String reqString = FileUtil.bodyStringFromFile("delivery/mock/mockGetOrderS7.xml", yandexId, ffTrackCode);

        return mockRequest(reqString, "/getOrder/mock");
    }

    @Step("Задаем данные для мока getOrder для одной коробки и товара с КИЗом")
    public int mockGetOrderCis(GetOrderInstancesData data) {
        log.debug(
            "Mocking getOrder data for yandexId={}, fulfillmentId={}...",
            data.getYandexId(),
            data.getFfTrackCode()
        );

        GetOrderInstancesItem item = data.getItems().get(0);
        String reqString = FileUtil.bodyStringFromFile(
            "delivery/mock/mockGetOrderCis.xml",
            data.getYandexId(),
            data.getFfTrackCode(),
            data.getSupplierId(),
            item.getShopSku(),
            item.getPrice(),
            item.getCis()
        );

        return mockRequest(reqString, "/getOrder/mock");
    }

    @Step("Задаем данные для мока getOrder для двух товаров с УИТом")
    public int mockGetOrderUit(GetOrderInstancesData data) {
        log.debug(
            "Mocking getOrder data for yandexId={}, fulfillmentId={}...",
            data.getYandexId(),
            data.getFfTrackCode()
        );

        GetOrderInstancesItem item1 = data.getItems().get(0);
        GetOrderInstancesItem item2 = data.getItems().get(1);

        String reqString = FileUtil.bodyStringFromFile(
            "delivery/mock/mockGetOrderUit.xml",
            data.getYandexId(),
            data.getFfTrackCode(),
            data.getSupplierId(),
            item1.getShopSku(),
            item1.getPrice(),
            item1.getUit(),
            item2.getShopSku(),
            item2.getPrice(),
            item2.getUit()
        );

        return mockRequest(reqString, "/getOrder/mock");
    }

    @Step("Задаем данные для мока getOrder для удаления товара из заказа")
    public int mockGetOrderItemRemoval(
        String yandexId,
        String ffTrackCode,
        String shopSku1,
        long supplierId,
        float price1,
        String shopSku2,
        float price2
    ) {
        log.debug("Mocking getOrder data for yandexId={}, fulfillmentId={}...", yandexId, ffTrackCode);

        String reqString = FileUtil.bodyStringFromFile(
            "delivery/mock/mockGetOrderItemRemoval.xml",
            yandexId,
            ffTrackCode,
            shopSku1,
            supplierId,
            price1,
            shopSku2,
            price2
        );

        return mockRequest(reqString, "/getOrder/mock");
    }
    @Step("Задаем данные для мока getOrder для удаления товара из заказа")
    public int mockGetOrderItemRemoval(
        String yandexId,
        String ffTrackCode,
        String shopSku,
        long supplierId,
        float price
    ) {
        log.debug("Mocking getOrder data for yandexId={}, fulfillmentId={}...", yandexId, ffTrackCode);

        String reqString = FileUtil.bodyStringFromFile(
            "delivery/mock/mockGetOrderSingleItemRemoval.xml",
            yandexId,
            ffTrackCode,
            shopSku,
            supplierId,
            price
        );

        return mockRequest(reqString, "/getOrder/mock");
    }

    @Step("Задаем данные для мока getOutboundDetails")
    public int mockGetOutboundDetails(long yandexId, String fulfillmentId) {
        log.debug("Mocking getOutboundDetails data for yandexId={}, fulfillmentId={}...", yandexId, fulfillmentId);

        String reqString = FileUtil.bodyStringFromFile(
            "delivery/mock/mockGetOutboundDetails.xml",
            yandexId,
            fulfillmentId
        );

        return mockRequest(reqString, "/getOutboundDetails/mock");
    }

    @Step("Задаем данные для мока getOutbound")
    public int mockGetOutbound(long yandexId, String fulfillmentId) {
        log.debug("Mocking getOutbound data for yandexId={}, fulfillmentId={}...", yandexId, fulfillmentId);

        String reqString = FileUtil.bodyStringFromFile("delivery/mock/mockGetOutbound.xml", yandexId, fulfillmentId);

        return mockRequest(reqString, "/getOutbound/mock");
    }

    @Step("Задаем данные для мока getOutboundHistory")
    public int mockGetOutboundHistory(long yandexId, String fulfillmentId) {
        log.debug("Mocking getOutboundHistory data for yandexId={}, fulfillmentId={}...", yandexId, fulfillmentId);

        String reqString = FileUtil.bodyStringFromFile(
            "delivery/mock/mockGetOutboundHistory.xml",
            yandexId,
            fulfillmentId,
            DateUtil.currentDateTime(),
            DateUtil.currentDateTimePlus(1),
            DateUtil.currentDateTimePlus(2),
            DateUtil.currentDateTimePlus(3)
        );

        return mockRequest(reqString, "/getOutboundHistory/mock");
    }

    @Step("Задаем данные для мока getOutboundsStatus")
    public int mockGetOutboundsStatus(long yandexId, String fulfillmentId) {
        log.debug("Mocking getOutboundsStatus data for yandexId={}, fulfillmentId={}...", yandexId, fulfillmentId);

        String reqString = FileUtil.bodyStringFromFile(
            "delivery/mock/mockGetOutboundsStatus.xml",
            yandexId,
            fulfillmentId,
            DateUtil.currentDateTime()
        );

        return mockRequest(reqString, "/getOutboundsStatus/mock");
    }

    @Step("Задаем данные для мока getInboundDetails")
    public int mockGetInboundDetails(long yandexId, String fulfillmentId) {
        log.debug("Mocking getInboundDetails data for yandexId={}, fulfillmentId={}...", yandexId, fulfillmentId);

        String reqString = FileUtil.bodyStringFromFile(
            "delivery/mock/mockGetInboundDetails.xml",
            yandexId,
            fulfillmentId
        );

        return mockRequest(reqString, "/getInboundDetails/mock");
    }

    @Step("Задаем данные для мока getInbound")
    public int mockGetInbound(long yandexId, String fulfillmentId, String mockFilePath) {
        log.debug("Mocking getInbound data for yandexId={}, fulfillmentId={}...", yandexId, fulfillmentId);

        String reqString = FileUtil.bodyStringFromFile(
            mockFilePath, yandexId, fulfillmentId);

        return mockRequest(reqString, "/getInbound/mock");
    }

    @Step("Задаем данные для мока getInboundHistory")
    public int mockGetInboundHistory(long yandexId, String fulfillmentId) {
        log.debug("Mocking getInboundHistory data for yandexId={}, fulfillmentId={}...", yandexId, fulfillmentId);

        String reqString = FileUtil.bodyStringFromFile(
            "delivery/mock/mockGetInboundHistory.xml",
            yandexId,
            fulfillmentId,
            DateUtil.currentDateTime(),
            DateUtil.currentDateTimePlus(1),
            DateUtil.currentDateTimePlus(2),
            DateUtil.currentDateTimePlus(3)
        );

        return mockRequest(reqString, "/getInboundHistory/mock");
    }

    @Step("Задаем данные для мока getInboundsStatus")
    public int mockGetInboundsStatus(long yandexId, String fulfillmentId) {
        log.debug("Mocking getInboundsStatus data for yandexId={}, fulfillmentId={}...", yandexId, fulfillmentId);

        String reqString = FileUtil.bodyStringFromFile(
            "delivery/mock/mockGetInboundsStatus.xml",
            yandexId,
            fulfillmentId,
            DateUtil.currentDateTime()
        );

        return mockRequest(reqString, "/getInboundsStatus/mock");
    }

    @Step("Задаем данные для мока getOrdersDeliveryDate")
    public int mockGetOrdersDeliveryDate(long orderId, String date) {
        log.debug("Mocking getOrdersDeliveryDate data for orderId={}, date={}...", orderId, date);

        String reqString = FileUtil.bodyStringFromFile(
            "delivery/mock/mockGetOrdersDeliveryDate.xml",
            orderId,
            date
        );

        return mockRequest(reqString, "/getOrdersDeliveryDate/mock");
    }

    @Step("Задаем данные для мока xdocCreateInbound")
    public int mockXdocCreateInbound() {
        log.debug("Mocking xdocCreateInbound");

        String reqString = FileUtil.bodyStringFromFile(
            "delivery/mock/mockXdocCreateInbound.xml");

        return mockRequest(reqString, "/xdocCreateInbound/mock");
    }

    @Step("Задаем данные для мока xdocGetInboundsStatus")
    public int mockXdocGetInboundsStatus() {
        log.debug("Mocking xdocGetInboundsStatus");

        String reqString = FileUtil.bodyStringFromFile(
            "delivery/mock/mockxDocGetInboundsStatus.xml");

        return mockRequest(reqString, "/xdocGetInboundsStatus/mock");
    }

    @Step("Задаем данные для мока xdocGetInboundHistory")
    public int mockXdocGetInboundHistory() {
        log.debug("Mocking xdocGetInboundHistory");

        String reqString = FileUtil.bodyStringFromFile(
            "delivery/mock/mockXdocGetInboundHistory.xml");

        return mockRequest(reqString, "/xdocGetInboundHistory/mock");
    }

    @Step("Задаем данные для мока xdocGetInboundDetailsXDoc")
    public int mockXdocGetInboundDetailsXDoc() {
        log.debug("Mocking xdocGetInboundDetailsXDoc");

        String reqString = FileUtil.bodyStringFromFile(
            "delivery/mock/mockXdocGetInboundDetailsXDoc.xml");

        return mockRequest(reqString, "/xdocGetInboundDetailsXDoc/mock");
    }

    @Step("Задаём данные для мока zaborkaRegisterGetOutbound")
    public int mockZaborkaRegisterGetOutbound(long requestId, long randomRegisterId) {
        log.debug("Mocking zaborkaRegisterGetOutbound");

        String reqString = FileUtil.bodyStringFromFile(
            "delivery/mock/mockZaborkaRegisterGetOutbound.xml",
            requestId,
            randomRegisterId
        );

        return mockRequest(reqString, "/autotest48099/getOutbound/mock");
    }

    @Step("Удаление созданного мока")
    public void deleteMockById(int mockId) {
        log.debug("Удаляем созданный мок");
        deleteCreatedMock(mockId);
    }

    @SneakyThrows
    private int mockRequest(String request, String path) {
        Response<ResponseBody> execute = dsFfMockApi.mockRequest(
            path,
            RequestBody.create(MediaType.parse("text/plain"), request)
        ).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось сделать мок " + path);
        Assertions.assertNotNull(execute.body());
        String[] splittedResponse = execute.body().string().split("(?<=mockId: )");
        return Integer.parseInt(splittedResponse[1]);
    }

    @SneakyThrows
    private void deleteCreatedMock(int mockId) {
        Response<ResponseBody> execute = dsFfMockApi.deletedMock("/mock/" + mockId).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось удалить mock " + mockId);
    }
}
