package ru.yandex.market.partner.content.common.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.partner.content.common.csku.judge.Judge;

/**
 * @author s-ermakov
 */
@Configuration
@Import({
    TestDataBaseConfiguration.class,
    ManagerConfig.class,
    ServicesConfig.class
})
public class CommonTestConfig {

    // мокаем бины для MDS, так настоящие требуют непустых явок/паролей к MDS в момент инициализации
    @Primary
    @Bean
    public MdsS3Client mdsS3Client() {
        return Mockito.mock(MdsS3Client.class);
    }

    @Primary
    @Bean
    public ResourceLocationFactory resourceLocationFactory() {
        return Mockito.mock(ResourceLocationFactory.class);
    }

    @Bean
    public JdbcTemplate yqlJdbcTemplate() {
        return Mockito.mock(JdbcTemplate.class);
    }

    @Bean
    public Judge judge() {return new Judge();}
}
