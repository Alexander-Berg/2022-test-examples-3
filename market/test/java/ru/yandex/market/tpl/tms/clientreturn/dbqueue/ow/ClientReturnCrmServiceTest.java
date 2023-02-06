package ru.yandex.market.tpl.tms.clientreturn.dbqueue.ow;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.external.crm.model.entity.CrmOrderTicketDto;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ClientReturnCrmServiceTest extends TplTmsAbstractTest {

    private final ClientReturnGenerator clientReturnGenerator;
    private final ClientReturnCrmService clientReturnOWService;

    @Test
    void test() {
        CrmOrderTicketDto.builder().build();
        ClientReturn clientReturn = clientReturnGenerator.generateReturnFromClient();
        CrmOrderTicketDto ticketForClientReturn = clientReturnOWService.getTicketForClientReturn(clientReturn.getId());
        assertThat(ticketForClientReturn).isEqualTo(CrmOrderTicketDto.builder()
                .orderId(clientReturn.getExternalOrderId())
                .comment(CrmOrderTicketDto.Comment.builder()
                        .metaClass("comment$internal")
                        .body(String.format("<p> Номер заказа: <b>%s</b></p>\n" +
                                        "<p> Товары:\n" +
                                        "<ol>\n" +
                                        "<li>\n" +
                                        "Название принятого заказа: Phone\n" +
                                        "<br> Комментарий курьера, принявшего возврат: нет комментария\n" +
                                        "</li>\n" +
                                        "</ol>\n" +
                                        "</p>\n" +
                                        "<p> Заказ принят курьером — всё в порядке. Нужно произвести выплату клиенту " +
                                        "за 24 часа. </p>\n",
                                clientReturn.getExternalOrderId())
                        )
                        .build())
                .title(String.format(
                        "Запрос в ГР от Курьерской Платформы: Возврат денег клиенту - заказ %s",
                        clientReturn.getExternalOrderId())
                )
                .categories(List.of("beruComplaintsGroupParse"))
                .service("beruComplaintsEscalation")
                .channel("mail")
                .build()
        );
    }

}
