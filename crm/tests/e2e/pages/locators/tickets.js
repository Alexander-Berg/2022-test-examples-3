const TicketsLocators = {
  TICKET_MODULE: 'a[title="Тикеты"]', // кнопка модуля Тикеты
  TEST_QUEUE: '//div[@data-unstable-testid="Filter"] //span[text()="Отдел CRM (тест).Autotest"]', // общий фильтр очереди "Отдел CRM (тест).Autotest"
  ACTIONS_IN_TICKET: '//button[contains(., "Действия")]', // кнопка Действия в старой шапке тикета
  ACTION_ADD_LINK: './/span[text()="Добавить связь"]', // пункт меню Добавить связь в Действиях в старой шапке тикета
  CREATE_TICKET: '[data-testid="issues-list/buttons"] button:last-of-type', // кнопка создания тикета (плюсик)
  CREATE_TICKET_FORM: 'div[data-testid="Modal"] [role="dialog"]', // модальное окно создания тикета
  CRM_TICKET_KEY_TO_LINK: 'input[name="targetId"]', // поле ввода Тикет на форме добавления связи к тикету
  SAVE_LINK_TO_CRM_TICKET: 'div[data-testid="Modal"] button[type="submit"]', // кнопка Сохранить на форме добавления связи к тикету
  UNLINK_CRM_TICKET: 'button[title="Отвязать"]', // кнопка отвязывания тикета на вкладке Связанные
  TICKET_CURRENT_TAB: '//button //span[text()="Текущий"]', // таб Текущий в тикете
  TICKET_HISTORY_TAB: 'button[data-testid="history-action-tab"]', // таб История изменений в тикете
  TICKET_HISTORY: 'iframe[title="История задача"]', // iframe с историей изменения
  TICKET_HISTORY_TABLE: 'tbody', // содержимое таблицы в истории изменений
  TICKET_HISTORY_TABLE_FIRST_ROW: 'tbody tr:first-child', // первая строка в истории изменений
  TICKET_HISTORY_TABLE_ROW: 'tbody tr', // любая строка в истории изменений
  ITS_A_TICKET:
    '//div[@data-testid="issue"] //div[@data-unstable-testid="IssueFieldLayout"] //div[text()="Тикет"]', //слово Тикет в шапке тикета

  TICKET_SORT_BUTTON_VALUE: 'div[data-unstable-testid="IssueSort"] span[class="Button2-Text"]', // текст на кнопке сортировки тикетов

  TICKET_ATTRIBUTE_KEY_VALUE: 'div[aria-label="reading area"]', // блок Название атрибута + Значение атрибута
  TICKET_ATTRIBUTE_LABEL: 'div[aria-label="label"] span', // Название атрибута
  TICKET_ATTRIBUTE_VALUE: 'div[aria-label="value"] span', // Значение атрибута

  TICKET_ASSIGNEE_ATTRIBUTE: 'div[data-testid="attribute_owner"]:not([hidden])', // атрибут Исполнитель (т.е. блок с названием и значением)
  TICKET_ASSIGNEE_POPUP:
    'div[data-testid="attribute_owner"]:not([hidden]) div[data-testid="Popup"]', // попап со списком возможных исполнителей
  TICKET_ASSIGNEE_INPUT:
    'div[data-testid="attribute_owner"]:not([hidden]) [contenteditable="true"]', // поле ввода Исполнителя (если в нем уже есть какое-то значение и нужно его изменить)
  TICKET_ASSIGNEE_LIST_POPUP:
    '//div[@data-testid="attribute_owner"] //div[@data-testid="Popup"] //div[@data-key="item-0"]', //первая строка в списке исполнителей
  TICKET_ASSIGNEE_VALUE: 'div[data-testid="attribute_owner"]:not([hidden]) div[aria-label="value"]', // значение поля Исполнитель
  TICKET_HOURS_AMOUNT: 'div[data-testid="attribute_standardHours"]:not([hidden])', // атрибут Количество часов (т.е. блок с названием и значением)
  TICKET_HOURS_AMOUNT_INPUT:
    'div[data-testid="attribute_standardHours"]:not([hidden]) [placeholder="Количество часов"]', // поле ввода Количества часов
  TICKET_TAGS_INPUT:
    'div[data-testid="attribute_tags"]:not([hidden]) [data-unstable-testid="ContentEditable"]', // поле ввода меток
  TICKET_DEADLINE_VALUE:
    'div[data-testid="attribute_deadlineDt"]:not([hidden]) div[aria-label="value"]', // значение поля Дедлайн

  TICKET_LIST_BLOCK_NAME:
    'div[data-unstable-testid="IssueList"] div[class="e3y6ztdqP7tS1GzpuAY2h _1siXrhTYH-QpNL0S5T-zc8"] span[data-unstable-testid="PreviewValue"]', //названия задач в списке задач

  TAKE_NEXT_TICKET: '//button[contains(@title, "Доступных тикетов в очереди")]', // кнопка "Взять следующий"
  SAVE_TICKET: '[data-testid="Modal"] button[type="submit"]', // кнопка Сохранить в форме создания нового тикета
  LATEST_CREATED_TICKET:
    'div[data-testid="InfiniteVirtualList"] div[data-unstable-testid="IssueList"]:first-child', // верхний тикет в списке
  LATEST_CREATED_TICKET_SUBJECT:
    'div[data-unstable-testid="IssueList"]:first-child [class="e3y6ztdqP7tS1GzpuAY2h _1siXrhTYH-QpNL0S5T-zc8"] span[data-unstable-testid="PreviewValue"]', // название верхнего тикета в списке
  REOPEN_INPROGRESS_POSTPONE_TICKET: 'button[name="actionStateChange"]', // кнопка следующего возможного статуса в тикете (например, если тикет в статусе Отложен, то это будет селектор для кнопки Открыть)
  CLOSE_TICKET: '[data-unstable-testid="ButtonGroup"] button:not([name])', // кнопка Закрыть в шапке тикета
  RESOLUTION_RESOLVED: './/span[text()="Решен"]', // резолюция Решен при закрытии тикета
  RESOLUTION_SPAM: './/span[text()="Спам"]', // резолюция Спам при закрытии тикета
  GO_TO_IN_PROGRESS_FILTER: './/span[text()="В работе"]', // фильтр "В работе" в папке "Мои - по статусам"

  SUMMARY_FOR_NEW_TICKET:
    '[data-testid="Modal"] [data-unstable-testid="Row"]:nth-of-type(1) [data-testid="TextInput"]', // поле ввода Тема на форме создания тикета
  QUEUE_FOR_NEW_TICKET:
    '[data-testid="Modal"] [data-unstable-testid="Row"]:nth-of-type(2) [data-testid="TextInput"]', // поле ввода Очередь на форме создания тикета
  SELECT_ACCOUNT_FOR_NEW_TICKET:
    '[data-testid="Modal"] [data-unstable-testid="Row"]:nth-of-type(3) button', // кнопка выбора аккаунта на форме создания тикета
  SAVE_ACCOUNT_BUTTON_FOR_NEW_TICKET:
    '[data-testid="Modal"] [data-unstable-testid="Toolbar"] button[aria-disabled="false"]', // активная кнопка сохранения аккаунта в форме выбора аккаунта
  OPTION_FOR_QUEUE: '[data-testid="Popup"] [role="listbox"]', // выпадающий список доступных очередей
  SELECT_CATEGORY_FOR_NEW_TICKET:
    '[data-testid="Modal"] [data-unstable-testid="Row"]:nth-of-type(4) button', // кнопка выбора категории на форме создания тикета
  SEARCH_CATEGORY_INPUT: '[data-unstable-testid="Toolbar"] input[placeholder="Поиск"]', // поле ввода Поиск на форме поиска категории
  TEST_CATEGORY: './/span[text()="Причина 1"]', // категория с названием "Причина 1" в списке
  ZERO_ACCOUNT_CHECKBOX: '[data-testid="Modal"] .Checkbox-Box', // чекбокс Нулевой клиент на форме поиска аккаунта

  //в зависимости от доступа робота к кнопкам значения в nth-child могут меняться
  QUEUE_IN_TICKET: '[data-testid="attribute_queue"] [aria-label="value"] span', //значение поля Очередь в атрибутах тикета
  ACCOUNT_IN_TICKET: '[data-testid="attribute_account"] [aria-label="value"] span', //значение поля Аккаунт в атрибутах тикета
  LINE_IN_TICKET: '[data-testid="attribute_ticketLine"] [aria-label="value"] span', //значение поля Линия в атрибутах тикета
  AUTHOR_IN_TICKET: '[data-testid="attribute_author"] [aria-label="value"] span', //значение поля Автор в атрибутах тикета
  PRIORITY_IN_TICKET: '[data-testid="attribute_priority"] [aria-label="value"] span', //значение поля Приоритет в атрибутах тикета
  COMM_TYPE_IN_TICKET: '[data-testid="attribute_communicationTypeId"] [aria-label="value"] span', //значение поля Тип коммуникации в атрибутах тикета

  CATEGORY_FIELD: '//div[@data-testid="attribute_category"] //span[text()="Категория"]', // поле Категория в атрибутах тикета
  //REMOVE_CATEGORY: '.Button2_pin_clear-round', // этой кнопки уже нет, нужно переписать тесты с ее использованием
  //SELECT_NEW_CATEGORY: 'form[action="#"] .Button2_width_auto', // используется в 150м тесте, а его нужно переписать
  //CATEGORY_FOR_CHANGE: './/span[text()="Причина autotest"]', // используется в 150м тесте, а его нужно переписать
  //SAVE_CATEGORY: 'form[action="#"] button[type="submit"]', // используется в 150м тесте, а его нужно переписать

  FOLLOWERS_FIELD: './/span[text()="Наблюдатели"]', // атрибут Наблюдатели
  FOLLOWERS_INPUT: 'div[placeholder="Наблюдатели"]', // поле ввода в атрибуте Наблюдатели
  ROBOT_FOLLOWER: 'img[src="//center.yandex-team.ru/api/v1/user/robot-crmcrown/avatar/64.jpg"]', // аватарка робота Crmcrown robot
  DELETE_FOLLOWER_FROM_ISSUE: 'div[data-testid="attribute_followers"] button[aria-label="remove"]', // крестик удаления наблюдателя
  ROBOT_TITLE_IN_SUGGEST: './/span[text()="Роботы CRM Space"]', // роботы CRM в видимом списке
  CRMCROWN_ROBOT_FOLLOWER: '//span[text()="Crmcrown Robot"]', // робот Crmcrown Robot

  LINK_ST_TICKET_BUTTON:
    '[data-testid="issue-header"] [data-unstable-testid="IssueFieldLayout"] [data-unstable-testid="ButtonGroup"] button[name="quickPanel"]:last-of-type', //Кнопка привязать ст тикет (в шапке)
  ST_TICKET_TO_LINK_INPUT:
    '[data-testid="Modal"] form[data-unstable-testid="ConnectTicketForm"] input', //поле ввода номера старт трек тикета

  WRITE_COMMENT_TO_TICKET:
    '//span[@data-unstable-testid="ToggleLayout"][contains(.,"Написать комментарий")]', // кнопка Написать комментарий в тикете
  COMMENT_FIELD_TO_TICKET: '[data-testid="Textarea"]', // поле ввода комментария
  SUBMIT_COMMENT_TO_TICKET: 'button[type="submit"]:nth-child(1)', // кнопка сохранения комментария
  DELETE_COMMENT_TO_TICKET: '[data-unstable-testid="Toolbar"] button:last-of-type', // кнопка удаления комментария
  COMMENT_DESCRIPTION_BODY:
    '[data-unstable-testid="Comment"] [data-unstable-testid="YfmFormatter"]', // текст комментария

  WRITE_MAIL_TO_TICKET: './/span[text()="Написать письмо"]', //кнопка "Написать письмо" в тикете
  WRITE_MAIL_FORM_TO_TICKET: 'div[value="mail"]', //форма написания письма
  MAIL_BODY: '#cke_body [contenteditable="true"]', //тело письма
  WRITE_MAIL_FIELD_TO: 'input[name="to"][placeholder="Кому"]', // поле Кому в форме написания письма
  WRITE_MAIL_FIELD_THEME: 'input[name="subject"][placeholder="Тема"]', // поле Тема в форме написания письма
  WRITE_MAIL_SEND_BUTTON: 'div[data-unstable-testid="Left"] button[type="submit"]', // кнопка Отправить в форме написания письма

  DRAFT_SAVED_POPUP:
    '//div[@data-unstable-testid="NotificationItem"] //div[text()="Черновик сохранен"]', //попап "Черновик сохранен"
  DRAFT_GREY_BUTTON: '//div[@data-unstable-testid="TimelineItem"] //i[text()="mail"]', // иконка письма
  MAIL_PREVIEW: '//div[contains(@id,"Mail")]//div[@data-unstable-testid="MailIssue"]', //превью письма в таймлайне

  QUEUE_FIELD: '//div[@data-testid="attribute_queue"] //span[text()="Очередь"]', // атрибут Очередь
  //REMOVE_QUEUE_FROM_TICKET: 'button[aria-label="remove"]',
  QUEUE_FOR_CHANGE: './/span[text()="МКС: Смарт-баннеры"]', // очередь "МКС: Смарт-баннеры" в выпадающем списке доступных очередей
  //SAVE_QUEUE: 'form[action="#"] button[type="submit"]',
  QUEUE_INPUT: '[data-testid="attribute_queue"] [contenteditable="true"]', // поле ввода атрибута Очередь

  ADD_MARK_BUTTON: 'button[title="Добавить метку"]', // кнопка добавления метки
  NEW_MARK_BUTTON: '[data-testid="Popup"] [data-testid="Button"]', // кнопка Новая метка
  NEW_MARK_NAME: '#name', // поле Название метки
  NEW_MARK_ACCESS: 'input[placeholder="Доступы"]', // поле Доступы
  OPTION_FOR_ACCESS: '[data-testid="Popup"] [role="listbox"]', // список доступных пользователей в Метках
  NEW_MARK_GREEN_COLOUR: 'label[style="background-color: rgb(19, 102, 25);"]', // зеленый цвет метки
  SAVE_NEW_MARK: '[data-testid="Modal"] [data-unstable-testid="Toolbar"] button[type="submit"]', // кнопка сохранения метки
  DELETE_MARK_FROM_TICKET: '[data-unstable-testid="Tags"] [aria-label="remove"]', // кнопка удаления метки

  ASSIGNEE_FIELD: './/span[text()="Исполнитель"]', // название атрибута Исполнитель
  ASSIGNEE_INPUT: 'div[data-testid="attribute_owner"] [contenteditable="true"]', // поле ввода атрибута Исполнитель
  DELETE_ASSIGNEE_FROM_ISSUE: 'div[data-testid="attribute_owner"] button[aria-label="remove"]', // крестик удаления в атрибуте Исполнитель

  CREATE_ST_TICKET_BUTTON: '//button[contains(., "Создать задачу ST")]', //кнопка в шапке тикета
  ST_CREATE_NEW_TICKET_HEADER:
    '//div[@data-testid="modal-create-st-ticket"] //span[contains(.,"Создать новый тикет ST")]', // заголовок в модальном окне создания нового тикета ST
  ST_TICKET_QUEUE_FIELD:
    '[data-unstable-testid="Form"] [data-unstable-testid="Row"]:nth-of-type(1) input', // поле ввода Очереди
  ST_TICKET_FORM: '[data-unstable-testid="ConnectTicketForm"]', // форма привязки тикета ST
  ST_TICKET_QUEUE_TO_SELECT_CRMMY: '//div[@role="listbox"] //span[contains(.,"CRMMY")]', // очередь CRMMY в списке доступных
  ST_TICKET_NAME_INPUT: 'input[name="title"]', // поле ввода названия тикета
  ST_TICKET_TYPE: '[data-unstable-testid="Row"]:nth-of-type(2) span button', // выпадушка с типами тикета
  ST_TICKET_TYPE_TO_SELECT_TASK: '//div[@data-testid="Popup"] //span[text()="Задача"]', // тип Задача

  SAVE_ST_TICKET_BUTTON: '[data-testid="Modal"] button[type="submit"]', // кнопка Связать в модальном окне
  SAVE_ST_TICKET_BUTTON_MGT:
    '[data-testid="modal-create-st-ticket"] [data-unstable-testid="Toolbar"] button[type="submit"]', // кнопка Сохранить в модальном окне создания тикета в ST
  LINKED_TICKETS: '//div[@data-testid="issue-header-tabs"] //span[text()="Связанные"]', // таб Связанные
  LINKED_ST_TICKET: '[data-testid="linked-issues-tab"] [tabindex="-1"]', // слинкованный тикет на табе Связанные
  UNLINK_ST_TICKET: 'button[name="unlinkStartrek"]', // кнопка удаления связи на табе Связанные
  NO_LINKS_MSG: '//div[text()="Связей нет"]', // надпись "Связей нет"
  LINKED_ST_TICKET_CRMMY: '//div[@data-unstable-testid="TrackerKey"][contains(., "CRMMY")]', // очередь CRMMY в связанном тикете на табе Связанные
  //HEADER_ST_TICKET: '//div[class="_3T2z1yDe9qwMI7LhQX2-Qr"]',
  //ST_CREATED_INFORMATION: 'div[class="_1MEf8Mg9YlOv02MGFQlDRE"]',
  //ST_TICKET_NEW_BODY: 'div[class="_1-mc1HtinLUVey_dBr5_3V _2j_bSL6I7Yn0MBDPL29-hz"] div[class="WGbvIs46qeF0mKwMDzcCi"] span[class="Textarea-Box"]',
  //ST_TICKET_CREATED_BODY: 'div[class="WikiFormatter _1XOEt5YSjrks93Qx8Me5jw"]',
  //ST_TICKET_STATUS: 'div[class="_3HDr0vw8LDHfJ2-Y36gpgI"] div[class="e3y6ztdqP7tS1GzpuAY2h _1TeMqMVZfpY2RVa6KJI9Lb _3SBgKYnqhh6m90Ka-8m4hY U93xkwmiTALdgQ4_Mna8B _117iqhUM5Oh_dCw9mN7fnc"] span[class="_2SnUjnIPgFBVov6tPBP7dn"]',
  ST_TICKET_TITLE:
    '[data-unstable-testid="IssueListView"] div[class="e3y6ztdqP7tS1GzpuAY2h _1siXrhTYH-QpNL0S5T-zc8"] span[data-unstable-testid="PreviewValue"]', // название ST-тикета на табе Связанные

  CREATE_TIMER: 'button[title="Создать новый таймер"]', // кнопка создания таймера
  DATE_FOR_TIMER: 'input[dateformat="DD.MM.YYYY"]', // поле ввода даты таймера
  INPUT_FOR_PEOPLE_TO_NOTIFY: 'input[placeholder="Уведомить"]', // поле ввода Уведомить
  SAVE_TIMER: '[data-testid="Modal"] [data-unstable-testid="Toolbar"] button[type="submit"]', // кнопка сохранения таймера
  CREATED_TIMER: 'button[title="Таймеры"]', // установленный таймер в атрибутах
  TIMER_IN_ATTRIBUTES: './/span[text()="Таймер"]', // атрибут Таймер
  REMOVE_TIMER: 'div[data-testid="attribute_timers"] button[aria-label="remove"]', // крестик удаления в атрибуте Таймер
  TIMER_IN_TWO_DAYS: 'button[data-offset-days="2"]', // кнопка "Через 2 дня" при создании таймера
  TIMER_COMMENT: '#comment', // комментарий в Таймере
  EDIT_TIMER: 'div[data-testid="attribute_timers"] span[role="button"]', // блок, по нажатию которого открывается форма редактирования таймера

  ACTION_CREATE_2ND_TICKET: './/span[text()="Создать тикет на 2 линию"]', // кнопка создания тикета на 2 линию
  SELECT_CATEGORY_2ND_TICKET: './/span[text()="не выбрана"]', // не заполненное поле категории
  MAIN_IDEA_FOR_2ND_TICKET: 'form textarea[name="answer_long_text_96199"]', // поле ввода "Суть вопроса"
  SAVE_2ND_TICKET: 'button[title="Отправить"]', // кнопка Отправить в форме создания тикета на 2 линию
  THANX_FOR_2ND_TICKET: '//span[text()="Спасибо за ответ!"]', // попап после отправки
  BLOCKED_BY_2ND_TICKET: 'div [value="1"]', // блок связанных задач
  OPEN_2ND_TICKET: '//div[@value="1"]//div[text()="Тикет"]', // тикет из блока связанных задач
  CONTENT_IN_2ND_TICKET: '[data-testid="preview-issue"] [data-unstable-testid="YfmFormatter"] p', // содержимое тикета на 2 линию

  TAB_ATTRIBUTES_OPENED: '//span[@class="NkCXQUW-WmWuKrlvm5314"][contains(text(),"Атрибуты")]', // панель атрибутов
  ATTRIBUTE_CATEGORY: 'div[data-testid="attribute_category"]', // блок атрибута Категория
  SECOND_TICKET_IN_LIST:
    'div[data-testid="InfiniteVirtualList"] div[data-unstable-testid="IssueList"]:nth-child(2)', // второй тикет в списке
  DRAFT_SAVED_MESSAGE: '//span[@data-unstable-testid="AutoSaveDate"][contains(., "Сохранено в")]', // надпись, что черновик письма автосохранился
  SAVE_DRAFT_BUTTON: '//button[contains(., "Сохранить как черновик")]', // кнопка Сохранить как черновик в форме письма
  FORM_INPUT_TO: 'input[name="to"]', //поле Кому в ответном письме
  CREATE_ST_TICKET_ON_MAIL_FORM: 'button[title="Создать тикет Ст"]', // кнопка создания ST-тикета из формы письма в таймлайне

  SMEJNIKAM_BUTTON_ON_MAIL_FORM:
    'div[data-unstable-testid="Toolbar"] button[title="Переслать смежникам"]', // кнопка Смежникам на форме письма
  SIGNATURE_BUTTON:
    'div[data-unstable-testid="Row"]:nth-child(1) Button[type="Button"]:nth-child(2)', // кнопка Подпись в форме написания письма
  INTERNAL_SIGNATURE_IN_LIST: '//span[text()="Внутренняя подпись"]', // Внутренняя подпись в списке подписей (она там одна)
  QUEUE_SIGNATURE_IN_LIST: '[data-testid="Popup"] div[data-key="item-0"]', // подпись, настроенная для очереди, первая в списке
  INTERNAL_SIGNATURE_IN_MAIL: 'div[class="spSignature"]', // блок с подписью в теле письма
  ANSWER_BUTTON_ON_MAIL_FORM:
    '//div[@data-unstable-testid="TimelineItem"][contains(@id, "Mail")] //div[@data-unstable-testid="Toolbar"] //button[1]', // кнопка Ответить на форме письма
};

module.exports = {
  TicketsLocators,
};
