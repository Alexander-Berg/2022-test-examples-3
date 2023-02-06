package ru.yandex.market.admin.service.remote;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.util.StringTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoteDatasourceUIServiceTest {

    @Test
    @DisplayName("Базовый сценарий получения комментария по изменению типов оплат")
    void testRenderPaymentTypesComment() {
        String comment = getCommentInput("comment/RemoteDatasourceUIServiceTest.testRenderPaymentTypesComment.txt");

        String expected = "Типы оплат для группы 44243: " +
                "наличными курьеру, картой курьеру, альтернативная предоплата, предоплата картой на сайте";
        assertEquals(expected, RemoteDatasourceUIService.renderComment(comment));
    }

    @Test
    @DisplayName("Получения комментария по изменению типов оплат, когда к группе не привязано ни одного типа оплаты")
    void testRenderPaymentTypesCommentWithNoData() {
        String comment = getCommentInput("comment/RemoteDatasourceUIServiceTest.testRenderPaymentTypesCommentWithNoData.txt");

        String expected = "Типы оплат для группы 44243: не выбраны";
        assertEquals(expected, RemoteDatasourceUIService.renderComment(comment));
    }

    private String getCommentInput(String filename) {
        return StringTestUtil.getString(getClass()
                .getResourceAsStream(filename))
                .replaceAll("\\s+", "");
    }
}
