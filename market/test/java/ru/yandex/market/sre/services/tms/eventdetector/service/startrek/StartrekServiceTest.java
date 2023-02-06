package ru.yandex.market.sre.services.tms.eventdetector.service.startrek;

import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.StartrekClientBuilder;
import ru.yandex.startrek.client.model.Component;
import ru.yandex.startrek.client.model.ComponentCreate;
import ru.yandex.startrek.client.model.QueueRef;

import static org.junit.Assert.assertEquals;

public class StartrekServiceTest {

    @Ignore
    @Test
    public void copyComponents() {
        final String from = "MARKETINCIDENTS", to = "MARKETALARMS";

        final Session session = new StartrekClientBuilder()
                .uri("https://st-api.yandex-team.ru")
                .socketTimeout(30, TimeUnit.SECONDS)
                .build("token");
        IteratorF<Component> source = session.components().getAll(from);
        IteratorF<Component> destination = session.components().getAll(to);

        QueueRef qFrom = session.queues().get(from);
        QueueRef qTo = session.queues().get(to);


        source.stream().forEach(component -> {
            if (destination.stream().anyMatch(c -> c.getName().equals(component.getName()))) {
                return;
            }
            session.components().create(ComponentCreate.builder()
                    .queue(qTo)
                    .name(component.getName())
                    .description(component.getDescription().getOrNull())
                    .archived(false)
                    .assignAuto(false)
                    .build());
        });

    }


    public void printComponents() {
        final String from = "MARKETINCIDENTS";

        final Session session = new StartrekClientBuilder()
                .uri("https://st-api.yandex-team.ru")
                .socketTimeout(30, TimeUnit.SECONDS)
                .build("<YOUR TOKEN>");
        IteratorF<Component> source = session.components().getAll(from);


        source.stream().forEach(component -> {
            System.out.println(
                    component.getName().toUpperCase()
                            .replaceAll(":", "_")
                            .replaceAll("-", "_") +
                            " (\"" + component.getName() + "\"),"
            );
        });

    }

    @Test
    public void header() {
        assertEquals("[ 2020-07-31 03:04 ] Превышен TTLB компонента Blue Touch Nginx", ("[ 2020-07-31 03:04 ] Превышен" +
                " **TTLB** компонента Blue Touch Nginx").replaceAll("\\*\\*", ""));
    }

}
