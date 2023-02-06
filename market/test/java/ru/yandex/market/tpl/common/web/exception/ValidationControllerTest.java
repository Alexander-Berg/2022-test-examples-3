package ru.yandex.market.tpl.common.web.exception;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ValidatorController.class)
@ExtendWith(SpringExtension.class)
@ComponentScan(basePackages = {"ru.yandex.market.tpl.common.web"})
public class ValidationControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    // need because of MockMvc does not handle UTF-8 characters by default
    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilter((request, response, chain) -> {
                    response.setCharacterEncoding("UTF-8");
                    chain.doFilter(request, response);
                }, "/*")
                .build();
    }

    @Test
    void validate() throws Exception {
        mockMvc.perform(
                post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("validation/request_validate.json")))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message", is("Ошибка валидации: " +
                        "Поле [ 'Внебрачный ребенок' -> 'Имя' ] не должно быть пустым. Получено значение: ''. " +
                        "Поле [ 'Дети (Номер 2)' -> 'Имя' ] не должно быть пустым. Получено значение: ''. " +
                        "Поле [ 'Дети (Номер 3)' -> 'Имя' ] не должно быть пустым. Получено значение: ''. " +
                        "Поле [ 'Цена' ] число вне допустимого диапазона (ожидалось <4 разрядов>.<2 разрядов>). " +
                        "Получено значение: '100000.299'. " +
                        "Поле [ 'День недели' ] не должно равняться null. Получено значение: 'null'. " +
                        "Поле [ 'Градусы' ] должно быть больше или равно 0. Получено значение: '-10'. " +
                        "Поле [ 'Отклонение' ] должно быть не больше 0. Получено значение: '5'. " +
                        "Поле [ 'Спутники Земли' ] размер должен находиться в диапазоне от 0 до 1. " +
                        "Получено значение: '[Луна, Венера]'. " +
                        "Поле [ 'Электронная почта' ] должно иметь формат адреса электронной почты. " +
                        "Получено значение: '12345'. " +
                        "Поле [ 'Дата конца света' ] должно содержать дату, которая еще не наступила. " +
                        "Получено значение: '2020-10-21'. " +
                        "Поле [ 'два плюс два равно пять' ] должно быть равно false. Получено значение: 'true'. " +
                        "Поле [ 'Друзья (Ключ 'Максим')' -> 'Имя' ] не должно быть пустым. Получено значение: ''. " +
                        "Поле [ 'Друзья (Ключ 'Федор')' -> 'Имя' ] не должно быть пустым. Получено значение: ''. " +
                        "Поле [ 'Рост' ] должно быть больше 0. Получено значение: '0'. " +
                        "Поле [ 'Оценка' ] должно быть не больше 5. Получено значение: '10'. " +
                        "Поле [ 'Месяц' ] должно равняться null. Получено значение: 'JANUARY'. " +
                        "Поле [ 'Имя' ] не должно быть пустым. Получено значение: ''. " +
                        "Поле [ 'Номер мобильного телефона' ] должно соответствовать \"^\\d{11}$\". " +
                        "Получено значение: '8000'. " +
                        "Поле [ 'Зарплата' ] должно быть больше, чем или равно 0. Получено значение: '-10.0'. " +
                        "Поле [ 'Навыки' ] не должно быть пустым. Получено значение: '[]'. " +
                        "Поле [ 'День распада СССР' ] должно содержать прошедшую дату. " +
                        "Получено значение: '2999-01-01'. " +
                        "Поле [ 'Скорость' ] должно быть не меньше 0. Получено значение: '-50.0'. " +
                        "Поле [ 'Сегодняшняя дата' ] должно содержать сегодняшнее число или дату, " +
                        "которая еще не наступила. " +
                        "Получено значение: '2020-10-21'. " +
                        "Поле [ 'дважды два - четыре' ] должно быть равно true. Получено значение: 'false'. " +
                        "Поле [ 'Зрение' ] должно быть меньше 0. Получено значение: '4'. " +
                        "Поле [ 'День рождения Яндекса' ] должно содержать прошедшую дату или сегодняшнее число. " +
                        "Получено значение: '2999-01-01'.")));
    }

    @SneakyThrows
    private String getFileContent(String filename) {
        return IOUtils.toString(Objects.requireNonNull(getSystemResourceAsStream(filename)), StandardCharsets.UTF_8);
    }
}
