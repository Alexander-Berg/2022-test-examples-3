package ru.yandex.market.logistics.tarifficator.admin.revisions;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.enums.RevisionItemXmlBuildingStatus;
import ru.yandex.market.logistics.tarifficator.model.filter.AdminRevisionItemXmlBuildingHistoryFilter;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение элементов истории построения XML выгрузок отчетов через админку")
@DatabaseSetup("/controller/admin/revisions/revision-item-xml-building-history/before/search_prepare.xml")
public class GetRevisionItemXmlBuildingHistoryTest extends AbstractContextualTest {

    private static final LocalDateTime FROM_DATETIME = LocalDateTime.of(2019, 9, 14, 11, 47, 11);
    private static final LocalDateTime TO_DATETIME = LocalDateTime.of(2019, 9, 14, 11, 47, 12);

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @MethodSource("arguments")
    @DisplayName("Получение элементов истории построения XML выгрузок отчетов")
    void getRevisionXmlBuildingHistoryElements(
        @SuppressWarnings("unused") String name,
        AdminRevisionItemXmlBuildingHistoryFilter filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get("/admin/revision-item-xml-building-history").params(TestUtils.toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent(responsePath));
    }

    private static Stream<Arguments> arguments() {
        return Stream.of(
            Arguments.of(
                "Без фильтров",
                new AdminRevisionItemXmlBuildingHistoryFilter(),
                "controller/admin/revisions/revision-item-xml-building-history/response/all.json"
            ),
            Arguments.of(
                "Фильтр по статусу",
                new AdminRevisionItemXmlBuildingHistoryFilter().setStatus(RevisionItemXmlBuildingStatus.OK),
                "controller/admin/revisions/revision-item-xml-building-history/response/history_1_2_3.json"
            ),
            Arguments.of(
                "Фильтр по ошибке",
                new AdminRevisionItemXmlBuildingHistoryFilter().setError("test1"),
                "controller/admin/revisions/revision-item-xml-building-history/response/history_4.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору прайс-листа",
                new AdminRevisionItemXmlBuildingHistoryFilter().setPriceListId(2L),
                "controller/admin/revisions/revision-item-xml-building-history/response/history_5_3_4.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору файла прайс-листа",
                new AdminRevisionItemXmlBuildingHistoryFilter().setPriceListFileId(1L),
                "controller/admin/revisions/revision-item-xml-building-history/response/history_5_3_4.json"
            ),
            Arguments.of(
                "Фильтр по нижней границе даты и времени",
                new AdminRevisionItemXmlBuildingHistoryFilter().setFromDatetime(FROM_DATETIME),
                "controller/admin/revisions/revision-item-xml-building-history/response/history_5_4_3_2_1.json"
            ),
            Arguments.of(
                "Фильтр по верхней границе даты и времени",
                new AdminRevisionItemXmlBuildingHistoryFilter().setToDatetime(TO_DATETIME),
                "controller/admin/revisions/revision-item-xml-building-history/response/history_5_4_1.json"
            ),
            Arguments.of(
                "Фильтр по всем полям",
                new AdminRevisionItemXmlBuildingHistoryFilter()
                    .setStatus(RevisionItemXmlBuildingStatus.FAIL)
                    .setError("test1")
                    .setPriceListId(2L)
                    .setToDatetime(TO_DATETIME)
                    .setFromDatetime(FROM_DATETIME),
                "controller/admin/revisions/revision-item-xml-building-history/response/history_4.json"
            )
        );
    }
}
