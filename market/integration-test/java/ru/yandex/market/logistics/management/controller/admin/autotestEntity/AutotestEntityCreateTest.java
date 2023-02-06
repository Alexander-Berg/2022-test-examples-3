package ru.yandex.market.logistics.management.controller.admin.autotestEntity;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.domain.dto.front.autotestEntity.AutotestEntityNewDto;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.pojoToString;

@DatabaseSetup("/data/controller/admin/autotestEntity/before/setup.xml")
public class AutotestEntityCreateTest extends AbstractContextualAspectValidationTest {

    @Test
    @DisplayName("Создать автотестовую сущность, будучи неавторизованным")
    void createAutotestEntityIsUnauthorized() throws Exception {
        createAutotestEntity(defaultCreateDto()).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Создать автотестовую сущность, не имея прав")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void createAutotestEntityIsForbidden() throws Exception {
        createAutotestEntity(defaultCreateDto()).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Создать автотестовую сущность, имея права только на чтение")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOCATION_ZONE})
    void createAutotestEntityReadOnly() throws Exception {
        createAutotestEntity(defaultCreateDto()).andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("invalidRequestProvider")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOCATION_ZONE_EDIT})
    void validateDto(AutotestEntityNewDto createDto, String message) throws Exception {
        createAutotestEntity(createDto)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].codes[0]").value(message));
    }

    static Stream<Arguments> invalidRequestProvider() {
        return Stream.of(
            Pair.of(defaultCreateDto().setPath(""), "NotBlank.autotestEntityNewDto.path"),
            Pair.of(defaultCreateDto().setPath(null), "NotBlank.autotestEntityNewDto.path")
        )
            .map(pair -> Arguments.of(pair.getFirst(), pair.getSecond()));
    }

    @Test
    @DisplayName("Создать автотестовую сущность")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_AUTOTEST_ENTITY_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/autotestEntity/after/create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createAutotestEntitySuccess() throws Exception {
        createAutotestEntity(defaultCreateDto())
            .andExpect(status().isCreated())
            .andExpect(header().string("location", "http://localhost/admin/lms/autotest-entity/3"));
    }

    @Nonnull
    private static AutotestEntityNewDto defaultCreateDto() {
        return new AutotestEntityNewDto().setPath("lms/partner/3");
    }

    @Nonnull
    private ResultActions createAutotestEntity(AutotestEntityNewDto createDto) throws Exception {
        return mockMvc.perform(post("/admin/lms/autotest-entity")
            .contentType(MediaType.APPLICATION_JSON)
            .content(pojoToString(createDto))
        );
    }
}
