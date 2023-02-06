'use strict';

const PO = require('../DistrPopup.page-object');

specs({
    feature: 'Popup на СЕРПе',
    type: 'Справа снизу',
}, function() {
    beforeEach(async function() {
        const PO = this.PO;

        await this.browser.yaOpenSerp({
            srcskip: 'YABS_DISTR',
            text: 'окна',
            foreverdata: '1985722415',
            yandex_login: 'stas.mihailov666',
            data_filter: 'distr-popup',
        }, PO.distrPopup());
    });

    hermione.also.in('chrome-desktop-1920');
    it('Внешний вид', async function() {
        await this.browser.assertView('right-bottom', PO.distrPopup(), {
            hideElements: [PO.rightColumn(), PO.yaplus()],
        });
    });

    it('Клик на кнопку отмены', async function() {
        await this.browser.yaCheckBaobabCounter(PO.distrPopup.closeButton(), {
            path: '/$page/promo_popup/close[@tags@close=1 and @behaviour@type="dynamic"]',
        });

        await this.browser.yaWaitForHidden(PO.distrPopup(), 3000, 'Попап-блок дистрибуции не закрылся');
    });

    it('Клик на кнопку установки', async function() {
        const PO = this.PO;

        await this.browser.yaCheckBaobabCounter(PO.distrPopup.installButton(), {
            path: '/$page/promo_popup/install',
        });
    });
});
