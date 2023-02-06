'use strict';

const PO = require('../DistrSmartbannerReact.page-object');

hermione.only.notIn('searchapp-phone', 'смартбаннера нет в поисковом приложении');
specs({
    feature: 'Смартбаннер React',
    type: 'default-vertical',
}, () => {
    hermione.also.in('iphone-dark');
    it('Вертикальный смартбаннер, полный размер, слева', async function() {
        await this.browser.yaOpenSerp({
            text: 'scorpions',
            foreverdata: 3908150083,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: 'no_results',
        }, PO.smartInfo());

        await this.browser.yaCheckBaobabServerCounter({
            path: '/$page/distr-smartbanner',
            attrs: {
                bannerid: '6746495432',
                layout: 'default',
                product: 'browser',
            },
        });

        await this.browser.yaAssertViewIsolated('smartbanner-vertical-size-l-side-left', PO.smartInfo());
        await this.browser.click(PO.smartInfo.close());
        await this.browser.yaWaitForHidden(PO.smartInfo(), 'Вертикальный смартбаннер не закрылся');
    });

    hermione.also.in('iphone-dark');
    it('Вертикальный смартбаннер, полный размер, справа', async function() {
        await this.browser.yaOpenSerp({
            text: 'scorpions',
            foreverdata: 4245856103,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: 'no_results',
        }, PO.smartInfo());

        await this.browser.yaAssertViewIsolated('smartbanner-vertical-size-l-side-right', PO.smartInfo());
        await this.browser.click(PO.smartInfo.close());
        await this.browser.yaWaitForHidden(PO.smartInfo(), 'Вертикальный смартбаннер не закрылся');
    });

    hermione.also.in('iphone-dark');
    it('Вертикальный смартбаннер, средний размер, слева', async function() {
        await this.browser.yaOpenSerp({
            text: 'scorpions',
            foreverdata: 4096537029,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: 'no_results',
        }, PO.smartInfo());

        await this.browser.yaAssertViewIsolated('smartbanner-vertical-size-m-side-left', PO.smartInfo());
        await this.browser.click(PO.smartInfo.close());
        await this.browser.yaWaitForHidden(PO.smartInfo(), 'Вертикальный смартбаннер не закрылся');
    });

    hermione.also.in('iphone-dark');
    it('Вертикальный смартбаннер, средний размер, справа', async function() {
        await this.browser.yaOpenSerp({
            text: 'scorpions',
            foreverdata: 1355842112,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: 'no_results',
        }, PO.smartInfo());

        await this.browser.yaAssertViewIsolated('smartbanner-vertical-size-m-side-right', PO.smartInfo());
        await this.browser.click(PO.smartInfo.close());
        await this.browser.yaWaitForHidden(PO.smartInfo(), 'Вертикальный смартбаннер не закрылся');
    });

    hermione.also.in('iphone-dark');
    it('Вертикальный смартбаннер, малый размер, слева', async function() {
        await this.browser.yaOpenSerp({
            text: 'scorpions',
            foreverdata: 2698071147,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: 'no_results',
        }, PO.smartInfo());

        await this.browser.yaAssertViewIsolated('smartbanner-vertical-size-s-side-left', PO.smartInfo());
        await this.browser.click(PO.smartInfo.close());
        await this.browser.yaWaitForHidden(PO.smartInfo(), 'Вертикальный смартбаннер не закрылся');
    });

    hermione.also.in('iphone-dark');
    it('Вертикальный смартбаннер, малый размер, справа', async function() {
        await this.browser.yaOpenSerp({
            text: 'scorpions',
            foreverdata: 2037883648,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: 'no_results',
        }, PO.smartInfo());

        await this.browser.yaAssertViewIsolated('smartbanner-vertical-size-s-side-right', PO.smartInfo());
        await this.browser.click(PO.smartInfo.close());
        await this.browser.yaWaitForHidden(PO.smartInfo(), 'Вертикальный смартбаннер не закрылся');
    });
});
