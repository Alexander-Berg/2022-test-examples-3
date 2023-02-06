const { assert } = require('chai');
const clientObjects = require('../page-objects/client');
const blurActiveElement = require('../helpers/blurActiveElement');

describe('Планирование встречи ->', () => {
    const mainUrl = '/';

    it('telemost-308: страница подключения с пустым инпутом', async function () {
        const url = mainUrl;
        const bro = this.browser;
        await bro.yaLoginFast('yndx-telemost-test-0');
        await bro.url(url);
        await bro.setCookie({ name: 'background', value: '2' });
        await bro.url(url);
        await bro.yaWaitForVisible(clientObjects.common.continueConnectButton());
        await bro.click(clientObjects.common.continueConnectButton());
        await bro.yaWaitForVisible(clientObjects.common.connectScreenInput());
        await bro.yaAssertView('telemost-connect-with-empty-input', 'body', {
            ignoreElements: [clientObjects.common.psHeader.user.unreadTicker()]
        });

        const urlBeforeClick = await bro.getUrl();
        await bro.click(clientObjects.common.submit());
        await bro.yaWaitForVisible(clientObjects.common.connectScreenInputWrapperError());
        await bro.pause(500);
        const urlAfterClick = await bro.getUrl();
        assert(
            urlBeforeClick === urlAfterClick,
            'Инпут пуст, но зафиксирован переход на другую страницу после клика по кнопке'
        );
    });

    it('telemost-309: страница подключения с заполненным инпутом', async function () {
        const inputValue = '111111111111111111';
        const url = mainUrl;
        const bro = this.browser;
        await bro.yaLoginFast('yndx-telemost-test-0');
        await bro.url(url);
        await bro.setCookie({ name: 'background', value: '2' });
        await bro.url(url);
        await bro.yaWaitForVisible(clientObjects.common.continueConnectButton());
        await bro.click(clientObjects.common.continueConnectButton());

        await bro.yaWaitForVisible(clientObjects.common.connectScreenInput());

        await bro.setValue(clientObjects.common.connectScreenInput(), inputValue);
        await blurActiveElement(bro);
        await bro.pause(500);
        await bro.yaAssertView('telemost-connect-with-filled-input', 'body', {
            ignoreElements: [clientObjects.common.psHeader.user.unreadTicker()]
        });

        const urlBeforeClick = await bro.getUrl();
        await bro.click(clientObjects.common.connectScreenInput());
        await bro.pause(500);
        const urlAfterClick = await bro.getUrl();
        assert(
            urlBeforeClick === urlAfterClick,
            'Инпут заполнен, но НЕ зафиксирован переход на другую страницу после клика по кнопке'
        );
    });

    it('telemost-310: переход на страницу планирования, ожидание появления iframe и закрытие страницы', async function () {
        const url = mainUrl;
        const bro = this.browser;
        await bro.yaLoginFast('yndx-telemost-test-0');
        await bro.url(url);
        await bro.setCookie({ name: 'background', value: '2' });
        await bro.url(url);

        await bro.yaWaitForVisible(clientObjects.common.planCallButton());
        await bro.click(clientObjects.common.planCallButton());
        await bro.yaWaitForVisible(clientObjects.common.planningIframe());

        await bro.click(clientObjects.common.closePopupButton());
        await bro.yaAssertView('telemost-connect-main-screen-after-close-planning', 'body', {
            ignoreElements: [clientObjects.common.psHeader.user.unreadTicker()]
        });
    });
});
