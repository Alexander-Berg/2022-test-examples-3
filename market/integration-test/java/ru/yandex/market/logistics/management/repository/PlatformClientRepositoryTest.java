package ru.yandex.market.logistics.management.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PlatformClient;
import ru.yandex.market.logistics.management.domain.entity.PlatformClientPartner;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
class PlatformClientRepositoryTest extends AbstractContextualTest {

    @Autowired
    private PlatformClientPartnerRepository repository;

    @Test
    @Sql("/data/repository/partner/partners.sql")
    @Sql("/data/repository/platform-client/platform_clients.sql")
    @Sql("/data/repository/platform-client/platform_clients_partners.sql")
    @Transactional
    void testReadPlatformClientsPartners() {
        PlatformClientPartner clientPartner = repository.findByIdOrThrow(1L);

        PlatformClient platformClient = clientPartner.getPlatformClient();
        softly.assertThat(platformClient)
            .as("Platform client doesn't exist").isNotNull();
        softly.assertThat(platformClient.getName())
            .as("Platform client does not have correct name").isEqualTo("Bringly");

        Partner partner = clientPartner.getPartner();
        softly.assertThat(partner).as("Partner should exist").isNotNull();
        softly.assertThat(partner.getName()).as("Partner should have correct name").isEqualTo("delivery");
    }
}
