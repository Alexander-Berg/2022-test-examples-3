package ru.yandex.market.logistics.management.repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerCountry;
import ru.yandex.market.logistics.management.domain.entity.PartnerCountry.PartnerCountryId;
import ru.yandex.market.logistics.management.domain.entity.RegionEntity;
import ru.yandex.market.logistics.management.exception.EntityNotFoundException;
import ru.yandex.market.logistics.management.exception.RegionTypeIsNotCountryException;
import ru.yandex.market.logistics.management.repository.geoBase.GeoBaseRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DatabaseSetup("/data/repository/partnerCountry/before/partner_country_setup.xml")
@DisplayName("Работа с таблицей связок партнёров со странами, куда они доставляют")
class PartnerCountryRepositoryTest extends AbstractContextualAspectValidationTest {

    private static final int RUSSIA_ID = 100;
    private static final int KAZAKHSTAN_ID = 101;
    private static final int UZBEKISTAN_ID = 102;
    private static final int ARMENIA_ID = 103;
    private static final int BELARUS_ID = 104;
    private static final int FRANCE_ID = 105;
    private static final int GERMANY_ID = 106;
    private static final int MOSCOW_ID = 102;
    private static final long PARTNER_1_ID = 100;
    private static final long PARTNER_2_ID = 101;
    private static final long PARTNER_3_ID = 102;

    @Autowired
    private PartnerCountryRepository partnerCountryRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private GeoBaseRepository geoBaseRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Успешное получение существующей связки партнёра со страной")
    void testFindByIdExists() {
        Partner partner = partnerRepository.findById(PARTNER_1_ID).orElseThrow();
        RegionEntity russia = geoBaseRepository.findById(RUSSIA_ID).orElseThrow();

        PartnerCountry partnerCountry =
            partnerCountryRepository.findById(new PartnerCountry(partner, russia).getId())
                .orElse(null);

        assertEqualsByPartnerAndCountry(partnerCountry, partner, russia);
    }

    @Test
    @DisplayName("Получение несуществующей связки партнёра со страной")
    void testFindByIdDoesNotExists() {
        Optional<PartnerCountry> partnerCountry =
            partnerCountryRepository.findById(new PartnerCountryId(PARTNER_1_ID, KAZAKHSTAN_ID));

        assertThat(partnerCountry).isNotPresent();
    }

    @Test
    @ExpectedDatabase(
        value = "/data/repository/partnerCountry/after/partner_country_save.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное сохранение связки партнёра со страной")
    void testSaveWithCountry() {
        Partner partner = partnerRepository.findById(PARTNER_2_ID).orElseThrow();
        RegionEntity russia = geoBaseRepository.findById(RUSSIA_ID).orElseThrow();
        RegionEntity kazakhstan = geoBaseRepository.findById(KAZAKHSTAN_ID).orElseThrow();

        PartnerCountry partnerCountry = partnerCountryRepository.save(new PartnerCountry(partner, russia));
        assertEqualsByPartnerAndCountry(partnerCountry, partner, russia);

        partnerCountry = partnerCountryRepository.save(new PartnerCountry(partner, kazakhstan));
        assertEqualsByPartnerAndCountry(partnerCountry, partner, kazakhstan);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/repository/partnerCountry/before/partner_country_setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Сохранение города вместо страны в связке партнёра со страной - ошибка")
    void testSaveWithCity() {
        Partner partner = partnerRepository.findById(PARTNER_1_ID).orElseThrow();
        RegionEntity moscow = geoBaseRepository.findById(MOSCOW_ID).orElseThrow();

        assertThatThrownBy(() -> partnerCountryRepository.save(new PartnerCountry(partner, moscow)))
            .isInstanceOf(RegionTypeIsNotCountryException.class)
            .hasMessage("Region type CITY(code:6,desc:город) is not a country");
    }

    @Test
    @ExpectedDatabase(
        value = "/data/repository/partnerCountry/before/partner_country_setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Сохранение города вместо страны в связке партнёра со страной напрямую в БД - ошибка")
    void testNativeInsert() {
        assertThatThrownBy(() -> jdbcTemplate.execute(
            String.format("INSERT INTO partner_country values (%d, %d)", PARTNER_1_ID, MOSCOW_ID)
        ))
            .hasRootCauseInstanceOf(PSQLException.class)
            .hasMessageContaining(
                "ERROR: region with id = 102 is not a country (country type = 3, but selected region type = 6)"
            );
    }

    @ParameterizedTest(name = "Ищем все связки со странами для партнёра c id = {0}")
    @MethodSource
    @DatabaseSetup("/data/repository/partnerCountry/before/partner_country_find_all.xml")
    @DisplayName("Поиск всех связок со странами для партнёра")
    void testFindAllByPartnerId(long partnerId, List<Integer> expectedCountriesIds) {
        List<Integer> actualCountryIds = partnerCountryRepository.findAllByPartnerId(partnerId)
            .stream()
            .map(PartnerCountry::getId)
            .map(PartnerCountryId::getCountryId)
            .collect(Collectors.toList());

        assertThat(actualCountryIds).containsExactlyInAnyOrderElementsOf(expectedCountriesIds);
    }

    @Test
    @DisplayName("Успешное получение существующей связки партнёра со страной методом findByIdOrThrow")
    void testFindByIdOrThrowExists() {
        PartnerCountryId partnerCountryId = new PartnerCountryId(100L, 100);

        PartnerCountry partnerCountry = partnerCountryRepository.findByIdOrThrow(partnerCountryId);

        assertThat(partnerCountry.getId())
            .as("Asserting that the partner country id is valid")
            .isEqualTo(partnerCountryId);
    }

    @Test
    @DisplayName("Получение несуществующей связки партнёра со страной методом findByIdOrThrow - ошибка")
    void testFindByIdOrThrowNotExists() {
        PartnerCountryId partnerCountryId = new PartnerCountryId(99L, 100);

        assertThatThrownBy(() -> partnerCountryRepository.findByIdOrThrow(partnerCountryId))
            .as("Asserting that the valid exception is thrown")
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage(
                "404 NOT_FOUND \"Can't find PartnerCountry "
                    + "with id=PartnerCountry.PartnerCountryId(partnerId=99, countryId=100)\""
            );
    }

    @Nonnull
    @SuppressWarnings("unused")
    private static Stream<Arguments> testFindAllByPartnerId() {
        return Stream.of(
            Arguments.of(
                PARTNER_1_ID,
                List.of(RUSSIA_ID, KAZAKHSTAN_ID, UZBEKISTAN_ID, ARMENIA_ID, BELARUS_ID)
            ),
            Arguments.of(
                PARTNER_2_ID,
                List.of(UZBEKISTAN_ID, ARMENIA_ID, BELARUS_ID, FRANCE_ID, GERMANY_ID)
            ),
            Arguments.of(
                PARTNER_3_ID,
                List.of()
            )
        );
    }

    private void assertEqualsByPartnerAndCountry(PartnerCountry partnerCountry, Partner partner, RegionEntity country) {
        assertThat(partnerCountry).isEqualTo(new PartnerCountry(partner, country));
    }
}
