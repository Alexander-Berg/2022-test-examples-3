package ru.yandex.market.api.partner.context;

import java.util.TimeZone;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.api.partner.view.JacksonMapperConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.guava.ForgetfulSuppliersInitializer;
import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.core.database.PreserveDictionariesDbUnitDataSet;
import ru.yandex.market.core.test.context.TestPropertiesInitializer;

/**
 * Корневой класс, поднимает контекст ПАПИ.
 * НАСЛЕДОВАТЬСЯ ОТ НЕГО ДЛЯ ВСЕХ ФУНКЦИОНАЛЬНЫХ ТЕСТОВ ПАПИ!!!
 * <p>
 * в {@link ru/yandex/market/api/partner/context/TestShop.csv} описан базовый магазин с контактами
 * и линками для проходения всех чекеров в ПАПИ.
 */
@PreserveDictionariesDbUnitDataSet
@DbUnitDataSet(before = "classpath:/ru/yandex/market/api/partner/context/TestShop.csv")
@ImportResource("classpath:WEB-INF/checkouter-client.xml")
@Import(JacksonMapperConfig.class)
@ActiveProfiles({"functionalTest", "rate-limits-ignored"})
@SpringJUnitConfig(
        locations = "classpath:/ru/yandex/market/api/partner/context/functional-test-config.xml",
        initializers = {TestPropertiesInitializer.class, ForgetfulSuppliersInitializer.class}
)
public abstract class FunctionalTest extends JupiterDbUnitTest {

    protected String urlBasePrefix;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    private ObjectMapper checkouterAnnotationObjectMapper;

    @PostConstruct
    public void postConstruct() {
        // Клиент чекаутера десериализует дату-время в Московской временной зоне, наши тесты во Владивостоке падают
        checkouterAnnotationObjectMapper.setTimeZone(TimeZone.getDefault());
    }

    @BeforeEach
    public void init() {
        Server jettyServer = (Server) applicationContext.getBean("server");
        int port = ((NetworkConnector) jettyServer.getConnectors()[0]).getLocalPort();
        String host = ((NetworkConnector) jettyServer.getConnectors()[0]).getHost();
        urlBasePrefix = "http://" + host + ":" + port;
    }
}
