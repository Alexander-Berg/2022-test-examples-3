import assert from 'assert';
import { authorize } from '../auth';

describe('Авторизация', function() {
    it('Неавторизованный пользователь перенаправляется на страницу авторизации', async({ browser }) => {
        await browser.url('/partner-office');

        const title = await browser.getTitle();
        assert(title.includes('Авторизация'));
    });

    it('Авторизованный пользователь не перенаправляется на страницу авторизации', async({ browser }) => {
        await authorize(browser);
        await browser.url('/partner-office');

        assert.strictEqual(await browser.getTitle(), 'Агентский кабинет');
    });
});
