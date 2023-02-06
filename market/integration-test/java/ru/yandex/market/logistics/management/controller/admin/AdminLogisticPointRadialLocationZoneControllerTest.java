package ru.yandex.market.logistics.management.controller.admin;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.filter.admin.AdminRadialLocationZoneFilter;
import ru.yandex.market.logistics.management.domain.dto.front.ActionDto;
import ru.yandex.market.logistics.management.domain.dto.front.logisticPointRadialZone.CreateLogisticPointRadialLocationZoneDto;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.service.client.PartnerService;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@WithBlackBoxUser(
    login = "lmsUser",
    uid = 1,
    authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_RADIAL_ZONE
)
@DisplayName("Привязка и отвязка радиальных зон к складу через админку")
@DatabaseSetup("/data/controller/admin/logisticPointRadialZone/before.xml")
class AdminLogisticPointRadialLocationZoneControllerTest extends AbstractContextualTest {
    private static final Set<PartnerStatus> VALID_FOR_DEFAULT_ADDING_PARTNER_STATUSES = Set.of(
        ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus.ACTIVE,
        ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus.TESTING
    );

    private static final String METHOD_URL = "/admin/lms/logistics-point-radial-zone";

    @Autowired
    private PartnerService partnerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("Найти радиальные зоны, связанные с логистической точкой")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource
    void getLogisticsPointRadialZoneGrid(
        @SuppressWarnings("unused") String caseName,
        AdminRadialLocationZoneFilter filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get(METHOD_URL).params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(responsePath));
    }

    @DisplayName("Найти радиальные зоны по поисковому запросу для склада")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource
    void getLogisticsPointRadialZoneForPoint(
        @SuppressWarnings("unused") String caseName,
        AdminRadialLocationZoneFilter filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get(METHOD_URL + "/options").params(toParams(filter)).param("parentId", "1"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(responsePath));
    }

    @Test
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_RADIAL_ZONE_EDIT
    )
    @DisplayName("Получить карточку для создания связи радиальной зоны и склада")
    void getCreateLogisticPointRadialZoneDto() throws Exception {
        mockMvc.perform(get(METHOD_URL + "/new").param("parentId", "1"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/logisticPointRadialZone/response/create_dto.json"));
    }

    @Test
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_RADIAL_ZONE_EDIT
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticPointRadialZone/after/after_delete.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Удалить связи радиальных зон и склада")
    void deleteLogisticPointRadialZone() throws Exception {
        mockMvc.perform(
                post(METHOD_URL + "/delete")
                    .param("parentId", "3")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new ActionDto().setIds(Set.of(6L, 7L))))
            )
            .andExpect(status().isOk())
            .andExpect(TestUtil.noContent());
    }

    @Test
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_RADIAL_ZONE_EDIT
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticPointRadialZone/after/after_create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Создать связь радиальной зоны и склада")
    void createLogisticPointRadialZone() throws Exception {
        performCreateLogisticsPointRadialZone(1L, 1L)
            .andExpect(status().isCreated())
            .andExpect(redirectedUrl("http://localhost/admin/lms/logistics-point/1"))
            .andExpect(TestUtil.noContent());
    }

    @Test
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_RADIAL_ZONE_EDIT
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticPointRadialZone/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Ошибка создания связи радиальной зоны и склада - склад не найден")
    void createLogisticPointRadialZoneFailNoPoint() throws Exception {
        performCreateLogisticsPointRadialZone(10101L, 1L)
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Cannot find point with id 10101"));
    }

    @Test
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_RADIAL_ZONE_EDIT
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticPointRadialZone/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Создать связь радиальной зоны и склада - связь уже существует")
    void createLogisticPointRadialZoneFail() throws Exception {
        performCreateLogisticsPointRadialZone(2L, 4L)
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Link between point 2 and zone 4 already exist"))
            .andExpect(TestUtil.noContent());
    }

    @Test
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_RADIAL_ZONE_EDIT
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticPointRadialZone/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Создать связь радиальной зоны и склада - разные регионы")
    void createLogisticPointRadialZoneFailRegion() throws Exception {
        performCreateLogisticsPointRadialZone(2L, 3L)
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(
                "Cannot create link between point 2 (region 2) and zone 3 (region 225) - different regions."
            ))
            .andExpect(TestUtil.noContent());
    }

    @Test
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_RADIAL_ZONE_EDIT
    )
    @DisplayName("Получить карточку связи радиальной зоны и склада")
    void getLogisticPointRadialZoneDto() throws Exception {
        mockMvc.perform(get(METHOD_URL + "/3").param("parentId", "3"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/logisticPointRadialZone/response/detail_dto.json"));
    }

    @Nonnull
    private static Stream<Arguments> getLogisticsPointRadialZoneGrid() {
        return Stream.of(
            Arguments.of(
                "Без фильтрации",
                AdminRadialLocationZoneFilter.newBuilder().logisticsPoint(3L).build(),
                "data/controller/admin/logisticPointRadialZone/response/search/no_filter.json"
            ),
            Arguments.of(
                "По названию",
                AdminRadialLocationZoneFilter.newBuilder().logisticsPoint(3L).name("мал").build(),
                "data/controller/admin/logisticPointRadialZone/response/search/name.json"
            ),
            Arguments.of(
                "По радиусу",
                AdminRadialLocationZoneFilter.newBuilder().logisticsPoint(3L).radius(99900L).build(),
                "data/controller/admin/logisticPointRadialZone/response/search/radius.json"
            ),
            Arguments.of(
                "По времени доставки в зону",
                AdminRadialLocationZoneFilter.newBuilder().logisticsPoint(3L).deliveryDuration(190L).build(),
                "data/controller/admin/logisticPointRadialZone/response/search/delivery_duration.json"
            ),
            Arguments.of(
                "По флагу приватности",
                AdminRadialLocationZoneFilter.newBuilder().logisticsPoint(3L).isPrivate(true).build(),
                "data/controller/admin/logisticPointRadialZone/response/search/is_private.json"
            ),
            Arguments.of(
                "По всем параметрам",
                AdminRadialLocationZoneFilter.newBuilder()
                    .logisticsPoint(3L)
                    .name("сред")
                    .radius(99900L)
                    .deliveryDuration(180L)
                    .build(),
                "data/controller/admin/logisticPointRadialZone/response/search/all.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> getLogisticsPointRadialZoneForPoint() {
        return Stream.of(
            Arguments.of(
                "По идентификатору",
                AdminRadialLocationZoneFilter.newBuilder().searchQuery("1").build(),
                "data/controller/admin/logisticPointRadialZone/response/getOptions/query_id.json"
            ),
            Arguments.of(
                "По имени",
                AdminRadialLocationZoneFilter.newBuilder().searchQuery("вторая").build(),
                "data/controller/admin/logisticPointRadialZone/response/getOptions/query_name.json"
            )
        );
    }

    @DisplayName("Успешно добавить все дефолтные зоны региона складу")
    @ParameterizedTest
    @MethodSource
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_RADIAL_ZONE_EDIT
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticPointRadialZone/after/after_add_default.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addDefaultZonesToPoint(PartnerStatus status) throws Exception {
        partnerService.changePartnerStatus(1L, status);
        performAddDefaultLogisticPointRadialZones("1")
            .andExpect(status().isOk())
            .andExpect(TestUtil.noContent());
    }

    @Nonnull
    private static Stream<Arguments> addDefaultZonesToPoint() {
        return VALID_FOR_DEFAULT_ADDING_PARTNER_STATUSES.stream().sorted().map(Arguments::of);
    }

    @DisplayName("Неверный статус партнера логистической точки при добавление дефолтных зон")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_RADIAL_ZONE_EDIT
    )
    void addDefaultZonesToPointStatuses(PartnerStatus partnerStatus) throws Exception {
        partnerService.changePartnerStatus(1L, partnerStatus);
        performAddDefaultLogisticPointRadialZones("1")
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Logistic point 1 should be point of active or testing partner"))
            .andExpect(TestUtil.noContent());
    }

    @Nonnull
    private static Stream<Arguments> addDefaultZonesToPointStatuses() {
        return Stream.of(PartnerStatus.values())
            .filter(status -> !VALID_FOR_DEFAULT_ADDING_PARTNER_STATUSES.contains(status))
            .map(Arguments::of);
    }

    @Test
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_RADIAL_ZONE_EDIT
    )
    @DisplayName("При добавлении дефолтных зон не используются зоны родительского региона")
    @DatabaseSetup(
        value = "/data/controller/admin/logisticPointRadialZone/before_remove_moscow_zones.xml",
        type = DatabaseOperation.DELETE
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticPointRadialZone/after/after_not_changed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addDefaultZonesToPointParent() throws Exception {
        performAddDefaultLogisticPointRadialZones("1")
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("No radial zones found for region 213"))
            .andExpect(TestUtil.noContent());
    }

    @DisplayName("Ошибка при добавлении дефолтных зон")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_RADIAL_ZONE_EDIT
    )
    void addDefaultZonesToPointErrors(
        String name,
        String pointId,
        ResultMatcher statusMatcher,
        String reason
    ) throws Exception {
        performAddDefaultLogisticPointRadialZones(pointId)
            .andExpect(statusMatcher)
            .andExpect(status().reason(reason))
            .andExpect(TestUtil.noContent());
    }

    @Nonnull
    private static Stream<Arguments> addDefaultZonesToPointErrors() {
        return Stream.of(
            Arguments.of(
                "Точка не найдена",
                "6",
                status().isNotFound(),
                "Can't find logistics point with id=6"
            ),
            Arguments.of(
                "Точка не относится к экспресс партнеру",
                "4",
                status().isBadRequest(),
                "Logistic point 4 should be point of dropship express partner"
            ),
            Arguments.of(
                "Точка не активна",
                "5",
                status().isBadRequest(),
                "Logistics point 5 should be active"
            )
        );
    }

    @Nonnull
    private ResultActions performCreateLogisticsPointRadialZone(Long pointId, Long zoneId) throws Exception {
        return mockMvc.perform(
            post(METHOD_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .params(TestUtil.toMultiValueMap(
                    Map.of("parentSlug", "logistics-point", "parentId", pointId.toString(), "idFieldName", "id")
                ))
                .content(objectMapper.writeValueAsString(
                    new CreateLogisticPointRadialLocationZoneDto()
                        .setLogisticPointId(pointId)
                        .setZoneId(zoneId)
                ))
        );
    }

    @Nonnull
    private ResultActions performAddDefaultLogisticPointRadialZones(String pointId) throws Exception {
        return mockMvc.perform(
            post(METHOD_URL + "/add-default")
                .contentType(MediaType.APPLICATION_JSON)
                .params(TestUtil.toMultiValueMap(
                    Map.of("parentSlug", "logistics-point", "parentId", pointId, "idFieldName", "id")
                ))
                .content(objectMapper.writeValueAsString(
                    new ActionDto().setIds(Set.of(1L, 2L, 3L))
                )) //с фронта передается, но не используется в ручке
        );
    }
}
