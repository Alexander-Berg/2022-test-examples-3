package ru.yandex.mail.hackathon.calendar;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.mail.hackathon.CalendarFacadeImpl;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class IntegrationTest {
    private final CalendarFacadeImpl facade = null;
    private final static long ROBOT_UID = 1120000000045832L;

    @Test
    public void randomItemShouldNotBeFound() throws IOException, URISyntaxException {
        Optional<String> externalId = facade.getTodoList(ROBOT_UID, "randomTodoNotFound");
        assertThat(externalId).isEmpty();
    }

    @Test
    public void defaultItemShouldBeFound() throws IOException, URISyntaxException {
        Optional<String> externalId = facade.getTodoList(ROBOT_UID, "Не забыть");
        assertThat(externalId).hasValue("8ETx68Qiyandex.ru");
    }

    @SuppressWarnings("JdkObsolete")
    @Test
    public void createItemInNewLayer() throws IOException, URISyntaxException {
        String listTitle = String.format("Рандомный список-%d", DateTime.now().toDate().getTime());
        Optional<String> externalIdOpt = facade.getTodoList(ROBOT_UID, listTitle);
        assertThat(externalIdOpt).isEmpty();
        String externalIdCreated = facade.createTodoList(ROBOT_UID, listTitle);
        facade.createTodoItem(ROBOT_UID, externalIdCreated, "Тестовая закупка");
    }

    @SuppressWarnings("JdkObsolete")
    @Test
    public void stressTest() throws IOException, URISyntaxException {
        String listTitle = String.format("Нагрузочный список-%d", DateTime.now().toDate().getTime());
        Optional<String> externalIdOpt = facade.getTodoList(ROBOT_UID, listTitle);
        assertThat(externalIdOpt).isEmpty();
        String externalIdCreated = facade.createTodoList(ROBOT_UID, listTitle);
        for (int i = 0;i<15;i++) {
            facade.createTodoItem(ROBOT_UID, externalIdCreated, "Тестовая закупка");
        }

    }

}

