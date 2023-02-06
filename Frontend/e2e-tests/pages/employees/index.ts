import { Page } from 'playwright';

import { authenticate, register } from '../../utils/auth';
import config from '../../config';
import { InvitesPage } from '../invites';

export class EmployeesPage extends InvitesPage {
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

    async toEmployeesList() {
        await this.#page.goto(`${config.origin}/${this.createdAdvertiserId}/employees`);
    }

    async openCreateInvitePopup() {
        await this.toEmployeesList();
        return this.#page.click('text="Пригласить сотрудника"');
    }

    async openEditEmployeePopup(email: string) {
        const row = await this.employeeRow(email);
        await row.locator('button').click();
        return this.#page.click('text="Редактировать"');
    }

    async openArchiveEmployeePopup(email: string) {
        const row = await this.employeeRow(email);
        await row.locator('button').click();
        return this.#page.click('text="Отозвать роль"');
    }

    async openInvite(id: string) {
        return this.#page.goto(`${config.origin}/employee-invite/${id}`);
    }

    async fillClientEmail(email: string) {
        return this.#page.fill('input[name="invitedEmail"]', email);
    }

    async clickButton(action: string) {
        return this.#page.click(`text="${action}"`, { timeout: 3000 });
    }

    async getSubmittedInvite() {
        const response = await this.#page.waitForResponse(/invites/);
        return await response.json();
    }

    async getEmployeeRoleCell(email: string) {
        const row = await this.employeeRow(email);
        return row.locator(':nth-match(td, 3)');
    }

    async employeeRow(email: string) {
        return this.#page.locator(`tr:has-text("${email}")`);
    }
}
