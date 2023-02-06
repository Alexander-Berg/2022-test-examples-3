package ru.yandex.market.crm.operatorwindow.services.customer;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.crm.operatorwindow.services.customer.vip.VipBuyerInfo;
import ru.yandex.market.crm.operatorwindow.services.customer.vip.VipCustomerMarkerProvider;
import ru.yandex.market.crm.operatorwindow.services.customer.vip.VipCustomersService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.GidService;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.ocrm.module.common.Customer;
import ru.yandex.market.ocrm.module.loyalty.ModuleLoyaltyTestConfiguration;
import ru.yandex.market.ocrm.module.order.domain.Order;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ModuleLoyaltyTestConfiguration.class)
public class VipCustomerMarkerProviderTest {
    @Inject
    private GidService gidService;
    private DbService dbService;
    private VipCustomersService vipCustomersService;

    private VipCustomerMarkerProvider provider;

    private static Order getOrder(Long uid) {
        var order = mock(Order.class);
        when(order.getBuyerUid()).thenReturn(uid);
        when(order.getBuyerPhone()).thenReturn(Phone.fromRaw("+79122222222"));
        when(order.getBuyerEmail()).thenReturn("some@mail.ru");
        when(order.getBuyerFirstName()).thenReturn("Василий");
        when(order.getBuyerMiddleName()).thenReturn("Васильевич");
        when(order.getBuyerLastName()).thenReturn("Васильев");
        return order;
    }

    private static Customer getCustomer(Long uid) {
        var customer = mock(Customer.class);
        when(customer.getUid()).thenReturn(uid);
        when(customer.getPhone()).thenReturn(Phone.fromRaw("+79122222222"));
        when(customer.getEmail()).thenReturn("some@mail.ru");
        return customer;
    }

    @BeforeEach
    public void setUp() {
        dbService = mock(DbService.class);
        vipCustomersService = mock(VipCustomersService.class);
        provider = new VipCustomerMarkerProvider(vipCustomersService, dbService, gidService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"order@123", "customer@123"})
    public void getMarkers_forNotExistingEntity_shouldReturnEmpty(String gid) {
        HasGid entity = getEntity(gid, 1L);
        when(dbService.get(entity.getGid())).thenReturn(null);

        List<String> markers = provider.getMarkers(entity, 1L);
        assertThat(markers, hasSize(0));
    }

    /**
     * https://testpalm2.yandex-team.ru/ocrm/testsuite/5f61a1ba8807112718f2756d?testcase=1364 ставит маркер, если vip
     * и entity класса order/customer
     */
    @ParameterizedTest
    @CsvSource({
            "order@123, true, true",
            "order@123, false, false",
            "customer@123, true, true",
            "customer@123, false, false",
            "ticket@123, true, false",
            "ticket@123, false, false"
    })
    public void getMarkers_shouldReturnVipIfExpected(String gid, boolean isVip, boolean markerExpected) {
        HasGid entity = getEntity(gid, 1L);
        when(dbService.get(entity.getGid())).thenReturn(entity);
        when(vipCustomersService.isVip(argThat((VipBuyerInfo x) -> matches(x, entity)))).thenReturn(isVip);

        List<String> markers = provider.getMarkers(entity, 1L);

        if (markerExpected) {
            assertThat(markers, hasSize(1));
            assertThat(markers.get(0), equalTo("VIP"));
        } else {
            assertThat(markers, hasSize(0));
        }
    }

    private boolean matches(VipBuyerInfo info, HasGid entity) {
        String fqn = gidService.parse(entity.getGid()).getMetaclass().getFqn().toString();
        switch (fqn) {
            case "customer":
                var customer = (Customer) entity;
                return info.getEmail().equals(customer.getEmail())
                        && info.getPhone().equals(customer.getPhone().getNormalized())
                        && info.getUid().equals(customer.getUid())
                        && info.getUserName().isEmpty();
            case "order":
                var order = (Order) entity;
                return info.getEmail().equals(order.getBuyerEmail())
                        && info.getPhone().equals(order.getBuyerPhone().getNormalized())
                        && info.getUid().equals(order.getBuyerUid())
                        && info.getUserName().getFirstName().equals(order.getBuyerFirstName())
                        && info.getUserName().getMiddleName().equals(order.getBuyerMiddleName())
                        && info.getUserName().getLastName().equals(order.getBuyerLastName());
            default:
                return false;
        }
    }

    private Entity getEntity(String gid, Long uid) {
        String fqn = gidService.parse(gid).getMetaclass().getFqn().toString();
        var entity = fqn.equals("customer")
                ? getCustomer(uid)
                : fqn.equals("order") ? getOrder(uid) : mock(Entity.class);
        when(entity.getGid()).thenReturn(gid);
        return entity;
    }
}
