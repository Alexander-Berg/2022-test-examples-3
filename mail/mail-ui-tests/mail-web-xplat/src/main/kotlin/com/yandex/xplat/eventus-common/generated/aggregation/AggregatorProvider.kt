// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM aggregation/aggregator-provider.ts >>>

package com.yandex.xplat.eventus.common

public interface AggregatorProvider {
    fun getAggregator(): Aggregator
    fun updateAggregator(eventName: EventusEvent): Boolean
}

public open class EmptyAggregatorProvider: AggregatorProvider {
    open override fun getAggregator(): Aggregator {
        return EmptyAggregator()
    }

    open override fun updateAggregator(_eventName: EventusEvent): Boolean {
        return false
    }

}

