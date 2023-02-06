import { Page } from 'playwright';

const selectors = {
    notificationToast: '.notification-toast',
    notificationToastCloseButton: '.notification-toast button',
};

export abstract class BasePage {
    #page: Page

    constructor(page: Page) {
        this.#page = page;
    }

    async getLocationPathname(): Promise<string> {
        return await this.#page.evaluate(() => window.location.pathname);
    }

    async screenshot() {
        return await this.#page.screenshot({ path: './screenshots/weird-screenshot.png' });
    }

    async getNotificationErrorText() {
        return this.#page.textContent(selectors.notificationToast);
    }

    noErrors(timeout: number = 5000) {
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                resolve(undefined);
            }, timeout);
            this.#page.on('pageerror', exception => {
                reject(`Expecting no errors, got : Uncaught exception: "${exception}"`);
            });

            this.#page.waitForSelector(selectors.notificationToast, { state: 'attached' }).then(() => {
                this.#page.textContent(selectors.notificationToast).then(text => {
                    reject(new Error(`Expecting no errors, got : ${text}`));
                });
            });
        });
    }

    async closeErrorNotifications() {
        await this.#page.locator(selectors.notificationToastCloseButton).click();
        await this.#page.waitForSelector(selectors.notificationToastCloseButton, { state: 'detached' });
    }

    async pause() {
        return this.#page.pause();
    }
}
