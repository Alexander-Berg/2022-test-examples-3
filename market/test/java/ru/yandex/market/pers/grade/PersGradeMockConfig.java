package ru.yandex.market.pers.grade;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.common.framework.core.ServantInfo;
import ru.yandex.market.pers.grade.config.PersGradeConfig;
import ru.yandex.market.pers.test.common.MemCachedMockUtils;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 26.11.2021
 */
// all configs in this grade configs package
@ComponentScan(basePackageClasses = PersGradeConfig.class)
@ComponentScan( // all @Services in project, but not configurations
    basePackageClasses = {PersGradeMain.class},
    excludeFilters = @ComponentScan.Filter(Configuration.class)
)
@Configuration
public class PersGradeMockConfig {

    @Autowired
    private WebApplicationContext wac;

    @Bean
    public Cache<String, Object> mockedCacheMap() {
        return CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    }

    @Bean
    public MemCachedAgent memCachedAgent() {
        return MemCachedMockUtils.buildMemCachedAgentMock(mockedCacheMap());
    }

    @Bean
    public MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Bean
    public ServantInfo servantInfo() {
        return new ServantInfo() {
            @Override
            public String getName() {
                return "pers-grade-test";
            }

            @Override
            public String getVersion() {
                return "none";
            }

            @Override
            public String getHostName() {
                return "guess";
            }
        };
    }
}
