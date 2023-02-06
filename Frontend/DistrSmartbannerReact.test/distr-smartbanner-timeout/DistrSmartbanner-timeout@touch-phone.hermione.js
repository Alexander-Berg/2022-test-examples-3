'use strict';

const PO = require('../DistrSmartbannerReact.page-object');

hermione.only.notIn('searchapp-phone', 'смартбаннера нет в поисковом приложении');
specs({
    feature: 'Смартбаннер React',
    type: 'таймаут появления',
}, () => {
    it('Появление смартбаннера через 5 секунд', async function() {
        await this.browser.yaOpenSerp({
            text: 'test',
            foreverdata: 4071267478,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: 'achievement',
        }, PO.page());

        await this.browser.yaShouldNotBeVisible(PO.smartInfo());

        // 5000 — таймаут появления баннера, 250 — анимация появления, 250 — запас
        await this.browser.yaWaitForVisible(PO.smartInfo(), 5500, 'смартбаннер не показался');

        await this.browser.yaAssertViewIsolated('smartbanner-timeout', PO.smartInfo());

        await this.browser.yaCheckBaobabCounter(() => { }, {
            path: '/$page/distr-smartbanner',
            event: 'tech',
            type: 'promofooter-show-timeout',
            data: {
                timeout: 5000,
            },
        });

        await this.browser.yaCheckBaobabCounter(PO.smartInfo.close(), {
            path: '/$page/distr-smartbanner/close[@tags@close=1 and @behaviour@type="dynamic"]',
        });

        await this.browser.yaWaitForHidden(PO.smartInfo(), 3500);
    });
});
