export class AuthSelectors {
  // Checkbox 'Запомнить меня'
  static rememberMe = '[data-testid="auth__remember-me"]'
  // Кнопка войти
  static submit = '[data-testid="auth__submit"]'
  // Кнопка забыл пароль
  static forgotPassword = '[data-testid="auth__forgot-password"]'
  // Поле ввода email
  static email = '[data-testid="auth__email"]'
  // Текст ошибки под полем ввода email
  static emailError = '[data-testid="auth__email-error"]'
  // Поле ввода пароля
  static password = '[data-testid="auth__password"]'
  // Текст ошибки под полем ввода пароля
  static passwordError = '[data-testid="auth__password-error"]'
  // Кнопка "Отменить" на странице сброса пароля
  static restorePasswordCancelBtn = '[data-testid="auth__restore-password-cancel-btn"]'
  // Кнопка "Восстановить" на странице сброса пароля
  static restorePasswordBtn = '[data-testid="auth__restore-password-btn"]'
  // Поле ввода email на странице сброса пароля
  static resetPasswordEmail = '[data-testid="auth__reset-password-email"]'
  // Кнопка "Войти" после сброса пароля
  static resetLoginBtn = '[data-testid="auth__reset-login-btn"]'
}
