export async function wait(this: WebdriverIO.Browser, ms: number) {
  await this.waitUntil(() => new Promise((resolve) => setTimeout(() => resolve(true), ms)))
}
