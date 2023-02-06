package ru.yandex.market.logistics.tarifficator.controller.shop;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
public class RegionGroupControllerTest extends AbstractContextualTest {

    @Test
    @DisplayName("Успешное получение региональных групп магазина")
    @DatabaseSetup("/controller/shop/region-group/db/getRegionGroups.before.xml")
    void getShopRegionGroups_regionGroupsExist() throws Exception {
        preformRegionGroupsSearch(774L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop/region-group/json/getAllRegionGroups.json"));
    }

    @Test
    @DisplayName("Получение региональных групп для ненастроившего доставку магазина")
    @DatabaseSetup("/controller/shop/region-group/db/getRegionGroups.before.xml")
    void getShopRegionGroups_regionGroupsEmpty() throws Exception {
        preformRegionGroupsSearch(775L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop/region-group/json/noRegionGroups.json"));
    }

    @Test
    @DisplayName("Получение региональных групп, у одной из которых нет статуса")
    @DatabaseSetup("/controller/shop/region-group/db/getRegionGroup.withoutStatus.before.xml")
    void getShopRegionGroups_regionGroupHasNoStatus() throws Exception {
        preformRegionGroupsSearch(774L)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Region group with id 200 has no status"));
    }

    @Test
    @DisplayName("Сохранение региональной группы (положительный сценарий)")
    @DatabaseSetup("/controller/shop/region-group/db/createRegionGroup.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/region-group/db/createRegionGroup.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createGroup() throws Exception {
        performRegionGroupCreation(774L, "controller/shop/region-group/json/createRegionGroup.valid.request.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop/region-group/json/createRegionGroup.response.json"));
    }

    @Test
    @DisplayName("Сохранение региональной группы с пустыми регионами")
    @DatabaseSetup("/controller/shop/region-group/db/createRegionGroup.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/region-group/db/createRegionGroup.withEmptyRegions.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createGroupWithEmptyRegions() throws Exception {
        performRegionGroupCreation(
            774L,
            "controller/shop/region-group/json/createRegionGroup.emptyRegions.request.json"
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop/region-group/json/createRegionGroup.response.json"));
    }

    @Test
    @DisplayName("Сохранение региональной группы с отсуствующими в запросе (null) регионами")
    @DatabaseSetup("/controller/shop/region-group/db/createRegionGroup.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/region-group/db/createRegionGroup.withEmptyRegions.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createGroupWithNullRegions() throws Exception {
        performRegionGroupCreation(774L, "controller/shop/region-group/json/createRegionGroup.nullRegions.request.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop/region-group/json/createRegionGroup.response.json"));
    }

    @Test
    @DisplayName("Сохранение собственной региональной группы с типами оплаты (DBS-партнёр)")
    @DatabaseSetup("/controller/shop/region-group/db/createLocalRegionGroup.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/region-group/db/createLocalRegionGroup.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createLocalRegionGroup() throws Exception {
        performRegionGroupCreation(774L, "controller/shop/region-group/json/createLocalRegionGroup.valid.request.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop/region-group/json/createRegionGroup.response.json"));
    }

    @Test
    @DisplayName("Сохранение не локальной региональной группы с копированием типов оплат из собственной (DBS-партнёр)")
    @DatabaseSetup("/controller/shop/region-group/db/createNonLocalRegionGroup.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/region-group/db/createNonLocalRegionGroup.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createNonLocalRegionGroup() throws Exception {
        performRegionGroupCreation(
            774L,
            "controller/shop/region-group/json/createNonLocalRegionGroup.valid.request.json"
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop/region-group/json/createRegionGroup.response.json"));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @DisplayName("Тестирование невалидных запросов")
    @MethodSource("testBadRequestForCreateGroup")
    @DatabaseSetup("/controller/shop/region-group/db/createRegionGroup.validation.before.xml")
    void createGroupWithValidationError(
        @SuppressWarnings("unused") String testName,
        long shopId,
        String requestPath,
        String errorMessage
    ) throws Exception {
        performRegionGroupCreation(shopId, requestPath)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(errorMessage));
    }

    @Test
    @DisplayName("Обновление региональной группы (положительный сценарий)")
    @DatabaseSetup("/controller/shop/region-group/db/updateRegionGroup.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/region-group/db/updateRegionGroup.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testUpdateGroup() throws Exception {
        performRegionGroupUpdate(774L, 1000L, "controller/shop/region-group/json/updateRegionGroup.valid.request.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop/region-group/json/updateRegionGroup.valid.response.json"));
    }

    @Test
    @DisplayName("Обновление региональной группы, пустые регионы (положительный сценарий)")
    @DatabaseSetup("/controller/shop/region-group/db/updateRegionGroup.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/region-group/db/updateRegionGroup.emptyRegions.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testUpdateGroupEmptyRegions() throws Exception {
        performRegionGroupUpdate(774L, 1000L, "controller/shop/region-group/json/updateRegionGroupNullRegions.json")
            .andExpect(status().isOk())
            .andExpect(
                jsonContent("controller/shop/region-group/json/updateRegionGroup.valid.response.json")
            );
    }

