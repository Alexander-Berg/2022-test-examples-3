package ru.yandex.market.tpl.core.query.common.mapper;

import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderTagsDto;
import ru.yandex.market.tpl.core.domain.base.property.TplPropertyType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderProperty;
import ru.yandex.market.tpl.core.domain.order.property.TplOrderProperties;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class TagsDtoMapperTest extends TplAbstractTest {

    private final TagsDtoMapper tagsDtoMapper;
    private final OrderGenerateService orderGenerateService;
    private final TransactionTemplate transactionTemplate;

    @DisplayName("Проверяем маппинг заказа для б2б с тегом 18+")
    @Test
    public void mapB2bOrderWithR18TagTest() {
        String verificationCodeProperty = TplOrderProperties.Names.VERIFICATION_CODE_BEFORE_HANDING.name();
        String customerTypeProperty = TplOrderProperties.Names.CUSTOMER_TYPE.name();
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("4512341")
                .items(OrderGenerateService.OrderGenerateParam.Items.builder().isR18(true).build())
                .properties(
                        Map.of(
                                verificationCodeProperty,
                                new OrderProperty(null, TplPropertyType.STRING, verificationCodeProperty,
                                        "verificationCode"),
                                customerTypeProperty,
                                new OrderProperty(null, TplPropertyType.STRING, customerTypeProperty,
                                        TplOrderProperties.CustomerValues.B2B_CUSTOMER_TYPE.name())
                        )
                )
                .build());

        Set<OrderTagsDto> tags = transactionTemplate.execute(t -> tagsDtoMapper.mapOrderTags(order));

        assertThat(tags).contains(OrderTagsDto.IS_R18);
        assertThat(tags).contains(OrderTagsDto.IS_NEED_ORDER_CODE_VALIDATE);
        assertThat(tags).doesNotContain(OrderTagsDto.SHOW_DOCUMENT);
    }

    @Test
    public void mapIsBoxingAvailableTag() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("45123441")
                        .properties(Map.of(
                                        TplOrderProperties.Names.IS_UNBOXING_AVAILABLE.name(),
                                        new OrderProperty(
                                                null,
                                                TplPropertyType.STRING,
                                                TplOrderProperties.Names.IS_UNBOXING_AVAILABLE.name(),
                                                TplOrderProperties.IS_UNBOXING_AVAILABLE.print(true))
                                )
                        )
                .build()
        );
        Set<OrderTagsDto> tags = transactionTemplate.execute(t -> tagsDtoMapper.mapOrderTags(order));

        assertThat(tags).contains(OrderTagsDto.IS_UNBOXING_AVAILABLE);
    }

}
