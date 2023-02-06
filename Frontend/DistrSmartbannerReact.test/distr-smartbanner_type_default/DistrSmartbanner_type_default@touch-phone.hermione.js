'use strict';

const PO = require('../DistrSmartbannerReact.page-object');

hermione.only.notIn('searchapp-phone', 'смартбаннера нет в поисковом приложении');
specs({
    feature: 'Смартбаннер React',
    type: 'default',
}, () => {
    hermione.also.in('iphone-dark');
    it('Внешний вид смартбаннера с заголовком', async function() {
        await this.browser.yaOpenSerp({
            text: 'test',
            foreverdata: 1239240421,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.smartInfo());

        await this.browser.yaCheckBaobabServerCounter({
            path: '/$page/distr-smartbanner',
            attrs: {
                bannerid: '6746495432',
                layout: 'default',
                product: 'browser',
            },
        });

        await this.browser.yaAssertViewIsolated('smartbanner-default-with-title', PO.smartInfo());
    });

    hermione.also.in('iphone-dark');
    it('Внешний вид смартбаннера с имитацией заголовка', async function() {
        await this.browser.yaOpenSerp({
            text: 'test',
            foreverdata: 78063548,
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.smartInfo());

        await this.browser.yaAssertViewIsolated('smartbanner-default-title-like', PO.smartInfo());
    });

    hermione.also.in('iphone-dark');
    it('Внешний вид смартбаннера без кнопки закрыть', async function() {
        await this.browser.yaOpenSerp({
            text: 'test',
            foreverdata: 1011945489,
            srcskip: 'YABS_DISTR',
            exp_flags: ['smartbanner_atom=1'],
            data_filter: false,
        }, PO.smartInfo());

        await this.browser.yaAssertViewIsolated('smartbanner-default-cross', PO.smartInfo());
    });
});
