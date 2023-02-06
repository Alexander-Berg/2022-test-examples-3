import { Page } from '@playwright/test';

import { AuthPage } from '../pages/auth';
import config from '../config';
import { RegisterPage } from '../pages/register';
import { REGISTRATION_URL } from '../tests/register/register.test';

export const authenticate = async(page: Page, credentials?: { email: string, password: string }) => {
    const authPage = new AuthPage(page);

    await page.goto(config.origin + '/auth/login');
    await authPage.fillEmailField(credentials?.email ?? config.auth.email);
    await authPage.fillPasswordField(credentials?.password ?? config.auth.password);
    await authPage.submitAuthForm();

    await page.waitForNavigation();
};

export const register = async(page: Page): Promise<{ email: string, password: string }> => {
    const registerPage = new RegisterPage(page);
    const credentials = {
        email: config.auth.getNewEmail(),
        password: config.auth.password,
    };

    await page.goto(config.origin + REGISTRATION_URL);
    const codeWaiting = registerPage.lookAtCode();
    await registerPage.fillEmailField(credentials.email);
    await registerPage.fillPasswordField(credentials.password);
    await registerPage.submitForm();
    const code = (await codeWaiting) as string;
    await registerPage.waitForCodeFormShowing();
    await registerPage.fillConfirmationCode(code);
    await registerPage.submitConfirmationForm();

    return credentials;
};
