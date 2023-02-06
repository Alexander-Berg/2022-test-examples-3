import {chromium} from 'playwright';

const opts = {
    headless: Boolean(process.env.TESTAMENT_BROWSER_HEADLESS ?? true),
};

export class TestamentBrowserWorker {
    #wsEndpoint?: string;

    async init(): Promise<void> {
        const browserServer = await chromium.launchServer({
            headless: opts.headless,
        });
        this.#wsEndpoint = browserServer.wsEndpoint();
    }

    async getWsEndpoint(): Promise<string> {
        return this.#wsEndpoint ?? '';
    }
}

export default new TestamentBrowserWorker();
