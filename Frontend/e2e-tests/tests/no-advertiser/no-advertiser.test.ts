import { test, expect } from '@playwright/test';

import { NoAdvertiserPage } from '../../pages/no-advertiser';

let page: NoAdvertiserPage;

test.describe('Создание нового кабинета адвертайзера', () => {
    const companyName: string = 'Test';

    test.beforeAll(async({ browser }) => {
        page = new NoAdvertiserPage(await browser.newPage());
        await page.auth();
        await page.call.goto('/no-advertiser');
    });

    test.beforeEach(async() => {
        await page.call.reload();
        await page.clickCreateCabinet();
        await page.waitForPopup();
    });

    test('Со страницы no-advertiser мы можем открыть модальное окно для создания кабинета', async() => {
        await expect(page.createCabinetForm).toBeVisible();
    });

    test('Если не указано название кабинета выводится ошибка', async() => {
        await page.submitForm();
        await expect(await page.getFormErrors()).toEqual('Название обязательно к заполнению');
    });

    test('При заполнении правильных данных происходит переход на стартовую страницу', async() => {
        await page.fillCompanyNameField(companyName);

        await page.submitForm();
        const response = await page.waitForSubmitResponse();

        const location = await page.getLocationPathname();
        expect(location.includes(response.id));
    });
});
