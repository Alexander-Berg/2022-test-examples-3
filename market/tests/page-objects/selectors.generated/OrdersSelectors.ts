export class OrdersSelectors {
  // Информация о клиенте
  static orderClientInfo = '[data-testid="orders__order-client-info"]'
  // Адрес клиента
  static orderClientAddress = '[data-testid="orders__order-client-address"]'
  // Кнопка управления картой
  static orderMapButton = '[data-testid="orders__order-map-button"]'
  // Оплата:
  static orderPaymentInfo = '[data-testid="orders__order-payment-info"]'
  // Позиция в составе заказа
  static orderPositionRoot = '[data-testid="orders__order-position-root"]'
  // Наименование позиции в составе заказа
  static orderPositionName = '[data-testid="orders__order-position-name"]'
  // Стоимость доставки
  static orderDeliveryCost = '[data-testid="orders__order-delivery-cost"]'
  // Итого по заказу
  static orderTotal = '[data-testid="orders__order-total"]'
  // Бейдж со статусом заказа
  static orderBadgeStatus = '[data-testid="orders__order-badge-status"]'
  // Бейдж со типом оплаты/компенсации за заказ
  static orderBadgeCompensationType = '[data-testid="orders__order-badge-compensation-type"]'
  // Бейдж с типом доставки
  static orderBadgeDeliveryType = '[data-testid="orders__order-badge-delivery-type"]'
  // Кол-во персон:
  static orderPersonsBadge = '[data-testid="orders__order-persons-badge"]'
  // Комментарий к заказу
  static orderComment = '[data-testid="orders__order-comment"]'
  // Приготовить к:
  static orderPrepareFor = '[data-testid="orders__order-prepare-for"]'
  // Доставить к
  static orderDeliveryFor = '[data-testid="orders__order-delivery-for"]'
  // Принято:
  static orderConfirmationTime = '[data-testid="orders__order-confirmation-time"]'
  // Дополнительные действия
  static orderUtilityActions = '[data-testid="orders__order-utility-actions"]'
  // Кнопка создания обращения в чатах
  static orderSupportChatButton = '[data-testid="orders__order-support-chat-button"]'
  // Кнопка "Отменить заказ" в модалке отмены заказа
  static confirmCancelButton = '[data-testid="orders__confirm-cancel-button"]'
  // Заказ из списка
  static listItem = '[data-testid="orders__list-item"]'
  // Заголовок заказа в списке
  static listItemTitle = '[data-testid="orders__list-item-title"]'
  // Статус заказа в списке
  static listItemStatus = '[data-testid="orders__list-item-status"]'
  // Номер заказа в списке
  static listItemId = '[data-testid="orders__list-item-id"]'
  // Тип доставки в списке
  static listItemDelivery = '[data-testid="orders__list-item-delivery"]'
  // Таймер заказа в списке
  static listItemProgress = '[data-testid="orders__list-item-progress"]'
  // Заказы в работе
  static listWip = '[data-testid="orders__list-wip"]'
  // Выполненные заказы
  static listCompleted = '[data-testid="orders__list-completed"]'
  // Страница заказов
  static page = '[data-testid="orders__page"]'
  // Кнопка перехода по статусу"
  static buttonPrimaryAction = '[data-testid="orders__button-primary-action"]'
  // Текст кнопки перехода по статусу"
  static buttonPrimaryActionText = '[data-testid="orders__button-primary-action-text"]'
  // Кнопка "Отменить заказ"
  static buttonCancel = '[data-testid="orders__button-cancel"]'
  // Заказ
  static orderRoot = '[data-testid="orders__order-root"]'
  // Номер заказ
  static orderId = '[data-testid="orders__order-id"]'
  // Название ресторана
  static orderRestaurantTitle = '[data-testid="orders__order-restaurant-title"]'
  // Виджет с причиной отмены заказа
  static cancellationReasonWidget = '[data-testid="orders__cancellation-reason-widget"]'
  // Звук отмены заказа
  static cancelOrderAlarm = '[data-testid="orders__cancel-order-alarm"]'
  // Звук изменения заказа
  static changeOrderAlarm = '[data-testid="orders__change-order-alarm"]'
  // Звук нового заказа
  static newOrderAlarm = '[data-testid="orders__new-order-alarm"]'
  // Значение свойства
  static orderPropertyValue = '[data-testid="orders__order-property-value"]'
}
