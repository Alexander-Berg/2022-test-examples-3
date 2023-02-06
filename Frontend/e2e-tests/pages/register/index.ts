import { Page } from 'playwright';

import { BasePage } from '../BasePage';
import config from '../../config';

export class RegisterPage extends BasePage {
    readonly #page: Page;

    constructor(page: Page) {
        super(page);
        this.#page = page;
    }

    async waitForAuthPopup() {
        await this.#page.waitForSelector('.register-form', { state: 'visible' });
    }

    async fillPasswordField(password: string) {
        await this.#page.fill('.register-form input[name="password"]', password);
    }

    async fillEmailField(email: string) {
        await this.#page.fill('.register-form input[name="email"]', email);
    }

    async submitForm() {
        await this.#page.click('.register-form button[type="submit"]');
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

    async goToRegister() {
        await this.#page.click('a[href="/auth/registration"]');
    }

    lookAtCode() {
        return new Promise(resolve => {
            this.#page.on('requestfinished', async request => {
                if (request.url().match(/\/code\/generate\//)) {
                    const answer = await request.response().then(res => res && res.json()).catch(() => {});
                    resolve(answer.code);
                }
            });
        });
    }

    async waitForCodeFormShowing() {
        await this.#page.waitForSelector('.code-confirmation-form', { state: 'visible' });
    }

    async fillConfirmationCode(code: string) {
        await this.#page.fill('.code-confirmation-form input[name="confirmationCode"]', code);
    }

    async submitConfirmationForm() {
        await this.#page.click('.code-confirmation-form button[type="submit"]');
    }

    async waitForRegisterRedirect(timeout = 5000) {
        return new Promise((resolve, reject) => {
            this.#page.on('requestfinished', async request => {
                if (request.url().match(/\/user\/register\//)) {
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
