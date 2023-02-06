package ru.yandex.travel.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    properties = {
        "secrets.yt-token=empty",
        "secrets.yt-user=empty",
    }
)
@ActiveProfiles("dev")
public class ApplicationDevProfileTests {

    @Test
    public void contextLoads() {
    }

}
