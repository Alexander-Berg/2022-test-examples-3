export function mockSocket(this: WebdriverIO.Browser) {
  return this.execute(() => {
    window.WebSocket = null
  })
}
