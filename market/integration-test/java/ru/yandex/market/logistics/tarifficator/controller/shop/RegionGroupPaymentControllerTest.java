package ru.yandex.market.logistics.tarifficator.controller.shop;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.exception.http.ResourceType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Интеграционный тест для контроллера DeliveryPaymentController")
@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@DatabaseSetup("/controller/shop/delivery-payment/db/before/delivery_region_group_payments.xml")
class RegionGroupPaymentControllerTest extends AbstractContextualTest {

    private static final String URL_TEMPLATE = "/v2/shops/{shopId}/region-groups/{regionGroupId}/payment-types";

    private static final int SHOP_ID = 774;
    private static final int SHOP_ID_DBS = 775;

    private static final int ILLEGAL_SHOP_ID = 79620;

    private static final int EXISTED_REGION_GROUP_ID = 101;
    private static final int EXISTED_REGION_GROUP_ID_DBS = 111;
    private static final int EXISTED_NON_SELF_REGION_GROUP_ID_DBS = 112;

    private static final int NON_EXISTENT_REGION_GROUP_ID = 1001;

    private static final int USER_ID = 221;

    @Test
    @DisplayName("Можно получить региональные типы оплат")
    void getPaymentTypes() throws Exception {
        mockMvc.perform(
            get(URL_TEMPLATE, SHOP_ID, EXISTED_REGION_GROUP_ID)
                .param("_user_id", String.valueOf(USER_ID))
        )
            .andExpect(status().isOk())
            .andExpect(content().json(extractFileContent(
                "controller/shop/delivery-payment/response/get_payment_types.json"
            ))
        );
    }

    @Test
    @DisplayName("Нельзя получить региональные типы оплат для несуществующей группы")
    void getPaymentTypesForNonExistentGroup() throws Exception {
        mockMvc.perform(
            get(URL_TEMPLATE, SHOP_ID, NON_EXISTENT_REGION_GROUP_ID)
                .param("_user_id", String.valueOf(USER_ID))
        )
            .andExpect(status().isNotFound())
            .andExpect(
                errorMessage(
                    String.format(
                        "Failed to find [%s] with ids [%s]",
                        ResourceType.SHOP_REGION_GROUP,
                        List.of(NON_EXISTENT_REGION_GROUP_ID)
                    )
                )
            );
    }

    @Test
    @DisplayName("Нельзя получить региональные типы оплат для группы другого магазина")
    void getPaymentTypesForGroupBelongingToOtherShop() throws Exception {
        mockMvc.perform(
            get(URL_TEMPLATE, ILLEGAL_SHOP_ID, EXISTED_REGION_GROUP_ID)
                .param("_user_id", String.valueOf(USER_ID))
        )
            .andExpect(status().isForbidden())
            .andExpect(
                errorMessage(
                    String.format(
                        "Unable to access [%s] with ids %s",
                        ResourceType.SHOP_REGION_GROUP,
                        List.of(EXISTED_REGION_GROUP_ID)
                    )
                )
            );
    }

    @Test
    @DisplayName("Можно добавить региональный тип оплаты")
    @ExpectedDatabase(
        value = "/controller/shop/delivery-payment/db/after/add_payment_type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addPaymentType() throws Exception {
        performPaymentTypesRequest(
            SHOP_ID,
            EXISTED_REGION_GROUP_ID,
            "controller/shop/delivery-payment/request/add_payment_type.json"
        )
            .andExpect(status().isOk())
            .andExpect(content().json(extractFileContent(
                "controller/shop/delivery-payment/response/add_payment_type.json"
            ))
        );
    }

    @Test
    @DisplayName("Можно добавить региональный тип оплаты (DBS-партнёр)")
    @ExpectedDatabase(
            value = "/controller/shop/delivery-payment/db/after/add_payment_type_dbs.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addPaymentTypeDbs() throws Exception {
        performPaymentTypesRequest(
            SHOP_ID_DBS,
            EXISTED_REGION_GROUP_ID_DBS,
            "controller/shop/delivery-payment/request/add_payment_type.json"
        )
            .andExpect(status().isOk())
            .andExpect(content().json(extractFileContent(
                "controller/shop/delivery-payment/response/add_payment_type_dbs.json"
            ))
        );
    }

