'use strict';

const PO = require('../DistrSmartbannerReact.page-object');

hermione.only.notIn(['iphone', 'searchapp-phone'], 'фича таргетированная');
hermione.also.in('safari13');
specs({
    feature: 'Смартбаннер React',
    type: 'notification',
}, () => {
    it('Внешний вид смартбаннера', async function() {
        await this.browser.yaOpenSerp({
            text: 'персифаль',
            foreverdata: 3282029074,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.page());

        await this.browser.yaCheckBaobabServerCounter({
            path: '/$page/distr-smartbanner',
            attrs: {
                bannerid: '6746495432',
                layout: 'notification',
                product: 'browser',
            },
        });

        await this.browser.yaScroll(600);
        await this.browser.yaWaitForVisible(PO.smartInfo(), 'Смартбаннер не появился');

        await this.browser.yaAssertViewIsolated('smartbanner-notification', PO.smartInfo());

        // нативный safari13 не умеет в mouseDown, нужен живой click
        const meta = await this.browser.getMeta();
        const isSafari13 = meta.url.includes('safari13') && meta.browserVersion === '13.0';
        if (isSafari13) {
            await this.browser.click(PO.smartInfo.title());
        } else {
            await this.browser.yaClickAtTheMiddle(PO.smartInfo.title());
        }
        await this.browser.yaCheckBaobabCounter(() => {}, {
            path: '/$page/distr-smartbanner/ok[@layout="notification"]',
        });
    });

    it('Кнопка "Закрыть"', async function() {
        await this.browser.yaOpenSerp({
            text: 'персифаль',
            foreverdata: 3282029074,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.page());

        await this.browser.yaScroll(600);
        await this.browser.yaWaitForVisible(PO.smartInfo(), 'Смартбаннер не появился');

        await this.browser.yaCheckBaobabCounter(PO.smartInfo.close(), {
            path: '/$page/distr-smartbanner/close[@tags@close=1 and @behaviour@type="dynamic" and @layout="notification"]',
        });
        await this.browser.yaWaitForHidden(PO.smartInfo(), 'Смартбаннер не закрылся');
    });
});
