const CategorizatorLocators = {
  SEARCH_FIELD: '//input[@placeholder="Поиск категории"]',
  SEARCH_RESULTS_LIST: '//div[@data-unstable-testid="SearchResults"] //span[text()="Подсказка"]',
  TOOLTIP_SEND_COMMENT: '//div[contains(text(),"Оставить комментарий")]',
  DISLIKE_BUTTON: '//button[@aria-label="dislike"]',
  DISLIKE_EXPAND_BUTTON: 'div[data-unstable-testid="FullScreen"] button[aria-label="dislike"]',
  LIKE_BUTTON: '//button[@aria-label="like"]',
  LIKE_EXPAND_BUTTON: 'div[data-unstable-testid="FullScreen"] button[aria-label="like"]',
  EXPAND_BUTTON: 'button[aria-label="expand"]',

  //Requests - Заявка
  REQUEST_BUTTON: '//button[text()="Заявка"]', //кнопка Заявка
  REQUEST_SELECT_CATEGORY: '//span[text()="не выбрана"]', //ссылка "не выбрана"
  REQUEST_FIND_CATEGORY: 'div[data-testid="Modal"] input[data-testid="TextInput"]', //поле ввода названия категории для ее поиска
  REQUEST_CATEGORY_WINDOW: 'div[data-unstable-testid="Body"]', //модальное окно с категориями
  REQUEST_CATEGORY_ACCOUNTS: '//span[contains(text(),"Аккаунты")]', //категория "Для заявок" -> "Аккаунты"
  REQUEST_SAVE_BUTTON:
    'div[class="Modal-Content"] div[data-unstable-testid="Toolbar"] button[aria-disabled="false"]', //кнопка Сохранить
  REQUEST_2LINE_FORM: '//iframe[@title="cf"]', //окно создания заявки на 2 линию
  REQUEST_TICKET_BODY: 'textarea[name="answer_long_text_96199"]', //блок "Суть вопроса" в заявке
  REQUEST_SEND_BUTTON: 'button[title="Отправить"]', //кнопка Отправить в форме создания заявки на 2 линию
  REQUEST_NOTIFICATIONS: '//span[contains(text(),"Оповещения")]', //блок Оповещения внизу справа (вынести в общие локаторы)
  REQUEST_POPUP: '//div[contains(text(),"Создан новый тикет:")]', //поп-ап о создании тикета
  REQUEST_POPUP_OPEN_BUTTON: '//button[contains(text(),"Открыть")]', //кнопка Открыть внутри поп-апа
  REQUEST_OPEN_CREATED_2LINE_TICKET:
    '//div[contains(text(),"Суть вопроса: test Ticket 2line creation")]', //созданный тикет содержит текст "Суть вопроса: test Ticket 2line creation"
  REQUEST_2LINE_TICKET_LINE: '//div[contains(text(),"Отдел CRM (тест). Second line")]', //значение поля Линия в созданном тикете
};

module.exports = {
  CategorizatorLocators,
};
