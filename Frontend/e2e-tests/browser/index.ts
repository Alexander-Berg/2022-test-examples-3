import playwright from 'playwright';

import config from '../config';

import type { Browser as PlaywrightBrowser, BrowserContext, Page } from 'playwright';

const browserType = 'chromium';

export async function getBrowser(): Promise<Browser> {
    const browser = await playwright[browserType].launch({ devtools: Boolean(process.env.PWDEBUG) });
    const context = await browser.newContext();
    const page = await context.newPage();
    return new Browser({ browser, context, page });
}

export class Browser {
    browser: PlaywrightBrowser;
    context: BrowserContext;
    page: Page;
    origin: string = config.origin;

    constructor({ browser, context, page }: {browser: PlaywrightBrowser, context: BrowserContext, page: Page}) {
        this.browser = browser;
        this.context = context;
        this.page = page;
    }

    async open(url: string) {
        await this.page.goto(`${this.origin}${url}`);
    }

    setOrigin(origin: string) {
        this.origin = origin;
    }

    close() {
        return this.browser.close();
    }
}
