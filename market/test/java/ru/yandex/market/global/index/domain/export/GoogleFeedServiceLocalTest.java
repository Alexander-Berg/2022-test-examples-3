package ru.yandex.market.global.index.domain.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.index.BaseLocalTest;

@Slf4j
@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GoogleFeedServiceLocalTest extends BaseLocalTest {
    private final GoogleFeedService googleFeedService;

    @Test
    public void test() {
        String feed = googleFeedService.createFeed();
        System.out.println(feed);
    }
}
