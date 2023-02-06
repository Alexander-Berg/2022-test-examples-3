import { Locator, Page } from 'playwright';

import { BasePage } from '../../pages/BasePage';
import { authenticate } from '../../utils/auth';

export class PageWithBalance extends BasePage {
    readonly #page: Page;

    balanceButton: Locator;

    constructor(page: Page) {
        super(page);
        this.#page = page;

        this.balanceButton = this.#page.locator('button:has-text("Пополнить")');
    }

    selector(selector: string) {
        return this.#page.locator(selector);
    }

    async authenticate() {
        return authenticate(this.#page);
    }

    async reload() {
        await this.#page.reload();
    }

    async waitForRedirect() {
        return this.#page.waitForNavigation();
    }

    async fillDoc() {
        await this.#page.locator('[name="doc"]').click();
        await this.#page.locator('[role="option"]').first().click();
    }

    async fillDeposit() {
        await this.#page.fill('text="Сумма пополнения"', '2000');
    }

    async fillPaymentType() {
        await this.#page.locator('label:has-text("Банковская карта")').click();
    }

    async submit() {
        await this.#page.locator('button:has-text("Продолжить")').click();
    }

    async getSuccessDialogHeading() {
        return this.#page.locator('text="Счет выставлен"');
    }
    async getFormErrors() {
        return await this.#page.textContent('.input-field-error:not(:empty)');
    }
}
