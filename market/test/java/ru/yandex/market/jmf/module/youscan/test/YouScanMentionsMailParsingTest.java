package ru.yandex.market.jmf.module.youscan.test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import javax.inject.Inject;
import javax.mail.MessagingException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.mail.impl.MailProcessingService;
import ru.yandex.market.jmf.module.mail.test.impl.MailMessageBuilderService;
import ru.yandex.market.jmf.module.mail.test.impl.MailTestUtils;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.module.youscan.ModuleYouScanTestConfiguration;
import ru.yandex.market.jmf.module.youscan.YouScanTicket;

@Transactional
@SpringJUnitConfig(classes = ModuleYouScanTestConfiguration.class)
public class YouScanMentionsMailParsingTest {
    @Inject
    private MailMessageBuilderService mailMessageBuilderService;
    @Inject
    private MailProcessingService mailProcessingService;
    @Inject
    private MailTestUtils mailTestUtils;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private EntityStorageService entityStorageService;

    @BeforeEach
    public void setUp() {
        mailTestUtils.createMailConnection("test");
        var testContext = ticketTestUtils.create();
        ticketTestUtils.createService(testContext.team0, testContext.serviceTime24x7, testContext.brand, Optional.of(
                "testService"));
    }

    @Test
    public void parseTwoMentions() throws MessagingException {
        var mailMessageBuilder = mailMessageBuilderService.getMailMessageBuilder(
                "test",
                "/ru/yandex/market/jmf/module/youscan/two_mentions.eml");
        var mailMessage = mailMessageBuilder.build();
        mailProcessingService.processInMessage(mailMessage);
        var tickets = entityStorageService.<YouScanTicket>list(Query.of(Fqn.of("ticket$testYouScan")));

        Assertions.assertEquals(2, tickets.size());
        var first = tickets.get(0);

        Assertions.assertEquals(OffsetDateTime.of(2020, 12, 7, 12, 8, 0, 0, ZoneOffset.of("+3")),
                first.getRequestDate());
        Assertions.assertEquals("Лучший товар по акции", first.getTitle());
        Assertions.assertEquals("Виктория Художилова", first.getClientName());
        Assertions.assertEquals("http://url3371.youscan.io/ls/click?upn=JRAcwcdR7RU8E5ZBv9BSQvX9VJ8Ws6fhWmJ" +
                        "-2FXmqFe4faFZNgqC49aroJ7JPZyaT2IH8mHsMpNi-2BfZWkTNWfpbg-3D-3Dh218_U7ELvkKXFbx" +
                        "-2BkENwq1b0KazqvsmM7B5PcSZjj0CHkYDXP7182mYDGR3sYJ5K4ppSXpBKqKHyKxFHM3Wgp3IvEbMxS" +
                        "-2BAVrxs48CINQFW5IAtSi4pUIKkvsuYViFPYSCF5SBpBCLEJXDbSjALKhwmCc5PhAGF4oLHjwo1hoHD2LLuaTymVW0Y" +
                        "-2BPpQDvWz0SpXcwU3AZhNtW043Y-2BffZ-2BdhvqOKCa9qWW04f9mBi3zzqsjIx4cOxCbO2N9sORheYlPuA2iOxjkw" +
                        "-2BLXevsLwP-2BLLCqWdA7DKwKhTxvW3Xa5Wz-2FZuNskG5i" +
                        "-2BdqyjzypmgMZLgfwKpKNFlcaWsMDmo6dM7jhEITP8yVc64cGeW0NY2Vt0-3D",
                first.getMentionSourceUrl().getHref());
        Assertions.assertEquals("http://url3371.youscan.io/ls/click?upn=GauYhhPVng1093fEwIpsG4Iw5kk87JhtXNbzz6tJ7t" +
                "-2Fitu5B2DXg0PsiVDp3xLxEOPg2u9vyP7qHUPbKar7G0XHQoMe6kPmKYBCfQptZ-2FGDz9kerMrhVK2NK-2FSP" +
                "-2FsvZ12fE8_U7ELvkKXFbx" +
                "-2BkENwq1b0KazqvsmM7B5PcSZjj0CHkYDXP7182mYDGR3sYJ5K4ppSXpBKqKHyKxFHM3Wgp3IvEbMxS" +
                "-2BAVrxs48CINQFW5IAtSi4pUIKkvsuYViFPYSCF5SBpBCLEJXDbSjALKhwmCc5PhAGF4oLHjwo1hoHD2LLtMwhes7BZ3w" +
                "-2FKlp3pLt3jD5lcA5rNTXZs0QQ20cO09JAT68ViUqBhCuTQxj-2FALbzgiBtPhdUq4kXcBmklkbn4gfqqdBsuH-2FIOzIW3p" +
                "-2Bqbu-2BvUN6qMcq-2FQ-2F9sGoSv-2FidCpGG-2Fj3Z-2FTvsnrG3srlA6PbW4SX" +
                "-2BQdztDZgNxrjmihw8wl5jupP2WIu0L1KlZiIlX8-3D", first.getYouScanMentionUrl().getHref());
        Assertions.assertEquals("vk.com", first.getMentionSourceUrl().getValue());
        Assertions.assertEquals("загляните в \n" +
                "<b>яндекс</b>. \n" +
                "<b>маркет</b> - там куча лохотрон с их акциями и люди купились - теперь нет не товара и денег " +
                "вернуть геморой на пару месяцев.", first.getDescription());

        var second = tickets.get(1);

        Assertions.assertEquals(OffsetDateTime.of(2020, 12, 6, 19, 58, 0, 0, ZoneOffset.of("+3")),
                second.getRequestDate());
        Assertions.assertEquals("Терминалы выдачи заказов PickPoint - Безобразно", second.getTitle());
        Assertions.assertEquals("Летящий", second.getClientName());
        Assertions.assertEquals("http://url3371.youscan.io/ls/click?upn=5g5vfuND8ujt1ZuYmo-2FVfvg" +
                "-2BYUAYITE3yhINNuysa3kA5QpWJRkPgzfhlMyP4DqFjLQi_U7ELvkKXFbx" +
                "-2BkENwq1b0KazqvsmM7B5PcSZjj0CHkYDXP7182mYDGR3sYJ5K4ppSXpBKqKHyKxFHM3Wgp3IvEbMxS" +
                "-2BAVrxs48CINQFW5IAtSi4pUIKkvsuYViFPYSCF5SBpBCLEJXDbSjALKhwmCc5PhAGF4oLHjwo1hoHD2LLtZxqxmV" +
                "-2FzGwe0bAXwYCBn4tOq8uAGByJqncADHg0-2BMdSF01wY-2BvXpKYEy0i1GAJYB8sqRh" +
                "-2Bz9AAaRc2tEptCzJi2K33ieSAsnrZx76QM-2B04KPCCee3Dk7IN0klpX35-2FyMjBWCKJIzAXurf8LsvZhn-2F5Xk0X" +
                "-2FhvzzspbGsKb3wYGe7j4-2FXA1rRu2xVxxIArHvg-3D", second.getMentionSourceUrl().getHref());
        Assertions.assertEquals("otzovik.com", second.getMentionSourceUrl().getValue());
        Assertions.assertEquals("http://url3371.youscan.io/ls/click?upn=GauYhhPVng1093fEwIpsG4Iw5kk87JhtXNbzz6tJ7t" +
                "-2Fitu5B2DXg0PsiVDp3xLxEkm1" +
                "-2BRdB5OSSzOayQxg9FdCtVotmD1ea2h582UQh2kXTUxoOehvNtkfq7zB6pnqZOzmXq_U7ELvkKXFbx" +
                "-2BkENwq1b0KazqvsmM7B5PcSZjj0CHkYDXP7182mYDGR3sYJ5K4ppSXpBKqKHyKxFHM3Wgp3IvEbMxS" +
                "-2BAVrxs48CINQFW5IAtSi4pUIKkvsuYViFPYSCF5SBpBCLEJXDbSjALKhwmCc5PhAGF4oLHjwo1hoHD2LLtW" +
                "-2FnKx7ugdhsf7HOf25RCCU1Lx-2FA1NAteqy4XJnN2H2jq1Bvxx-2FCq9Zt4XCSBxesbv-2Bjnvz7dJYYmYQu7pqRKIZXjGDueb" +
                "-2FSRJpIW4bfInAlRJwRhrIHxJf91PsRcC3WIfbAWkXclrDfBblxNRX9PpaDNmU1pg7GwNU-2F1wYTJ4viXzN" +
                "-2B5GgqfFcftuTYyvwBM-3D", second.getYouScanMentionUrl().getHref());
        Assertions.assertEquals("Нет \n" +
                        "<br />Описала выше.Курьер решает,куда ему удобнее доставить товар. \n" +
                        "<br />Заказала товар на \n" +
                        "<b>Яндекс</b> . \n" +
                        "<b>Маркет</b>., написала, куда доставить. Приходит сообщение, что заказ можно получить... но" +
                        " по " +
                        "другому адресу. В чем дело??? Я инвалид 2 группы, заказываю туда, где мне удобно получить. " +
                        "Почему " +
                        "курьер выполняет работу не как удобно клиенту, а как надо ему??? Я заказываю товар, выбираю " +
                        "место " +
                        "доставки, оплачиваю заказ и доставку. Так почему курьер меняет мне место доставки???",
                second.getDescription());
    }

