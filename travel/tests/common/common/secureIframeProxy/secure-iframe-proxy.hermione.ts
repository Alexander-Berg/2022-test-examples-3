import {assert} from 'chai';

import {SECOND} from 'helpers/constants/dates';

import TestControlPanelApp from 'helpers/project/testControlPanel/app/TestControlPanelApp';

describe('SecureIframeProxy', () => {
    it('Двусторонний обмен сообщениями', async function () {
        const app = new TestControlPanelApp(this.browser);

        const test3DSPage = await app.goToTest3DSPage();

        const {test3DSDemoPage} = test3DSPage;

        await test3DSDemoPage.waitUntilLoaded();

        await test3DSDemoPage.messageList.sendButton.click();

        assert.equal(
            await test3DSDemoPage.messageList.messages.count(),
            0,
            'Список сообщений на странице должен быть пуст',
        );

        await test3DSDemoPage.iframe.workInFrame(async () => {
            const {test3DSFramePage} = test3DSPage;

            await test3DSFramePage.waitUntilLoaded();

            assert.equal(
                await test3DSFramePage.messageList.messages.count(),
                1,
                'Список сообщений внутри фрейма должен содержать одно сообщение',
            );

            await test3DSFramePage.messageList.sendButton.clickJS();
        });

        assert.equal(
            await test3DSDemoPage.messageList.messages.count(),
            1,
            'Список сообщений на странице должен содержать одно сообщение',
        );
    });

    it('Открытие страниц вне политик csp', async function () {
        const app = new TestControlPanelApp(this.browser);

        const test3DSExternalDemoPage = await app.goToTest3DSExternalDemoPage();

        await test3DSExternalDemoPage.iframe.workInFrame(async () => {
            const logoElement = this.browser.$('svg');

            await logoElement.waitForExist({timeout: 10 * SECOND});

            assert.isTrue(
                await logoElement.isDisplayed(),
                'Должен отобразиться логотип',
            );
        });
    });
});
