package ru.yandex.market.partner.notification.transport.webui.dao;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.mj.generated.server.model.WebUINotificationResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BillingCutOffMessageDaoTest  extends AbstractFunctionalTest {

    @Autowired
    BillingCutOffMessageDao billingCutOffMessageDao;

    @Test
    @DbUnitDataSet(before = "BillingCutOffMessageDao.shouldGetMessages.before.csv")
    public void shouldGetMessages() {
        var actual = billingCutOffMessageDao.getBillingMessages(List.of(111L,222L,333L));
        var expected = List.of(
                buildMessage(111L,"subj","body",11111L,1L,1L,10101L),
                buildMessage(222L,"subj","body",22222L,2L,2L,20202L),
                buildMessage(333L,"subj","body",33333L,3L,3L,30303L)
        );
        assertEquals(expected, actual);
    }

    private WebUINotificationResponse buildMessage(
            Long groupId,
            String subject,
            String body,
            Long shopId,
            Long notificationType,
            Long priority,
            Long userId
    ) {
        var message = new WebUINotificationResponse();
        message.setGroupId(groupId);
        message.setSubject(subject);
        message.setBody(body);
        message.setShopId(shopId);
        message.setNotificationTypeId(notificationType);
        message.setPriority(priority);
        message.setUserId(userId);
        return message;
    }
}
