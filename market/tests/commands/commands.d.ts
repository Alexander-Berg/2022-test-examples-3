declare namespace WebdriverIO {
  interface Browser {
    mockSocket(): Promise<void>
    authorize(): Promise<void>
    authorize(email: string, password: string): Promise<void>
    logout(): Promise<void>

    wait(ms: number): Promise<void>

    /**
     * @deprecated - Use clearValue on element
     */
    clearValue(selector: string): Promise<void>

    waitForHidden(selector: string, ms?: number): Promise<boolean>

    scrollIntoView(selector: string): Promise<void>

    mockDate(date: Date): Promise<void>
  }
}
