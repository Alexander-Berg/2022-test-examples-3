'use strict';

const PO = require('../DistrPopup.page-object');

const findResourceRequested = require('../../../../../hermione/client-scripts/find-resource-requested');
const YABS_LINKHEAD = 'yabs?type=show';
const YABS_URL_DOWNLOAD = 'yabs?type=download';

hermione.only.in('chrome-desktop', 'Счетчики не браузерозависимые');
specs({
    feature: 'Popup на СЕРПе',
    type: 'C паранджой для скачивания Я.Бро (паранджекрутилка)',
}, () => {
    it('Проверка паранджи', async function() {
        const linknext = '&linknext';

        await this.browser.yaOpenSerp({
            srcskip: 'YABS_DISTR',
            text: 'test',
            foreverdata: '2362684871',
        }, PO.page());

        await this.browser.yaWaitForVisible(PO.distrPopup(), 3000);

        await this.browser.yaCheckServerCounter({
            path: '/tech/distr/external/paranja',
            vars: '-reqid=*',
        });

        await this.browser.yaCheckBaobabCounter(() => { }, {
            path: '/$page/promo_popup',
            event: 'tech',
            type: 'distr-popup-show',
        });

        await this.browser.yaWaitUntil('Счётчик на показ не сработал', async () => {
            const result = await this.browser.execute(findResourceRequested, YABS_LINKHEAD + linknext);
            const counter = result;

            return counter && typeof counter === 'string';
        }, 1000);

        await this.browser.yaCheckBaobabCounter(PO.distrPopup.installButton(), {
            path: '/$page/promo_popup/promo-curtain[@product="browser" and @type="paas"]',
            event: 'tech',
            type: 'promo-curtain-show',
        });

        await this.browser.yaWaitUntil('Счётчик начала скачивания не сработалл', async () => {
            const result = await this.browser.execute(findResourceRequested, YABS_URL_DOWNLOAD);
            const counter = result;

            return counter && typeof counter === 'string';
        }, 1000);

        await this.browser.yaWaitForVisible('iframe:nth-of-type(2)', 'Должна быть показана паранджа');

        await this.browser.assertView(
            'paranja',
            'iframe:nth-of-type(2)',
            { invisibleElements: 'body > *:not(iframe)' },
        );

        await this.browser.execute(function(selector) {
            $(selector).click();
        }, '.paranja__close');

        await this.browser.yaWaitForHidden('iframe', 3000, 'Должна быть скрыта паранджа');
        await this.browser.yaWaitForHidden(PO.distrPopup(), 3000, 'Должен быть скрыт попап-блок дистрибуции');
    });

    it('Внешний вид паранджи', async function() {
        await this.browser.yaOpenSerp({
            srcskip: 'YABS_DISTR',
            text: 'test',
            foreverdata: '1121542744',
            data_filter: 'no_results',
        }, PO.page());

        await this.browser.yaWaitForVisible(PO.distrPopup(), 3000);

        await this.browser.click(PO.distrPopup.installButton());

        await this.browser.yaWaitForVisible('iframe:nth-of-type(2)', 'Должна быть показана паранджа');
        await this.browser.assertView(
            'paranja browser2',
            'iframe:nth-of-type(2)',
            { invisibleElements: 'body > *:not(iframe)' },
        );
    });
});
