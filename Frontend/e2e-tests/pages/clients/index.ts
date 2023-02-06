import { Page } from 'playwright';

import { BasePage } from '../BasePage';

export class ClientsPage extends BasePage {
    readonly #page: Page;

    constructor(page: Page) {
        super(page);
        this.#page = page;
    }

    init() {
        this.#page;
    }
}