    @Test
    public void parseTwoMentions2() throws MessagingException {
        var mailMessageBuilder = mailMessageBuilderService.getMailMessageBuilder(
                "test",
                "/ru/yandex/market/jmf/module/youscan/two_mentions_2.eml");
        var mailMessage = mailMessageBuilder.build();
        mailProcessingService.processInMessage(mailMessage);
        var tickets = entityStorageService.<YouScanTicket>list(Query.of(Fqn.of("ticket$testYouScan")));

        Assertions.assertEquals(2, tickets.size());
        var first = tickets.get(0);

        Assertions.assertEquals(OffsetDateTime.of(2021, 1, 15, 16, 17, 0, 0, ZoneOffset.of("+3")),
                first.getRequestDate());
        Assertions.assertEquals("У нас пятница", first.getTitle());
        Assertions.assertEquals("iva_in_lj", first.getClientName());
        Assertions.assertEquals("http://url3371.youscan.io/ls/click?upn=67sE9YuTL-2BG3STY3exQ0Ch" +
                        "-2Bwv7t2bfiGakqYo5rbPyNdslsC99lkhIUNjFWUc8M-2BwNiNWdfym0B9LV-2FOZQt-2FiFiKefFM-2FYEtSSb" +
                        "-2B2U628R-2BqxZZbU5de2tpeGVO0IIjkEcdN_YKMNI0yuPf1cBOV7AyLmBwAflwYK9wMLh8yDsqUDvsz" +
                        "-2BRXnjsAjUnlzuIb9p0gKv2OZM9seEYrUfwfwbKK-2BHg6XDFk5R1347NykPOx9MugdI0c" +
                        "-2FKWKEmvZ8FFYRkHWwMAduMno6yjerS5UL-2FtTF7rm2FM-2F-2FqLCVBSLZc9PD" +
                        "-2FwudAmZG3PGlVFAOIZIEbbtcdaKfEH8LcG6bBqCX4D8aqXrXVYHEIOHxEDwMqcRM5zqVdt8FcAkR3kPguHLVMGCk1wSDydKW9xl3tGzDjzsFFAHWaPgRJ6VYz-2B9ZJ60AP-2BH-2Fpw7x4sN9KXqdF-2B24n6BtJlwa1H9ztp3b3SvxW7XzkzbUYWOkpXjZOwk7ba7MBbOov8wzVH3XOmmRPoDefDYz3",
                first.getMentionSourceUrl().getHref());
        Assertions.assertEquals("http://url3371.youscan.io/ls/click?upn=GauYhhPVng1093fEwIpsG4Iw5kk87JhtXNbzz6tJ7t" +
                "-2Fitu5B2DXg0PsiVDp3xLxEIPklkWjbCqg0Lli" +
                "-2BVIkX3KYKVYuXGalVFzsyPqNshSUqBTiiiovdu01aVs6yYeB9hLiW_YKMNI0yuPf1cBOV7AyLmBwAflwYK9wMLh8yDsqUDvsz" +
                "-2BRXnjsAjUnlzuIb9p0gKv2OZM9seEYrUfwfwbKK-2BHg6XDFk5R1347NykPOx9MugdI0c" +
                "-2FKWKEmvZ8FFYRkHWwMAduMno6yjerS5UL-2FtTF7rm2FM-2F-2FqLCVBSLZc9PD-2FwudAmZG3PGlVFAOIZIEbbtcd" +
                "-2FQx4hLy71VIUyLqdWyIVZ4Aq2X3WWIanI38LWUI0GCnDTS61VnUiwgKLwZDQrzEEyDZS6Aa4sYZ7JLamI" +
                "-2FvJQKZQ5wfDdBLoMGk8sxUyNuGv2RFsL9JGKEV6a4qJikKqR1MTh9ADbemQCfTckG1cUUQWqU55CSh8BG4tSIdwsiywHmyYLx" +
                "-2FKJILx2lSGEx66", first.getYouScanMentionUrl().getHref());
        Assertions.assertEquals("livejournal.com", first.getMentionSourceUrl().getValue());
        Assertions.assertEquals("мягкий круг обшитый тканью мне больше типа такого https://\n" +
                "<b>market</b>.\n" +
                "<b>yandex</b>.ru/offer/105rpFQQLfAAEhV3Z2DuDQ?from-show-uid&#61;16107092724100492781400000&amp;" +
                "cpa&#61;0&amp;onstock&#61;1\n" +
                "<br />мне больше нравится, т.к. сгибается в два-три раза и его удобно положить в авоську (и туда же " +
                "лопаты, снежколепы, запасные варежки и т.д). А небольшие пластиковые у меня ни в какую сумку не " +
                "помещаются (даже в большой рюкзак с трудом и их неудобно оттуда доставать)", first.getDescription());

        var second = tickets.get(1);

        Assertions.assertEquals(OffsetDateTime.of(2021, 1, 15, 5, 49, 0, 0, ZoneOffset.of("+3")),
                second.getRequestDate());
        Assertions.assertEquals("Новое упоминание от Медтехника + на facebook.com", second.getTitle());
        Assertions.assertEquals("Медтехника +", second.getClientName());
        Assertions.assertEquals("http://url3371.youscan" +
                ".io/ls/click?upn=qv2acXUAf-2Bl" +
                "-2FQ97a2iN5DxlroZ59gzJlJhFf4uUMMwgq7wWXlFiOrejp2yvPktOQvHIejgH9j1bZ2BMmAe-2F6w0dqOy7oq7NFN78L9uw" +
                "-2FaboIm0WoC3iiiEKi7sBU-2F900JMV2_YKMNI0yuPf1cBOV7AyLmBwAflwYK9wMLh8yDsqUDvsz" +
                "-2BRXnjsAjUnlzuIb9p0gKv2OZM9seEYrUfwfwbKK-2BHg6XDFk5R1347NykPOx9MugdI0c" +
                "-2FKWKEmvZ8FFYRkHWwMAduMno6yjerS5UL-2FtTF7rm2FM-2F-2FqLCVBSLZc9PD" +
                "-2FwudAmZG3PGlVFAOIZIEbbtcdQkDHeAYzT1TlTcCZo5pVcbrPMqp3DpXHK6yLCC6yYSM45Wdmv38ORmcz7oxKj3WWbXfqC4K4XTNJVhCnIpP-2Fil39zoYNuncuP-2FsQypHlKoUpsnkUiTxKKeFdwx1fIy0XHg-2Bo-2F3uT6Pg8NPdRGrzdwVdlEG-2BXWoNh3qg594qB8Eqf1HuT1QdMSk7nUJe9GNnZ", second.getMentionSourceUrl().getHref());
        Assertions.assertEquals("facebook.com", second.getMentionSourceUrl().getValue());
        Assertions.assertEquals("http://url3371.youscan.io/ls/click?upn=GauYhhPVng1093fEwIpsG4Iw5kk87JhtXNbzz6tJ7t" +
                "-2Fitu5B2DXg0PsiVDp3xLxE4aQDqn08INgRBHyYWY-2BMaNKZ4eub6T67C3Wywn7T-2F-2F8bGgQzHtB" +
                "-2BGIkM9jVzMcmojBkf_YKMNI0yuPf1cBOV7AyLmBwAflwYK9wMLh8yDsqUDvsz" +
                "-2BRXnjsAjUnlzuIb9p0gKv2OZM9seEYrUfwfwbKK-2BHg6XDFk5R1347NykPOx9MugdI0c" +
                "-2FKWKEmvZ8FFYRkHWwMAduMno6yjerS5UL-2FtTF7rm2FM-2F-2FqLCVBSLZc9PD" +
                "-2FwudAmZG3PGlVFAOIZIEbbtcd0GzU0GDVWYjCi2eiFWBzZYBWzfaEJ" +
                "-2FzCCsLutTZGitZHXQ1VbxpwWr63pyvgSKY1MOtwaXGAZS8-2F5zKpWbx7yiaCB1cT2MbM3" +
                "-2BljQaFoPsOCHz8fW5IjLIHn38duHGHoGa-2F-2FoGpzJ07peHXEdz2yGodCbgivAq3E1tR3IfvtbQ4gWilUZ9-2FTj9akLGe4" +
                "-2Bxpd", second.getYouScanMentionUrl().getHref());
        Assertions.assertEquals("Получили на Флампе отзыв о работе магазина на ул. Зыряновская, 57 о покупке " +
                        "ортопедической подушки с доставкой.\n" +
                        "<br />Спасибо!❤☺\n" +
                        "<br />\n" +
                        "<br />Все отзывы можно прочитать на нашем сайте, на Флампе, на \n" +
                        "<b>Яндекс</b> \n" +
                        "<b>Маркете</b>, на BLIZKO.\n" +
                        "<br />#отзыв #отзывпокупателя #медтехника",
                second.getDescription());
    }

