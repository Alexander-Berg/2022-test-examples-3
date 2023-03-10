// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/logging/testopithecus-registry.ts >>>

import Foundation

open class TestopithecusRegistry {
  public static var timeProvider: TimeProvider = NativeTimeProvider()
  private static var eventReporterInstance: EventReporter = EmptyEventReporter()
  private static var aggregatorProviderInstance: AggregatorProvider = MapAggregatorProvider()
  open class func setEventReporter(_ reporter: EventReporter) {
    eventReporterInstance = reporter
  }

  @discardableResult
  open class func eventReporter() -> EventReporter {
    return eventReporterInstance
  }

  open class func setAggregatorProvider(_ provider: AggregatorProvider) {
    aggregatorProviderInstance = provider
  }

  @discardableResult
  open class func aggregatorProvider() -> AggregatorProvider {
    return aggregatorProviderInstance
  }
}
