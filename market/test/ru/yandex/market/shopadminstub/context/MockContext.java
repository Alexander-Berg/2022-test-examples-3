package ru.yandex.market.shopadminstub.context;

import java.util.Arrays;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.google.common.collect.ImmutableMap;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeperMain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.Resource;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.checkout.checkouter.mocks.MockMvcFactory;
import ru.yandex.market.checkout.common.TestHelper;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.common.util.ZooPropertiesSetter;
import ru.yandex.market.checkout.storage.ZooScriptExecutor;
import ru.yandex.market.checkout.util.report.CommonReportResponseGenerator;
import ru.yandex.market.checkout.util.report.generators.geo.GeoFoundOutletInsertGenerator;
import ru.yandex.market.checkout.util.report.generators.geo.GeoGeneratorParameters;
import ru.yandex.market.checkout.util.report.generators.geo.GeoPlaceStubGenerator;
import ru.yandex.market.checkout.util.report.generators.offerinfo.OfferInfoFoundOfferInsertGenerator;
import ru.yandex.market.checkout.util.report.generators.offerinfo.OfferInfoGeneratorParameters;
import ru.yandex.market.checkout.util.report.generators.offerinfo.OfferInfoPlaceStubGenerator;
import ru.yandex.market.common.report.model.CurrencyConvertRequestUrlBuilder;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.helpers.HelpersRoot;
import ru.yandex.market.shopadminstub.Main;
import ru.yandex.market.util.UtilRoot;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
@ComponentScan(
        basePackages = {"ru.yandex.market.shopadminstub"},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = Main.class)
        }
)
@ComponentScan(basePackageClasses = {UtilRoot.class, HelpersRoot.class}, includeFilters = {
        @ComponentScan.Filter(TestHelper.class),
        @ComponentScan.Filter(WebTestHelper.class)
})
public class MockContext {

    @Bean(initMethod = "start")
    public WireMockServer feedDispatcherMock() {
        return newWireMockServer();
    }

    @Bean(initMethod = "start")
    public WireMockServer reportMock() {
        return newWireMockServer();
    }

    @Bean(initMethod = "start")
    public WireMockServer svnMock() {
        return newWireMockServer();
    }

    @Bean(initMethod = "start")
    public WireMockServer aquaMock() {
        return newWireMockServer();
    }

    private WireMockServer newWireMockServer() {
        return new WireMockServer(new WireMockConfiguration().dynamicPort().notifier(new ConsoleNotifier(true)));
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    @DependsOn("zooPropertiesSetter")
    public TestingServer testingZooKeeperServer() throws Exception {
        return new TestingServer(false);
    }

    @Bean
    public ZooPropertiesSetter zooPropertiesSetter() {
        return new ZooPropertiesSetter();
    }

    @Bean
    @Autowired
    @DependsOn("testingZooKeeperServer")
    public ZooScriptExecutor zooScriptExecutor(
            CuratorFramework curatorFramework,
            @Value("classpath:files/zookeeper-init.zk") Resource resource
    ) throws Exception {
        ZooKeeper zooKeeper = curatorFramework.getZookeeperClient().getZooKeeper();
        ZooKeeperMain zooKeeperMain = new ZooKeeperMain(zooKeeper);
        return new ZooScriptExecutor(zooKeeperMain, resource);
    }

    @Bean
    public MockMvcFactory mockMvcFactory() {
        return new MockMvcFactory();
    }

    @Bean
    public MockMvc mockMvc(MockMvcFactory mockMvcFactory) {
        return mockMvcFactory.getMockMvc();
    }

    @Bean
    public CommonReportResponseGenerator<OfferInfoGeneratorParameters> commonReportResponseGenerator() {
        CommonReportResponseGenerator<OfferInfoGeneratorParameters> commonReportResponseGenerator =
                new CommonReportResponseGenerator<>();
        commonReportResponseGenerator.setGeneratorsMap(ImmutableMap.of(
                MarketReportPlace.OFFER_INFO, Arrays.asList(
                        new OfferInfoPlaceStubGenerator<>(),
                        new OfferInfoFoundOfferInsertGenerator()
                )
        ));
        return commonReportResponseGenerator;
    }

    @Bean
    public CommonReportResponseGenerator<GeoGeneratorParameters> geoReportResponseGenerator() {
        CommonReportResponseGenerator<GeoGeneratorParameters> geoReportResponseGenerator =
                new CommonReportResponseGenerator<>();
        geoReportResponseGenerator.setGeneratorsMap(ImmutableMap.of(
                MarketReportPlace.GEO, Arrays.asList(
                        new GeoPlaceStubGenerator<>(),
                        new GeoFoundOutletInsertGenerator()
                )
        ));
        return geoReportResponseGenerator;
    }

    @Bean
    public CurrencyConvertRequestUrlBuilder currencyConvertRequestUrlBuilder(@Value("${market.red.search.url}") String marketSearchUrl) {
        return new CurrencyConvertRequestUrlBuilder(marketSearchUrl);
    }
}
