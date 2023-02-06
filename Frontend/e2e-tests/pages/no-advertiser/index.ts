import { Locator, Page } from 'playwright';

import { authenticate } from '../../utils/auth';
import { BasePage } from '../BasePage';

export class NoAdvertiserPage extends BasePage {
    readonly #page: Page;
    readonly selector: string = '.create-cabinet-form'
    readonly createCabinetForm: Locator;

    constructor(page: Page) {
        super(page);
        this.#page = page;
        this.createCabinetForm = page.locator(this.selector);
    }

    get call() {
        return this.#page;
    }

    async auth() {
        await authenticate(this.#page);
    }

    async clickCreateCabinet() {
        await this.#page.click('data-test-id=create-cabinet-button');
    }

    async waitForPopup() {
        await this.#page.waitForSelector(this.selector, { state: 'visible' });
    }

    async fillCompanyNameField(companyName: string) {
        await this.#page.fill(`${this.selector} input[name="cabinetName"]`, companyName);
    }

    async submitForm() {
        await this.#page.click(`${this.selector} button[type="submit"]`);
    }

    async getFormErrors() {
        return await this.#page.textContent('.input-field-error');
    }

    async getFormTermsError() {
        return await this.#page.textContent('[data-test-id=create-cabinet-terms] .input-field-error');
    }

    async waitForRedirect() {
        return this.#page.waitForNavigation();
    }

    async waitForSubmitResponse() {
        const response = await this.#page.waitForResponse(/\/api\/v1\/advertisers/);
        return await response.json();
    }
}
