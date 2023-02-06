export class MenuSelectors {
  // Категория со списком блюд. (root div)
  static categoryRoot = '[data-testid="menu__category-root"]'
  // Опция "Редактировать" в контекстном меню категории
  static categoryContextEdit = '[data-testid="menu__category-context-edit"]'
  // Опция "Удалить" в контекстном меню категории
  static categoryContextDelete = '[data-testid="menu__category-context-delete"]'
  // Checkbox включения/выключения категории
  static categoryCheckbox = '[data-testid="menu__category-checkbox"]'
  // Заголовок категории
  static categoryHeader = '[data-testid="menu__category-header"]'
  // Количество блюд в категории
  static categoryDishCount = '[data-testid="menu__category-dish-count"]'
  // Кнопка "Добавить позицию" в категории
  static categoryAddPosition = '[data-testid="menu__category-add-position"]'
  // Кнопка "Добавить категорию"
  static addCategory = '[data-testid="menu__add-category"]'
  // Количество неактивных категорий
  static disableCategoriesLength = '[data-testid="menu__disable-categories-length"]'
  // Контейнер для неактивных категорий
  static disableCategoriesList = '[data-testid="menu__disable-categories-list"]'
  // Количество активных категорий
  static activeCategoriesLength = '[data-testid="menu__active-categories-length"]'
  // Список активных категорий
  static activeCategoriesList = '[data-testid="menu__active-categories-list"]'
  // Опция "Редактировать" в контекстном меню позиции
  static itemContextEdit = '[data-testid="menu__item-context-edit"]'
  // Опция "Создать копию" в контекстном меню позиции
  static itemContextCopy = '[data-testid="menu__item-context-copy"]'
  // Опция "Удалить" в контекстном меню позиции
  static itemContextDelete = '[data-testid="menu__item-context-delete"]'
  // Checkbox включения/выключения позиции
  static categoryItemCheckbox = '[data-testid="menu__category-item-checkbox"]'
  // Кнопка раскрытия подробного просмотра позиции в категории
  static categoryPositionExpand = '[data-testid="menu__category-position-expand"]'
  // Кнопка "Сохранить изменения"
  static saveBtn = '[data-testid="menu__save-btn"]'
  // Wrapper контрола редактирования названия категории
  static categoryNameControl = '[data-testid="menu__category-name-control"]'
  // Селект списка готовых категорий (root)
  static categoriesListSelect = '[data-testid="menu__categories-list-select"]'
  // Кнопка Добавить/Применить в модалке редактирования категории
  static editCategorySubmitBtn = '[data-testid="menu__edit-category-submit-btn"]'
  // Инпут селекта из списка готовых категорий
  static selectStandardCategory = '[data-testid="menu__select-standard-category"]'
  // Ввод имени категории при создании/редактировании
  static inputCategoryName = '[data-testid="menu__input-category-name"]'
  // Выбор режима кастомной категории
  static radioButtonCustom = '[data-testid="menu__radio-button-custom"]'
  // Выбор из списка готовых категорий
  static radioButtonPrimary = '[data-testid="menu__radio-button-primary"]'
  // Редактирование позиции. Чекбокс, что опция обязательна для выбора
  static positionOptionRequiredCheckbox = '[data-testid="menu__position-option-required-checkbox"]'
  // Кнопка удаления группы опций
  static positionOptionsGroupRemoveBtn = '[data-testid="menu__position-options-group-remove-btn"]'
  // Редактирование позиции. Кнопка "Добавить опцию"
  static positionAddOptionBtn = '[data-testid="menu__position-add-option-btn"]'
  // Редактирование позиции. Инпут имени группы опций
  static positionOptionsGroupNameInput = '[data-testid="menu__position-options-group-name-input"]'
  // Редактирование позиции. Инпут ввода имени опции
  static positionOptionNameInput = '[data-testid="menu__position-option-name-input"]'
  // Редактирование позиции. Инпут цены блюда
  static positionOptionPriceInput = '[data-testid="menu__position-option-price-input"]'
  // Редактирование позиции. Кнопка "Добавить группу опций"
  static positionAddOptionsGroupBtn = '[data-testid="menu__position-add-options-group-btn"]'
  // Селект меры измерения веса блюда
  static positionWeightMeasureSelect = '[data-testid="menu__position-weight-measure-select"]'
  // Селект НДС блюда
  static positionVatSelect = '[data-testid="menu__position-vat-select"]'
  // Список групп опций позиции
  static positionOptions = '[data-testid="menu__position-options"]'
  // Кнопка сохранения новой/отредактированной позиции
  static positionSubmitBtn = '[data-testid="menu__position-submit-btn"]'
  // Инпут названия блюда
  static positionNameInput = '[data-testid="menu__position-name-input"]'
  // Инпут состава блюда
  static positionContentInput = '[data-testid="menu__position-content-input"]'
  // Инпут веса блюда
  static positionWeightInput = '[data-testid="menu__position-weight-input"]'
  // Инпут цены блюда
  static positionPriceInput = '[data-testid="menu__position-price-input"]'
  // Checkbox cо статусои включения категории
  static categoryCheckboxChecked = '[data-testid="menu__category-checkbox-checked"]'
  // Checkbox наполовину окрашен в черный
  static categoryCheckboxSemiBlack = '[data-testid="menu__category-checkbox-semi-black"]'
  // Кнопка открытия контекстного меню категории или позиции
  static itemContextMenu = '[data-testid="menu__item-context-menu"]'
  // Категория меню. category.name - имя категории
  static category(categoryName: string | number) {
    return `[data-testid="menu__category-${categoryName}"]`
  }
  // Блюдо в списке
  static item(itemName: string | number) {
    return `[data-testid="menu__item-${itemName}"]`
  }
  // Опция в селекте выбора названия для новой категории
  static categoriesListOption(categoryName: string | number) {
    return `[data-testid="menu__categories-list-option-${categoryName}"]`
  }
  // Опция меры измерения веса в селекте
  static positionWeightMeasureOption(oName: string | number) {
    return `[data-testid="menu__position-weight-measure-option-${oName}"]`
  }
  // Опция НДС блюда в селекте
  static positionVatOption(oName: string | number) {
    return `[data-testid="menu__position-vat-option-${oName}"]`
  }
}
