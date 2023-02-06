import { test, expect } from '@playwright/test';

import { getBrowser } from '../../browser';
import { EmployeesPage } from '../../pages/employees';

let adminPage: EmployeesPage;
let employeePage: EmployeesPage;

let employeeEmail: string;

test.describe.serial('Выдача роли сотрудника адвертайзера пользователю', () => {
    test.beforeAll(async() => {
        adminPage = new EmployeesPage((await getBrowser()).page);
        await adminPage.register();

        employeePage = new EmployeesPage((await getBrowser()).page);
        const clientCredentials = await employeePage.register();
        employeeEmail = clientCredentials.email;

        await adminPage.createAdvertiser('Агентство');
    });

    test('Владелец адвертайзера может пригласить сотрудника', async() => {
        await adminPage.openCreateInvitePopup();
        await adminPage.fillClientEmail(employeeEmail);
        await adminPage.clickRadio('Администратор');
        await adminPage.clickButton('Добавить');
        const inviteId = (await adminPage.getSubmittedInvite()).id;

        await expect(await adminPage.getSuccessInviteSubmitNotification()).toBeVisible();

        await employeePage.openInvite(inviteId);
        await employeePage.clickButton('Подтвердить');

        await employeePage.waitForRedirect();

        await adminPage.toEmployeesList();
        await expect(await adminPage.employeeRow(employeeEmail)).toBeVisible();
    });
});
