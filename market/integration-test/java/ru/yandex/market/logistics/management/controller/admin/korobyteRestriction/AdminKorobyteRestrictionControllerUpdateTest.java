package ru.yandex.market.logistics.management.controller.admin.korobyteRestriction;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.domain.dto.front.korobyteRestriction.KorobyteRestrictionUpdateDto;
import ru.yandex.market.logistics.management.domain.dto.front.korobyteRestriction.KorobyteRestrictionUpdateDto.KorobyteRestrictionUpdateDtoBuilder;
import ru.yandex.market.logistics.management.domain.entity.validation.ValidKorobyteRestriction;
import ru.yandex.market.logistics.management.service.graph.LogisticServiceEntityService;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.KOROBYTE_RESTRICTION_ID_1;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.KOROBYTE_RESTRICTION_ID_2;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.KOROBYTE_RESTRICTION_ID_3;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.READ_ONLY;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.defaultUpdateDto;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.emptyUpdateDto;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.update;
import static ru.yandex.market.logistics.management.controller.admin.korobyteRestriction.AdminKorobyteRestrictionControllerTestHelper.updateDto;

@DatabaseSetup("/data/controller/admin/korobyteRestrictions/before/setup.xml")
class AdminKorobyteRestrictionControllerUpdateTest extends AbstractContextualAspectValidationTest {

    @Autowired
    LogisticServiceEntityService logisticServiceEntityService;

    @AfterEach
    void teardown() {
        verifyNoMoreInteractions(logisticServiceEntityService);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_ONLY)
    void shouldGetForbidden_whenHasReadOnlyAuthority() throws Exception {
        performUpdate(KOROBYTE_RESTRICTION_ID_2, updateDto())
            .andExpect(status().isForbidden());
        verifyNoInteractions(logisticServiceEntityService);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    @ExpectedDatabase(
        value = "/data/controller/admin/korobyteRestrictions/after/update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void shouldUpdate() throws Exception {
        performUpdate(KOROBYTE_RESTRICTION_ID_3, updateDto())
            .andExpect(status().isOk());
        verify(logisticServiceEntityService).touchByKorobyteRestrictionId(KOROBYTE_RESTRICTION_ID_3);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("validationArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    void shouldFailOnValidation(
        KorobyteRestrictionUpdateDtoBuilder updateDto,
        String fieldName,
        String errMsg
    ) throws Exception {
        performUpdate(KOROBYTE_RESTRICTION_ID_1, updateDto)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors.length()").value(1))
            .andExpect(jsonPath("errors[0].field").value(fieldName))
            .andExpect(jsonPath("errors[0].defaultMessage").value(errMsg));
        verifyNoInteractions(logisticServiceEntityService);
    }

    static Stream<Arguments> validationArguments() {
        return Stream.of(
            Arguments.of(defaultUpdateDto().key(null), "key", "Обязательно для заполнения"),
            Arguments.of(defaultUpdateDto().key(""), "key", "Обязательно для заполнения"),
            Arguments.of(defaultUpdateDto().key("   "), "key", "Обязательно для заполнения"),
            Arguments.of(defaultUpdateDto().minWeightG(-1), "minWeightG", "Значение должно быть не меньше нуля"),
            Arguments.of(defaultUpdateDto().maxWeightG(-1), "maxWeightG", "Значение должно быть не меньше нуля"),
            Arguments.of(defaultUpdateDto().minLengthCm(-1), "minLengthCm", "Значение должно быть не меньше нуля"),
            Arguments.of(defaultUpdateDto().maxLengthCm(-1), "maxLengthCm", "Значение должно быть не меньше нуля"),
            Arguments.of(defaultUpdateDto().minWidthCm(-1), "minWidthCm", "Значение должно быть не меньше нуля"),
            Arguments.of(defaultUpdateDto().maxWidthCm(-1), "maxWidthCm", "Значение должно быть не меньше нуля"),
            Arguments.of(defaultUpdateDto().minHeightCm(-1), "minHeightCm", "Значение должно быть не меньше нуля"),
            Arguments.of(defaultUpdateDto().maxHeightCm(-1), "maxHeightCm", "Значение должно быть не меньше нуля"),
            Arguments.of(defaultUpdateDto().minSidesSumCm(-1), "minSidesSumCm", "Значение должно быть не меньше нуля"),
            Arguments.of(defaultUpdateDto().maxSidesSumCm(-1), "maxSidesSumCm", "Значение должно быть не меньше нуля")
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("korobyteRestrictionValidationArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    void shouldGetConflict_whenFailOnKorobyteRestrictionValidation(
        @SuppressWarnings("unused") String displayName,
        KorobyteRestrictionUpdateDtoBuilder updateDto
    ) throws Exception {
        performUpdate(KOROBYTE_RESTRICTION_ID_2, updateDto)
            .andExpect(status().isConflict())
            .andExpect(status().reason(ValidKorobyteRestriction.ERR_MSG));
        verifyNoInteractions(logisticServiceEntityService);
    }

    static Stream<Arguments> korobyteRestrictionValidationArguments() {
        return Stream.of(
            Arguments.of("один минимальный размер", emptyUpdateDto().minLengthCm(0)),
            Arguments.of("один минимальный размер", emptyUpdateDto().minWidthCm(0)),
            Arguments.of("один минимальный размер", emptyUpdateDto().minHeightCm(0)),
            Arguments.of("два минимальных размера", emptyUpdateDto().minLengthCm(0).minWidthCm(0)),
            Arguments.of("два минимальных размера", emptyUpdateDto().minLengthCm(0).minHeightCm(0)),
            Arguments.of("два минимальных размера", emptyUpdateDto().minWidthCm(0).minHeightCm(0)),
            Arguments.of("один максимальный размер", emptyUpdateDto().maxLengthCm(0)),
            Arguments.of("один максимальный размер", emptyUpdateDto().maxWidthCm(0)),
            Arguments.of("один максимальный размер", emptyUpdateDto().maxHeightCm(0)),
            Arguments.of("два максимальных размера", emptyUpdateDto().maxLengthCm(0).maxWidthCm(0)),
            Arguments.of("два максимальных размера", emptyUpdateDto().maxLengthCm(0).maxHeightCm(0)),
            Arguments.of("два максимальных размера", emptyUpdateDto().maxWidthCm(0).maxHeightCm(0)),
            Arguments.of("минимум больше максимума", emptyUpdateDto().minLengthCm(1).maxLengthCm(0)),
            Arguments.of("минимум больше максимума", emptyUpdateDto().minWidthCm(1).maxWidthCm(0)),
            Arguments.of("минимум больше максимума", emptyUpdateDto().minHeightCm(1).maxHeightCm(0))
        );
    }

    @Nonnull
    private ResultActions performUpdate(
        long korobyteRestrictionId,
        KorobyteRestrictionUpdateDto updateDto
    ) throws Exception {
        return mockMvc.perform(update(korobyteRestrictionId, updateDto));
    }

    @Nonnull
    private ResultActions performUpdate(
        long korobyteRestrictionId,
        KorobyteRestrictionUpdateDtoBuilder updateDto
    ) throws Exception {
        return performUpdate(korobyteRestrictionId, updateDto.build());
    }
}
