// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM event-reporter.ts >>>

package com.yandex.xplat.eventus.common

public interface EventReporter {
    fun report(event: LoggingEvent): Unit
}

public open class EmptyEventReporter: EventReporter {
    open override fun report(_event: LoggingEvent): Unit {
    }

}
