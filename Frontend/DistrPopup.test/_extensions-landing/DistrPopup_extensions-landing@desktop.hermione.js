'use strict';

const PO = require('../DistrPopup.page-object');

const findResourceRequested = require('../../../../../hermione/client-scripts/find-resource-requested');
const YABS_LINKHEAD = 'yabs?type=show';
const YABS_COUNTER_CLOSE = 'yabs?type=close';
const MCOUNTER = 'https://mc.yandex.ru/watch/731962';

hermione.only.in('chrome-desktop', 'Счетчики не браузерозависимые');
specs({
    feature: 'Popup на СЕРПе',
    type: 'C паранджой для перехода на расширение',
}, () => {
    describe('Общие проверки', function() {
        beforeEach(async function() {
            const linknext = '&linknext';

            await this.browser.yaOpenSerp({
                srcskip: 'YABS_DISTR',
                text: 'test',
                foreverdata: '2512676532',
                data_filter: 'distr-popup',
            }, PO.distrPopup());

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
            await this.browser.yaWaitForHidden(PO.distrPopup(), 'Попап дистрибуции не закрылся');
        });

        hermione.only.in('chrome-desktop', 'Установка расширений только в хром браузерах');
        it('Проверка кнопки установки', async function() {
            const bannerId = '5239839716:6926193014139001391';
            const showId = encodeURIComponent('6102' + bannerId); // 6102 - префикс попапа в бк + bannerid

            await this.browser.yaCheckBaobabCounter(PO.distrPopup.installButton(), {
                path: '/$page/promo_popup/install',
            }); // 6102 - префикс попапа в бк + bannerid

            await this.browser.pause(250);
            const result = await this.browser.execute(findResourceRequested, MCOUNTER);
            assert.isString(result, 'Счётчик метрики цели не сработал');
            await this.browser.yaWaitForHidden(PO.distrPopup(), 2000, 'Должен быть скрыт попап дистрибуции');
            await this.browser.yaCheckCookie('ys', 'distr-promo-popup', 'bnrd.' + showId);
        });
    });
});
