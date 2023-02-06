package ru.yandex.market.logistics.management.controller.admin.scheduleDay;

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
import static ru.yandex.market.logistics.management.controller.admin.scheduleDay.Helper.READ_WRITE;
import static ru.yandex.market.logistics.management.controller.admin.scheduleDay.Helper.multipartFile;
import static ru.yandex.market.logistics.management.controller.admin.scheduleDay.Helper.uploadAdd;
import static ru.yandex.market.logistics.management.controller.admin.scheduleDay.Helper.uploadReplace;
import static ru.yandex.market.logistics.management.util.TestUtil.hasResolvedExceptionContainingMessage;

@DisplayName("Валидация файла при загрузке расписания работы конечных точек")
@DatabaseSetup("/data/controller/admin/scheduleDay/before/prepare_data.xml")
public class AdminDeliveryIntervalFileControllerUploadValidationTest extends AbstractContextualTest {

    @DisplayName("Ошибки валидации")
    @ParameterizedTest(name = AbstractContextualTest.TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = READ_WRITE)
    @ExpectedDatabase(
        value = "/data/controller/admin/scheduleDay/before/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvErrorValidation(
        @SuppressWarnings("unused") String caseName,
        MockMultipartHttpServletRequestBuilder requestBuilder,
        String pathToCsvFile,
        String errorMessage,
        Integer status
    ) throws Exception {
        mockMvc.perform(requestBuilder.file(multipartFile(
            TestUtil.pathToJson(pathToCsvFile))))
            .andExpect(status().is(status))
            .andExpect(hasResolvedExceptionContainingMessage(errorMessage));
    }

    @Nonnull
    private static Stream<Arguments> getArguments() {
        return Stream.of(
            Arguments.of(
                "Добавить - Неправильный формат csv файла",
                uploadAdd(),
                "data/controller/admin/scheduleDay/request/upload_add_invalid_csv.csv",
                "Неправильный формат csv файла в строке 2",
                400
            ),
            Arguments.of(
                "Добавить - Ошибка javax-валидации данных",
                uploadAdd(),
                "data/controller/admin/scheduleDay/request/upload_add_invalid_dtos.csv",
                "Обнаружены следующие ошибки:\n" +
                    "Строка: 2, Поле: partnerId, Сообщение: Обязательно для заполнения\n" +
                    "Строка: 3, Поле: weekDay, Сообщение: Обязательно для заполнения\n" +
                    "Строка: 4, Поле: locationId, Сообщение: Обязательно для заполнения\n" +
                    "Строка: 5, Поле: timeFrom, Сообщение: Обязательно для заполнения\n" +
                    "Строка: 6, Поле: timeTo, Сообщение: Обязательно для заполнения\n" +
                    "Строка: 7, Поле: timeTo, Сообщение: Обязательно для заполнения",
                400
            ),
            Arguments.of(
                "Добавить - Регионы не найдены",
                uploadAdd(),
                "data/controller/admin/scheduleDay/request/upload_add_regions_not_found.csv",
                "No regions with ids [161, 165] found",
                404
            ),
            Arguments.of(
                "Добавить - Партнеры не найдены",
                uploadAdd(),
                "data/controller/admin/scheduleDay/request/upload_add_partners_not_found.csv",
                "Partners with ids [3002, 3003] not found",
                404
            ),
            Arguments.of(
                "Заменить - Неправильный формат csv файла",
                uploadReplace(),
                "data/controller/admin/scheduleDay/request/upload_add_invalid_csv.csv",
                "Неправильный формат csv файла в строке 2",
                400
            ),
            Arguments.of(
                "Заменить - Ошибка javax-валидации данных",
                uploadReplace(),
                "data/controller/admin/scheduleDay/request/upload_add_invalid_dtos.csv",
                "Обнаружены следующие ошибки:\n" +
                    "Строка: 2, Поле: partnerId, Сообщение: Обязательно для заполнения\n" +
                    "Строка: 3, Поле: weekDay, Сообщение: Обязательно для заполнения\n" +
                    "Строка: 4, Поле: locationId, Сообщение: Обязательно для заполнения\n" +
                    "Строка: 5, Поле: timeFrom, Сообщение: Обязательно для заполнения\n" +
                    "Строка: 6, Поле: timeTo, Сообщение: Обязательно для заполнения\n" +
                    "Строка: 7, Поле: timeTo, Сообщение: Обязательно для заполнения",
                400
            ),
            Arguments.of(
                "Заменить - Регионы не найдены",
                uploadReplace(),
                "data/controller/admin/scheduleDay/request/upload_add_regions_not_found.csv",
                "No regions with ids [161, 165] found",
                404
            ),
            Arguments.of(
                "Заменить - Партнеры не найдены",
                uploadReplace(),
                "data/controller/admin/scheduleDay/request/upload_add_partners_not_found.csv",
                "Partners with ids [3002, 3003] not found",
                404
            )
        );
    }
}
