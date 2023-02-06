export class MailCatcherSelectors {
  // Input поиска по эмайлам
  static SearchInput = 'input[name=search]'
  // Первый эмэйл в списке
  static FirstMailRow = 'tr:not([style])[data-message-id]'
  // Ссылка на сброс пароля в письме
  static ResetPasswordLink = 'a.email__footer-link'
}

export class MailCatcherPO {
  constructor(protected browser: WebdriverIO.Browser) {}

  async open() {
    await this.browser.url('http://testing.lxc.eda.tst.yandex.net:1080')
    await this.browser.wait(1000)
  }

  /*
  Можно фильтровать по любой колонке mail catcher From, To, Subject, Received
   */
  async filterMails(filter: string) {
    await this.browser.$(MailCatcherSelectors.SearchInput).setValue(filter)
    await this.browser.wait(2000)
  }

  async clickFirstMail() {
    await this.browser.$(MailCatcherSelectors.FirstMailRow).click()
  }

  async focusMailIframe() {
    await this.browser.switchToFrame(0)
  }
}
