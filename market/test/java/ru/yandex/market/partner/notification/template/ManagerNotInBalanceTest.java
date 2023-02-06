package ru.yandex.market.partner.notification.template;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.xml.impl.NamedContainer;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.market.partner.notification.service.NotificationSendContext;
import ru.yandex.market.partner.notification.service.NotificationService;

/**
 * Тесты для шаблона 1650534404 "Уведомление менеджеру об отсутствии прав в Балансе".
 */
public class ManagerNotInBalanceTest extends AbstractFunctionalTest {
    private static final int TEMPLATE_ID = 1650534404;

    @Autowired
    NotificationService notificationService;

    @Test
    @DbUnitDataSet(after = "managerNotInBalance/test.after.csv")
    void test() {

        List<Object> data = new ArrayList<>();
        data.add(new NamedContainer("business-name", "Ромашка"));

        var ctx = new NotificationSendContext.Builder()
                .setTypeId(TEMPLATE_ID)
                .setData(data)
                .setRecepientEmail("manager@yandex-team.ru")
                .setRenderOnly(true)
                .build();

        notificationService.send(ctx);
    }
}
