package ru.yandex.market.pvz.core.domain.ow.mapper;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pvz.core.domain.operator_window.dto.OwTicketDto;
import ru.yandex.market.pvz.core.domain.operator_window.mapper.ReceivedReturnTicketMapper;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnRequestItemParams;
import ru.yandex.market.pvz.core.domain.returns.model.ReturnRequestParams;

import static org.assertj.core.api.Assertions.assertThat;

public class ReceivedReturnTicketMapperTest {

    private final ReceivedReturnTicketMapper mapper = new ReceivedReturnTicketMapper();

    @Test
    void mapToTicket() {
        String orderId = "481293";

        var requestParams = ReturnRequestParams.builder()
                .orderId(orderId)
                .items(List.of(
                        ReturnRequestItemParams.builder()
                                .name("Пиво")
                                .operatorComment("Разбита бутылка")
                                .build(),
                        ReturnRequestItemParams.builder()
                                .name("Чипсы")
                                .build()
                ))
                .build();

        var body = "<p> Номер заказа: <b>" + orderId + "</b></p>\n" +
                "<p> Товары:\n" +
                "<ol>\n" +

                "<li>\n" +
                "Название принятого заказа: Пиво\n" +
                "<br> Комментарий сотрудника ПВЗ, который принял возврат: Разбита бутылка\n" +
                "</li>\n" +

                "<li>\n" +
                "Название принятого заказа: Чипсы\n" +
                "<br> Комментарий сотрудника ПВЗ, который принял возврат: нет комментария\n" +
                "</li>\n" +

                "</ol>\n" +
                "</p>\n" +

                "<p> Заказ принят на ПВЗ — всё в порядке. Нужно произвести выплату клиенту за 24 часа. </p>\n";



        var expected = OwTicketDto.builder()
                .title("Запрос в ГР от ПВЗ: Возврат денег клиенту - заказ " + orderId)
                .orderId(orderId)
                .categories(List.of("beruComplaintsGroupParse"))
                .service("beruComplaintsEscalation")
                .channel("mail")
                .comment(OwTicketDto.Comment.builder()
                        .metaClass("comment$internal")
                        .body(body)
                        .build())
                .build();

        assertThat(mapper.map(requestParams)).isEqualTo(expected);
    }
}
