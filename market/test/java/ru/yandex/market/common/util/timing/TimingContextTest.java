package ru.yandex.market.common.util.timing;

import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.yt.utils.JsonUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class TimingContextTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimingContextTest.class);

    @Test
    void testObjects() {
        var ctx = new TimingContext();
        var total = ctx.getTotal();
        var t = ctx.getTiming("total");
        assertNotSame(total, t);
        assertSame(t, ctx.getTiming("total"));
        assertNotSame(t, ctx.getTiming("total2"));

        var ch = ctx.getGroup("total");
        assertSame(ch, ctx.getGroup("total"));
        assertNotSame(ch, ctx.getGroup("total2"));
        assertNotSame(t, ch.getTiming("total"));
    }

    @Test
    void testMerge() {
        testMergeImpl(TimingContext::mergeFrom);
    }

    //CHECKSTYLE:OFF
    void testMergeImpl(BiConsumer<TimingContext, TimingContext> impl) {
        var ctx = new TimingContext();
        {
            {
                var t = ctx.getTotal();
                t.addRowCount(55);
                t.addActiveTime(56);
                t.addWaitingTime(57);
            }
            {
                var t1 = ctx.getTiming("t1");
                t1.addRowCount(1);
                t1.addActiveTime(2);
                t1.addWaitingTime(3);
            }

            {
                var t2 = ctx.getTiming("t2");
                t2.addRowCount(3);
                t2.addActiveTime(4);
                t2.addActiveTime(5);
                t2.addWaitingTime(6);
                t2.addWaitingTime(7);
            }

            {
                var ctxC1 = ctx.getGroup("c1");
                var t3 = ctxC1.getTiming("t3");
                t3.addRowCount(10);
                t3.addActiveTime(20);
                t3.addWaitingTime(30);
            }


            {
                var ctxC2 = ctx.getGroup("c2");
                var t4 = ctxC2.getTiming("t4");
                t4.addRowCount(100);
                t4.addActiveTime(200);
                t4.addWaitingTime(300);
            }

        }

        var otherCtx = new TimingContext();
        {
            {
                var t = otherCtx.getTotal();
                t.addRowCount(155);
                t.addActiveTime(156);
                t.addWaitingTime(157);
            }
            {
                var t2 = otherCtx.getTiming("t2");
                t2.addRowCount(15);
                t2.addActiveTime(16);
                t2.addWaitingTime(17);
            }
            {
                var t3 = otherCtx.getTiming("t3");
                t3.addRowCount(18);
                t3.addActiveTime(19);
                t3.addWaitingTime(20);
            }


            {
                var otherCtxC1 = otherCtx.getGroup("c1");
                var t3 = otherCtxC1.getTiming("t3");
                t3.addRowCount(11);
                t3.addActiveTime(21);
                t3.addWaitingTime(31);
            }

            {
                var otherCtxC3 = otherCtx.getGroup("c3");
                var t3 = otherCtxC3.getTiming("t3");
                t3.addRowCount(101);
                t3.addActiveTime(201);
                t3.addWaitingTime(301);
            }
        }

        LOGGER.info("{}", JsonUtils.toJsonString(ctx));
        LOGGER.info("{}", JsonUtils.toJsonString(otherCtx));

        impl.accept(ctx, otherCtx);
        LOGGER.info("{}", JsonUtils.toJsonString(ctx));


        var target = new TimingContext();
        {
            {
                var t = target.getTotal();
                t.addRowCount(55);
                t.addActiveTime(56);
                t.addWaitingTime(57);
                t.addRowCount(155);
                t.addActiveTime(156);
                t.addWaitingTime(157);
            }
            {
                var t1 = target.getTiming("t1");
                t1.addRowCount(1);
                t1.addActiveTime(2);
                t1.addWaitingTime(3);
            }

            {
                var t2 = target.getTiming("t2");
                t2.addRowCount(3);
                t2.addActiveTime(4);
                t2.addActiveTime(5);
                t2.addWaitingTime(6);
                t2.addWaitingTime(7);
                t2.addRowCount(15);
                t2.addActiveTime(16);
                t2.addWaitingTime(17);
            }

            {
                var t3 = target.getTiming("t3");
                t3.addRowCount(18);
                t3.addActiveTime(19);
                t3.addWaitingTime(20);
            }

            {
                var ctxC1 = target.getGroup("c1");
                var t3 = ctxC1.getTiming("t3");
                t3.addRowCount(10);
                t3.addActiveTime(20);
                t3.addWaitingTime(30);
                t3.addRowCount(11);
                t3.addActiveTime(21);
                t3.addWaitingTime(31);
            }


            {
                var ctxC2 = target.getGroup("c2");
                var t4 = ctxC2.getTiming("t4");
                t4.addRowCount(100);
                t4.addActiveTime(200);
                t4.addWaitingTime(300);
            }

            {
                var otherCtxC3 = target.getGroup("c3");
                var t3 = otherCtxC3.getTiming("t3");
                t3.addRowCount(101);
                t3.addActiveTime(201);
                t3.addWaitingTime(301);
            }
        }

        LOGGER.info("{}", JsonUtils.toJsonString(target));
        assertEquals(JsonUtils.toJsonString(target), JsonUtils.toJsonString(ctx));
        assertEquals(target, ctx);
    }
    //CHECKSTYLE:ON

}
