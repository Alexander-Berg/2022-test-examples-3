package ru.yandex.mail.cerberus.yt.mapper;

import io.micronaut.test.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.mail.cerberus.LocationId;
import ru.yandex.mail.cerberus.client.dto.Location;
import ru.yandex.mail.cerberus.yt.data.YtOfficeInfo;
import ru.yandex.mail.cerberus.yt.staff.StaffConstants;
import ru.yandex.mail.cerberus.yt.staff.dto.Meta;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffLocalizedString;
import ru.yandex.mail.cerberus.yt.staff.dto.StaffOffice;

import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
public class DefaultYtOfficeMapperTest {
    @Inject
    private YtOfficeMapper mapper;

    private static final LocationId LOCATION_ID = new LocationId(42L);
    private static final StaffLocalizedString NAME = new StaffLocalizedString("имя", "name");
    private static final String CODE = "code";
    private static final StaffLocalizedString CITY_NAME = new StaffLocalizedString("город", "city");
    private static final ZoneId TIMEZONE = ZoneId.of("Europe/Moscow");
    private static final Meta META = new Meta(OffsetDateTime.MAX);
    private static final StaffOffice OFFICE = new StaffOffice(
        META,
        LOCATION_ID,
        NAME,
        CODE,
        new StaffOffice.City(CITY_NAME),
        TIMEZONE,
        false
    );

    @Test
    @DisplayName("Verify that 'mapToLocation' returns expected location object")
    void testMapToLocation() {
        val expectedLocation = new Location<YtOfficeInfo>(
            LOCATION_ID,
            StaffConstants.YT_OFFICE_TYPE,
            "name",
            Optional.of(new YtOfficeInfo(
                NAME,
                CODE,
                CITY_NAME,
                TIMEZONE,
                false
            ))
        );
        val location = mapper.mapToLocation(OFFICE);
        assertThat(location)
            .isEqualTo(expectedLocation);
    }
}
