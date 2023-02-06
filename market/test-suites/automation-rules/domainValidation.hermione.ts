import {expect} from 'chai';

import {fillAutomationRuleWithHttpRequestAction} from './helpers/createAutomationRuleWithHttpRequestAction';
import Button from '../../page-objects/button';
import {CHECK_INTERVAL, LONG_TIMEOUT_MS} from '../../constants';

const RED_COLOR = 'rgba(211,47,47,1)';

/**
 * Проверяем, что появляется вся необходимая информация о том, что
 * правило автоматизации нельзя создать при указании некорректного домена в URL при выборе TVM авторизации
 */
describe(`ocrm-1693: Валидация домена в URL при выборе TVM авторизации в правилах автоматизации`, () => {
    it('работает корректно', async function() {
        await fillAutomationRuleWithHttpRequestAction(this, 'https://test.org', 'safetyTvm');
        const saveButton = new Button(this.browser, 'body', '[data-ow-test-modal-controls="save"]');

        const redTitle = this.browser.$('p=URL не подходит для выбранной TVM авторизации');
        const redPanel = this.browser.$('span=Все нужные поля должны быть корректно заполнены');

        await saveButton.clickButton();
        const isRedTitleDisplayed = await redTitle.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
            timeoutMsg: 'Не дождались появления текста о некорректности заполнения поля',
        });

        expect(isRedTitleDisplayed).to.equal(true, 'Под полем URL не отобразился нужный текст');

        const isRedPanelDisplayed = await redPanel.isDisplayed();

        expect(isRedPanelDisplayed).to.equal(true, 'В верхней части модального окна не отобразилось нужное сообщение');

        const input = this.browser.$('[data-ow-test-http-request-parameter="url"] fieldset');

        this.browser.waitUntil(
            async () => String((await input.getCSSProperty('border-bottom-color')).value) === RED_COLOR,
            {
                timeout: LONG_TIMEOUT_MS,
                timeoutMsg: 'Не дождались подсветки инпута красным',
                interval: CHECK_INTERVAL,
            }
        );
    });
});
