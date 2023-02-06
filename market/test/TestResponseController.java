package ru.yandex.market.mbi.partner_stat.mvc.test;

import javax.annotation.ParametersAreNonnullByDefault;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.mbi.partner_stat.mvc.advice.XlsxReportGenerator;
import ru.yandex.market.mbi.partner_stat.mvc.test.model.SuccessDTO;
import ru.yandex.market.partner.error.info.exception.BadRequestException;
import ru.yandex.market.partner.error.info.model.ErrorInfo;
import ru.yandex.market.partner.error.info.util.Errors;

/**
 * Контроллер для тестов обработчиков.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@RestController
@Api("Тестовый контроллер для проверки форматов ответа")
@ParametersAreNonnullByDefault
public class TestResponseController {

    /**
     * Исключение BadRequestException.
     */
    @ApiOperation(value = "Ответ в формате json", notes = "Демонстрация ответа из ручки, у которой produces = json")
    @ApiResponses({
            @ApiResponse(
                    code = HttpStatus.SC_OK,
                    message = "Успешное получение тестового ответа",
                    response = SuccessDTO.class
            ),
            @ApiResponse(
                    code = HttpStatus.SC_BAD_REQUEST,
                    message = "Произошло исключение. Пример 400 кода",
                    response = ErrorInfo.class
            ),
    })
    @GetMapping("testResponse/{fail}")
    public SuccessDTO test(
            @ApiParam(value = "Нужно ли бросить исключение", required = true)
            @PathVariable final boolean fail
    ) {
        if (fail) {
            throw new BadRequestException(Errors.invalidField("fieldName"));
        }

        return new SuccessDTO("success");
    }

    /**
     * Исключение BadRequestException и produces != json.
     */
    @XlsxReportGenerator
    @ApiOperation(value = "Ответ НЕ в формате json", notes = "Демонстрация ответа из ручки, у которой produces != json")
    @GetMapping(value = "testResponse/{fail}/excel")
    @ApiResponses({
            @ApiResponse(
                    code = HttpStatus.SC_OK,
                    message = "Успешное получение тестового ответа",
                    response = byte[].class
            ),
            @ApiResponse(
                    code = HttpStatus.SC_BAD_REQUEST,
                    message = "Произошло исключение. Пример 400 кода",
                    response = ErrorInfo.class
            ),
    })
    public ResponseEntity<?> testExcel(
            @ApiParam(value = "Нужно ли бросить исключение", required = true)
            @PathVariable final boolean fail
    ) {
        if (fail) {
            throw new BadRequestException(Errors.invalid());
        }

        return ResponseEntity.ok(new byte[]{0, 4, 5});
    }
}
