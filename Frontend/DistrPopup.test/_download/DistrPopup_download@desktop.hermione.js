'use strict';

const PO = require('../DistrPopup.page-object');

const findResourceRequested = require('../../../../../hermione/client-scripts/find-resource-requested');
const YABS_LINKHEAD = 'yabs?type=show';
const YABS_COUNTER_CLOSE = 'yabs?type=close';
const YABS_URL_DOWNLOAD = 'yabs?type=download';

hermione.only.in('chrome-desktop', 'Счетчики не браузерозависимые');
specs({
    feature: 'Popup на СЕРПе',
    type: 'C паранджой для скачивания Я.Бро',
}, () => {
    beforeEach(async function() {
        const linknext = '&linknext';

        await this.browser.yaOpenSerp({
            srcskip: 'YABS_DISTR',
            text: 'test',
            foreverdata: '1318899850',
            data_filter: 'distr-popup',
        }, PO.page());

        await this.browser.yaWaitForVisible(PO.distrPopup(), 'Не показался промопопап');

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
        const PO = this.PO;

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
        await this.browser.yaCheckBaobabCounter(PO.distrPopup.installButton(), {
            path: '/$page/promo_popup/install',
        });

        await this.browser.pause(250);
        const result = await this.browser.execute(findResourceRequested, YABS_URL_DOWNLOAD);
        assert.isString(result, 'Счётчик начала скачивания не сработал');
        await this.browser.yaWaitForVisible(PO.promoCurtain(), 'Должна быть показана паранджа');
    });

    it('Проверка паранджи', async function() {
        await this.browser.yaCheckBaobabCounter(PO.distrPopup.installButton(), {
            path: '/$page/promo_popup/promo_popup[@product="browser" and @type="download"]',
            event: 'tech',
            type: 'promo-curtain-show',
        });

        await this.browser.pause(250);
        const result = await this.browser.execute(findResourceRequested, YABS_URL_DOWNLOAD);
        assert.isString(result, 'Счётчик начала скачивания не сработал');
        await this.browser.yaWaitForVisible(PO.promoCurtain(), 'Должна быть показана паранджа');

        await this.browser.assertView('promo-curtain', PO.promoCurtain(), {
            allowViewportOverflow: true,
        });

        await this.browser.yaCheckBaobabCounter(PO.promoCurtain.close(), {
            path: '/$page/promo_popup/promo_popup[@product="browser" and @type="download"]',

            data: {
                tags: { close: 1 },
            },
        });

        await this.browser.yaWaitForHidden(PO.promoCurtain(), 3000, 'Должна быть скрыта паранджа');
        await this.browser.yaWaitForHidden(PO.distrPopup(), 3000, 'Должен быть скрыт попап-блок дистрибуции');
    });

    it('Взаимодействие с саджестом', async function() {
        await this.browser.yaCheckBaobabCounter(PO.header.arrow.input.control(), {
            path: '/$page/promo_popup[@product="browser" and @type="download"]',
            event: 'tech',
            type: 'distr-popup-auto-closed',
        }, 'Не сработал счётчик автоматического скрытия попап-блока');

        await this.browser.click(PO.header.arrow.input.control());
        await this.browser.yaWaitForVisible(PO.mainSuggest.content(), 'Должен открыться саджест');
        await this.browser.yaWaitForHidden(PO.distrPopup(), 'Попап-блок дистрибуции не закрылся');
        await this.browser.moveToObject('body', 10, 10);
        await this.browser.buttonDown();
        await this.browser.yaWaitForHidden(PO.mainSuggest.content(), 'Саджест не скрылся');
        await this.browser.yaShouldNotBeVisible(PO.distrPopup(), 'Попап-блок дистрибуции не должен показываться');
    });
});
