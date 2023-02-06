import { Locator, Page } from 'playwright';

import { BasePage } from '../BasePage';
import { authenticate } from '../../utils/auth';
import config from '../../config';

export class AdvertisersPage extends BasePage {
    readonly #page: Page;

    readonly apiURL = /\/api\/v1\/advertisers\/\?/;

    advertiserCardMenuButton: Locator;

    constructor(page: Page) {
        super(page);
        this.#page = page;

        this.advertiserCardMenuButton = page.locator('[data-test-id=advertiserCardButton]');
    }

    async waitForAdvertisersResponseData() {
        await this.#page.goto(config.origin + '/advertiser');
        const response = await this.#page.waitForResponse(this.apiURL);
        return await response.json();
    }

    async currentAdvertiserName() {
        return this.#page.locator('data-test-id=currentAdvertiserName').textContent();
    }

    async currentAdvertiserNameVisible() {
        return this.#page.locator('data-test-id=currentAdvertiserName').waitFor({ state: 'visible' });
    }

    menuAdvertiserLinkSelector(name: string, id: number) {
        return this.#page.locator(`a[href*="${id}"]`).locator(`text=${name}`);
    }

    selector(selector: string) {
        return this.#page.locator(selector);
    }

    async routeStatus(url: string, status: number) {
        return this.#page.route(config.origin + url, route => route.fulfill({ status }));
    }

    async routeEmptyAdvertisersList() {
        await this.#page.route(this.apiURL, route => route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ advertisers: [], total_count: 0 }),
        }));

        return () => this.#page.unroute(this.apiURL);
    }

    async authenticate() {
        return authenticate(this.#page);
    }

    async waitForRedirect() {
        return this.#page.waitForNavigation();
    }

    async waitForRedirects() {
        return this.#page.waitForNavigation({ waitUntil: 'networkidle' });
    }

    async setLocalStorageAdvertiserID(id: string) {
        await this.#page.evaluate(id => localStorage.setItem('alpaca:selectedAdvertiser', id), id);
    }
}
