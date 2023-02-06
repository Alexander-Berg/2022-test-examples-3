package ru.yandex.travel.acceptance;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ru.yandex.travel.orders.OrdersApplication.class)
@ActiveProfiles("acceptance")
@DirtiesContext
public class OrdersApplicationAcceptanceProfileTests {
    @Test
    public void contextLoads() {
    }
}
