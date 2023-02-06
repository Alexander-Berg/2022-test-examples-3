const AccountHistoryLocators = {
  ACCOUNT_HISTORY_BUTTON: '//span[text()="История аккаунта"]/parent::*', // кнопка История аккаунта в карточке аккаунта
  HISTORY_WINDOW: 'div[name="activity"]', // таблица с данными в истории аккаунта
  ISSUE_CARD: '[name="activity"] button[title="Скопировать ссылку"]', // ссылка в шапке тикета в раскрытой карточке коммуникации в истории аккаунта
  FILTERS_DROPLIST: '//span[text()="Фильтры"]/parent::*', // кнопка Фильтры в шапке истории аккаунта
  TICKET_FILTER: 'form button[title="Тикет"]', // фильтр по Тикетам
  FILTERS_BODY: '._3M9zHDek3Rv1U70UIdKbsS', // все фильтры на форме выбора фильтров
  ACCOUNT_FILES_BUTTON: '//span[text()="Файлы аккаунта"]/parent::button', // кнопка Файлы аккаунта
  ACCOUNT_FILES_BODY: '[data-testid="account-files"] div[name="files"]', // таблица файлов на табе Файлы аккаунта
  DOWNLOAD_FILE: '[data-testid="account-files"] a[title="Скачать файл"]', // кнопка Скачать файл рядом с любым файлом на табе Файлы аккаунта
  FILE_SIZE: '[data-testid="account-files"] .crm-fileSize', // размер файла
  OPEN_FILE: '[data-testid="account-files"] a[title="Открыть в docviewer"]', // ссылка на открытие файла
  OPENED_FILE: 'img[alt="image.png"]', // открытый в docviewer файл
  CLOSE_FILE: '//span[text()="×"]', // кнопка закрытия открытого в docviewer файла
};

module.exports = {
  AccountHistoryLocators,
};
