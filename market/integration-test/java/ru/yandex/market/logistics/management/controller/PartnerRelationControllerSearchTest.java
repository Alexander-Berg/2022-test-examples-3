package ru.yandex.market.logistics.management.controller;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@DatabaseSetup("/data/controller/partnerRelation/prepare_data.xml")
@ParametersAreNonnullByDefault
class PartnerRelationControllerSearchTest extends AbstractContextualTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Успешный поиск связок")
    void searchPartnerRelationsSuccess(
        @SuppressWarnings("unused") String testName,
        String requestJson,
        String responseJson
    ) throws Exception {
        executePut(requestJson)
            .andExpect(status().isOk())
            .andExpect(testJson(responseJson));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Ошибка поиска связок")
    void searchPartnerRelationsFailure(
        @SuppressWarnings("unused") String testName,
        String requestJson,
        String responseJson
    )
        throws Exception {
        executePut(requestJson)
            .andExpect(status().isBadRequest())
            .andExpect(testJson(responseJson, Option.IGNORING_EXTRA_FIELDS));
    }

    @Nonnull
    private ResultActions executePut(String path) throws Exception {
        return mockMvc.perform(
            MockMvcRequestBuilders
                .request(HttpMethod.PUT, "/externalApi/partner-relation/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson(path))
        );
    }

    @Nonnull
    private static Stream<Arguments> searchPartnerRelationsSuccess() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                "data/controller/partnerRelation/search_all_filter.json",
                "data/controller/partnerRelation/search_all_response.json"
            ),
            Arguments.of(
                "По идентификаторам",
                "data/controller/partnerRelation/search_by_ids_filter.json",
                "data/controller/partnerRelation/partner_relation_1_response.json"
            ),
            Arguments.of(
                "По идентификаторам партнеров откуда и куда",
                "data/controller/partnerRelation/search_by_partner_ids_filter.json",
                "data/controller/partnerRelation/partner_relation_1_response.json"
            ),
            Arguments.of(
                "По идентификатору партнера откуда",
                "data/controller/partnerRelation/search_by_from_partner_id_filter.json",
                "data/controller/partnerRelation/partner_relation_2_3_response.json"
            ),
            Arguments.of(
                "По типу партнера откуда",
                "data/controller/partnerRelation/search_by_from_partner_types_filter.json",
                "data/controller/partnerRelation/search_all_response.json"
            ),
            Arguments.of(
                "По типу партнера куда",
                "data/controller/partnerRelation/search_by_to_partner_types_filter.json",
                "data/controller/partnerRelation/search_all_response.json"
            ),
            Arguments.of(
                "По активности",
                "data/controller/partnerRelation/search_by_enabled_filter.json",
                "data/controller/partnerRelation/partner_relation_2_3_response.json"
            ),
            Arguments.of(
                "По несуществующему идентификатору партнера",
                "data/controller/partnerRelation/search_by_non_existent_partner_id_filter.json",
                "data/controller/partnerRelation/empty_list.json"
            ),
            Arguments.of(
                "По набору идентификаторов партнеров",
                "data/controller/partnerRelation/search_by_partners_ids_set_filter.json",
                "data/controller/partnerRelation/partner_relation_1_3_response.json"
            ),
            Arguments.of(
                "По набору идентификаторов партнеров и идентификатору партнера откуда",
                "data/controller/partnerRelation/search_by_partners_ids_set_and_from_id_filter.json",
                "data/controller/partnerRelation/partner_relation_1_3_response.json"
            ),
            Arguments.of(
                "По набору идентификаторов откуда",
                "data/controller/partnerRelation/search_by_from_partners_set_filter.json",
                "data/controller/partnerRelation/search_all_response.json"
            ),
            Arguments.of(
                "По набору идентификаторов куда",
                "data/controller/partnerRelation/search_by_to_partners_set_filter.json",
                "data/controller/partnerRelation/partner_relation_2_response.json"
            ),
            Arguments.of(
                "По набору с несуществующими идентификаторами партнеров",
                "data/controller/partnerRelation/search_by_non_existent_partner_id_in_set_filter.json",
                "data/controller/partnerRelation/empty_list.json"
            ),
            Arguments.of(
                "По пустому набору партнеров откуда",
                "data/controller/partnerRelation/search_by_partners_ids_set_empty_from_filter.json",
                "data/controller/partnerRelation/empty_list.json"
            ),
            Arguments.of(
                "По пустому набору партнеров куда",
                "data/controller/partnerRelation/search_by_partners_ids_set_empty_to_filter.json",
                "data/controller/partnerRelation/empty_list.json"
            ),
            Arguments.of(
                "По пустому набору идентификаторов",
                "data/controller/partnerRelation/search_by_ids_set_empty_filter.json",
                "data/controller/partnerRelation/empty_list.json"
            )
        );

    }

    @Nonnull
    private static Stream<Arguments> searchPartnerRelationsFailure() {
        return Stream.of(
            Arguments.of(
                "Набор идентификаторов откуда содержит null",
                "data/controller/partnerRelation/search_by_partners_ids_set_invalid_from_filter.json",
                "data/controller/partnerRelation/search_by_partners_ids_set_invalid_from_response.json"
            ),
            Arguments.of(
                "Набор идентификаторов куда содержит null",
                "data/controller/partnerRelation/search_by_partners_ids_set_invalid_to_filter.json",
                "data/controller/partnerRelation/search_by_partners_ids_set_invalid_to_response.json"
            ),
            Arguments.of(
                "Набор идентификаторов содержит null",
                "data/controller/partnerRelation/search_by_ids_set_invalid_filter.json",
                "data/controller/partnerRelation/search_by_ids_set_invalid_response.json"
            )
        );
    }
}
