'use strict';

const PO = require('../DistrPopup.page-object');

const findResourceRequested = require('../../../../../hermione/client-scripts/find-resource-requested');
const YABS_LINKHEAD = 'yabs?type=show';
const YABS_COUNTER_CLOSE = 'yabs?type=close';

hermione.only.in('chrome-desktop', 'Счетчики не браузерозависимые');
specs({
    feature: 'Popup на СЕРПе',
    type: 'C промо для скачивания Я.Бро',
}, () => {
    beforeEach(async function() {
        const linknext = '&linknext';

        await this.browser.yaOpenSerp({
            srcskip: 'YABS_DISTR',
            text: 'окна',
            foreverdata: '1222369131',
            data_filter: 'distr-popup',
        }, PO.distrPopup());

        await this.browser.yaCheckBaobabCounter(() => { }, {
            path: '/$page/promo_popup',
            event: 'tech',
            type: 'distr-popup-show',
        });

        await this.browser.pause(250);
        const result = await this.browser.execute(findResourceRequested, YABS_LINKHEAD + linknext);
        assert.isString(result, 'Счётчик на показ не сработал');
    });

    it('Внешний вид', async function() {
        await this.browser.assertView('plain', PO.distrPopup());
    });

    it('Проверка кнопки закрыть', async function() {
        await this.browser.yaCheckBaobabCounter(PO.distrPopup.closeButton(), {
            path: '/$page/promo_popup/close[@tags@close=1 and @behaviour@type="dynamic"]',
        });

        await this.browser.pause(250);
        const result = await this.browser.execute(findResourceRequested, YABS_COUNTER_CLOSE);
        assert.isString(result, 'Счётчик на закрытие не сработал');
        await this.browser.yaWaitForHidden(PO.distrPopup(), 'Попап-блок дистрибуции не закрылся');
    });

    it('Проверка кнопки установить', async function() {
        const url = await this.browser.yaParseHref(PO.distrPopup.installButton());
        assert.include(url.href, 'yabs?type=landing');

        await this.browser.yaCheckBaobabCounter(PO.distrPopup.installButton(), {
            path: '/$page/promo_popup/install',
        });

        await this.browser.pause(250);
        await this.browser.yaWaitForHidden(PO.distrPopup(), 3000, 'Должен быть скрыт попап-блок дистрибуции');
    });

    it('Взаимодействие с саджестом', async function() {
        await this.browser.click(PO.header.arrow.input.control());
        await this.browser.yaWaitForVisible(PO.mainSuggest.content(), 'Должен открыться саджест');
        await this.browser.yaWaitForHidden(PO.distrPopup(), 'Попап-блок дистрибуции не закрылся');
        await this.browser.moveToObject('body', 10, 10);
        await this.browser.buttonDown();
        await this.browser.yaWaitForHidden(PO.mainSuggest.content(), 'Саджест не скрылся');
        await this.browser.yaShouldNotBeVisible(PO.distrPopup(), 'Попап-блок дистрибуции не должен показываться');
    });
});
