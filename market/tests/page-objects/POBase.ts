import {UiSelectors} from './selectors.generated/UiSelectors'

export abstract class POBase {
  abstract readonly pageName: string

  constructor(protected browser: WebdriverIO.Browser) {}

  async open() {
    await this.browser.$(UiSelectors.sidebarSection(this.pageName)).click()
    await this.waitForOpen()
  }

  async confirmDialog(hideTimeout = 1000) {
    await this.browser.$(UiSelectors.dialogApplyButton).waitForDisplayed()
    await this.browser.$(UiSelectors.dialogApplyButton).waitForClickable()
    await this.browser.$(UiSelectors.dialogApplyButton).click()
    await this.browser.waitForHidden(UiSelectors.dialog, hideTimeout)
  }

  async rejectDialog() {
    await this.browser.$(UiSelectors.dialogCancelButton).waitForDisplayed()
    await this.browser.$(UiSelectors.dialogCancelButton).click()
    await this.browser.waitForHidden(UiSelectors.dialog)
  }

  protected async waitForOpen() {
    await this.browser.$(UiSelectors.pageContent).waitForDisplayed({timeout: 15000})
    const hasSpinner = await this.browser.$(UiSelectors.spinner).isDisplayed()
    if (hasSpinner) {
      await this.browser.waitForHidden(UiSelectors.spinner)
    }
  }
}