    @Test
    @DisplayName("Можно изменить региональные типы оплаты")
    @ExpectedDatabase(
        value = "/controller/shop/delivery-payment/db/after/change_payment_type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changePaymentTypes() throws Exception {
        performPaymentTypesRequest(
            SHOP_ID,
            EXISTED_REGION_GROUP_ID,
            "controller/shop/delivery-payment/request/change_payment_type.json"
        )
            .andExpect(status().isOk())
            .andExpect(content().json(extractFileContent(
                "controller/shop/delivery-payment/response/change_payment_type.json"
            ))
        );
    }

    @Test
    @DisplayName("Можно изменить региональные типы оплаты (DBS-партнёр)")
    @ExpectedDatabase(
            value = "/controller/shop/delivery-payment/db/after/change_payment_type_dbs.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changePaymentTypesDbs() throws Exception {
        performPaymentTypesRequest(
            SHOP_ID_DBS,
            EXISTED_REGION_GROUP_ID_DBS,
            "controller/shop/delivery-payment/request/change_payment_type.json"
        )
            .andExpect(status().isOk())
            .andExpect(content().json(extractFileContent(
                "controller/shop/delivery-payment/response/change_payment_type_dbs.json"
            ))
        );
    }

    @Test
    @DisplayName("Можно изменить региональные типы оплаты не локальной группы (DBS-партнёр)")
    @ExpectedDatabase(
            value = "/controller/shop/delivery-payment/db/after/change_non_selfgroup_payment_type_dbs.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changePaymentTypesForNonSelfGroup() throws Exception {
        performPaymentTypesRequest(
            SHOP_ID_DBS,
            EXISTED_NON_SELF_REGION_GROUP_ID_DBS,
            "controller/shop/delivery-payment/request/change_payment_type.json"
        )
            .andExpect(status().isOk())
            .andExpect(content().json(extractFileContent(
                "controller/shop/delivery-payment/response/change_payment_type_non_local_dbs.json"
            ))
        );
    }

    @Test
    @DisplayName("Можно удалить региональный тип оплаты")
    @ExpectedDatabase(
        value = "/controller/shop/delivery-payment/db/after/remove_payment_type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void removePaymentType() throws Exception {
        performPaymentTypesRequest(
            SHOP_ID,
            EXISTED_REGION_GROUP_ID,
            "controller/shop/delivery-payment/request/remove_payment_type.json"
        )
            .andExpect(status().isOk())
            .andExpect(content().json(extractFileContent(
                "controller/shop/delivery-payment/response/remove_payment_type.json"
            ))
        );
    }

    @Test
    @DisplayName("Можно удалить региональный тип оплаты (DBS-партнёр)")
    @ExpectedDatabase(
            value = "/controller/shop/delivery-payment/db/after/remove_payment_type_dbs.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void removePaymentTypeDbs() throws Exception {
        performPaymentTypesRequest(
            SHOP_ID_DBS,
            EXISTED_REGION_GROUP_ID_DBS,
            "controller/shop/delivery-payment/request/remove_payment_type.json"
        )
            .andExpect(status().isOk())
            .andExpect(content().json(extractFileContent(
                "controller/shop/delivery-payment/response/remove_payment_type_dbs.json"
            ))
        );
    }

    @Test
    @DisplayName("Можно удалить все региональные типы оплаты")
    @ExpectedDatabase(
        value = "/controller/shop/delivery-payment/db/after/remove_all_payment_types.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void removeAllPaymentTypes() throws Exception {
        performPaymentTypesRequest(
            SHOP_ID,
            EXISTED_REGION_GROUP_ID,
            "controller/shop/delivery-payment/request/remove_all_payment_types.json"
        )
            .andExpect(status().isOk())
            .andExpect(content().json(extractFileContent(
                "controller/shop/delivery-payment/response/remove_all_payment_types.json"
            ))
        );
    }

