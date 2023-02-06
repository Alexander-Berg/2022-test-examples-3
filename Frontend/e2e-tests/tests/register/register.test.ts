import { test, expect } from '@playwright/test';

import { getBrowser, Browser } from '../../browser';
import { RegisterPage } from '../../pages/register';
import config from '../../config';

export const REGISTRATION_URL = '/auth/registration';

let browser: Browser;
let page: RegisterPage;

test.describe('Register', () => {
    test.beforeAll(async() => {
        return getBrowser().then(browserInstance => {
            browser = browserInstance;
            page = new RegisterPage(browser.page);
        });
    });

    test('С экрана логина мы можем перейти на экран регистрации', async() => {
        await browser.open('/auth/login');
        await page.goToRegister();
        const location = await page.getLocationPathname();
        expect(location).toEqual(REGISTRATION_URL);
    });

    test('Если не указан email выводится ошибка', async() => {
        await browser.open(REGISTRATION_URL);
        await page.fillPasswordField(config.auth.password);
        await page.submitForm();
        expect(await page.getFormErrors()).toEqual('Электронная почта обязательна к заполнению');
    });

    test('Если не указан пароль выводится ошибка', async() => {
        await browser.open(REGISTRATION_URL);
        await page.fillEmailField(config.auth.email);
        await page.submitForm();
        expect(await page.getFormErrors()).toEqual('Пароль обязателен к заполнению');
    });

    test('Если указан существующий email выводится ошибка', async() => {
        await browser.open(REGISTRATION_URL);
        await page.fillEmailField(config.auth.email);
        await page.fillPasswordField(config.auth.password);
        await page.submitForm();
        expect(await page.getNotificationErrorText()).toEqual('Пользователь с таким email уже существует');
    });

    test('При заполнении правильных данных происходит переход на окно подтверждения почты', async() => {
        await browser.open(REGISTRATION_URL);
        await page.fillEmailField(config.auth.getNewEmail());
        await page.fillPasswordField(config.auth.password);
        await page.submitForm();
        await page.waitForCodeFormShowing();
    });

    // сохраняем предыдущее состояние
    test('После перехода на окно подтверждения почты мы не можем отправить форму без кода подтверждения', async() => {
        page.submitConfirmationForm();
        expect(await page.getFormErrors()).toEqual('Код подтверждения обязателен к заполнению');
    });

    // сохраняем предыдущее состояние
    test('После перехода на окно подтверждения почты вводим длинный код', async() => {
        await page.fillConfirmationCode('это не код подтверждения');
        await page.submitConfirmationForm();
        expect(await page.getFormErrors()).toEqual('Код подтверждения должен содержать 6 символов');
    });

    // сохраняем предыдущее состояние
    test('После перехода на окно подтверждения почты вводим неправильный код', async() => {
        await page.fillConfirmationCode('не-код');
        await page.submitConfirmationForm();
        expect(await page.getNotificationErrorText()).toEqual('Код подтверждения неверен');
    });

    test('После перехода на окно подтверждения почты мы можем подтвердить почту', async() => {
        await browser.open(REGISTRATION_URL);
        const codeWaiting = page.lookAtCode();
        await page.fillEmailField(config.auth.getNewEmail());
        await page.fillPasswordField(config.auth.password);
        await page.submitForm();
        const code = (await codeWaiting) as string;
        await page.waitForCodeFormShowing();
        await page.fillConfirmationCode(code);
        page.submitConfirmationForm();
        await page.waitForRegisterRedirect();
    });
});
