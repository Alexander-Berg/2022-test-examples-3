import { test, expect } from '@playwright/test';

import { Browser, getBrowser } from '../../browser';
import { PageWithBalance } from '../../components/balance';

let browser: Browser;
let page: PageWithBalance;

test.describe('Диалог пополнения баланса', () => {
    test.beforeAll(async() => {
        return getBrowser().then(async browserInstance => {
            browser = browserInstance;
            page = new PageWithBalance(browser.page);
            await page.authenticate();
        });
    });

    test.beforeEach(async() => {
        await page.reload();
    });

    test('Отправка правильных данных пополнения баланса', async() => {
        await page.balanceButton.click();

        await page.fillDoc();
        await page.fillPaymentType();
        await page.fillDeposit();
        await page.submit();

        await expect(await page.getSuccessDialogHeading()).toBeVisible();
    });

    test('Если не указан договор, то отображается ошибка', async() => {
        await page.balanceButton.click();

        await page.fillPaymentType();
        await page.fillDeposit();
        await page.submit();

        expect(await page.getFormErrors()).toEqual('Договор обязателен к заполнению');
    });

    test('Если не указан способ пополненя, то отображается ошибка', async() => {
        await page.balanceButton.click();

        await page.fillDoc();
        await page.fillDeposit();
        await page.submit();

        expect(await page.getFormErrors()).toEqual('Способ пополнения обязателен к заполнению');
    });

    test('Если не указана сумма пополнения, то отображается ошибка', async() => {
        await page.balanceButton.click();

        await page.fillDoc();
        await page.fillPaymentType();
        await page.submit();

        expect(await page.getFormErrors()).toEqual('Сумма пополнения обязательна к заполнению');
    });
});
