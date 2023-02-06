package ru.yandex.market.jmf.utils.test.impl;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.test.context.event.BeforeTestMethodEvent;
import org.springframework.test.context.event.annotation.AfterTestMethod;
import org.springframework.test.context.event.annotation.BeforeTestMethod;

import ru.yandex.market.crm.util.Dates;
import ru.yandex.market.jmf.time.AllowDirectClockSetting;
import ru.yandex.market.jmf.time.Now;
import ru.yandex.market.jmf.utils.test.time.FixedTime;

@Component
public class NowAnnotationEventListener {
    private final AtomicReference<Clock> clockRef = new AtomicReference<>();

    @BeforeTestMethod
    @AllowDirectClockSetting
    public void beforeTestMethod(BeforeTestMethodEvent event) {
        FixedTime fixedTime = Optional.ofNullable(AnnotationUtils.findAnnotation(
                        event.getTestContext().getTestMethod(), FixedTime.class
                ))
                .orElse(AnnotationUtils.findAnnotation(event.getTestContext().getTestClass(), FixedTime.class));
        if (fixedTime != null) {
            OffsetDateTime offsetDateTime = Dates.parseDateTime(
                    (String) AnnotationUtils.getValue(fixedTime, "time")
            );
            if (!clockRef.compareAndSet(null, Now.clock())) {
                throw new IllegalStateException("Clock was already changed");
            }
            Now.setClock(Clock.fixed(offsetDateTime.toInstant(), Now.TZ_ID));
        }
    }

    @AfterTestMethod
    @AllowDirectClockSetting
    public void afterTestMethod() {
        Clock clock = clockRef.get();
        if (clock != null) {
            Now.setClock(clock);
            clockRef.set(null);
        }
    }
}
