'use strict';

const PO = require('./Zen_channel.page-object');

specs({
    feature: 'Колдунщик Дзена',
    type: 'Аватар и мета в сниппете блоггера',
}, function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            foreverdata: '2944813093',
            text: 'foreverdata',
            srcskip: 'YABS_DISTR',
            data_filter: 'zen',
        }, PO.zenChannel());
    });

    hermione.also.in('iphone-dark');
    it('Внешний вид', async function() {
        await this.browser.assertView('meta-without-greenurl-top-info', PO.zenChannel());
    });

    it('Тайтл', async function() {
        await this.browser.yaCheckLink2({
            selector: PO.zenChannel.title.link(),
            baobab: {
                path: '/$page/$main/$result/zen/title',
            },
            message: 'Неверная ссылка в заголовке',
        });
    });

    it('Шапка с метой о блоггере', async function() {
        await this.browser.yaCheckLink2({
            selector: PO.zenChannel.zenHeader.link(),
            baobab: {
                path: '/$page/$main/$result/zen/zen-header/author-link',
            },
            message: 'Неверная ссылка в шапке',
        });
    });
});
