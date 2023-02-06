const { oldBrowsersStub } = require('../page-objects/client');

describe('Заглушка для старых браузеров ->', () => {
    it('diskclient-1839: Проверка заглушки в старом браузере неавторизованным юзером', async function() {
        const bro = this.browser;
        await bro.url('/');
        await bro.assertView('diskclient-1839', 'body');
    });

    it('diskclient-1715: Проверка заглушки в старом браузере авторизованным юзером', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-01');
        await bro.url('/');
        await bro.yaWaitForVisible(oldBrowsersStub());
    });
});
