export function waitForHidden(this: WebdriverIO.Browser, selector: string, ms = 5000) {
  const element = this.$(selector)
  return element.waitForDisplayed({timeout: ms, reverse: true})
}
