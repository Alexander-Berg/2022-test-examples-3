package ru.yandex.manual;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bannerstorage.harvester.integration.direct.DirectClient;
import ru.yandex.bannerstorage.harvester.queues.screenshooter.ScreenShooterSubscriber;
import ru.yandex.direct.screenshooter.client.configuration.ScreenShooterConfiguration;
import ru.yandex.direct.screenshooter.client.service.ScreenShooterClient;

@Ignore("Для ручного запуска")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ScreenShooterConfiguration.class})
public class GetScreenshotTest {
    @Autowired
    ScreenShooterClient screenShooterClient;

    @Test
    public void testScreenshotOneCreative() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("net.sourceforge.jtds.jdbc.Driver");
        dataSource.setUrl("jdbc:jtds:sqlserver://rapidc.yandex.ru/BannerStorage");
        dataSource.setUsername("changeit");
        dataSource.setPassword("changeit");
        dataSource.setValidationQuery("select 1");
        dataSource.setMaxTotal(10);
        dataSource.setMaxIdle(3);
        dataSource.setRemoveAbandonedTimeout(300);

        ScreenShooterSubscriber screenShooterSubscriber = new ScreenShooterSubscriber(
                new JdbcTemplate(dataSource),
                screenShooterClient,
                new DirectClient("http://intapi.direct.yandex.ru:8080")
        );

        // Один из старых креативов
        screenShooterSubscriber.notify(
                8019419
        );
    }
}
