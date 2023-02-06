package ru.yandex.market.ocrm.module.order;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.crm.util.Result;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.comment.test.impl.CommentTestUtils;
import ru.yandex.market.jmf.module.geo.impl.suggest.GeoSuggestCity;
import ru.yandex.market.jmf.module.geo.impl.suggest.GeoSuggestClient;
import ru.yandex.market.jmf.module.ou.Employee;
import ru.yandex.market.jmf.module.ou.impl.EmployeeTestUtils;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.ocrm.module.order.domain.CourierOrderDeliveryAddress;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;
import ru.yandex.market.ocrm.module.tpl.MarketTplClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ModuleOrderTestConfiguration.class)
public class UpdateCourierOrderDeliveryAddressTest {

    private static final int REGION_ID = 54;
    private static final String OPERATOR_NAME = "Пупкин Максим";

    @Inject
    BcpService bcpService;

    @Inject
    OrderTestUtils orderTestUtils;

    @Inject
    EntityStorageService entityStorageService;

    @Inject
    MarketTplClient marketTplClient;

    @Inject
    MockSecurityDataService securityDataService;

    @Inject
    EmployeeTestUtils employeeTestUtils;

    @Inject
    OuTestUtils ouTestUtils;

    @Inject
    CommentTestUtils commentTestUtils;

    @Inject
    GeoSuggestClient geoSuggestClient;

    private Employee employee;

    @BeforeEach
    public void setUp() {
        when(marketTplClient.updateOrder(any(), any())).thenReturn(Result.newResult(null));
        employee = employeeTestUtils.createEmployee(ouTestUtils.createOu(), Map.of(Employee.TITLE, OPERATOR_NAME));
        securityDataService.setInitialEmployee(employee);

        GeoSuggestCity city = Mockito.mock(GeoSuggestCity.class);
        when(city.getRegionId()).thenReturn(REGION_ID);
        when(geoSuggestClient.getCities(any(), any())).thenReturn(List.of(city));
    }

    /**
     * Добавление служебного комментария при изменении адреса доставки
     * https://testpalm2.yandex-team.ru/testcase/ocrm-1391
     * <p>
     * {@link ru.yandex.market.ocrm.module.order.operations.UpdateCourierOrderDeliveryAddressOperationHandler}
     */
    @Test
    public void addCommentWhenCourierOrderDeliveryAddressChanged() {
        Order order = orderTestUtils.createOrder(Maps.of(Order.DELIVERY_REGION_ID, REGION_ID));
        String gid = CourierOrderDeliveryAddress.FQN.gidOf(order.getOrderId());
        CourierOrderDeliveryAddress deliveryAddress = entityStorageService.get(gid);
        bcpService.edit(deliveryAddress, Map.of(
                CourierOrderDeliveryAddress.CITY, "Екатеринбург",
                CourierOrderDeliveryAddress.STREET, "Хохрякова",
                CourierOrderDeliveryAddress.HOUSE, "10",
                CourierOrderDeliveryAddress.ENTRANCE, "1",
                CourierOrderDeliveryAddress.ENTRY_PHONE, "666",
                CourierOrderDeliveryAddress.FLOOR, "14",
                CourierOrderDeliveryAddress.APARTMENT, "777",
                CourierOrderDeliveryAddress.RECIPIENT_NAME, "Пупкин Дима",
                CourierOrderDeliveryAddress.RECIPIENT_PHONE, "+79870010203"
        ));

        List<Comment> comments = commentTestUtils.getComments(order);
        assertEquals(1, comments.size());

        String expectedComment = "Адрес доставки изменён на г. Екатеринбург, ул. Хохрякова, д. 10, "
                + "подъезд 1, домофон 666, этаж 14, кв. 777\n"
                + "Получатель: Пупкин Дима, тел. &#43;79870010203\n"
                + "Автор изменений: " + OPERATOR_NAME;
        assertEquals(expectedComment, comments.get(0).getBody());
    }
}
