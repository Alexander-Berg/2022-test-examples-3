import { authorize as authCommand } from '@yandex-int/hermione-auth-commands/build/util';
import { VaultClient } from '@yandex-int/vault-client';

let userPromise: Promise<{ login: string, pass: string } | null> = Promise.resolve(null);

if (process.env.VAULT_TOKEN) {
    const client = new VaultClient(process.env.VAULT_TOKEN, {}, { rejectUnauthorized: false });
    userPromise = client.getVersion('ver-01g1n85rzq55ycacyaqq9mhnz8').then(items => {
        const item = items[0];

        if (!item) {
            throw new Error('VAULT_TOKEN problem');
        }
        return {
            login: item.key,
            pass: item.value,
        };
    });
} else if (process.env.TEST_LOGIN && process.env.TEST_PASSWORD) {
    userPromise = Promise.resolve({
        login: process.env.TEST_LOGIN,
        pass: process.env.TEST_PASSWORD,
    });
}

export async function authorize(browser: WebdriverIO.Browser) {
    const user = await userPromise;

    if (!user) {
        throw new Error('No test user auth info');
    }

    await authCommand(
        browser,
        'https://passport.yandex.ru',
        user.login,
        user.pass,
    );
}
