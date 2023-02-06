package ru.yandex.travel.orders.entities;

import java.util.EnumSet;

import lombok.Value;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.travel.orders.commons.proto.EDisplayOrderType;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class UserOrderCounterTest {

    @RunWith(Parameterized.class)
    @Value
    public static class UnknownTypeTest {

        private EDisplayOrderType type;

        private UserOrderCounter counter = UserOrderCounter.initForUser(1L);

        @Parameterized.Parameters(name = "{0}")
        public static EnumSet<EDisplayOrderType> unknownTypes() {
            return EnumSet.of(
                    EDisplayOrderType.DT_UNKNOWN,
                    EDisplayOrderType.UNRECOGNIZED
            );
        }

        @Test(expected = RuntimeException.class)
        public void throwsErrorForUnknownTypesOnConfirmed() {
            counter.orderConfirmed(type);
        }

        @Test(expected = RuntimeException.class)
        public void throwsErrorForUnknownTypesOnRefunded() {
            counter.orderRefunded(type);
        }

        @Test(expected = RuntimeException.class)
        public void throwsErrorForUnknownTypesOnGet() {
            counter.getConfirmedOrders(type);
        }
    }

    @RunWith(Parameterized.class)
    @Value
    public static class KnownTypeTest {

        private EDisplayOrderType type;

        private UserOrderCounter counter = UserOrderCounter.initForUser(1L);

        @Parameterized.Parameters(name = "{0}")
        public static EnumSet<EDisplayOrderType> knownTypes() {
            return EnumSet.complementOf(UnknownTypeTest.unknownTypes());
        }

        @Test
        public void handlesKnownTypeCorrectly() {
            counter.orderConfirmed(type);
            assertEquals(Integer.valueOf(1), counter.getConfirmedOrders(type));
            counter.orderConfirmed(type);
            assertEquals(Integer.valueOf(2), counter.getConfirmedOrders(type));
            counter.orderRefunded(type);
            assertEquals(Integer.valueOf(1), counter.getConfirmedOrders(type));
        }
    }
}
