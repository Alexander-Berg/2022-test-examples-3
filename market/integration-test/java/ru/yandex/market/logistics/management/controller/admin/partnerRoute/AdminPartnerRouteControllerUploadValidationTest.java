package ru.yandex.market.logistics.management.controller.admin.partnerRoute;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.partnerRoute.Helper.uploadUpsert;
import static ru.yandex.market.logistics.management.util.TestUtil.hasResolvedExceptionContainingMessage;

@DisplayName("Валидация загрузки расписания магистралей партнеров файлом")
@DatabaseSetup("/data/controller/admin/partnerRoute/before/prepare_data.xml")
public class AdminPartnerRouteControllerUploadValidationTest extends AbstractContextualTest {

    @DisplayName("Ошибки валидации")
    @ParameterizedTest(name = AbstractContextualTest.TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerRoute/before/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvErrorValidation(
        @SuppressWarnings("unused") String caseName,
        MockMultipartHttpServletRequestBuilder requestBuilder,
        String pathToCsvFile,
        String errorMessage,
        Integer status
    ) throws Exception {
        mockMvc.perform(requestBuilder.file(Helper.file(TestUtil.pathToJson(pathToCsvFile))))
            .andExpect(status().is(status))
            .andExpect(hasResolvedExceptionContainingMessage(errorMessage));
    }

    @Nonnull
    private static Stream<Arguments> getArguments() {
        return Stream.of(
            Arguments.of(
                "Добавить - Неправильный формат csv файла",
                uploadUpsert(),
                "data/controller/admin/partnerRoute/request/upload_add_invalid_csv.csv",
                "Неправильный формат csv файла в строке 2",
                400
            ),
            Arguments.of(
                "Добавить - Ошибка javax-валидации данных",
                uploadUpsert(),
                "data/controller/admin/partnerRoute/request/upload_add_invalid_dtos.csv",
                "Обнаружены следующие ошибки:\n" +
                    "Строка: 2, Поле: friday, Сообщение: Обязательно для заполнения\n" +
                    "Строка: 3, Поле: locationTo, Сообщение: Обязательно для заполнения\n" +
                    "Строка: 4, Поле: sunday, Сообщение: Обязательно для заполнения\n" +
                    "Строка: 5, Поле: sunday, Сообщение: Обязательно для заполнения",
                400
            ),
            Arguments.of(
                "Добавить - Ограничения ВГХ не найдены",
                uploadUpsert(),
                "data/controller/admin/partnerRoute/request/korobyte_restrictions_not_found.csv",
                "Not found korobyte restrictions with keys [NOT_FOUND_1, NOT_FOUND_2]",
                404
            ),
            Arguments.of(
                "Добавить - Регионы не найдены",
                uploadUpsert(),
                "data/controller/admin/partnerRoute/request/upload_add_regions_not_found.csv",
                "No regions with ids [161, 165] found",
                404
            ),
            Arguments.of(
                "Добавить - Партнеры не найдены",
                uploadUpsert(),
                "data/controller/admin/partnerRoute/request/upload_add_partners_not_found.csv",
                "Partners with ids [3002, 3003] not found",
                404
            ),
            Arguments.of(
                "Заменить - Неправильный формат csv файла",
                uploadUpsert(),
                "data/controller/admin/partnerRoute/request/upload_add_invalid_csv.csv",
                "Неправильный формат csv файла в строке 2",
                400
            ),
            Arguments.of(
                "Заменить - Ошибка javax-валидации данных",
                uploadUpsert(),
                "data/controller/admin/partnerRoute/request/upload_add_invalid_dtos.csv",
                "Обнаружены следующие ошибки:\n" +
                    "Строка: 2, Поле: friday, Сообщение: Обязательно для заполнения\n" +
                    "Строка: 3, Поле: locationTo, Сообщение: Обязательно для заполнения\n" +
                    "Строка: 4, Поле: sunday, Сообщение: Обязательно для заполнения\n" +
                    "Строка: 5, Поле: sunday, Сообщение: Обязательно для заполнения",
                400
            ),
            Arguments.of(
                "Заменить - Регионы не найдены",
                uploadUpsert(),
                "data/controller/admin/partnerRoute/request/upload_add_regions_not_found.csv",
                "No regions with ids [161, 165] found",
                404
            ),
            Arguments.of(
                "Заменить - Партнеры не найдены",
                uploadUpsert(),
                "data/controller/admin/partnerRoute/request/upload_add_partners_not_found.csv",
                "Partners with ids [3002, 3003] not found",
                404
            )
        );
    }
}
