export default abstract class Page {
    bro: WebdriverIO.Browser;
    abstract get title(): string;
    abstract get indicator(): string;

    constructor(bro: WebdriverIO.Browser) {
        this.bro = bro;
    }

    async openBase(path: string, indicator?: string) {
        await this.bro.url('/' + path);
        await this.bro.yaWaitForPageLoad();
        if (indicator) {
            await this.bro.yaWaitForVisible(indicator);
        }
    }
}
