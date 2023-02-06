package ru.yandex.market.logistics.logistics4shops.admin;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.admin.enums.AdminPartnerType;
import ru.yandex.market.logistics.logistics4shops.admin.model.AdminPartnerMappingFilterDto;
import ru.yandex.market.logistics.logistics4shops.utils.RestAssuredFactory;

import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/admin/partnerMapping/prepare.xml")
@DisplayName("Сответствия lms и mbi партнеров в админке")
@ParametersAreNonnullByDefault
class AdminPartnerMappingTest extends AbstractIntegrationTest {
    private static final String URL = "/admin/partner-mapping";

    @ValueSource(strings = {"mbiPartner", "lmsPartner"})
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Поиск маппингов по пустым коллекциям")
    void searchEmptyCollections(String collectionName) {
        RestAssuredFactory.assertGetSuccess(URL, "admin/partnerMapping/response/empty.json", collectionName, "");
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Поиск маппингов")
    void search(
        @SuppressWarnings("unused") String displayName,
        AdminPartnerMappingFilterDto filter,
        String expectedResponseJsonPath
    ) {
        RestAssuredFactory.assertGetSuccess(URL, expectedResponseJsonPath, toParams(filter));
    }

    @Nonnull
    private static Stream<Arguments> search() {
        return Stream.of(
            Arguments.of(
                "По идентификаторам mbi-партнеров",
                new AdminPartnerMappingFilterDto().setMbiPartner(List.of(400L, 1000L)),
                "admin/partnerMapping/response/3_5.json"
            ),
            Arguments.of(
                "По идентификаторам lms-партнеров",
                new AdminPartnerMappingFilterDto().setLmsPartner(List.of(200L, 600L)),
                "admin/partnerMapping/response/1_3_4.json"
            ),
            Arguments.of(
                "По типу",
                new AdminPartnerMappingFilterDto().setType(AdminPartnerType.DROPSHIP),
                "admin/partnerMapping/response/1_3.json"
            ),
            Arguments.of(
                "По дате создания от",
                new AdminPartnerMappingFilterDto().setCreatedFrom(LocalDate.of(2022, 1, 2)),
                "admin/partnerMapping/response/1_3_4_5.json"
            ),
            Arguments.of(
                "По дате создания до",
                new AdminPartnerMappingFilterDto().setCreatedTo(LocalDate.of(2022, 1, 2)),
                "admin/partnerMapping/response/1_2.json"
            ),
            Arguments.of(
                "По всем полям",
                new AdminPartnerMappingFilterDto()
                    .setMbiPartner(List.of(100L))
                    .setLmsPartner(List.of(200L))
                    .setType(AdminPartnerType.DROPSHIP)
                    .setCreatedTo(LocalDate.of(2022, 1, 2))
                    .setCreatedFrom(LocalDate.of(2022, 1, 2)),
                "admin/partnerMapping/response/1.json"
            ),
            Arguments.of(
                "По пустому фильтру",
                new AdminPartnerMappingFilterDto(),
                "admin/partnerMapping/response/all.json"
            )
        );
    }
}
