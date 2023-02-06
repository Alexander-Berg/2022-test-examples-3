package ru.yandex.travel.api.endpoints.booking_flow;

import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
        properties = {
                // pollutes log with errors
                "train-offer.mode=EMPTY",
        }
)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class AbstractBookingFlowIntegrationTest {
}
