const IssuesLocators = {
  ISSUES_MODULE: 'a[title="Задачи"]',
  ISSUE_LIST: '[data-test-id="InfiniteVirtualList"]',
  ISSUE_LIST_BLOCK: 'div[data-unstable-testid="IssueList"]', //блок с одной задачей в листе задач
  ISSUE_LIST_BLOCK_NAME:
    'div[class="e3y6ztdqP7tS1GzpuAY2h _1siXrhTYH-QpNL0S5T-zc8"] span[data-unstable-testid="PreviewValue"]', //название задачи в конкретно найденном блоке
  LATEST_CREATED_ISSUE:
    '[data-testid="issues-list/list"] [data-unstable-testid="IssueList"]:nth-of-type(1)', // верхняя в списке задача
  LATEST_CREATED_LINKED_ISSUE:
    'div[style="position: absolute; left: 0px; top: 0px; height: 70px; width: 100%;"]',
  LATEST_CREATED_ISSUE_NAME:
    'div[data-testid="InfiniteVirtualList"] div[class="_1HxS0P_kkZRGhxiQ7phVBK"]:nth-child(1) span[class="_2SnUjnIPgFBVov6tPBP7dn"]',
  LATEST_CREATED_ISSUE_ACCOUNT:
    'div[data-testid="InfiniteVirtualList"] div[class="_1HxS0P_kkZRGhxiQ7phVBK"]:nth-child(1) a',
  OPEN_ISSUE_NAME:
    'div[class="e3y6ztdqP7tS1GzpuAY2h _1siXrhTYH-QpNL0S5T-zc8 _1RvM_D03tpl8Z45AalvlKo _117iqhUM5Oh_dCw9mN7fnc"] span[class="_2SnUjnIPgFBVov6tPBP7dn"]', //название открытой задачи
  OPEN_ISSUE_ACCOUNT: 'div[class="_2IOEgFgEafj47mcuGZAI9R"] a[href^="#/account"]', //аккаунт в открытой задаче
  ISSUE_STATUS:
    'div[class="_5WjUDM3n78zW5qPub3cuj"] div[class="e3y6ztdqP7tS1GzpuAY2h _1TeMqMVZfpY2RVa6KJI9Lb _3SBgKYnqhh6m90Ka-8m4hY U93xkwmiTALdgQ4_Mna8B"] span[class="_2SnUjnIPgFBVov6tPBP7dn"]', //статус в карточке задачи
  INPUT_NEW_ISSUE: 'input[name="name"]',
  CREATE_ISSUE_BUTTON: 'button[type="submit"]:nth-of-type(1)',
  SEARCH_ISSUE: 'input[data-testid="search-input"]', // поле ввода Поиска задачи
  SEARCH_BUTTON: 'button[data-testid="search-Button"][type="submit"]', // кнопка Найти в форме поиска задачи
  // задизабленная кнопка Действия в задачах
  ACTIONS_IN_ISSUE_DISABLED:
    '//*[@id="container"]/div/div[1]/div[1]/div/div[2]/div/div[2]/div[1]/div/div/div[2]/div/div/div[2]/div/div/div[2]/div[2]/div/div/div[1]/div/div[2]/div[2]/div[2]/div[2]/div[2]/div/button[@aria-disabled="true"]',
  // активная кнопка Действия в задачах
  ACTIONS_IN_ISSUE_ACTIVATED:
    '//*[@id="container"]/div/div[1]/div[1]/div[4]/div[1]/div[1]/div/div[2]/div[2]/div[2]/div[2]/div[2]/div/button[@aria-disabled="false"]',
  DELETE_ISSUE: './/span[text()="Удалить"]',
  ROBOT_TITLE_IN_SUGGEST: './/span[text()="Роботы CRM Space"]',
  INPUT_RENAME_ISSUE: 'input[name="value"]',
  MAIN_INFO_ON_ISSUE:
    'div[data-testid="InfiniteVirtualList"] div[class="_1HxS0P_kkZRGhxiQ7phVBK"]:nth-child(1) ._2UDsazevxRqFkW9esElfky',

  SORT_ISSUES_LIST: 'button[role="listbox"]',
  SORT_BY_CREATE_DATE: 'div[role="option"]:nth-of-type(2)',

  WRITE_COMMENT: './/span[text()="Написать комментарий"]', // кнопка Написать комментарий
  COMMENT_FIELD: 'textarea[placeholder="Написать комментарий"]', // поле ввода комментария
  SUBMIT_COMMENT: '[data-unstable-testid="WFInputWithPreview"] button[type="submit"]', // кнопка сохранения комментария
  COMMENT_DESCRIPTION_BODY: '[data-unstable-testid="YfmFormatter"] p',

  CLOSE_OPEN_ISSUE_BUTTON: 'button[name="actionStateChange"]', //кнопка Закрыть в задачах
  CLOSE_OPEN_ISSUE_CIRCLE_NOT_CHECKED:
    'div[style="position: absolute; left: 0px; top: 0px; height: 50px; width: 100%;"] .Button2_pin_circle-circle', // on first issue only
  CLOSE_OPEN_ISSUE_CIRCLE_CHECKED:
    'div[style="position: absolute; left: 0px; top: 0px; height: 50px; width: 100%;"] .Button2_view_action.Button2_pin_circle-circle', // on first issue only
  DELETE_COMMENT: '.Button2:nth-of-type(3)',

  ASSIGNEE_NAME: 'span[style="color: rgb(0, 0, 0);"]',
  ASSIGNEE_FIELD: './/span[text()="Исполнитель"]',
  ASSIGNEE_INPUT: 'form[action="#"] input',
  DELETE_ASSIGNEE_FROM_ISSUE: 'form[action="#"] button[aria-label="remove"]',
  SAVE_ASSIGNEE_BUTTON: 'form[action="#"] button[type="submit"].Button2_size_xs',
  OPTION_FOR_ASSIGNEE: '.SuggestBase-List',

  ROBOT_FOLLOWER: 'img[src="//center.yandex-team.ru/api/v1/user/robot-crmcrown/avatar/64.jpg"]',

  EDIT_DESCRIPTION_BUTTON: 'div[value="0"] .Button2:nth-child(2)',
  DESCRIPTION_FIELD: '.Textarea-Control',
  SAVE_DESCRIPTION: 'div[value="0"] button[type="submit"]',

  ADD_MARK_BUTTON: 'button[title="Добавить метку"]',
  FIND_MARK_BUTTON: 'div[class="Popup2-ChildrenWrap"] input[placeholder="Поиск"]', //поле ввода на форме поиска метки
  FIRST_MARK_IN_LIST:
    'div[class="Popup2-ChildrenWrap"] span[style="background-color: rgba(0, 173, 255, 0.3);"]', //первая метка в списке
  NEW_MARK_BUTTON: '.Popup2-ChildrenWrap .Button2',
  NEW_MARK_NAME: '#name',
  NEW_MARK_ACCESS: 'input[placeholder="Доступы"]',
  OPTION_FOR_ACCESS: '.SuggestBase-List',
  NEW_MARK_GREEN_COLOUR: 'label[style="background-color: rgb(19, 102, 25);"]',
  SAVE_NEW_MARK: '.Modal-Content button[type="submit"]',
  DELETE_MARK_FROM_ISSUE: 'button[style="color: rgb(255, 255, 255);"]',

  DEADLINE_INPUT: 'div[placeholder="Дедлайн"]',
  DEADLINE_CALENDER: 'span[class="_3VHEddpRlTsZdZStZPsBJm _2GBN0mZkh6awO0oplhMipZ"]',
  DEADLINE_FIELD: './/span[text()="Дедлайн"]',
  DEADLINE_CALENDER_LAST_DATE:
    'div[class="react-datepicker__month-container"] div[class="react-datepicker__week"]:last-of-type div[role="button"]:last-of-type', //последняя доступная дата в открытом календаре
  CLEAR_DEADLINE_BUTTON: '.Popup2-ChildrenWrap .Button2_view_danger',
  SAVE_DEADLINE_BUTTON: '.Popup2-ChildrenWrap button[type="submit"]',
  CALENDAR_ICON:
    'div[style="position: absolute; left: 0px; top: 0px; height: 50px; width: 100%;"] .fa-calendar', // on first issue only
  DEADLINE_ON_FIRST_ISSUE:
    'div[style="position: absolute; left: 0px; top: 0px; height: 50px; width: 100%;"] div[data-event-sp',

  LINKED_ISSUES: './/span[text()="Связанные"]',
  INPUT_LINKED_ISSUE: 'div[value="1"] input[name="name"]',
  SAVE_LINKED_ISSUE: 'div[value="1"] button[type="submit"]',
  UNLINK_ISSUES: 'button[title="Отвязать"]',
  LINK_TO_PARENT_ISSUE: 'div[tabindex="-1"] a[rel="noopener noreferrer"]',

  SELECT_ACCOUNT_BUTTON: 'button[title="Выбрать аккаунт"]',
  ZERO_ACCOUNT_CHECKBOX: '._2XVOAjvORDsVkngf66qlrc .Checkbox-Box',
  SAVE_ACCOUNT_BUTTON: '.Modal-Content .Button2_view_action:nth-child(1)',
  LINK_TO_ACCOUNT: 'div[style="align-items: center; flex-wrap: wrap;"] a[href="#/account/1"]', // on first issue only
  REMOVE_ACCOUNT: 'button[title="Удалить аккаунт"]',

  CREATE_TIMER: 'button[title="Создать новый таймер"]',
  DATE_FOR_TIMER: 'input[dateformat="DD.MM.YYYY"]',
  INPUT_FOR_PEOPLE_TO_NOTIFY: 'input[placeholder="Уведомить"]',
  SAVE_TIMER: '.Modal-Content button[type="submit"]',
  CREATED_TIMER: 'button[title="Таймеры"]',
  TIMER_IN_ATTRIBUTES: './/span[text()="Таймер"]',
  REMOVE_TIMER: 'div[data-testid="attribute_timers"] button[aria-label="remove"]',
  TIMER_IN_TWO_DAYS: 'button[data-offset-days="2"]',
  TIMER_COMMENT: '#comment',
  EDIT_TIMER: 'div[data-testid="attribute_timers"] span[role="button"]',
  WORKFLOW_LIST:
    'div[class="Popup2 Popup2_visible Popup2_view_default HN1oheq1p8o_j2E7wE62S YMTAVgu2yPqqNfKB73QCd"]',
  STANDART_WORKFLOW_OPTION: './/span[text()="Стандартный тикет"]',
  SAVE_WORKFLOW_BUTTON: '.resizable button[type="submit"]',
  STANDART_WORKFLOW_BUTTON_ACCEPT: 'button[component="ButtonGroup"]',
  WORKFLOW_FIELD: '//div[@aria-label="reading area"] //span[text()="Воркфлоу"]',

  FILTER_MY: 'input[value="My"]', // фильтр задач Мои (панель над списком задач)
  FILTER_DELEGATED: 'input[value="Author"]', // фильтр задач Делегированы
  FILTER_FOLLOWER: 'input[value="Follower"]', // фильтр задач Наблюдаю
  FILTER_MY_GROUP: 'input[value="MyGroup"]', // фильтр задач Моя группа

  FILTER_ALL: 'input[value="None"]', // фильтр задач Все
  FILTER_OPEN: 'input[value="Open"]', // фильтр задач Открытые
  FILTER_CLOSE: 'input[value="Closed"]', // фильтр задач Закрытые
};

module.exports = {
  IssuesLocators,
};
