package ru.yandex.travel.hotels.searcher;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles(profiles = {
        "dev",
        "dev-grpc"
})
public class ApplicationDevProfileTest {

    @Test
    public void contextLoads() {
        /* No need to make any specific check */
    }
}
