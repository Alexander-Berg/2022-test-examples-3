package ru.yandex.market.logistics.management.repository;

import java.util.Comparator;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.PartnerSettingsMethodSyncData;

@SuppressWarnings("checkstyle:MagicNumber")
public class SettingsMethodSyncJdbcRepositoryTest extends AbstractContextualTest {

    @Autowired
    SettingsMethodSyncJdbcRepository repository;

    @Test
    @DatabaseSetup(
        value = "/data/repository/settings/partner_with_all_settings_and_methods.xml",
        connection = "dbUnitQualifiedDatabaseConnection"
    )
    void findAllFailedSettingsMethodSyncAndPartnerInfoTest() {
        List<PartnerSettingsMethodSyncData> data = repository.findAllFailedSettingsMethodSyncAndPartnerInfo();
        data.sort(Comparator.comparing(PartnerSettingsMethodSyncData::getPartnerId));
        var first = data.get(0);
        var second = data.get(1);

        softly.assertThat(data.size()).isEqualTo(2);

        softly.assertThat(first.getPartnerId()).isEqualTo(3L);
        softly.assertThat(first.getPartnerStatus()).isEqualTo("testing");
        softly.assertThat(first.getPartnerType()).isEqualTo("DELIVERY");
        softly.assertThat(first.getMethodId()).isEqualTo(6L);
        softly.assertThat(first.getMessage()).isEqualTo("500 Server Error");

        softly.assertThat(second.getPartnerId()).isEqualTo(6L);
        softly.assertThat(second.getPartnerStatus()).isEqualTo("active");
        softly.assertThat(second.getPartnerType()).isEqualTo("OWN_DELIVERY");
        softly.assertThat(second.getMethodId()).isEqualTo(2L);
        softly.assertThat(second.getMessage()).isEqualTo("I/O error on POST request for 'http://test'");
    }
}
