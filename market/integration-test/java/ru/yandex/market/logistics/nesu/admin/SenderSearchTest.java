package ru.yandex.market.logistics.nesu.admin;

import java.time.LocalDate;
import java.time.Month;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.admin.model.request.AdminSenderFilter;
import ru.yandex.market.logistics.nesu.enums.SenderStatus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/repository/sender/before/prepare_for_search.xml")
class SenderSearchTest extends AbstractContextualTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArguments")
    @DisplayName("Поиск сендеров")
    void search(String caseName, AdminSenderFilter filter, String jsonPath) throws Exception {
        mockMvc.perform(get("/admin/senders").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(jsonPath));
    }

    @Nonnull
    private static Stream<Arguments> searchArguments() {
        return Stream.<Triple<String, AdminSenderFilter, String>>of(
            Triple.of(
                "По поисковому запросу: идентификатор сендера",
                filter().setFullTextSearch("40001"),
                "controller/admin/sender-search/1.json"
            ),
            Triple.of(
                "По поисковому запросу",
                filter().setFullTextSearch("40001 @dostavkin КнИ .ru итриеви остАв Денис 111111"),
                "controller/admin/sender-search/1.json"
            ),
            Triple.of(
                "По поисковому запросу: подстрока названия",
                filter().setFullTextSearch("КнИ"),
                "controller/admin/sender-search/12.json"
            ),
            Triple.of(
                "По поисковому запросу: ФИО контакта",
                filter().setFullTextSearch("итриеви остАв Денис"),
                "controller/admin/sender-search/12.json"
            ),
            Triple.of(
                "По поисковому запросу: номер телефона",
                filter().setFullTextSearch("111111"),
                "controller/admin/sender-search/12.json"
            ),
            Triple.of(
                "По поисковому запросу: email контакта",
                filter().setFullTextSearch("@dostavkin"),
                "controller/admin/sender-search/12.json"
            ),
            Triple.of(
                "По идентификатору магазина",
                filter().setShopId(50001L),
                "controller/admin/sender-search/12.json"
            ),
            Triple.of(
                "По поисковому запросу: адрес сайта",
                filter().setFullTextSearch(".Рф"),
                "controller/admin/sender-search/23.json"
            ),
            Triple.of(
                "По статусу",
                filter().setSenderStatus(SenderStatus.DELETED),
                "controller/admin/sender-search/3.json"
            ),
            Triple.of(
                "По дате создания",
                filter().setSenderCreated(LocalDate.of(2019, Month.SEPTEMBER, 11)),
                "controller/admin/sender-search/3.json"
            )
        )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }

    @Nonnull
    private static AdminSenderFilter filter() {
        return new AdminSenderFilter();
    }
}
