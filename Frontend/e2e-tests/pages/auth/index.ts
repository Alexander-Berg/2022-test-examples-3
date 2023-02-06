import { Page } from 'playwright';

import { BasePage } from '../BasePage';
import config from '../../config';

export class AuthPage extends BasePage {
    readonly #page: Page;

    constructor(page: Page) {
        super(page);
        this.#page = page;
    }

    async waitForAuthPopup() {
        await this.#page.waitForSelector('.auth-form', { state: 'visible' });
    }

    async fillPasswordField(password: string) {
        await this.#page.fill('.auth-form input[name="password"]', password);
    }

    async fillEmailField(email: string) {
        await this.#page.fill('.auth-form input[name="email"]', email);
    }

    async submitAuthForm() {
        await this.#page.click('.auth-form button[type="submit"]');
    }

    noErrors(timeout: number = 5000) {
        return Promise.all([
            super.noErrors(timeout),
            new Promise(async(resolve, reject) => {
                setTimeout(resolve, timeout);
                this.#page.textContent('.input-field-error:not(:empty)').then(error => {
                    reject(new Error(`Expecting no errors, got: ${error}`));
                });
            }),
        ]);
    }

    async getFormErrors() {
        return await this.#page.textContent('.input-field-error:not(:empty)');
    }

    async waitForAuthRedirect(timeout = 5000) {
        return new Promise((resolve, reject) => {
            this.#page.on('requestfinished', async request => {
                if (request.url().match(/\/user\/login\//)) {
                    let timeoutNotExceeded = true;
                    let timeStart = Date.now();
                    while (timeoutNotExceeded) {
                        const location = await this.getLocationPathname();
                        if (location === config.startURL) {
                            return resolve(undefined);
                        }
                        timeoutNotExceeded = Date.now() - timeStart < timeout;
                    }
                    reject();
                }
            });
        });
    }
}
