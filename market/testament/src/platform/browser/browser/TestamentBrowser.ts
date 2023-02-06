import {BrowserContext} from 'playwright';

import {TestamentBrowserPool} from './pool/BrowserPool';

const opts = {
    max: parseInt(process.env.TESTAMENT_BROWSER_POOL_MAX ?? '', 10) || 50,
    min: 0,
};

export class TestamentBrowser {
    #browserPool: TestamentBrowserPool;

    constructor(wsEndpoint: string) {
        this.#browserPool = new TestamentBrowserPool(wsEndpoint, opts);
    }

    getContext(): Promise<BrowserContext> {
        return this.#browserPool.getContext();
    }

    closeContext(context: BrowserContext): Promise<void> {
        return this.#browserPool.closeContext(context);
    }

    destroy(): Promise<void> {
        return this.#browserPool.drain();
    }
}
