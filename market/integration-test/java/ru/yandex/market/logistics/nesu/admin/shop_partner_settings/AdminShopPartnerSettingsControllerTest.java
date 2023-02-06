package ru.yandex.market.logistics.nesu.admin.shop_partner_settings;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.admin.model.response.AdminShopPartnerSettingsNewDto;
import ru.yandex.market.logistics.nesu.jobs.producer.PushFfLinkToMbiProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.PushPartnerMappingToL4SProducer;
import ru.yandex.market.logistics4shops.client.api.PartnerMappingApi;
import ru.yandex.market.logistics4shops.client.model.DeletePartnerMappingRequest;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Операции с настройками партнеров и магазинов")
@DatabaseSetup("/repository/shop-partner/before/prepare_for_create.xml")
public class AdminShopPartnerSettingsControllerTest extends AbstractContextualTest {

    private static final String SLUG = "/admin/shops/partners/";

    @Autowired
    private MbiApiClient mbiApiClient;

    @Autowired
    private PartnerMappingApi partnerMappingApi;

    @Autowired
    private PushFfLinkToMbiProducer pushFfLinkToMbiProducer;

    @Autowired
    private PushPartnerMappingToL4SProducer pushPartnerMappingToL4SProducer;

    @BeforeEach
    void setup() {
        doNothing().when(pushFfLinkToMbiProducer).produceTask(anyLong(), anyLong());
        doNothing().when(pushPartnerMappingToL4SProducer).produceTask(anyLong(), anyLong());
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(
            pushFfLinkToMbiProducer,
            pushPartnerMappingToL4SProducer,
            partnerMappingApi,
            mbiApiClient
        );
    }

    @Test
    @DisplayName("Получить страницу создания настройки")
    void getCreationForm() throws Exception {
        mockMvc.perform(get(SLUG + "new"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/shop-partner/creation_form.json"));
    }

    @Test
    @DisplayName("Получить страницу настройки")
    void getSetting() throws Exception {
        mockMvc.perform(get(SLUG + "1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/shop-partner/setting_1.json"));
    }

    @Test
    @DisplayName("Получить страницу несуществующей настройки")
    void getSettingFail() throws Exception {
        mockMvc.perform(get(SLUG + "100"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP_PARTNER_SETTINGS] with ids [100]"));
    }

    @Test
    @DisplayName("Удалить настройкку")
    @ExpectedDatabase(
        value = "/repository/shop-partner/after/after_delete.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteSetting() throws Exception {
        mockMvc.perform(delete(SLUG + "1"))
            .andExpect(status().isOk())
            .andExpect(noContent());

        DeletePartnerMappingRequest deletePartnerMappingRequest = new DeletePartnerMappingRequest();
        deletePartnerMappingRequest.setLmsPartnerId(101L);
        deletePartnerMappingRequest.setMbiPartnerId(50001L);

        verify(mbiApiClient).deletePartnerFulfillmentLinks(50001, Set.of(101L));
        verify(partnerMappingApi).deletePartnerMapping(deletePartnerMappingRequest);
    }

    @Test
    @DisplayName("Удалить несуществующую настройкку")
    @ExpectedDatabase(
        value = "/repository/shop-partner/before/prepare_for_create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteSettingFail() throws Exception {
        mockMvc.perform(delete(SLUG + "100"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP_PARTNER_SETTINGS] with ids [100]"));
    }

    @Test
    @DisplayName("Успешное создание настройки")
    @ExpectedDatabase(
        value = "/repository/shop-partner/after/after_create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createSuccess() throws Exception {
        create(new AdminShopPartnerSettingsNewDto().setShopId(50003L).setPartnerId(103L))
            .andExpect(status().isOk())
            .andExpect(content().string("4"));

        verify(pushPartnerMappingToL4SProducer).produceTask(103L, 50003L);
        verify(pushFfLinkToMbiProducer).produceTask(103L, 50003L);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Ошибка создания настройки")
    @ExpectedDatabase(
        value = "/repository/shop-partner/before/prepare_for_create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createFail(
        @SuppressWarnings("unused") String displayName,
        AdminShopPartnerSettingsNewDto dto,
        ResultMatcher statusMatcher,
        String responsePath
    ) throws Exception {
        create(dto).andExpect(statusMatcher).andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> createFail() {
        return Stream.of(
            Arguments.of(
                "Не указаны shopId и partnerId",
                new AdminShopPartnerSettingsNewDto(),
                status().isBadRequest(),
                "controller/admin/shop-partner/no_shop_id_partner_id.json"
            ),
            Arguments.of(
                "Не указан shopId",
                new AdminShopPartnerSettingsNewDto().setPartnerId(1L),
                status().isBadRequest(),
                "controller/admin/shop-partner/no_shop_id.json"
            ),
            Arguments.of(
                "Не указан partnerId",
                new AdminShopPartnerSettingsNewDto().setShopId(1L),
                status().isBadRequest(),
                "controller/admin/shop-partner/no_partner_id.json"
            ),
            Arguments.of(
                "Магазин не существует",
                new AdminShopPartnerSettingsNewDto().setShopId(1L).setPartnerId(1L),
                status().isNotFound(),
                "controller/admin/shop-partner/invalid_shop.json"
            ),
            Arguments.of(
                "Настройка существует",
                new AdminShopPartnerSettingsNewDto().setShopId(50001L).setPartnerId(101L),
                status().isBadRequest(),
                "controller/admin/shop-partner/setting_exist.json"
            )
        );
    }

    @Nonnull
    private ResultActions create(AdminShopPartnerSettingsNewDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.POST, SLUG, request));
    }
}
