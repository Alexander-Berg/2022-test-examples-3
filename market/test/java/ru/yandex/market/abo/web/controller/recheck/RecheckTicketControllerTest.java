package ru.yandex.market.abo.web.controller.recheck;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketType;

/**
 * @author komarovns
 * @date 09.04.19
 */
class RecheckTicketControllerTest {

    /**
     * Проверяем, что у методов с 'params = "ticketType=*"' валидный ticketType
     */
    @Test
    void testTicketTypeParamName() {

        for (Method method : RecheckTicketController.class.getMethods()) {
            RequestMapping annotation = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (annotation == null) {
                continue;
            }
            Arrays.stream(annotation.params())
                    .flatMap(p -> Arrays.stream(p.split(",")))
                    .map(String::trim)
                    .filter(param -> param.startsWith("ticketType="))
                    .map(param -> param.replace("ticketType=", ""))
                    .forEach(param -> Assertions.assertDoesNotThrow(() -> RecheckTicketType.valueOf(param)));
        }
    }
}
