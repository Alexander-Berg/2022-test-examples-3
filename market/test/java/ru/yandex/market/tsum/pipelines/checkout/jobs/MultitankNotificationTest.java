package ru.yandex.market.tsum.pipelines.checkout.jobs;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.clients.startrek.NotificationUtils;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.events.NotificationEventMeta;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekTicket;

import static ru.yandex.market.tsum.pipelines.checkout.jobs.NotifyBeforeShootingJobNotifications.MULTITANK_SHOOTING_PLANNED_EVENT_META;

public class MultitankNotificationTest {
    @Test
    public void generateNotification() {
        final NotificationEventMeta.DefaultMessages defaultMessages =
            MULTITANK_SHOOTING_PLANNED_EVENT_META.getDefaultMessages();
        final String template = defaultMessages.getTelegramDefault();
        Assert.assertNotNull(template);
        final PandoraTankConfigImpl tankConfig = new PandoraTankConfigImpl();
        tankConfig.setTankBaseUrl("<tankUrl>");
        final String message = NotificationUtils.render(template, PandoraNotifyBeforeShootingJob.getNotificationContext(
            new StartrekTicket("MARKETLOAD-1"),
            "login",
            "<url>",
            LocalDateTime.of(2020, Month.APRIL, 1, 3, 7, 54),
            List.of(MultipleTanksShootingBundle.builder()
                    .withMutableConfig(PandoraMutableConfigImpl.builder()
                        .setDuration(75 * 60)
                        .setOrdersPerHour(4509)
                        .build())
                    .withCheckouterConfig(PandoraCheckouterConfigImpl.builder()
                        .setBalancer("<balancer0>")
                        .build())
                    .withTankConfig(tankConfig)
                    .withRegionSpecificConfig(PandoraRegionSpecificConfigImpl.builder()
                        .setRegionId("<region0>")
                        .setDeliveryServices("<delivery0>")
                        .setWarehouseId("<warehouse0>")
                        .build())
                    .withShootingDelayOption(new ShootingDelayOption(45))
                    .build(),
                MultipleTanksShootingBundle.builder()
                    .withMutableConfig(PandoraMutableConfigImpl.builder()
                        .setDuration(75 * 60)
                        .setOrdersPerHour(4509)
                        .build())
                    .withCheckouterConfig(PandoraCheckouterConfigImpl.builder()
                        .setBalancer("<balancer1>")
                        .build())
                    .withTankConfig(tankConfig)
                    .withRegionSpecificConfig(PandoraRegionSpecificConfigImpl.builder()
                        .setRegionId("<region1>")
                        .setDeliveryServices("<delivery1>")
                        .setWarehouseId("<warehouse1>")
                        .build())
                    .withShootingDelayOption(new ShootingDelayOption(90))
                    .build()), 5
        ));
        Assert.assertEquals(
            "Привет!\n" +
                "\n" +
                "Будут запущены автоматические стрельбы заказами в проде.\n" +
                "Тикет [MARKETLOAD-1](https://st.yandex-team.ru/MARKETLOAD-1).\n" +
                "\n" +
                "Настройки стрельб:\n" +
                "\n" +
                "*Время запуска: 03:57:54*\n" +
                "*Танк: <tankUrl>*\n" +
                "*Длительность: 1:15:00*\n" +
                "*Целевой чекаутер: <balancer0>*\n" +
                "*Регион доставки: <region0>*\n" +
                "*Службы доставки: <delivery0>*\n" +
                "*Склад: <warehouse0>*\n" +
                "*Мощность: 4509/час*\n" +
                "\n" +
                "*Время запуска: 04:42:54*\n" +
                "*Танк: <tankUrl>*\n" +
                "*Длительность: 1:15:00*\n" +
                "*Целевой чекаутер: <balancer1>*\n" +
                "*Регион доставки: <region1>*\n" +
                "*Службы доставки: <delivery1>*\n" +
                "*Склад: <warehouse1>*\n" +
                "*Мощность: 4509/час*\n" +
                "\n" +
                "\n" +
                "Пайплайн стрельб запущен: *login* в 03:07:54 01.04.2020\n" +
                "Ссылка на пайплайн: <url>\n" +
                "\n" +
                "Если вы считаете, что стрельбы сейчас/с такими настройками проводить недопустимо — напишите " +
                "пожалуйста [login](https://staff.yandex-team.ru/login).\n" +
                "\n" +
                "Отчет по стрельбам можно будет увидеть тут: https://stat.yandex-team" +
                ".ru/Market/Checkouter/Shooting/DeliveryIntegration\n" +
                "Дашборд по состоянию сервисов во время стрельбы: https://grafana.yandex-team" +
                ".ru/d/keFSBTnnk/metriki-zaprosov-v-chekauter-iz-strel-b-po-produ?orgId=1&refresh=10s&from=now&to=now" +
                "%2B4h",
            message);
    }
}
