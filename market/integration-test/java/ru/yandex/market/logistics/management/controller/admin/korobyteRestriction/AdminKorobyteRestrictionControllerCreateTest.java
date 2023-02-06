package ru.yandex.market.logistics.management.controller.admin.korobyteRestriction;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.domain.dto.front.korobyteRestriction.KorobyteRestrictionNewDto;
import ru.yandex.market.logistics.management.domain.dto.front.korobyteRestriction.KorobyteRestrictionNewDto.KorobyteRestrictionNewDtoBuilder;
import ru.yandex.market.logistics.management.domain.entity.validation.ValidKorobyteRestriction;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.READ_ONLY;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.create;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.defaultNewDto;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.emptyNewDto;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.newDto;

@DatabaseSetup("/data/controller/admin/korobyteRestrictions/before/empty.xml")
class AdminKorobyteRestrictionControllerCreateTest extends AbstractContextualAspectValidationTest {

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_ONLY)
    void shouldGetForbidden_whenHasReadOnlyAuthority() throws Exception {
        performCreate(newDto())
            .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("createArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    @ExpectedDatabase(
        value = "/data/controller/admin/korobyteRestrictions/after/create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void shouldCreate(
        @SuppressWarnings("unused") String displayName,
        KorobyteRestrictionNewDtoBuilder newDto
    ) throws Exception {
        performCreate(newDto)
            .andExpect(status().isCreated())
            .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/admin/lms/korobyte-restrictions/1")));
    }

    static Stream<Arguments> createArguments() {
        return Stream.of(
            Arguments.of("все ограничения", defaultNewDto()),
            Arguments.of("без ограничений", emptyNewDto()),
            Arguments.of("все минимальные размеры", emptyNewDto().minLengthCm(0).minWidthCm(0).minHeightCm(0)),
            Arguments.of("все максимальные размеры", emptyNewDto().maxLengthCm(0).maxWidthCm(0).maxHeightCm(0)),
            Arguments.of(
                "все минимальные и максимальные размеры",
                emptyNewDto().minLengthCm(0).maxLengthCm(0).minWidthCm(0).maxWidthCm(0).minHeightCm(0).maxHeightCm(0)
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("validationArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    void shouldFailOnValidation(
        KorobyteRestrictionNewDtoBuilder newDto,
        String fieldName,
        String errMsg
    ) throws Exception {
        performCreate(newDto)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors.length()").value(1))
            .andExpect(jsonPath("errors[0].field").value(fieldName))
            .andExpect(jsonPath("errors[0].defaultMessage").value(errMsg));
    }

    static Stream<Arguments> validationArguments() {
        return Stream.of(
            Arguments.of(defaultNewDto().key(null), "key", "Обязательно для заполнения"),
            Arguments.of(defaultNewDto().key(""), "key", "Обязательно для заполнения"),
            Arguments.of(defaultNewDto().key("   "), "key", "Обязательно для заполнения"),
            Arguments.of(defaultNewDto().minWeightG(-1), "minWeightG", "Значение должно быть не меньше нуля"),
            Arguments.of(defaultNewDto().maxWeightG(-1), "maxWeightG", "Значение должно быть не меньше нуля"),
            Arguments.of(defaultNewDto().minLengthCm(-1), "minLengthCm", "Значение должно быть не меньше нуля"),
            Arguments.of(defaultNewDto().maxLengthCm(-1), "maxLengthCm", "Значение должно быть не меньше нуля"),
            Arguments.of(defaultNewDto().minWidthCm(-1), "minWidthCm", "Значение должно быть не меньше нуля"),
            Arguments.of(defaultNewDto().maxWidthCm(-1), "maxWidthCm", "Значение должно быть не меньше нуля"),
            Arguments.of(defaultNewDto().minHeightCm(-1), "minHeightCm", "Значение должно быть не меньше нуля"),
            Arguments.of(defaultNewDto().maxHeightCm(-1), "maxHeightCm", "Значение должно быть не меньше нуля"),
            Arguments.of(defaultNewDto().minSidesSumCm(-1), "minSidesSumCm", "Значение должно быть не меньше нуля"),
            Arguments.of(defaultNewDto().maxSidesSumCm(-1), "maxSidesSumCm", "Значение должно быть не меньше нуля")
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("korobyteRestrictionValidationArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    void shouldGetConflict_whenFailOnKorobyteRestrictionValidation(
        @SuppressWarnings("unused") String displayName,
        KorobyteRestrictionNewDtoBuilder newDto
    ) throws Exception {
        performCreate(newDto)
            .andExpect(status().isConflict())
            .andExpect(status().reason(ValidKorobyteRestriction.ERR_MSG));
    }

    static Stream<Arguments> korobyteRestrictionValidationArguments() {
        return Stream.of(
            Arguments.of("один минимальный размер", emptyNewDto().minLengthCm(0)),
            Arguments.of("один минимальный размер", emptyNewDto().minWidthCm(0)),
            Arguments.of("один минимальный размер", emptyNewDto().minHeightCm(0)),
            Arguments.of("два минимальных размера", emptyNewDto().minLengthCm(0).minWidthCm(0)),
            Arguments.of("два минимальных размера", emptyNewDto().minLengthCm(0).minHeightCm(0)),
            Arguments.of("два минимальных размера", emptyNewDto().minWidthCm(0).minHeightCm(0)),
            Arguments.of("один максимальный размер", emptyNewDto().maxLengthCm(0)),
            Arguments.of("один максимальный размер", emptyNewDto().maxWidthCm(0)),
            Arguments.of("один максимальный размер", emptyNewDto().maxHeightCm(0)),
            Arguments.of("два максимальных размера", emptyNewDto().maxLengthCm(0).maxWidthCm(0)),
            Arguments.of("два максимальных размера", emptyNewDto().maxLengthCm(0).maxHeightCm(0)),
            Arguments.of("два максимальных размера", emptyNewDto().maxWidthCm(0).maxHeightCm(0)),
            Arguments.of("минимум больше максимума", emptyNewDto().minLengthCm(1).maxLengthCm(0)),
            Arguments.of("минимум больше максимума", emptyNewDto().minWidthCm(1).maxWidthCm(0)),
            Arguments.of("минимум больше максимума", emptyNewDto().minHeightCm(1).maxHeightCm(0))
        );
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    void shouldGetConflict_whenTryToCreateWithTheSameKey() throws Exception {
        var newDto = newDto();
        performCreate(newDto)
            .andExpect(status().isCreated())
            .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/admin/lms/korobyte-restrictions/1")));
        performCreate(newDto)
            .andExpect(status().isConflict())
            .andExpect(status().reason("Korobyte restriction already exists for key CREATE_TEST"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    void shouldGetBadRequest_whenIncorrectSidesRestrictions() throws Exception {
        performCreate(defaultNewDto().minLengthCm(null))
            .andExpect(status().isConflict())
            .andExpect(status().reason(ValidKorobyteRestriction.ERR_MSG));
    }

    @Nonnull
    private ResultActions performCreate(KorobyteRestrictionNewDto newDto) throws Exception {
        return mockMvc.perform(create(newDto));
    }

    @Nonnull
    private ResultActions performCreate(KorobyteRestrictionNewDtoBuilder newDto) throws Exception {
        return performCreate(newDto.build());
    }
}
