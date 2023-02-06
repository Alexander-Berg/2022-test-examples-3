const MassmailLocators = {
  NEW_MASSMAIL: '//a[text()="Новая рассылка"]',
  VIEW_AND_SEND_BUTTON:
    'button[class="Button2 Button2_view_action Button2_size_s Button2_direction_horizontal"]', //Кнопка "Посмотреть и отправить"
  MAIL_TOPIC_INPUT: 'input[placeholder="Тема"]', //поле Тема в рассылке
  MAIL_COPY_INPUT: 'input[placeholder="Копия"]', //поле Копия в рассылке
  SAVE_DRAFT_BUTTON: '.Button2_view_pseudo.Button2_size_s:first-of-type', //кнопка "Сохранить как черновик"
  DELETE_DRAFT_BUTTON: '.Button2_view_pseudo.Button2_size_s:last-of-type',
  LATEST_CREATED_MASSMAIL: '//a[contains(@href, "#/massmail/")][1]', //первая строка в списке рассылок
  MAIL_BODY: '#cke_body',
  MAIL_BODY_INPUT: '#cke_body [contenteditable="true"]',
  ADD_ACCOUNT_LINK: '//a[text()="Аккаунт"]',
  FROM_WHOM_BLOCK: '.Button2_width_max.Button2_size_s',
  MACROS_LIST_BUTTON:
    'button[class="Button2 Button2_view_default Button2_size_s Button2_direction_horizontal"][type="button"]:nth-child(2)',
  SIGNATURE_LIST_BUTTON: '.Button2_view_default:nth-child(3)',
  FIRST_SIGNATURE: 'div[data-key="item-0"]',
  SECOND_SIGNATURE: 'div[data-key="item-1"]',
  ATTACHED_FILE: '.crm-fileNameFull',
  TEMPLATE_LIST_BUTTON: '//button[position()=3]',
  TEMPLATE_OPTION: '.Menu-Text',
  OPENED_FILE: 'div[class="_2MyX6Qi9Z_pE1o_K7lfRCG Pqx1kqfhUeUdlEXvUDPl_"][title="image.png"]',
  ERROR_BY_OPENING_FILE: '//div[contains(text(), "Произошла ошибка, попробуйте еще раз")]', //ошибка при открытии файла
  CLOSE_FILE: '//span[text()="×"]',
  MACROS_MIDDLE_NAME: 'div[class="Popup2-ChildrenWrap"] div[data-key="item-0"]',
  MACROS_FIRST_NAME: 'div[class="Popup2-ChildrenWrap"] div[data-key="item-1"]',
  MACROS_LAST_NAME: 'div[class="Popup2-ChildrenWrap"] div[data-key="item-2"]',
  MACROS_ACCOUNT_NAME: 'div[class="Popup2-ChildrenWrap"] div[data-key="item-3"]',
  SEND_MAIL_BUTTON: '//span[text()="Отправить"]',
  SEND_MAIL_BUTTON_BORDER:
    'button[class="Button2 Button2_size_s Button2_view_danger Button2_direction_horizontal"]', //Сама кнопка Отправить
  DELETE_MAIL_BUTTON: '.Button2_view_pseudo.Button2_size_s:nth-of-type(2)',
  PREVIEWED_MAIL_BODY: '._1lsByiwno8EBPi8vHwwnJF',
  ADD_ACCOUNT_FROM_FILE: 'span input[type="file"]',
  ADD_CONTACT_ICON: 'i[title="Добавить контакт"]',
  CRMAUTOTEST_MASSMAIL_CONTACT: '//div[text()="crmautotest@yandex.ru"]',
  CONTACT_TYPE_LISTBOX: '.Button2_width_max.Button2_size_xs',
  CONTACT_TYPE_ALL: 'div[role="option"]:last-of-type',
  CONTACT_TYPE_MASSMAIL: 'div[role="option"]:first-of-type',
  NUMBER_OF_CONTACTS: '._17YN3oC0p1G3Y1Yo-J5XFC',
  ERROR_ICON_CANT_SEND:
    'span[class="VJ8tEghKBZwM_jz52yFak _3YEJS8cdkbOvAr8lkwwDbP _37YaQgyppU81r59H-gZSq"] i[class="material-icons"]', //иконка с красным треугольником, если отправка невозможна
  ALLOW_REPEATE_RECEPIENTS_CHECKBOX:
    'div[class="_3DU5IQZmgeNpzbqxuOwpOi _22O1TWVrMMAlxtu8AVkJ_0"] div[class="_1kPdVONu2cULoHhQP4jvcA"]:nth-of-type(5) span[class="Checkbox-Box"]', //Чекбокс "Разрешить повторение адресатов"
  COMMON_MAIL_FOR_ACCOUNT_CHECKBOX:
    'div[class="_3DU5IQZmgeNpzbqxuOwpOi _22O1TWVrMMAlxtu8AVkJ_0"] div[class="_1kPdVONu2cULoHhQP4jvcA"]:nth-of-type(4) span[class="Checkbox-Box"]', //Чекбокс "Общее письм на аккаунт"
  CONTACT_NAME: 'div[data-unstable-testid="Contact"] div[class="_3ejCW8Fl_No4qdoO-pxpWk"]',
  MAIL_TOPIC_DRAFT:
    'div[data-unstable-testid="Info"] div[class="_21_Fm2-K_Ad-n-OG3qg0Pu"] div[class="_1MCS6zIyyem6xn8CvV5YGq"]:nth-child(3) :nth-child(2)',
  CONTACTS_LIST: 'div[data-unstable-testid="Contacts"]',
  SELECLED_ACCOUNT:
    'div[data-unstable-testid="MassmailAccountsItem"] div[class="_2IOJkTG9UyPxo7wn9IQaAF"]', //Выбранный аккаунт (первый)
  REPEAT_MASSMAIL: '//a[text() = "Повторить рассылку"]',
  MESSAGE_MASSMAIL_SENT: '//div[text() = "Рассылка отправлена"]', //Зеленая всплывашка, что рассылка отправлена
  MESSAGE_MASSMAIL_SAVED: '//div[text() = "Рассылка сохранена"]', //Зеленая всплывашка, рассылка сохранена
  MESSAGE_MASSMAIL_TEXT: 'div[class="_1DhOymC5RKT8DaDYGKSzKb _1JbPXjMIhv5wBLsh4HJn_"]', //Оранжевая вплывашка. Рассылка не может быть отправлена на адрес не из саджеста
  SUGGEST_OF_MANAGERS: 'div[class="A8qEiqoB8xpJDAfRzn05G"]', //появляется по клику в поле Копия
  ITEM_IN_SUGGEST_OF_MANAGERS: 'div[data-unstable-testid="MenuItemComponent"]', //Все элементы в саджесте имеют такой локатор
};

module.exports = {
  MassmailLocators,
};
