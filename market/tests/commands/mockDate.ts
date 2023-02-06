export function mockDate(this: WebdriverIO.Browser, date: Date) {
  return this.execute((isoDate) => {
    Date.now = () => new Date(isoDate).getTime()
  }, date.toISOString())
}
