package ru.yandex.market.tsum.tms.tasks.support;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.startrek.client.model.Issue;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class MbiSupportTaskTest {

    private MbiSupportTask mbiSupportTask = new MbiSupportTask(null, null);

    @Test
    public void renderMessageWithTickets() {
        String message = mbiSupportTask.renderMessage(
            Cf.list(
                new Issue("1", null, "MBI-1", "asdf", 0, Cf.map(), null),
                new Issue("2", null, "MBI-2", "fdsa", 0, Cf.map(), null)
            ), 15, 30, "queue: MBI"
        );
        System.out.println(message);
        Assert.assertEquals(
            "Сегодня создано 15 тикетов . Первые 2 по весу:\n" +
                "\n" +
                "[MBI-1](https://st.yandex-team.ru/MBI-1) asdf\n" +
                "\n" +
                "[MBI-2](https://st.yandex-team.ru/MBI-2) fdsa\n" +
                "\n" +
                "[Всего неразобранных тикетов](https://st.yandex-team.ru/filters/filter?query=queue%3A+MBI): 30.\n"
                    .trim(),
            message.trim()
        );
    }

    @Test
    public void renderMessageWithout() {
        String message = mbiSupportTask.renderMessage(
            Cf.list(), 0, 30, "queue: MBI"
        );
        System.out.println(message);
        Assert.assertEquals(
            "Сегодня неразобранных тикетов нет.\n" +
                "\n" +
                "[Всего неразобранных тикетов](https://st.yandex-team.ru/filters/filter?query=queue%3A+MBI): 30.\n"
                    .trim(),
            message.trim()
        );
    }
}