package ru.yandex.market.logistics.management.controller.admin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.LinkedMultiValueMap;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.PlatformClientPartner;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.exception.EntityNotFoundException;
import ru.yandex.market.logistics.management.repository.PlatformClientPartnerRepository;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PLATFORM_CLIENT_PARTNERS;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PLATFORM_CLIENT_PARTNERS_EDIT;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@CleanDatabase
@Sql("/data/controller/admin/platformClient/prepare_data.sql")
class LmsControllerPlatformClientPartnerTest extends AbstractContextualTest {

    @Autowired
    private PlatformClientPartnerRepository platformClientPartnerRepository;

    @Test
    void platformClientPartnerGridUnauthorized() throws Exception {
        getPlatformClientPartnerGrid()
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void platformClientPartnerGridForbidden() throws Exception {
        getPlatformClientPartnerGrid()
            .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @MethodSource("platformClientPartnerGridReadonlyQueryParams")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PLATFORM_CLIENT_PARTNERS)
    void platformClientPartnerGridReadonly(
        Long platformClientId,
        Long partnerId,
        String responseFilepath
    ) throws Exception {
        getPlatformClientPartnerGrid(platformClientId, partnerId)
            .andExpect(status().isOk())
            .andExpect(testJson(responseFilepath, false));
    }

    @Nonnull
    private static Stream<Arguments> platformClientPartnerGridReadonlyQueryParams() {
        return Stream.of(
            Triple.of(null, null, "data/controller/admin/platformClient/platform_client_partner_grid_readonly.json"),
            Triple.of(null, 1L, "data/controller/admin/platformClient/platform_client_partner_grid_readonly_31.json"),
            Triple.of(3L, null, "data/controller/admin/platformClient/platform_client_partner_grid_readonly_31.json"),
            Triple.of(3L, 1L, "data/controller/admin/platformClient/platform_client_partner_grid_readonly_31.json")
        )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }

    @ParameterizedTest
    @MethodSource("platformClientPartnerGridEditableQueryParams")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {
        AUTHORITY_ROLE_PLATFORM_CLIENT_PARTNERS,
        AUTHORITY_ROLE_PLATFORM_CLIENT_PARTNERS_EDIT,
    })
    void platformClientPartnerGridEditable(
        Long platformClientId,
        Long partnerId,
        String responseFilepath
    ) throws Exception {
        getPlatformClientPartnerGrid(platformClientId, partnerId)
            .andExpect(status().isOk())
            .andExpect(testJson(responseFilepath, false));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {
            AUTHORITY_ROLE_PLATFORM_CLIENT_PARTNERS,
            AUTHORITY_ROLE_PLATFORM_CLIENT_PARTNERS_EDIT,
    })
    void platformClientPartnerGridPagerTest() throws Exception {
        mockMvc.perform(get("/admin/lms/platform-client-partner?sort=id&size=1"))
                .andExpect(status().isOk());
    }

    @Nonnull
    private static Stream<Arguments> platformClientPartnerGridEditableQueryParams() {
        return Stream.of(
            Triple.of(null, null, "data/controller/admin/platformClient/platform_client_partner_grid_editable.json"),
            Triple.of(null, 1L, "data/controller/admin/platformClient/platform_client_partner_grid_editable_31.json"),
            Triple.of(3L, null, "data/controller/admin/platformClient/platform_client_partner_grid_editable_31.json"),
            Triple.of(3L, 1L, "data/controller/admin/platformClient/platform_client_partner_grid_editable_31.json")
        )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }

