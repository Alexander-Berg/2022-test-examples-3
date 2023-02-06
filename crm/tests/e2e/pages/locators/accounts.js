const AccountsLocators = {
  ACCOUNT_MODULE: 'a[title="Аккаунты"]', // кнопка модуля Аккаунты
  ADD_CONTACT: '[data-unstable-testid="Block"]:nth-of-type(2) span button', // кнопка "Создать" в блоке Контакты в карточке аккаунта
  NAME_INPUT: 'input[placeholder="Имя"]', // поле Название формы ввода контакта аккаунта
  EMAIL_INPUT: 'input[placeholder="Email"]', // поле Email формы ввода контакта аккаунта
  PHONE_INPUT: 'input[placeholder="Телефон"]', // поле Телефон формы ввода контакта аккаунта
  SAVE_CONTACT_BUTTON: 'form[theme="modal"] button[type="submit"]', // кнопка Сохранить формы ввода контакта аккаунта
  SAVED_CONTACT: 'div[offset="0"]', // блок Контакты на карточке аккаунта
  DELETE_CONTACT: '[data-unstable-testid="ContactFull"] button:last-child', // кнопка удаления контакта в блоке контакта
  EDIT_CONTACT_BUTTON: '[data-unstable-testid="ContactFull"] button:first-child', // кнопка редактирования контакта в блоке контакта

  NEW_ISSUE_INPUT: 'input[placeholder="Новая задача"]', // поле ввода новой задачи в карточке аккаунта
  SAVE_ISSUE_BUTTON: '[data-unstable-testid="IssueNew"] button[aria-disabled="false"]', // активная кнопка Сохранить для новой задачи
  FIRST_OPEN_ISSUE: '//span[text()="Открыта"]', // первая в списке задача со статусом Открыта

  NEW_COMMENT_INPUT: 'textarea[placeholder="Комментарий"]', // поле ввода коментария в блоке Комментарии
  SAVE_COMMENT_BUTTON:
    '[data-unstable-testid="Comments"] button[type="submit"][aria-disabled="false"]', //кнопка Отправить формы ввода комментария в карточке аккаунта
  SAVED_COMMENT: '//div[contains(@name, "COMMENTS_")]', // таблица с комментариями
  DELETE_COMMENT_BUTTON: '[data-unstable-testid="EditableComment"] button[title="Удалить"]', // кнопка удаления комментария в блоке с комментарием
  EDIT_COMMENT_BUTTON: '[data-unstable-testid="EditableComment"] button[title="Редактировать"]', // кнопка редактирования комментария в блоке с комментарием
  COMMENT_INPUT_FOR_EDIT:
    '[data-unstable-testid="CommentBase"] textarea[placeholder="Написать комментарий"]', // поле ввода изменяемого комментария

  CREATE_ACCOUNT_BUTTON: '//button[text()="Создать аккаунт"]', // кнопка "Создать аккаунт"
  NEW_ACCOUNT_NAME_INPUT: 'input[placeholder="Название"]', // поле ввода Названия в форме создания аккаунта
  NEW_ACCOUNT_TYPE_LIST: '.Modal-Content button[role="listbox"]', // поле Тип аккаунта в форме создания аккаунта

  LEAD_TYPE: '//span[text()="Лид"]', // пункт меню Лид в списке типов аккаунта
  COUNTERAGENT_TYPE: '//span[text()="Контрагент"]', // пункт меню Контрагент в списке типов аккаунта
  METRICA_TYPE: './/span[text()="Клиент Метрики"]', // пункт меню Клиент Метрики в списке типов аккаунта
  MOBILE_APP_TYPE: './/span[text()="Мобильное приложение"]', // пункт меню Мобильное приложение в списке типов аккаунта

  SAVE_NEW_ACCOUNT_BUTTON: 'div[name="CREATE_CLIENT"] button[type="submit"][aria-disabled="false"]', // кнопка Сохранить в форме создания нового аккаунта
  ACCOUNT_INFO_BLOCK: 'form[data-unstable-testid="InfoForm"]', // блок с информацией об аккаунте
  METRICA_LOGIN_INPUT: 'input[placeholder="Логин Метрики"]', // поле ввода логина Метрики на форме создания нового аккаунта метрики
  COUNTERAGENT_ORG: '[data-unstable-testid="FieldWrap"]:last-of-type button[role="listbox"]', // поле ввода организации на форме создания нового аккаунта контрагента
  ORG_OPTION: '.Popup2_visible [data-key="item-0"]', // первое значение в списке предлагаемых организаций для заведения нового контрагента

  EDIT_ACCOUNT_BUTTON: 'form[data-unstable-testid="InfoForm"] button[type="button"]', // кнопка редактирования блока с информацией об аккаунте
  ACCOUNT_NAME_INPUT_EDIT: 'form[data-unstable-testid="InfoForm"] input[name="name"]', // поле Название в блоке редактирования информации об аккаунте
  INDUSTRY_FIELD: '//label[contains(., "Индустрия")]', // поля, содержащие слово Индустрия
  DOMAIN_INPUT: 'input[name="domain"]', // поле Домен в блоке редактирования информации об аккаунте
  SAVE_ACCOUNT_EDITS: 'button[type="submit"][aria-disabled="false"]', // кнопка Сохранить в блоке с информацией об аккаунте
  ATTACH_FILE_TO_COMMENTS: 'input[type="file"]', // кнопка Выбрать файлы в блоке Комментарии
};

module.exports = {
  AccountsLocators,
};
