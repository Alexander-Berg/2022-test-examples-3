'use strict';

const PO = require('./YaTalk.page-object/');

specs({
    feature: 'Спецсниппет Яндекс.Толк',
}, function() {
    hermione.also.in(['chrome-desktop-dark']);
    it('Обязательные проверки', async function() {
        await this.browser.yaOpenSerp(
            { text: 'test', foreverdata: '1543017841', data_filter: { values: ['ya-talk', 'extended-snippet'], operation: 'AND' } },
            PO.yaTalk(),
        );

        await this.browser.assertView('plain', PO.yaTalk());

        await this.browser.yaCheckLink2({
            selector: PO.yaTalk.title(),
            baobab: {
                path: '/$page/$main/$result/title',
            },
        });

        await this.browser.yaCheckLink2({
            selector: PO.yaTalk.greenUrl.link(),
            baobab: {
                path: '/$page/$main/$result/path/urlnav',
            },
        });

        await this.browser.yaCheckLink2({
            selector: PO.yaTalk.thumb(),
            baobab: {
                path: '/$page/$main/$result/thumb',
            },
        });

        await this.browser.yaCheckLink2({ selector: PO.yaTalk.meta.link() });
    });

    it('Данные для гринурла', async function() {
        await this.browser.yaOpenSerp(
            { text: 'test', foreverdata: '3400514339', data_filter: { values: ['ya-talk', 'extended-snippet'], operation: 'AND' } },
            PO.yaTalk(),
        );

        await this.browser.assertView('plain', PO.yaTalk());

        await this.browser.yaCheckLink2({
            selector: PO.yaTalk.greenUrl.link(),
            baobab: {
                path: '/$page/$main/$result/path/urlnav',
            },
        });
    });
});
