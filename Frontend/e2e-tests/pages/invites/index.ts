import { Page } from 'playwright';

import { authenticate, register } from '../../utils/auth';
import { NoAdvertiserPage } from '../no-advertiser';
import config from '../../config';

export class InvitesPage extends NoAdvertiserPage {
    readonly #page: Page;

    createdAdvertiserId?: string;

    constructor(page: Page) {
        super(page);
        this.#page = page;
    }

    async authenticate() {
        return authenticate(this.#page);
    }

    async register() {
        return register(this.#page);
    }

    async createAdvertiser(type: string) {
        await this.#page.goto(`${config.origin}/no-advertiser`);
        await this.clickCreateCabinet();
        await this.waitForPopup();

        await this.fillCompanyNameField(`autotest_${type}`);
        await this.clickRadio(type);

        await this.submitForm();
        const response = await this.waitForSubmitResponse();
        this.createdAdvertiserId = response.id;

        return response.id;
    }

    async openCreateInvitePopup() {
        await this.#page.goto(`${config.origin}/${this.createdAdvertiserId}/clients`);
        return this.#page.click('text="Добавить клиента"');
    }

    async openInvite(id: string) {
        return this.#page.goto(`${config.origin}/invite/${id}`);
    }

    async clickContinueButton() {
        return this.#page.click('text="Продолжить"');
    }

    async clickRadio(type: string) {
        await this.#page.click(`text="${type}"`);
    }

    async fillAdvertiserName(name: string) {
        return this.#page.fill('input[name="name"]', name);
    }

    async fillClientEmail(email: string) {
        return this.#page.fill('input[name="inviteEmail"]', email);
    }

    async fillClientId(id: string) {
        return this.#page.fill('input[name="clientId"]', id);
    }

    async clickSubmitInviteButton() {
        return this.#page.click('text="Создать"');
    }

    async clickAcceptButton() {
        return this.#page.click('text="Подтвердить"');
    }

    async clickCloseInviteButton() {
        return this.#page.click('text="Понятно"');
    }

    async getSubmittedInvite() {
        const response = await this.#page.waitForResponse(/invites/);
        return await response.json();
    }

    async getSuccessInviteSubmitNotification() {
        return this.#page.locator('p:has-text("Приглашение в кабинет отправлено")');
    }
}
