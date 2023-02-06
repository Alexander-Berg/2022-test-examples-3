import { AggregatorProvider, MapAggregatorProvider } from './agregation/aggregator-provider';
import { EmptyEventReporter, EventReporter } from './event-reporter';
import { NativeTimeProvider, TimeProvider } from './time-provider'

/**
 * Статический класс для настроек стандартных настроек. Поддерживаемые настройки:
 * <ul>
 *   <li>Обработчик событий {@link eventReporter}</li>
 *   <li>Агрегатор {@link aggregatorProvider}</li>
 * </ul>
 *
 * Для настройки логирования в приложении необходимо установить корректный {@link EventReporter}
 */
export class TestopithecusRegistry {
  public static timeProvider: TimeProvider = new NativeTimeProvider()
  private static eventReporterInstance: EventReporter = new EmptyEventReporter()
  private static aggregatorProviderInstance: AggregatorProvider = new MapAggregatorProvider()

  /**
   * Устанавливает новый обработчик событий по умолчанию
   *
   * @param reporter Новый обработчик событий
   */
  public static setEventReporter(reporter: EventReporter): void {
    this.eventReporterInstance = reporter
  }

  /**
   * Возвращает текущий испоьлзуемый обработчик
   */
  public static eventReporter(): EventReporter {
    return this.eventReporterInstance;
  }

  public static setAggregatorProvider(provider: AggregatorProvider): void {
    this.aggregatorProviderInstance = provider
  }

  /**
   * Возвращает текущий испоьлзуемый агрегатор (не используется)
   */
  public static aggregatorProvider(): AggregatorProvider {
    return this.aggregatorProviderInstance;
  }

}
