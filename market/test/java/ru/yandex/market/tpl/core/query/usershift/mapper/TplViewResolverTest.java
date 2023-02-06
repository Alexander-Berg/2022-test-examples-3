package ru.yandex.market.tpl.core.query.usershift.mapper;

import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.usershift.TestUserShiftFactory;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class TplViewResolverTest {

    private final TplViewResolver tplViewResolver = new TplViewResolver();

    public static final long EXISTED_MOVEMENT_ID = 1L;

    @Test
    void resolveOrdinalNumbers() {
        //given

        int expectedOrdinalNumber = 123;
        UserShift userShift = TestUserShiftFactory.buildWithDropOffTask(EXISTED_MOVEMENT_ID);
        userShift.streamLockerDeliveryTasks()
                .forEach(task -> task.setOrdinalNumber(expectedOrdinalNumber));

        //when
        TplViewResolver.OrdinalNumber ordinalNumber = tplViewResolver.resolveOrdinalNumbers(userShift);

        //then
        assertTrue(ordinalNumber.getForMovements().containsKey(EXISTED_MOVEMENT_ID));
        assertEquals(expectedOrdinalNumber, ordinalNumber.getForMovements().get(EXISTED_MOVEMENT_ID));
    }
}
