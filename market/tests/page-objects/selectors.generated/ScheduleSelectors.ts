export class ScheduleSelectors {
  // Удаление периода основного графика
  static removeWeekBtn = '[data-testid="schedule__remove-week-btn"]'
  // Контейнер содержащий селектор дней и времени
  static periodSelectorsContainer = '[data-testid="schedule__period-selectors-container"]'
  // Кнопка "Добавить еще дни"
  static addMoreDaysBtn = '[data-testid="schedule__add-more-days-btn"]'
  // Кнопка сохранить
  static buttonSave = '[data-testid="schedule__button-save"]'
  // Кнопка "Закрыть" в календаре выбора периода дат
  static datePeriodModalClose = '[data-testid="schedule__date-period-modal-close"]'
  // Кнопка "Ок" в календаре выбора периода дат
  static datePeriodModalOk = '[data-testid="schedule__date-period-modal-ok"]'
  // Кнопка сохранить в модалке редактирования основного графика работы
  static saveHolidaySchedule = '[data-testid="schedule__save-holiday-schedule"]'
  // Кнопка сохранить в модалке редактирования основного графика работы
  static saveWorkdaysSchedule = '[data-testid="schedule__save-workdays-schedule"]'
  // Кнопка "Выберите дни"
  static dayRangePicker = '[data-testid="schedule__day-range-picker"]'
  // Checkbox работаем круглосуточно
  static work24 = '[data-testid="schedule__work-24"]'
  // Checkbox не работаем весь день
  static notWork = '[data-testid="schedule__not-work"]'
  // Кнопка открывающая tooltip для выбора диапазона времени
  static timeRangeTooltip = '[data-testid="schedule__time-range-tooltip"]'
  // Инпут врмени от
  static timeFrom = '[data-testid="schedule__time-from"]'
  // Инпут времени до
  static timeTo = '[data-testid="schedule__time-to"]'
  // Селект выбора дней основного графика работы
  static weekSelector = '[data-testid="schedule__week-selector"]'
  // Опции селектора дней
  static weekSelectorOption(item: string | number) {
    return `[data-testid="schedule__week-selector-option-${item}"]`
  }
}