    @Test
    public void parseOneMention() throws MessagingException {
        var mailMessageBuilder = mailMessageBuilderService.getMailMessageBuilder(
                "test",
                "/ru/yandex/market/jmf/module/youscan/one_mention.eml");
        var mailMessage = mailMessageBuilder.build();
        mailProcessingService.processInMessage(mailMessage);
        var tickets = entityStorageService.<YouScanTicket>list(Query.of(Fqn.of("ticket$testYouScan")));

        Assertions.assertEquals(1, tickets.size());
        var first = tickets.get(0);

        Assertions.assertEquals(OffsetDateTime.of(2020, 12, 5, 1, 22, 0, 0, ZoneOffset.of("+3")),
                first.getRequestDate());
        Assertions.assertEquals("Служба экспресс-доставки \"DPD\" (Россия, Москва) - Ужасно!", first.getTitle());
        Assertions.assertEquals("Ammika", first.getClientName());
        Assertions.assertEquals("http://url3371.youscan.io/ls/click?upn=5g5vfuND8ujt1ZuYmo-2FVfvg" +
                        "-2BYUAYITE3yhINNuysa3nluwyjs-2FU5IorfLvjKfJUt2Kq7_U7ELvkKXFbx" +
                        "-2BkENwq1b0KazqvsmM7B5PcSZjj0CHkYDXP7182mYDGR3sYJ5K4ppSXpBKqKHyKxFHM3Wgp3IvEbMxS" +
                        "-2BAVrxs48CINQFW5IAtSi4pUIKkvsuYViFPYSCF5X8wQP9JO9I1Zmi08zyi7DnS2p7Zo0L55ugL09UqLI61cu6c1R6ZXWnHsFp7Lc3QTRWyGJW5-2BQ51RfUz3-2FiPgNEfILCoL3U1JFUT9Vuzldi8iBPWLk-2BejI4UNoAAFvJl7rdfsvobGhHALY3WDBaLfBoO2Skhr8OrkIudTzJBOuA27zfM49lXT3-2Btr5m7iRn63WvAMdMcnYyno3JcbAeTYRiwjP-2Fr3AvjxFcCbTUQCU7A-3D",
                first.getMentionSourceUrl().getHref());
        Assertions.assertEquals("otzovik.com", first.getMentionSourceUrl().getValue());
        Assertions.assertEquals("http://url3371.youscan.io/ls/click?upn=GauYhhPVng1093fEwIpsG4Iw5kk87JhtXNbzz6tJ7t" +
                "-2Fitu5B2DXg0PsiVDp3xLxE8OgHsMKSWoR18bmdvuLNpMCc8Ewk3gFooevTtqD-2BkRiAN-2FLzMtK" +
                "-2F1uYSK3DjH8sezEVp_U7ELvkKXFbx" +
                "-2BkENwq1b0KazqvsmM7B5PcSZjj0CHkYDXP7182mYDGR3sYJ5K4ppSXpBKqKHyKxFHM3Wgp3IvEbMxS" +
                "-2BAVrxs48CINQFW5IAtSi4pUIKkvsuYViFPYSCF5X8wQP9JO9I1Zmi08zyi7DnS2p7Zo0L55ugL09UqLI62JuAuRHF7K6FjT43ldgFkxwpNnpRPKAST0uDOsAhOyfMn-2BbqVtluZ7gNJEw26F1myx43hudoNw4Ab1uT2Y3Akon6-2B9Oc-2FT4G8eEo3LHAN4Xc1uD3FDW-2Fb7k4P8MsfqcESfDQ6j8BVSE2Hc6pTV5TFfWikUv-2F7BYKhXf-2FPJX8rwqsFkV-2F5mgs7mfk-2BazZ8efWk-3D", first.getYouScanMentionUrl().getHref());
        Assertions.assertEquals("нет \n" +
                "<br />крупный минус всей доставке безответственной \n" +
                "<br />Заказала электрическую плиту через \n" +
                "<b>яндекс</b> \n" +
                "<b>маркет</b>, т. к товар крупногабаритный автоматически назначали перевозчиком DPD. Несколько дней " +
                "подряд переносят доставку, причем по тихому, пока ты ждешь звонка курьера, в твоем личном кабинете " +
                "тупо поменялась дата доставки, даже не согласовывают можешь ты или нет! Компании плевать, что у " +
                "людей есть другие дела, что приходится подстраиваться под них и элементарно не на чем готовить. " +
                "Первый раз вижу такое безответственное отношение к клиенту!", first.getDescription());
    }
}
