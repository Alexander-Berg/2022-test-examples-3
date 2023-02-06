export class UiSelectors {
  // Сообщение в всплывашке
  static snackbarMessage = '[data-testid="ui__snackbar-message"]'
  // Страница целиком без сайдбара
  static page = '[data-testid="ui__page"]'
  // Контент страницы
  static pageContent = '[data-testid="ui__page-content"]'
  // Кнопка показа/скрытия сайдбара
  static sidebarBurger = '[data-testid="ui__sidebar-burger"]'
  // Шторка сайдбара (навигация)
  static sidebarDrawer = '[data-testid="ui__sidebar-drawer"]'
  // Dialog root
  static dialog = '[data-testid="ui__dialog"]'
  // Верхняя кнопка в компоненте Dialog
  static dialogCancelButton = '[data-testid="ui__dialog-cancel-button"]'
  // Нижняя кнопка в компоненте Dialog
  static dialogApplyButton = '[data-testid="ui__dialog-apply-button"]'
  // Шторка
  static drawer = '[data-testid="ui__drawer"]'
  // Загрузчик файлов
  static fileUpload = '[data-testid="ui__file-upload"]'
  // input элемент загрузки файлов
  static fileUploadInput = '[data-testid="ui__file-upload-input"]'
  // Modal root
  static modal = '[data-testid="ui__modal"]'
  // Modal close icon
  static modalClose = '[data-testid="ui__modal-close"]'
  // Refresh button
  static refreshButton = '[data-testid="ui__refresh-button"]'
  // input элемент в селекторе ресторанов
  static autocompleteInput = '[data-testid="ui__autocomplete-input"]'
  // Spinner
  static spinner = '[data-testid="ui__spinner"]'
  // Ссылка на раздел в сайдбаре. link.name - название раздела
  static sidebarSection(linkName: string | number) {
    return `[data-testid="ui__sidebar-section-${linkName}"]`
  }
}
