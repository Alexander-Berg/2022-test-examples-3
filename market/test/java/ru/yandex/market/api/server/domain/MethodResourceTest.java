package ru.yandex.market.api.server.domain;

import org.junit.Test;
import org.springframework.web.bind.annotation.RequestMethod;

import ru.yandex.market.api.integration.UnitTestBase;

import static org.junit.Assert.*;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class MethodResourceTest extends UnitTestBase {

    /**
     * Для совместимости с текущими заведенными лимитами при создании объекта ресурса мы вырезаем все что попадает
     * между фигурными скобками.
     */
    @Test
    public void shouldCutCurlyBrackets() {
        String testResourceName = "test1/{}/test2/{}/test3";
        MethodResource resource = new MethodResource(
            RequestMethod.GET,
            testResourceName,
            MethodResource.Group.SPECIAL,
            testResourceName
        );

        assertEquals("test1/{}/test3", resource.getName());
        assertEquals("GET_test1/{}/test2/{}/test3", resource.toString());
    }
}
