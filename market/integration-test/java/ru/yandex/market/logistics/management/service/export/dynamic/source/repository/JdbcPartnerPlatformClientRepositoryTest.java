package ru.yandex.market.logistics.management.service.export.dynamic.source.repository;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
@Sql("/data/repository/jdbc/partner-platform-client.sql")
class JdbcPartnerPlatformClientRepositoryTest extends AbstractContextualTest {
    public static final long PARTNER_ID_1 = 123L;
    public static final long PARTNER_ID_2 = 124L;

    @Autowired
    private JdbcPartnerPlatformClientRepository jdbcPartnerPlatformClientRepository;

    @Test
    public void testGetStatusByPartnerIdMap() {
        softly
            .assertThat(jdbcPartnerPlatformClientRepository.getStatusByPartnerIdMap(Constants.BERU))
            .isEqualTo(Map.of(
                PARTNER_ID_1, PartnerStatus.ACTIVE,
                PARTNER_ID_2, PartnerStatus.INACTIVE
            ));

        softly
            .assertThat(jdbcPartnerPlatformClientRepository.getStatusByPartnerIdMap(Constants.BRINGLY))
            .isEqualTo(Map.of(
                PARTNER_ID_1, PartnerStatus.FROZEN,
                PARTNER_ID_2, PartnerStatus.TESTING
            ));

    }
}
