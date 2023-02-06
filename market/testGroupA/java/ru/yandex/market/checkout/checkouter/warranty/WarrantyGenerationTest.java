package ru.yandex.market.checkout.checkouter.warranty;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderProperty;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.archive.StorageOrderArchiveService;
import ru.yandex.market.checkout.checkouter.order.status.actions.GenerateWarrantyAction;
import ru.yandex.market.checkout.checkouter.request.BasicOrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.shopinfo.SupplierInfo;
import ru.yandex.market.shopinfo.SupplierInfoService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class WarrantyGenerationTest extends AbstractWebTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(WarrantyGenerationTest.class);

    @Mock
    private SupplierInfoService supplierInfoService;
    @Mock
    private PersonalDataService personalDataService;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CheckouterClient client;
    @Autowired
    private StorageOrderArchiveService orderArchiveService;
    @Autowired
    private GenerateWarrantyAction generateWarrantyAction;

    @BeforeEach
    public void setup() {
        initMocks(this);
        mockSupplierInfoService();
        generateWarrantyAction.setEnabled(true);
    }

    @AfterEach
    public void tearDown() {
        generateWarrantyAction.setEnabled(false);
    }

    @Test
    public void generateWarranty() {
        Parameters orderParameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(orderParameters);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertThat(order.getStatus(), equalTo(OrderStatus.DELIVERED));
        Optional<String> url = getPropertyValue(order.getId(), OrderPropertyType.WARRANTY_URL);
        LOG.info("Warranty URL: {}", url.get());
        assertThat(url.get(), notNullValue());
        assertThat(url.get(), containsString("warranty_"));

    }

    @Test
    public void notGenerateWarrantyForClickAndCollectOrders() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.clickAndCollectOrderParameters());
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertThat(order.getRgb(), equalTo(Color.BLUE));
        assertThat(order.getStatus(), equalTo(OrderStatus.DELIVERED));
        Optional<String> url = getPropertyValue(order.getId(), OrderPropertyType.WARRANTY_URL);
        assertFalse(url.isPresent());
    }

    @Test
    public void successfulDownloadWarranty() throws Exception {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertThat(order.getStatus(), equalTo(OrderStatus.DELIVERED));
        InputStream is = client.getWarrantyPdf(order.getId(), ClientRole.SYSTEM, 1L);
        assertThat(is, notNullValue());
    }

    @Test
    public void shouldFailDownloadWarrantyForArchivedOrder() throws Exception {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        orderUpdateService.addOrderProperty(
                new OrderProperty(order.getId(), OrderPropertyType.WARRANTY_URL.getName(), null)
        );
        orderArchiveService.archiveOrders(Set.of(order.getId()), false);
        assertThat(order.getStatus(), equalTo(OrderStatus.DELIVERED));
        checkouterProperties.setArchiveAPIEnabled(true);
        RequestClientInfo clientInfo = new RequestClientInfo(ClientRole.SYSTEM, 1L);
        long orderId = order.getId();
        ErrorCodeException exception = assertThrows(ErrorCodeException.class,
                () -> client.getWarrantyPdf(clientInfo, BasicOrderRequest.builder(orderId).withArchived(true).build()));
        assertEquals("Could not generate warranty PDF from archived order!", exception.getMessage());
    }

    private void mockSupplierInfoService() {
        SupplierInfo info = new SupplierInfo();
        info.setOgrn("1027700229193");
        info.setSupplierName("ООО \"ЯНДЕКС\"");
        info.setShopPhoneNumber("+7(812)123-13-13");
        when(supplierInfoService.getShopInfo(Mockito.anyLong())).thenReturn(Optional.of(info));
    }

    private Optional<String> getPropertyValue(long orderId, OrderPropertyType propertyType) {
        List<String> data = jdbcTemplate.query(
                "SELECT text_value FROM order_property WHERE order_id=? AND name=?",
                (rs, rowNum) -> rs.getString(1),
                orderId,
                propertyType.getName()
        );

        return data.stream().findAny();
    }
}
