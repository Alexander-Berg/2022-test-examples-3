package ru.yandex.market.admin;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.core.database.PreserveDictionariesDbUnitDataSet;
import ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest;

@SpringJUnitConfig(classes = FunctionalTestConfig.class)
@ActiveProfiles("functionalTest")
@PreserveDictionariesDbUnitDataSet
public abstract class FunctionalTest extends JupiterDbUnitTest {
    @Autowired
    protected PartnerNotificationClient partnerNotificationClient;

    @BeforeEach
    void commonSetUp() {
        PartnerNotificationApiServiceTest.setUpClient(partnerNotificationClient);
    }
}
