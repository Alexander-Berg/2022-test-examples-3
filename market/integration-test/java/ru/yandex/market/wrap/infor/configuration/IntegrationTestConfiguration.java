package ru.yandex.market.wrap.infor.configuration;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.market.fulfillment.wrap.core.api.RequestType;
import ru.yandex.market.fulfillment.wrap.core.configuration.CommonControllersConfiguration;
import ru.yandex.market.fulfillment.wrap.core.configuration.RequestProcessingConfiguration;
import ru.yandex.market.fulfillment.wrap.core.configuration.SecurityConfiguration;
import ru.yandex.market.fulfillment.wrap.core.configuration.TokenContextHolderConfiguration;
import ru.yandex.market.fulfillment.wrap.core.configuration.xml.XmlMappingConfiguration;
import ru.yandex.market.fulfillment.wrap.core.processing.validation.TokenValidator;
import ru.yandex.market.logistic.api.model.common.request.Token;
import ru.yandex.market.logistics.iris.client.api.TrustworthyInfoClient;
import ru.yandex.market.wrap.infor.configuration.iris.IrisClientConfiguration;
import ru.yandex.market.wrap.infor.configuration.property.LoadTestingProperties;
import ru.yandex.market.wrap.infor.configuration.property.MutableWarehousesProperties;
import ru.yandex.market.wrap.infor.configuration.property.WarehousePropertiesConfiguration;
import ru.yandex.market.wrap.infor.service.solomon.SolomonPushService;
import ru.yandex.market.wrap.infor.service.yt.PageableYtWriter;
import ru.yandex.passport.tvmauth.TvmClient;

import java.util.Arrays;

@Import({
    MutableWarehousesProperties.class,
    WarehousePropertiesConfiguration.class,
    XmlMappingConfiguration.class,
    CommonControllersConfiguration.class,
    InforClientConfiguration.class,
    InforTestClientProperties.class,
    PingCheckersConfiguration.class,
    SecurityConfiguration.class,
    RequestProcessingConfiguration.class,
    TokenContextHolderConfiguration.class,
    WmsDataSourceTypeContextHolderConfiguration.class,
    IntegrationTestDataSourcesConfiguration.class,
    IntegrationTestDbUnitConfiguration.class,
    // keep liquibase configuration bean after datasource configuration bean to execute DB populators before liquibase
    IntegrationTestLiquibaseConfiguration.class,
    DateTimeTestConfiguration.class,
    IdentifierMappingConfiguration.class,
    LoadTestingProperties.class,
    LgwClientConfiguration.class,
    IrisClientConfiguration.class
})
@Configuration
@EnableAutoConfiguration(exclude = {
    JpaRepositoriesAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    QuartzAutoConfiguration.class,
})
@ComponentScan({
    "ru.yandex.market.wrap.infor.fulfillment",
    "ru.yandex.market.wrap.infor.repository",
    "ru.yandex.market.wrap.infor.service",
    "ru.yandex.market.wrap.infor.controller"
})
@MockBean({
    PageableYtWriter.class,
    YtTransactions.class,
    SolomonPushService.class,
    TvmClient.class,
    TrustworthyInfoClient.class
})
public class IntegrationTestConfiguration {
    @Bean
    public TokenValidator tokenValidator() {
        return new TokenValidator(this::getMap);
    }

    private Multimap<Token, RequestType> getMap() {
        Multimap<Token, RequestType> multimap = ArrayListMultimap.create();
        multimap.putAll(new Token("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"),
            Arrays.asList(RequestType.values()));
        multimap.put(new Token("xxxxxxxxxxxxxxxxxxxxxxxxxTestTokenxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"),
            RequestType.CREATE_ORDER);
        multimap.put(new Token("xxxxxxxxxxxxxxxxxxxxxxxxxTestTokenxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"),
            RequestType.CANCEL_INBOUND);
        multimap.put(new Token("xxxxxxxxxxxxxxxxxxxxxxxxxTestToken2xxxxxxxxxxxxxxxxxxxxxxxxxxxxx"),
            RequestType.CREATE_ORDER);
        multimap.put(new Token("xxxxxxxxxxxxxxxxxxxxxxxxxTestToken2xxxxxxxxxxxxxxxxxxxxxxxxxxxxx"),
            RequestType.CANCEL_INBOUND);
        multimap.putAll(new Token("xxxxxxxxxxxxxxxxxxxxxxxxxTestToken3xxxxxxxxxxxxxxxxxxxxxxxxxxxxx"),
            Arrays.asList(RequestType.values()));
        multimap.put(new Token("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxunknownxxxxxxxxxxxxxxxxxxxxxx"),
            RequestType.CREATE_ORDER);

        return multimap;
    }
}