    @Test
    @DisplayName("Можно удалить все региональные типы оплаты (DBS-партнёр)")
    @ExpectedDatabase(
            value = "/controller/shop/delivery-payment/db/after/remove_all_payment_types_dbs.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void removeAllPaymentTypesDbs() throws Exception {
        performPaymentTypesRequest(
            SHOP_ID_DBS,
            EXISTED_REGION_GROUP_ID_DBS,
            "controller/shop/delivery-payment/request/remove_all_payment_types.json"
        )
            .andExpect(status().isOk())
            .andExpect(content().json(extractFileContent(
                "controller/shop/delivery-payment/response/remove_all_payment_types_dbs.json"
            ))
        );
    }

    @Test
    @DisplayName("При отправке списка типов 1-в-1 типы оплаты не изменяются")
    @ExpectedDatabase(
        value = "/controller/shop/delivery-payment/db/before/delivery_region_group_payments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void postSamePaymentTypes() throws Exception {
        performPaymentTypesRequest(
            SHOP_ID,
            EXISTED_REGION_GROUP_ID,
            "controller/shop/delivery-payment/request/same_payment_types.json"
        )
            .andExpect(status().isOk())
            .andExpect(
                content()
                    .json(extractFileContent("controller/shop/delivery-payment/response/same_payment_types.json"))
            );
    }

    @Test
    @DisplayName("При отправке списка типов 1-в-1 типы оплаты не изменяются (DBS-партнёр)")
    @ExpectedDatabase(
            value = "/controller/shop/delivery-payment/db/after/same_region_group_payments_dbs.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void postSamePaymentTypesDbs() throws Exception {
        performPaymentTypesRequest(
            SHOP_ID_DBS,
            EXISTED_REGION_GROUP_ID_DBS,
            "controller/shop/delivery-payment/request/same_payment_types.json"
        )
            .andExpect(status().isOk())
            .andExpect(content().json(extractFileContent(
                    "controller/shop/delivery-payment/response/same_payment_types_dbs.json"
            ))
        );
    }

    @Test
    @DisplayName("Нельзя настроить региональные типы оплат для несуществующей региональной группы")
    void changePaymentTypesForNonExistentGroup() throws Exception {
        performPaymentTypesRequest(
            SHOP_ID,
            NON_EXISTENT_REGION_GROUP_ID,
            "controller/shop/delivery-payment/request/add_payment_type.json"
        )
            .andExpect(status().isNotFound())
            .andExpect(
                errorMessage(
                    String.format(
                        "Failed to find [%s] with ids [%s]",
                        ResourceType.SHOP_REGION_GROUP,
                        List.of(NON_EXISTENT_REGION_GROUP_ID)
                    )
                )
            );
    }

    @Test
    @DisplayName("Нельзя настроить региональные типы оплат для группы другого магазина")
    void changePaymentTypesForGroupBelongingToOtherShop() throws Exception {
        performPaymentTypesRequest(
            ILLEGAL_SHOP_ID,
            EXISTED_REGION_GROUP_ID,
            "controller/shop/delivery-payment/request/add_payment_type.json"
        )
            .andExpect(status().isForbidden())
            .andExpect(
                errorMessage(
                    String.format(
                        "Unable to access [%s] with ids %s",
                        ResourceType.SHOP_REGION_GROUP,
                        List.of(EXISTED_REGION_GROUP_ID)
                    )
                )
            );
    }

    @Nonnull
    private ResultActions performPaymentTypesRequest(
            int shopId,
            int existedRegionGroupId,
            String requestBodyFileName
    ) throws Exception {
        return mockMvc.perform(
            post("/v2/shops/{shopId}/region-groups/{regionGroupId}/payment-types", shopId, existedRegionGroupId)
                .param("_user_id", String.valueOf(USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestBodyFileName))
        );
    }

}
