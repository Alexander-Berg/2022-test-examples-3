package ru.yandex.market.sre.services.tms.eventdetector.service.startrek;

import java.util.concurrent.TimeUnit;

import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.StartrekClientBuilder;

public class ComponentsServiceTest {

    public void getComponents() {
        final Session session = new StartrekClientBuilder()
                .uri("https://st-api.yandex-team.ru")
                .socketTimeout(30, TimeUnit.SECONDS)
                .build("<TOKEN>");
        ComponentsService componentsService = new ComponentsService(session);
        componentsService.loadComponents("MARKETINCIDENTS").forEach((component, id) -> System.out.println(component +
                ":" + id));

    }
}
