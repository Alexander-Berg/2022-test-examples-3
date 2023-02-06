const PlannerLocators = {
  PLANNER_MODULE: 'a[title="Активности"]', //кнопка модуля Активности
  TAB_DAY_PLANES: 'div[data-unstable-testid="TodosPage"]', //таб "Дела на день"
  TAB_CALENDER: 'a["href="#/activities/calendar"]', //таб "Календарь
  SCHEDULE_ACTIVITY: 'div[data-testid="ActionsPanel"]', //кнопка "Запланировать активность"
  TASK_ACTIVITY:
    '//div[@class="Popup2-ChildrenWrap"] //div[@role="menuitem"] //span[contains(., "Задача")]', //кнопка Задача
  CALL_ACTIVITY:
    '//div[@class="Popup2-ChildrenWrap"] //div[@role="menuitem"] //span[contains(., "Звонок")]', //кнопка Звонок
  ACTIVITY_CREATE_FORM: 'div[class="Modal-Content"]', //форма создания задачи или звонка
  CLOSE_CREATE_FORM: 'div[data-unstable-testid="ModalContent"] .ModalContent-Close', //крестик закрытия формы создания/редактирования активности
  CREATE_FORM_NAME:
    '//form[@class="_3WfwqIAi8yseOcAFFEnJWO"] //span[@class="CUcVwkF7pJmSKCX6_MA1f"] //span[contains(., "Название")]', //заголовок поля "Название"
  CREATE_FORM_DEADLINE:
    '//form[@class="_3WfwqIAi8yseOcAFFEnJWO"] //span[@class="CUcVwkF7pJmSKCX6_MA1f"] //span[contains(., "Дедлайн")]', //заголовок поля "Дедлайн"
  CREATE_FORM_START_DATETIME:
    '//form[@class="_3WfwqIAi8yseOcAFFEnJWO"] //span[@class="CUcVwkF7pJmSKCX6_MA1f"] //span[contains(., "Дата и время начала")]', //заголовок поля "Дата и время начала"
  CREATE_FORM_END_DATETIME:
    '//form[@class="_3WfwqIAi8yseOcAFFEnJWO"] //span[@class="CUcVwkF7pJmSKCX6_MA1f"] //span[contains(., "Дата и время окончания")]', //заголовок поля "Дата и время окончания"
  CREATE_FORM_DESCRIPTION:
    '//form[@class="_3WfwqIAi8yseOcAFFEnJWO"] //span[@class="CUcVwkF7pJmSKCX6_MA1f"] //span[contains(., "Описание")]', //заголовок поля "Описание"
  CREATE_FORM_DONE:
    '//form[@class="_3WfwqIAi8yseOcAFFEnJWO"] //span[@class="CUcVwkF7pJmSKCX6_MA1f"] //span[contains(., "Выполнено")]', //заголовок поля "Выполнено"
  CREATE_FORM_OPPORTUNITIES:
    '//form[@class="_3WfwqIAi8yseOcAFFEnJWO"] //span[@class="CUcVwkF7pJmSKCX6_MA1f"] //span[contains(., "Сделки")]', //заголовок поля "Сделки"
  SAVE_ACTIVITY_BUTTON: 'button[type="submit"]', //кнопка "Сохранить" на форме создания задачи
  FIRST_POPUP_IN_LIST:
    'div[class="uQdNcmMkcD5deGrVTSCtd notifications_root"] div[data-unstable-testid="NotificationItem"]:nth-of-type(1)', //первый в списке попап
  SECOND_POPUP_IN_LIST:
    'div[class="uQdNcmMkcD5deGrVTSCtd notifications_root"] div[data-unstable-testid="NotificationItem"]:nth-of-type(2)', //второй в списке попап
  ACTIVITY_NAME_FIELD: 'input[data-testid="TextInput"][name="Textname"]', //поле ввода Названия на форме создания/редактирования активности
  EVENT_IN_CALENDER:
    'div[class="rbc-day-slot rbc-time-column rbc-now rbc-today"] div[class="rbc-events-container"] div[data-event="true"]', //событие в календаре
  TABLE_TITLE: '//div[@data-unstable-testid="Layout"] //span[contains(text(), "Дела на день")]', //заголовок таблицы Дела на день
  FULL_CALENDER: 'div[class="rbc-day-slot rbc-time-column rbc-now rbc-today"]', //все события в календаре
  EMPTY_TABLE: 'div[data-testid="EmptyTable"]', //пустая таблица с активностями
  FULL_TABLE: 'div[data-testid="Table"]', //вся таблица с активностями
  TABLE_ROW:
    'div[data-testid="Table"] div[class="M3qOGNtZ_a1-fJs47nj3_"] div[data-unstable-testid="Row"]', //строка таблица
  NAME_IN_TABLE_ROW:
    'div[data-unstable-testid="Cell"]:nth-of-type(2) span[data-unstable-testid="Text"]', //название активности в строке таблицы
  EDIT_BUTTON:
    'div[data-testid="Table"] div[class="M3qOGNtZ_a1-fJs47nj3_"] div[data-unstable-testid="Row"] div[data-unstable-testid="Cell"] div[data-unstable-testid="EditButton"]', //кнопка Редактировать в строке
  DELETE_BUTTON:
    'div[data-testid="Table"] div[class="M3qOGNtZ_a1-fJs47nj3_"] div[data-unstable-testid="Row"] div[data-unstable-testid="Cell"] div[data-testid="removeRowButton"]', //кнопка Удалить в строке
  EDIT_ACTIVITY_FORM: 'div[data-unstable-testid="ModalForm"]', //форма редактирования активности

  DEADLINE_DATE_INPUT:
    'div[class="_1-mc1HtinLUVey_dBr5_3V _2j_bSL6I7Yn0MBDPL29-hz"]:nth-child(2) input[placeholder="дд.мм.гггг"]', //поле ввода дедлайна
  DEADLINE_DATE_CLEAR:
    'div[class="_1-mc1HtinLUVey_dBr5_3V _2j_bSL6I7Yn0MBDPL29-hz"]:nth-child(2) [data-testid="DateInput"] .Icon', //крестик очистки даты дедлайна
  DEADLINE_TIME_INPUT:
    'div[class="_1-mc1HtinLUVey_dBr5_3V _2j_bSL6I7Yn0MBDPL29-hz"]:nth-child(2) input[placeholder="чч:мм"]', //поле времени дедлайна
  DEADLINE_TIME_CLEAR:
    'div[class="_1-mc1HtinLUVey_dBr5_3V _2j_bSL6I7Yn0MBDPL29-hz"]:nth-child(2) [data-testid="TimeInput"] .Icon', //очистка времени дедлайна по крестику

  START_DATE_INPUT:
    'div[class="_1-mc1HtinLUVey_dBr5_3V _2j_bSL6I7Yn0MBDPL29-hz"]:nth-child(3) input[placeholder="дд.мм.гггг"]', //поле ввода Даты начала
  START_DATE_CLEAR:
    'div[class="_1-mc1HtinLUVey_dBr5_3V _2j_bSL6I7Yn0MBDPL29-hz"]:nth-child(3) [data-testid="DateInput"] .Icon', //крестик очистки Даты начала
  START_TIME_INPUT:
    'div[class="_1-mc1HtinLUVey_dBr5_3V _2j_bSL6I7Yn0MBDPL29-hz"]:nth-child(3) input[placeholder="чч:мм"]', //поле времени Даты начала
  START_TIME_CLEAR:
    'div[class="_1-mc1HtinLUVey_dBr5_3V _2j_bSL6I7Yn0MBDPL29-hz"]:nth-child(3) [data-testid="TimeInput"] .Icon', //очистка времени Даты начала по крестику

  END_DATE_INPUT:
    'div[class="_1-mc1HtinLUVey_dBr5_3V _2j_bSL6I7Yn0MBDPL29-hz"]:nth-child(4) input[placeholder="дд.мм.гггг"]', //поле ввода Даты окончания
  END_DATE_CLEAR:
    'div[class="_1-mc1HtinLUVey_dBr5_3V _2j_bSL6I7Yn0MBDPL29-hz"]:nth-child(4) [data-testid="DateInput"] .Icon', //крестик очистки даты Даты окончания
  END_TIME_INPUT:
    'div[class="_1-mc1HtinLUVey_dBr5_3V _2j_bSL6I7Yn0MBDPL29-hz"]:nth-child(4) input[placeholder="чч:мм"]', //поле времени Даты окончания
  END_TIME_CLEAR:
    'div[class="_1-mc1HtinLUVey_dBr5_3V _2j_bSL6I7Yn0MBDPL29-hz"]:nth-child(4) [data-testid="TimeInput"] .Icon', //очистка времени Даты окончания по крестику

  DESCRIPTION_INPUT: 'div[data-unstable-testid="ModalContent"] [name="Textareadescription"]', //поле Описание на форме создания/редактирования активности

  ACTIVITY_NAME_IN_FIRST_TABLE_ROW:
    'div[data-testid="Table"] div[class="_1C9mUePKt0mEgH7h6tbM6x"]:first-of-type div[class="blO65Ou1Gn3i9YR-dJnHc"]:nth-of-type(2) span', //название активности в первой строке таблицы
};

module.exports = {
  PlannerLocators,
};
