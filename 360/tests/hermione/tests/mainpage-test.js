const { assert } = require('chai');
const clientObjects = require('../page-objects/client');

describe('Главная страница ->', () => {
    const mainUrl = '/';

    it('telemost: внешний вид главной страницы с единой шапкой персональных сервисов', async function () {
        const bro = this.browser;
        await bro.yaLoginFast('yndx-telemost-test-0');
        await bro.url(mainUrl);
        await bro.setCookie({ name: 'background', value: '2' });
        await bro.url(mainUrl);
        await bro.yaWaitForVisible(clientObjects.common.createConferenceButton());
        await bro.yaWaitForVisible(clientObjects.common.psHeader());
        assert(!(await bro.isVisible(clientObjects.common.psHeaderMorePopup())));
        await bro.click(clientObjects.common.psHeader.more());
        await bro.yaWaitForVisible(clientObjects.common.psHeaderMorePopup());

        await bro.yaResetPointerPosition(); // unhover "More" icon
        await bro.pause(1000);
        await bro.yaAssertView('telemost-mainpage-with-ps-header', 'body', {
            ignoreElements: [
                clientObjects.common.psHeader.user.unreadTicker(),
                clientObjects.common.psHeaderMorePopup.calendarIcon()
            ]
        });
    });

    it('telemost: кнопка входа для неавторизованного пользователя', async function () {
        const bro = this.browser;
        const url = `${mainUrl}?test-id=0`;
        await bro.url(url);
        await bro.setCookie({ name: 'background', value: '2' });
        await bro.url(url);
        await bro.yaAssertView('telemost-mainpage-with-login', 'body', {
            ignoreElements: [clientObjects.common.psHeader.center()]
        });

        await bro.click(clientObjects.common.psHeader.login());
        const passportUrl = await bro.getUrl();
        assert(passportUrl.startsWith('https://passport.yandex.ru'));
    });
});
