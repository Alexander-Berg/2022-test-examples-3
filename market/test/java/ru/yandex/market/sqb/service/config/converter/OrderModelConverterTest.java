package ru.yandex.market.sqb.service.config.converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.RandomUtils;

import ru.yandex.market.sqb.model.conf.OrderModel;
import ru.yandex.market.sqb.model.conf.OrderType;
import ru.yandex.market.sqb.model.conf.ParameterModel;
import ru.yandex.market.sqb.model.vo.OrderVO;
import ru.yandex.market.sqb.service.config.converter.common.AbstractNameModelConverterTest;
import ru.yandex.market.sqb.test.ObjectGenerationUtils;

/**
 * Unit-тесты для {@link OrderModelConverter}.
 *
 * @author Vladislav Bauer
 */
class OrderModelConverterTest extends AbstractNameModelConverterTest<OrderVO, OrderModel> {

    private static final List<ParameterModel> PARAMETERS = generateParameters();
    private static final String TEST_NAME = "TEST_NAME";


    @Nonnull
    @Override
    protected OrderVO createObject() {
        final OrderType orderType = randomOrderType();
        final OrderVO orderVO = new OrderVO();
        orderVO.setName(TEST_NAME);
        orderVO.setType(orderType.name());
        return orderVO;
    }

    @Nonnull
    @Override
    protected ModelConverter<OrderVO, OrderModel> createConverter() {
        return new OrderModelConverter(PARAMETERS);
    }


    private static List<ParameterModel> generateParameters() {
        return Arrays.stream(ObjectGenerationUtils.namesLegal())
                .map(ObjectGenerationUtils::createNamedParameter)
                .collect(Collectors.toList());
    }

    private OrderType randomOrderType() {
        final OrderType[] orderTypes = OrderType.values();
        final int orderTypeIndex = RandomUtils.nextInt(0, orderTypes.length);
        return orderTypes[orderTypeIndex];
    }

}
