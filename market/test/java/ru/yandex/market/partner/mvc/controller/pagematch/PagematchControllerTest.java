package ru.yandex.market.partner.mvc.controller.pagematch;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тест на работоспособность метода ПИ /pagematch.
 * Поскольку данные берутся из конфигурации спринга, для теста нужно создать и поддерживать
 * отдельный spring config ПИ, это накладно и не вписывается в концепцию наследования всех
 * тестов от FunctionalTest. Для того чтобы максимально покрыть функциональность тестом,
 * есть тест контроллера в mbi-web на моках.
 *
 * @author stani on 06.03.18.
 */
public class PagematchControllerTest extends FunctionalTest {

    @Test
    public void pagematchTest() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/pagematch");
        Assertions.assertTrue(response.getBody().split("\n").length > 1);
    }
}
