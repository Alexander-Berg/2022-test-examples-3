package ru.yandex.market.crm.operatorwindow.domain;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.operatorwindow.domain.customer.VipCustomersData;
import ru.yandex.market.ocrm.module.checkouter.UserName;

public class VipCustomersDataTest {

    @Test
    public void vipByEmail() {
        VipCustomersData vipData = new VipCustomersData(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("test@example.com"),
                Collections.emptyList());

        Assertions.assertTrue(vipData.hasEmail("test@example.com"));
        Assertions.assertTrue(vipData.hasEmail("Test@Example.com"));
        Assertions.assertTrue(vipData.hasEmail(" Test@example.com "));
        Assertions.assertFalse(vipData.hasEmail("another@domain.com "));
    }

    @Test
    public void vipByName() {
        VipCustomersData vipData = new VipCustomersData(
                Arrays.asList(
                        new UserName(
                                "Иван",
                                "Иванович",
                                "Иванов"),
                        new UserName(
                                "",
                                "",
                                "")
                ),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList());

        Assertions.assertTrue(vipData.hasCustomerName(new UserName(
                "Иван",
                "Иванович",
                "Иванов"
        )));
        Assertions.assertTrue(vipData.hasCustomerName(new UserName(
                "иван",
                "иванович",
                "иванов"
        )));
        Assertions.assertTrue(vipData.hasCustomerName(new UserName(
                "Иванов",
                "Иванович",
                "Иван"
        )));
        Assertions.assertFalse(vipData.hasCustomerName(new UserName(
                "Иван",
                "",
                "Иванов"
        )));
        Assertions.assertFalse(vipData.hasCustomerName(new UserName(
                "Федор",
                "Федорович",
                "Федоров"
        )));
        Assertions.assertTrue(vipData.hasCustomerName(new UserName(
                " Иван ",
                " Иванович ",
                " Иванов "
        )));
        Assertions.assertFalse(vipData.hasCustomerName(
                new UserName(
                        "",
                        "",
                        "")
        ));
    }

    @Test
    public void vipByUid() {
        VipCustomersData vipData = new VipCustomersData(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList(123L));

        Assertions.assertTrue(vipData.hasUid(123L));
        Assertions.assertFalse(vipData.hasUid(234L));
        Assertions.assertFalse(vipData.hasUid(null));
    }

    @Test
    public void vipByPhone() {
        VipCustomersData vipData = new VipCustomersData(
                Collections.emptyList(),
                Collections.singletonList("+79123456789"),
                Collections.emptyList(),
                Collections.singletonList(123L));

        Assertions.assertTrue(vipData.hasPhone("+79123456789"));
        Assertions.assertTrue(vipData.hasPhone("9123456789"));
        Assertions.assertFalse(vipData.hasPhone("+79000000000"));
        Assertions.assertFalse(vipData.hasPhone(null));
    }
}
