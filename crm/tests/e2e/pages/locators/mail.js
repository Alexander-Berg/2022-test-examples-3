const MailLocators = {
  MODULE_MAIL: 'span[class="_2UKVyFPBed4OK8stw1r-3_"] a[title="Почта"]',
  CHECK_NEW_EMAIL_BUTTON: '//button[contains(., "Проверить новые письма")]', //Кнопка Проверить новые письма
  FIRST_MAIL_IN_LIST:
    '[data-unstable-testid="InfiniteList"] [data-unstable-testid="MailListItem"]:first-of-type', //первое письмо в списке
  THEME_IN_FIRST_MAIL_IN_LIST:
    '[data-unstable-testid="InfiniteList"] [data-unstable-testid="MailListItem"]:first-of-type div[class="_2xMWmK_zDZVU5GVlWbItdk"]',
  WRITE_MAIL: '//button[contains(., "Написать письмо")]', // кнопка "Написать письмо"
  MAIL_FORM: 'form[role="presentation"]', // форма написания письма

  CHOOSE_ACCOUNT: '//div[@data-unstable-testid="Account"] //button[contains(.,"Выбрать")]', // кнопка "Выбрать" для аккаунта при написании нового письма
  EDIT_ACCOUNT: '//a[contains(., "изменить")]', // кнопка "изменить", если аккаунт уже был выбран
  CHOOSE_ACCOUNT_INPUT: 'div[data-testid="Modal"] input[data-testid="TextInput"]', // поле ввода Значение
  CHOOSE_ACCOUNT_FIND: '//div[@data-testid="Modal"] //span[text()="Найти"]/parent::button', // кнопка Найти в форме поиска аккаунта
  CHOOSE_ACCOUNT_ACCOUNT_LINK: 'a[href="#/account/9955466"] div:nth-child(2)', // ячейка "Найдено по" в строке с аккаунтом 9955466
  CHOOSE_FIRST_FINDED_ACCOUNT:
    '//div[@role="presentation"][@tabindex="0"] //a[@href[contains(., "account")]]', // первый найденный аккаунт в списке
  FORM_INPUT_TO: 'input[name="to"]', // поле Кому в ответном письме
  FORM_INPUT_SUBJECT: 'input[name="subject"]', // поле Тема в ответном письме
  CHOOSE_ACCOUNT_TYPE_SELECT:
    '//span[text()="Искать по"]/parent::label //span[@class="Select2 Select2_fixKeyDown Select2_width_max"]', // кнопка "Искать по" в форме поиска аккаунта
  CHOOSE_ACCOUNT_TYPE_SELECT_TEXT:
    '//span[text()="Искать по"]/parent::label //span[@class="Button2-Text"]', // текущее значение поля кнопки "Искать по" в форме поиска аккаунта
  CHOOSE_ACCOUNT_ANY_TYPE: '//span[text()="Любое поле"]/parent::div', // пункт "Любое поле" в списке "Искать по"
  TAG_ADD: 'button[title="Добавить метку"]', // кнопка "Добавить метку"
  LIST_MAIL: '[name="mailModule/"] [data-unstable-testid="MailListItem"]', // письмо в списке
  TAG_MENU: '[data-testid="Popup"] [data-unstable-testid="SetTagsList"]', // список доступных меток
  TAG_FIRST: '[data-unstable-testid="SetTagsList"] [data-key="item-0"]', // первая метка в списке
  CHOOSED_TAG:
    '[data-unstable-testid="TagsWithField"] [data-unstable-testid="Tags"] [data-unstable-testid="Tag"]', // выбранный тэг в письме
  FORM_BODY: '#cke_body [contenteditable="true"]', // тело письма
  SAVE_DRAFT_BUTTON:
    '//*[@id="container"]/div/div[1]/div[1]/div/div[2]/div[4]/form/div[3]/div/button[1]',
  SEND_BUTTON:
    '//*[@id="container"]/div/div[1]/div[1]/div/div[2]/div[4]/form/div[3]/div/span/button[1]', //кнопка отправки письма
  DELAYED_SEND_BUTTON:
    '//*[@id="container"]/div/div[1]/div/div/div[2]/div[4]/form/div[3]/div/span/button[2]', //кнопка отложенной отправки письма
  DELAYED_SEND_TODAY_OPTION: '.react-date-picker__month-view-day--today-highlight', //выбрать сегодняшний день в календарике при отложенной отправке
  DELAYED_SEND_INCREASE_MINUTE:
    '/html/body/div[4]/div/span/span/div/span/span[3]/div/div[1]/button', //кнопка увеличения минут на календарике
  MAIL_NOT_SELECTED: '//*[@id="container"]/div/div[1]/div[1]/div/div[2]/div[4]/span',
  FOLDERS_BUTTON: '//button[contains(., "Папки")]', //кнопка "Папки" слева от фильтров почты
  DRAFT_FOLDER_BUTTON: 'div[title="Черновики"]', //папка "Черновики" в фильтрах
  //FIRST_MAIL_IN_LIST: '//*[@id="container"]/div/div[1]/div/div/div[2]/div[2]/div/div[2]/div/div[1]',
  FIRST_MAIL_ACCOUNT_NAME:
    '//*[@id="container"]/div/div[1]/div[1]/div/div[2]/div[2]/div/div[2]/div/div[1]/div[2]/div[3]/span/span/span',
  OPEN_DRAFT_BUTTON:
    '//*[@id="container"]/div/div[1]/div[1]/div/div[2]/div[4]/div[1]/div[1]/div/button[1]', //кнопка "Открыть черновик"
  CHOOSED_ACCOUNT_NAME:
    '//*[@id="container"]/div/div[1]/div[1]/div/div[2]/div[4]/form/div[1]/div[2]/div/div[1]',
  INBOX_FOLDER_BUTTON:
    '//*[@id="container"]/div/div[1]/div[1]/div/div[2]/div[1]/div[1]/div/div/div[2]/div/div[1]/div/div/div[1]/div[1]',
  DELETED_FOLDER_BUTTON:
    '//*[@id="container"]/div/div[1]/div/div/div[2]/div[1]/div[1]/div/div/div[2]/div/div[1]/div/div/div[5]/div/div',
  OUTBOX_FOLDER_BUTTON:
    '//*[@id="container"]/div/div[1]/div[1]/div/div[2]/div[1]/div[1]/div/div/div[2]/div/div[1]/div/div/div[2]/div',
  DELAYED_OUTBOX_FOLDER_BUTTON:
    '//*[@id="container"]/div/div[1]/div/div/div[2]/div[1]/div[1]/div/div/div[2]/div/div[1]/div/div/div[6]/div', //папка Исходящие
  SPAM_FOLDER_BUTTON:
    '//*[@id="container"]/div/div[1]/div[1]/div/div[2]/div[1]/div[1]/div/div/div[2]/div/div[1]/div/div/div[4]/div',
  LIST_MAIL_PREVIEW_TEXT:
    '//*[@id="container"]/div/div[1]/div/div/div[2]/div[2]/div/div[2]/div/div[1]/div[2]/div[1]/div[3]',
  MAIL_PREVIEW: '//*[@id="container"]/div/div[1]/div/div/div[2]/div[4]/div[1]',
  PREVIEW_FROM_FIELD:
    '//*[@id="container"]/div/div[1]/div/div/div[2]/div[4]/div[1]/div[2]/div[1]/div[1]/div[1]/div[1]/span[2]',
  PREVIEW_TO_FIELD:
    '//*[@id="container"]/div/div[1]/div/div/div[2]/div[4]/div[1]/div[2]/div[1]/div[1]/div[1]/div[2]/span[2]',
  PREVIEW_SUBJECT_FIELD:
    '//*[@id="container"]/div/div[1]/div/div/div[2]/div[4]/div[1]/div[2]/div[1]/div[1]/div[1]/div[3]/span[2]',
  PREVIEW_BODY: '//*[@id="container"]/div/div[1]/div/div/div[2]/div[4]/div[1]/div[2]/div[2]',
  IMPORTANT_FILTER_BUTTON:
    '//*[@id="container"]/div/div[1]/div[1]/div/div[2]/div[1]/div[1]/div/div/div[2]/div/div[2]/button[1]',
  UNREAD_FILTER_BUTTON:
    '//*[@id="container"]/div/div[1]/div[1]/div/div[2]/div[1]/div[1]/div/div/div[2]/div/div[2]/button[2]',
  INBOX_DROPZONE:
    '//*[@id="container"]/div/div[1]/div/div/div[2]/div[1]/div[1]/div/div/div[2]/div/div[1]/div/div/div[1]/div/div/span/span',
  INBOX_MAIL_COUNTER: '//*[@id="container"]/div/header/div[1]/span[2]/a[2]/span/span[2]', //счетчик на иконке почты слева
  ATTRIBUTES_BUTTON:
    '//*[@id="container"]/div/div[1]/div[1]/div/div[2]/div[4]/div[2]/div[2]/button[1]',
  ATTRIBUTE_ACCOUNT_BUTTON:
    '//*[@id="container"]/div/div[1]/div[1]/div/div[2]/div[4]/div[2]/div[1]/div/div/div[2]/div/div[1]/div/span/button', //кнопка Выбрать в поле Аккаунт в Атрибутах письма
  ACCOUNT_HISTORY_BUTTON:
    '//*[@id="container"]/div/div[1]/div[1]/div/div[2]/div[4]/div[2]/div[2]/button[2]',
  ACCOUNT_FILES_BUTTON:
    '//*[@id="container"]/div/div[1]/div[1]/div/div[2]/div[4]/div[2]/div[2]/button[3]',
  ATTRIBUTE_ACCOUNT_NAME:
    '//*[@id="container"]/div/div[1]/div[1]/div/div[2]/div[4]/div[2]/div[1]/div/div/div[2]/div/div[1]/div[2]/div[1]',
  MAIL_IMPORTANT_FLAG: '[data-testid="MailImportantFlag"]',
  MOVE_TO_FOLDER_BUTTON:
    '//*[@id="container"]/div/div[1]/div/div/div[2]/div[4]/div[1]/div[1]/div/button[7]',
  READ_MAIL_BUTTON:
    '//*[@id="container"]/div/div[1]/div/div/div[2]/div[4]/div[1]/div[1]/div/button[6]',
  REPLY_MAIL_BUTTON:
    '//*[@id="container"]/div/div[1]/div/div/div[2]/div[4]/div[1]/div[1]/div/button[1]',
  READ_MAIL_CIRCLE_BUTTON: '[data-testid="Mail__readButton"]',
  SPAM_MAIL_BUTTON:
    '//*[@id="container"]/div/div[1]/div/div/div[2]/div[4]/div[1]/div[1]/div/button[5]',
  DELETE_MAIL_BUTTON:
    '//*[@id="container"]/div/div[1]/div/div/div[2]/div[4]/div[1]/div[1]/div/button[4]',
  FOLDER_POPUP: '[data-testid="Popup"]', //список доступных папок при нажатии кнопки "Переместить в папку"
  FOLDER_POPUP_SPAM_OPTION: '[data-testid="Popup"] [title="Спам"]', //папка Спам из списка доступных папок при нажатии кнопки "Переместить в папку"
  CHOOSE_FILES_INPUT:
    '//*[@id="container"]/div/div[1]/div/div/div[2]/div[4]/form/div[1]/div[8]/div/input',
  SYSTEM_MESSAGE: '//*[@id="container"]/div/div[2]/div',
  WILL_SEND_TEXT: '//span[@class="k3IfwnaXuWsxIkosMFG8K"][contains( ., "Будет отправлено: ")]',
  MAIL_SUBJECT_IN_FIRST_MAIL_IN_LIST:
    '/html/body/div[1]/div/div[1]/div[1]/div/div[2]/div[2]/div/div[2]/div/div[1]/div[2]/div[1]/div[2]/div',
  COPY_RECIPIENTS_LIST:
    '[data-unstable-testid="Info"] [class="_1MCS6zIyyem6xn8CvV5YGq"]:nth-child(2) span:last-child',
};

module.exports = {
  MailLocators,
};
