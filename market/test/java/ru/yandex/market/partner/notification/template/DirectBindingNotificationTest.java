package ru.yandex.market.partner.notification.template;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.xml.impl.NamedContainer;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.market.partner.notification.service.NotificationSendContext;
import ru.yandex.market.partner.notification.service.NotificationService;

public class DirectBindingNotificationTest extends AbstractFunctionalTest {

    @Autowired
    NotificationService notificationService;


    @DisplayName("Успешно отправили уведомление о подтверждении привязки бизнеса к аккаунту в Директе.")
    @DbUnitDataSet(
            after = "directBinding/directNotificationSuccess.after.csv"
    )
    @Test
    public void directNotificationSuccess() {

        List<Object> data = new ArrayList<>();
        data.add(new NotificationInfo("eogoreltseva"));
        data.add(new NamedContainer("business-name", "БизнесТест"));

        NotificationSendContext ctx = new NotificationSendContext.Builder()
                .setBusinessId(1L)
                .setTypeId(1647332487)
                .setData(data)
                .setRenderOnly(true)
                .build();

        notificationService.send(ctx);
    }

    @DisplayName("Успешно отправлен запрос о подтверждении привязки бизнеса к аккаунту в Директе.")
    @DbUnitDataSet(
            after = "directBinding/directNotificationInvitationSuccess.after.csv"
    )
    @Test
    public void directNotificationInvitationSuccess() {

        List<Object> data = new ArrayList<>();
        data.add(new NotificationInfo("eogoreltseva",
                "9c7a68cc-7bd8-4ea1-bfb1-bf8b395ab78c", "partner.market.yandex.ru"));
        data.add(new NamedContainer("business-name", "БизнесТест"));

        NotificationSendContext ctx = new NotificationSendContext.Builder()
                .setBusinessId(1L)
                .setTypeId(1647502310)
                .setData(data)
                .setRenderOnly(true)
                .build();

        notificationService.send(ctx);
    }

    public static final class NotificationInfo implements Serializable {
        private final String login;
        private final String invitationId;
        private final String partnerUrl;

        public NotificationInfo(String login) {
            this.login = login;
            this.invitationId = null;
            this.partnerUrl = null;
        }

        public NotificationInfo(String login, String invitationId, String partnerUrl) {
            this.login = login;
            this.invitationId = invitationId;
            this.partnerUrl = partnerUrl;
        }

        public String getLogin() {
            return login;
        }

        public String getInvitationId() {
            return invitationId;
        }

        public String getPartnerUrl() { return partnerUrl; }
    }
}
