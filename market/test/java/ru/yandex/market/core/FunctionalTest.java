package ru.yandex.market.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.market.common.test.guava.ForgetfulSuppliersInitializer;
import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.core.database.PreserveDictionariesDbUnitDataSet;
import ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest;
import ru.yandex.market.core.tanker.dao.CachedTankerDaoImpl;
import ru.yandex.market.core.test.context.AllowSpiesInitializer;

/**
 * @author fbokovikov
 */
@ActiveProfiles("functionalTest")
@SpringJUnitConfig(
        locations = "classpath:/ru/yandex/market/core/config/functional-test-config.xml",
        initializers = {AllowSpiesInitializer.class, ForgetfulSuppliersInitializer.class}
)
@PreserveDictionariesDbUnitDataSet
public abstract class FunctionalTest extends JupiterDbUnitTest {
    @Autowired
    private CachedTankerDaoImpl tankerDao;

    @Autowired
    protected PartnerNotificationClient partnerNotificationClient;

    @BeforeEach
    void commonSetUp() {
        PartnerNotificationApiServiceTest.setUpClient(partnerNotificationClient);
    }

    @AfterEach
    void commonTearDown() {
        tankerDao.cleanUpCache();
    }

}
