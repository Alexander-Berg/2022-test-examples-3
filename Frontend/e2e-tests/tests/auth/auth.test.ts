import { test, expect } from '@playwright/test';

import { getBrowser, Browser } from '../../browser';
import { AuthPage } from '../../pages/auth';
import config from '../../config';

let browser: Browser;
let page: AuthPage;

test.describe('Authorization', () => {
    test.beforeAll(() => {
        return getBrowser().then(browserInstance => {
            browser = browserInstance;
            page = new AuthPage(browser.page);
        });
    });

    test('Для неавторизованного пользователя происходит редирект на страницу авторизации', async() => {
        await browser.open('/');
        const location = await page.getLocationPathname();
        expect(location).toEqual('/auth/login');
    });

    test('Для неавторизованного пользователя происходит редирект с любой страницы на страницу авторизации', async() => {
        await browser.open(config.startURL);
        const location = await page.getLocationPathname();
        expect(location).toEqual('/auth/login');
    });

    test('Попап авторизации виден', async() => {
        await page.waitForAuthPopup();
    });

    test('Не заполнено поле email', async() => {
        await page.fillPasswordField('incorrect password');
        await page.submitAuthForm();
        expect(await page.getFormErrors()).toEqual('Электронная почта обязательна к заполнению');
    });

    test('Не заполнено поле пароль', async() => {
        await page.fillEmailField(config.auth.email);
        await page.fillPasswordField('');
        await page.submitAuthForm();
        expect(await page.getFormErrors()).toEqual('Пароль обязателен к заполнению');
    });

    test('Отправка неправильных данных авторизации', async() => {
        await page.fillEmailField(config.auth.email);
        await page.fillPasswordField('incorrect password');
        await page.submitAuthForm();
        expect(await page.getNotificationErrorText()).toEqual('Неверный email или пароль');
    });

    test('Отправка правильных данных авторизации', async() => {
        await page.fillEmailField(config.auth.email);
        await page.fillPasswordField(config.auth.password);
        await page.submitAuthForm();
        await page.waitForAuthRedirect();
    });
});
