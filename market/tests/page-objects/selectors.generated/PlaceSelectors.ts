export class PlaceSelectors {
  // Кнопка "Включить" ресторан
  static actionButtonEnable = '[data-testid="place__action-button-enable"]'
  // Кнопка "Подробнее" если выключен через автостоп
  static actionButtonLearnMore = '[data-testid="place__action-button-learn-more"]'
  // Кнопка "Отключить" ресторан
  static actionButtonDisable = '[data-testid="place__action-button-disable"]'
  // Плашка модерации о том, что нельзя изменять данные
  static infoModerationBanner = '[data-testid="place__info-moderation-banner"]'
  // Кнопка "Изменить" график работы в рабочие дни
  static infoScheduleEditWorkdaysBtn = '[data-testid="place__info-schedule-edit-workdays-btn"]'
  // Кнопка "Изменить" расписание праздничных дней в секции График работы
  static infoScheduleEditHolidayBtn = '[data-testid="place__info-schedule-edit-holiday-btn"]'
  // Иконка типа доставки. Самовывоз
  static deliveryIconPickup = '[data-testid="place__delivery-icon-pickup"]'
  // Иконка типа доставки. Доставка ресторана
  static deliveryIconMarketplace = '[data-testid="place__delivery-icon-marketplace"]'
  // Иконка типа доставки. Доставка Яндекса
  static deliveryIconNative = '[data-testid="place__delivery-icon-native"]'
  // Кнопка сохранить в модалке редактирования адреса
  static saveAddressBtn = '[data-testid="place__save-address-btn"]'
  // Кнопка Отмена редактирования адреса в модалке редактирования адреса
  static cancelEditAddressBtn = '[data-testid="place__cancel-edit-address-btn"]'
  // Кнопка сохранить в модалке редактирования номеров телефонов
  static savePhonesBtn = '[data-testid="place__save-phones-btn"]'
  // Контейнер с табами навигации на странице ресторана
  static navigationTabs = '[data-testid="place__navigation-tabs"]'
  // Таб "Рейтинг"
  static navigationTabsRating = '[data-testid="place__navigation-tabs-rating"]'
  // Таб "Отзывы"
  static navigationTabsReviews = '[data-testid="place__navigation-tabs-reviews"]'
  // Таб "Информация"
  static navigationTabsInfo = '[data-testid="place__navigation-tabs-info"]'
  // Селект фильтра по рейтингу
  static feedbackRatingFilter = '[data-testid="place__feedback_rating-filter"]'
  // Номер телефона в секции общая информация
  static infoPhoneNumber(title: string | number) {
    return `[data-testid="place__info-phone-number-${title}"]`
  }
  // Значение из секции общей информации о ресторане
  static commonInfoValue(title: string | number) {
    return `[data-testid="place__common-info-value-${title}"]`
  }
  // Кнопки "Изменить" для редактирования общей информации о ресторане
  static commonInfoBtnEdit(title: string | number) {
    return `[data-testid="place__common-info-btn-edit-${title}"]`
  }
  // Карточка ресторана в разделе "Рестораны"
  static card(placeSelfName: string | number) {
    return `[data-testid="place__card-${placeSelfName}"]`
  }
  // Поля телефонных номеров ресторана
  static phoneField(title: string | number) {
    return `[data-testid="place__phone-field-${title}"]`
  }
  // Поля комментариев к телефонам
  static phoneCommentField(title: string | number) {
    return `[data-testid="place__phone-comment-field-${title}"]`
  }
}
