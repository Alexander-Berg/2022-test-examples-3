package ru.yandex.market.direct.feed;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.core.direct.feed.RefreshFeedYtDao;

import static org.mockito.Mockito.mock;

@Configuration
public class RefreshFeedYtDaoTestConfig {
    @Bean
    public RefreshFeedYtDao refreshFeedYtDao() {
        return mock(RefreshFeedYtDao.class);
    }
}
