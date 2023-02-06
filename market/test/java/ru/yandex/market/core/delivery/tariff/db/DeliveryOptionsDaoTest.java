package ru.yandex.market.core.delivery.tariff.db;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.tariff.db.dao.DeliveryOptionsDao;
import ru.yandex.market.core.delivery.tariff.model.DeliveryOption;
import ru.yandex.market.core.delivery.tariff.model.OptionGroup;

import static org.junit.Assert.assertEquals;

/**
 * Тесты для {@link DeliveryOptionsDao}.
 *
 * @author Vladislav Bauer
 */
@DbUnitDataSet(before = "DeliveryTariffDaoTest.before.csv")
class DeliveryOptionsDaoTest extends FunctionalTest {

    private static final long REGION_GROUP_ID2 = 102;

    @Autowired
    private DeliveryOptionsDao deliveryOptionsDao;

    @Test
    void testGetDeliveryOptions() {
        final OptionGroup expectedGroup = new OptionGroup(1L, REGION_GROUP_ID2, (short) 1, null, null);
        final Collection<DeliveryOption> expectedOptions = expectedGroup.getOptions();
        expectedOptions.add(new DeliveryOption(1L, (short) 1, BigDecimal.valueOf(10000, 2), (short) 0, (short) 0, (byte) 15));
        expectedOptions.add(new DeliveryOption(1L, (short) 2, BigDecimal.valueOf(0, 2), (short) 1, (short) 7, (byte) 15));

        final List<OptionGroup> expected = new ArrayList<>();
        expected.add(expectedGroup);

        final List<OptionGroup> options = deliveryOptionsDao.getOptions(REGION_GROUP_ID2);
        assertEquals(expected, options);
        assertEquals(expected.get(0).getOptions(), options.get(0).getOptions());
    }

}
