package ru.yandex.market.wms.receiving;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.TimeZone;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.wms.common.model.enums.ReceiptStatus;
import ru.yandex.market.wms.common.spring.dao.entity.ExpectedAndReceivedSku;
import ru.yandex.market.wms.common.spring.dao.entity.Receipt;
import ru.yandex.market.wms.common.spring.dao.entity.ReceiptDetail;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailDao;
import ru.yandex.market.wms.common.spring.dao.implementation.SkuDaoImpl;
import ru.yandex.market.wms.common.spring.dao.implementation.TrailerDao;
import ru.yandex.market.wms.common.spring.pojo.ReceiptDetailKey;
import ru.yandex.market.wms.common.spring.service.ReceiptService;
import ru.yandex.market.wms.receiving.dao.AnomalyLotLocTypeDao;
import ru.yandex.market.wms.receiving.dao.ReceiptAnomalyDao;
import ru.yandex.market.wms.receiving.dao.TaskDetailCheckerDao;
import ru.yandex.market.wms.receiving.dao.entity.ReceiptAnomaly;
import ru.yandex.market.wms.receiving.properties.AppProperties;
import ru.yandex.market.wms.receiving.properties.StarTrekTaskProperties;
import ru.yandex.market.wms.receiving.service.startrek.ReceiptPdfReportService;
import ru.yandex.market.wms.receiving.service.startrek.impl.AnomalyNotificationServiceImpl;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;
import ru.yandex.market.wms.shared.libs.configproperties.dao.NSqlConfigDao;
import ru.yandex.market.wms.transportation.client.TransportationClient;

import static ru.yandex.market.wms.common.model.enums.ReceiptStatus.VERIFIED_CLOSED;
import static ru.yandex.market.wms.common.model.enums.ReceiptType.DEFAULT;
import static ru.yandex.market.wms.common.spring.enums.ReceivingItemType.DAMAGED;

public abstract class BaseReceivingTest {

    private static final double EPS = 1E-9;

    protected SoftAssertions assertions;

    @Mock
    protected ReceiptService receiptService;
    @Mock
    protected JmsTemplate jmsTemplate;
    @Mock
    protected ReceiptDao receiptDao;
    @Mock
    protected ReceiptDetailDao receiptDetailDao;
    @Mock
    protected ReceiptAnomalyDao anomalyDao;
    @Mock
    protected StarTrekTaskProperties starTrekTaskProperties;
    @Mock
    protected NSqlConfigDao nSqlConfigDao;
    @Mock
    protected SkuDaoImpl skuDao;
    @Mock
    protected ReceiptPdfReportService receiptPdfReportService;
    @Mock
    protected AnomalyNotificationServiceImpl anomalyNotificationService;
    @Mock
    protected SecurityDataProvider securityDataProvider;
    @Mock
    protected TaskDetailCheckerDao taskDetailDao;
    @Mock
    protected AnomalyLotLocTypeDao anomalyLotLocTypeDao;
    @Mock
    protected TransportationClient transportationClient;
    @Mock
    protected TrailerDao trailerDao;
    @Mock
    protected AppProperties appProperties;

    @BeforeEach
    public void setup() {
        assertions = new SoftAssertions();
    }

    @AfterEach
    public void triggerAssertions() {
        assertions.assertAll();
    }

    @BeforeAll
    public static void setTestTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
    }

    protected void assertDoubleEquals(double actual, double expected) {
        assertions.assertThat(actual).isEqualTo(expected, Offset.offset(EPS));
    }

    protected Receipt receipt(String receiptKey) {
        return Receipt.builder()
                .receiptKey(receiptKey)
                .externReceiptKey("ext" + receiptKey)
                .trailerKey("trailer")
                .receiptDate(Instant.now())
                .trailerNumber("trailerNumber")
                .storer("storer")
                .status(VERIFIED_CLOSED)
                .type(DEFAULT)
                .supplier("supplier")
                .build();
    }

    protected ReceiptDetail receiptDetail(String receiptKey, int i, ReceiptStatus status) {
        return ReceiptDetail.builder()
                .receiptDetailKey(new ReceiptDetailKey(receiptKey, String.format("%05d", i)))
                .skuId(new SkuId("storer", "sku" + i))
                .status(status)
                .build();
    }

    protected ReceiptDetail receiptDetail(String receiptKey, int i, ReceiptStatus status, float quantity) {
        return ReceiptDetail.builder()
                .receiptDetailKey(new ReceiptDetailKey(receiptKey, String.format("%05d", i)))
                .skuId(new SkuId("storer", "sku" + i))
                .status(status)
                .quantityReceived(new BigDecimal(quantity))
                .build();
    }

    protected ReceiptAnomaly receiptAnomaly(Receipt receipt, int i) {
        return ReceiptAnomaly.builder()
                .receiptKey(receipt.getReceiptKey())
                .transportUnitId("trUnitId" + i)
                .supplierName("anomaly supplier name")
                .supplier("anomaly supplier")
                .type(DAMAGED)
                .anomalyType("Брак")
                .amount(BigDecimal.ONE)
                .unitPrice(new BigDecimal("123.45"))
                .anomalyDescription("anomaly desc")
                .receiptType(receipt.getType().getCode())
                .sku(receipt.getReceiptKey() + "_sku_" + i)
                .altSku("altSku" + receipt.getReceiptKey())
                .build();
    }

    protected ExpectedAndReceivedSku sku(Receipt receipt, int i) {
        return ExpectedAndReceivedSku.builder()
                .key(ExpectedAndReceivedSku.Key.of(
                        SkuId.of("storer", receipt.getReceiptKey() + "_sku_" + i)))
                .quantityExpected(new BigDecimal(2))
                .quantityReceived(BigDecimal.ONE)
                .build();
    }
}
