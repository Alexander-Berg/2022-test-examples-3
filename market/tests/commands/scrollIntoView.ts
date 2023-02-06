export async function scrollIntoView(this: WebdriverIO.Browser, selector: string) {
  const element = this.$(selector)
  await element.scrollIntoView()
  await element.waitForDisplayed()
}