    @Test
    void platformClientPartnerDetailUnauthorized() throws Exception {
        getPlatformClientPartnerDetail()
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void platformClientPartnerDetailForbidden() throws Exception {
        getPlatformClientPartnerDetail()
            .andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PLATFORM_CLIENT_PARTNERS)
    void platformClientPartnerDetailReadonly() throws Exception {
        getPlatformClientPartnerDetail()
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/platformClient/platform_client_partner_detail_readonly.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {
        AUTHORITY_ROLE_PLATFORM_CLIENT_PARTNERS,
        AUTHORITY_ROLE_PLATFORM_CLIENT_PARTNERS_EDIT,
    })
    void platformClientPartnerDetailDeletable() throws Exception {
        getPlatformClientPartnerDetail()
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/platformClient/platform_client_partner_detail_deletable.json",
                false
            ));
    }

    @Test
    void platformClientPartnerNewUnauthorized() throws Exception {
        getPlatformClientPartnerNew()
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PLATFORM_CLIENT_PARTNERS)
    void platformClientPartnerNewForbidden() throws Exception {
        getPlatformClientPartnerNew()
            .andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PLATFORM_CLIENT_PARTNERS_EDIT)
    void platformClientPartnerNew() throws Exception {
        getPlatformClientPartnerNew()
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/platformClient/platform_client_partner_create_form.json",
                false
            ));
    }

    @Test
    void platformClientPartnerCreateUnauthorized() throws Exception {
        createPlatformClientPartner("platform_client_partner_create_new")
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PLATFORM_CLIENT_PARTNERS)
    void platformClientPartnerCreateForbidden() throws Exception {
        createPlatformClientPartner("platform_client_partner_create_new")
            .andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PLATFORM_CLIENT_PARTNERS_EDIT)
    void platformClientPartnerCreateExisting() throws Exception {
        createPlatformClientPartner("platform_client_partner_create_existing")
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PLATFORM_CLIENT_PARTNERS_EDIT)
    void platformClientPartnerCreateNew() throws Exception {
        createPlatformClientPartner("platform_client_partner_create_new")
            .andExpect(status().isCreated())
            .andExpect(header().string("location", "http://localhost/admin/lms/platform-client-partner/3"));
    }

    @Test
    void platformClientPartnerEditUnauthorized() throws Exception {
        editPlatformClientPartner("platform_client_partner_create_edit", 1L)
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PLATFORM_CLIENT_PARTNERS)
    void platformClientPartnerEditForbidden() throws Exception {
        editPlatformClientPartner("platform_client_partner_create_edit", 1L)
            .andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PLATFORM_CLIENT_PARTNERS_EDIT)
    void platformClientPartnerEditNotExisting() throws Exception {
        editPlatformClientPartner("platform_client_partner_create_edit", 42L)
            .andExpect(status().isNotFound());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PLATFORM_CLIENT_PARTNERS_EDIT)
    void platformClientPartnerEditValidation() throws Exception {
        editPlatformClientPartner("platform_client_partner_create_edit_validation", 1L)
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PLATFORM_CLIENT_PARTNERS_EDIT)
    void platformClientPartnerEdit() throws Exception {
        editPlatformClientPartner("platform_client_partner_create_edit", 1L)
            .andExpect(status().isOk());

        PlatformClientPartner platformClientPartner = platformClientPartnerRepository.findById(1L)
            .orElseThrow(EntityNotFoundException::new);

        softly.assertThat(platformClientPartner.getStatus()).as("Status must be frozen")
            .isEqualTo(PartnerStatus.INACTIVE);
    }

    @Nonnull
    private ResultActions getPlatformClientPartnerGrid() throws Exception {
        return mockMvc.perform(get("/admin/lms/platform-client-partner"));
    }

    @Nonnull
    private ResultActions getPlatformClientPartnerGrid(Long platformClientId, Long partnerId) throws Exception {
        MockHttpServletRequestBuilder builder = get("/admin/lms/platform-client-partner?sort=id");
        Map<String, List<String>> queryParams = Stream.of(
            Optional.ofNullable(platformClientId)
                .map(id -> Pair.of("platformClient", ImmutableList.of(String.valueOf(id)))),
            Optional.ofNullable(partnerId).map(id -> Pair.of("partner", ImmutableList.of(String.valueOf(id))))
        )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        return mockMvc.perform(builder.params(new LinkedMultiValueMap<>(queryParams)));
    }

    @Nonnull
    private ResultActions getPlatformClientPartnerDetail() throws Exception {
        return mockMvc.perform(get("/admin/lms/platform-client-partner/1"));
    }

    @Nonnull
    private ResultActions getPlatformClientPartnerNew() throws Exception {
        return mockMvc.perform(get("/admin/lms/platform-client-partner/new"));
    }

    @Nonnull
    private ResultActions createPlatformClientPartner(String file) throws Exception {
        return mockMvc.perform(
            post("/admin/lms/platform-client-partner")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/platformClient/" + file + ".json"))
        );
    }

    @Nonnull
    private ResultActions editPlatformClientPartner(String requestPath, long platformClientPartnerId) throws Exception {
        return mockMvc.perform(
            put("/admin/lms/platform-client-partner/" + platformClientPartnerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/platformClient/" + requestPath + ".json"))
        );
    }
}
