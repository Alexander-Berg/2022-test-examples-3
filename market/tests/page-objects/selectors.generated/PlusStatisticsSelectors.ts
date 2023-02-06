export class PlusStatisticsSelectors {
  // плейсхолдер если ресторан не в Плюсе
  static placeholder = '[data-testid="plusStatistics__placeholder"]'
  // виджет статистики (линейная с дельтой)
  static lineWithDelta = '[data-testid="plusStatistics__line-with-delta"]'
  // виджет статистики (линейная)
  static line = '[data-testid="plusStatistics__line"]'
  // виджет статистики (bar или bar_stacked)
  static(widgetChart_type: string | number) {
    return `[data-testid="plusStatistics__${widgetChart_type}"]`
  }
}
