package ru.yandex.market.mbo.cms.config.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.mbo.cms.core.dao.SchemaClientDao;

@Configuration
@Profile("testing")
@Import({
        CmsDbConfig.class,
})
public class TestingCoreConfig {

    private final CmsDbConfig cmsDbConfig;

    public TestingCoreConfig(CmsDbConfig cmsDbConfig) {
        this.cmsDbConfig = cmsDbConfig;
    }

    @Bean
    public SchemaClientDao schemaClientDao() {
        return new SchemaClientDao(cmsDbConfig.namedPgJdbcTemplate(), false);
    }

}
