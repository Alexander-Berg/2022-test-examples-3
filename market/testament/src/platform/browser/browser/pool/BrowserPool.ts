import {chromium, Browser, BrowserContext} from 'playwright';
import * as genericPool from 'generic-pool';
import {Pool} from 'generic-pool';

export interface TestamentBrowserPoolOpts {
    max: number;
    min: number;
}

export class TestamentBrowserPool {
    readonly #opts: Partial<Readonly<TestamentBrowserPoolOpts>>;

    #browserPromise?: Promise<Browser>;

    #contextPool!: Pool<BrowserContext>;

    #browserPool!: Pool<Browser>;

    #wsEndpoint: string;

    constructor(wsEndpoint: string, opts: TestamentBrowserPoolOpts) {
        this.#opts = {...opts};
        this.#wsEndpoint = wsEndpoint;

        this.initBrowserPool();
        this.initContextPool();
    }

    public getContext(): Promise<BrowserContext> {
        return this.#contextPool.acquire();
    }

    public closeContext(context: BrowserContext): Promise<void> {
        return this.#contextPool.release(context);
    }

    public async drain(): Promise<void> {
        await this.#contextPool.clear();
        await (await this.getBrowser()).close();
        await this.#browserPool?.clear();
    }

    private initBrowserPool(): void {
        const factory = {
            create: () => chromium.connect(this.#wsEndpoint),
            destroy: (browser: Browser) => browser.close(),
        };

        try {
            // На будущее делаем пул браузеров
            this.#browserPool = genericPool.createPool(factory, {
                max: 1,
                min: 0,
            });
        } catch (e) {
            throw new Error(`error while creating browser pool ${e}`);
        }
    }

    private initContextPool(): void {
        const factory = {
            create: async () => {
                const browser = await this.getBrowser();

                return browser.newContext();
            },
            destroy: (context: BrowserContext) => context.close(),
        };

        try {
            this.#contextPool = genericPool.createPool(factory, {
                max: this.#opts.max,
                min: this.#opts.min,
            });
        } catch (e) {
            throw new Error(`error while creating browser context pool ${e}`);
        }
    }

    private getBrowser(): Promise<Browser> {
        this.#browserPromise =
            this.#browserPromise ?? this.#browserPool.acquire();

        return this.#browserPromise;
    }
}
