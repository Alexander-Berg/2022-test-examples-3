export async function clearValue(this: WebdriverIO.Browser, selector: string) {
  const element = this.$(selector)
  await element.scrollIntoView()
  await element.waitForDisplayed()
  await element.click()
  await this.keys(['Control', 'a', 'Control', 'Backspace'])
}