    @Test
    @DisplayName("Обновление региональной группы (группа не найдена)")
    @DatabaseSetup("/controller/shop/region-group/db/updateRegionGroup.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/region-group/db/updateRegionGroup.before.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testUpdateNotExistingGroup() throws Exception {
        performRegionGroupUpdate(774L, 2000L, "controller/shop/region-group/json/updateRegionGroup.valid.request.json")
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP_REGION_GROUP] with ids [[2000]]"));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @DisplayName("Тестирование невалидных запросов на обновление региональной группы")
    @MethodSource("testBadRequestForUpdateGroup")
    @DatabaseSetup("/controller/shop/region-group/db/createRegionGroup.validation.before.xml")
    void testUpdateGroupWithValidationError(
        @SuppressWarnings("unused") String testName,
        long shopId,
        String requestPath,
        String errorMessage
    ) throws Exception {
        performRegionGroupUpdate(shopId, 2000L, requestPath)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(errorMessage));
    }

    @Test
    @DisplayName("Удаление региональной группы (успешное)")
    @DatabaseSetup("/controller/shop/region-group/db/deleteRegionGroup.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/region-group/db/deleteRegionGroup.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testDeleteRegionGroup() throws Exception {
        performRegionGroupDelete(774L, 300L)
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Удаление региональной группы (неуспех, удаление собственной группы)")
    @DatabaseSetup("/controller/shop/region-group/db/deleteRegionGroup.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/region-group/db/deleteRegionGroup.before.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testDeleteRegionGroup_selfRegion() throws Exception {
        performRegionGroupDelete(774L, 200L)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Can't delete region group for local delivery region"));
    }

    @Test
    @DisplayName("Тест батчового удаления региональных групп")
    @DatabaseSetup("/controller/shop/region-group/db/deleteSeveralRegionGroups.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/region-group/db/deleteSeveralRegionGroups.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testDeleteSeveralRegionGroups() throws Exception {
        performBatchRegionGroupsDelete(774L, Set.of(300L, 400L, 500L, 600L))
            .andExpect(status().isOk());
    }

    @Nonnull
    static Stream<Arguments> testBadRequestForCreateGroup() {
        return Stream.of(
            of(
                "Нет названия группы",
                774L,
                "controller/shop/region-group/json/createRegionGroup.invalid.request.json",
                "Following validation errors occurred:\n"
                    + "Field: 'groupName', message: 'must not be null'"
            ),
            of(
                "Пытаемся создать вторую группу в локальном регионе",
                774L,
                "controller/shop/region-group/json/createRegionGroup.invalid.selfRegion.request.json",
                "Trying to create 2 local delivery region groups"
            ),
            of(
                "Пытаемся создать 21 группу",
                776L,
                "controller/shop/region-group/json/createRegionGroup.valid.request.json",
                "Only 20 region groups can be created for shop"
            ),
            of(
                "Пытаемся добавить невалидный регион",
                774L,
                "controller/shop/region-group/json/createRegionGroup.invalidRegion.request.json",
                "Invalid regions: [12312132121312]"
            )
        );
    }

    @Nonnull
    static Stream<Arguments> testBadRequestForUpdateGroup() {
        return Stream.of(
            of(
                "Нет названия группы",
                774L,
                "controller/shop/region-group/json/createRegionGroup.invalid.request.json",
                "Following validation errors occurred:\n"
                    + "Field: 'groupName', message: 'must not be null'"
            ),
            of(
                "Пытаемся добавить невалидный регион",
                774L,
                "controller/shop/region-group/json/createRegionGroup.invalidRegion.request.json",
                "Invalid regions: [12312132121312]"
            )
        );
    }

    @Nonnull
    private ResultActions performRegionGroupCreation(long shopId, String requestFilePath) throws Exception {
        return mockMvc.perform(
            post("/v2/shops/" + shopId + "/region-groups?_user_id=100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestFilePath))
        );
    }

    @Nonnull
    private ResultActions performRegionGroupUpdate(
        long shopId,
        long regionGroupId,
        String requestFilePath
    ) throws Exception {
        return mockMvc.perform(
            post("/v2/shops/" + shopId + "/region-groups/" + regionGroupId + "?_user_id=100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestFilePath))
        );
    }

    @Nonnull
    private ResultActions performRegionGroupDelete(long shopId, long regionGroupId) throws Exception {
        return mockMvc.perform(
            delete("/v2/shops/" + shopId + "/region-groups/" + regionGroupId + "?_user_id=100")
                .contentType(MediaType.APPLICATION_JSON)
        );
    }

    @Nonnull
    private ResultActions performBatchRegionGroupsDelete(long shopId, Set<Long> groupsToBeDeleted) throws Exception {
        StringBuilder sb = new StringBuilder("/v2/shops/" + shopId + "/region-groups/?_user_id=100");

        groupsToBeDeleted.forEach(
            id -> sb.append("&regionId=").append(id)
        );

        return mockMvc.perform(delete(sb.toString()).contentType(MediaType.APPLICATION_JSON));
    }

    @Nonnull
    private ResultActions preformRegionGroupsSearch(long shopId) throws Exception {
        return mockMvc.perform(
            get("/v2/shops/" + shopId + "/region-groups?")
                .contentType(MediaType.APPLICATION_JSON)
        );
    }
}
