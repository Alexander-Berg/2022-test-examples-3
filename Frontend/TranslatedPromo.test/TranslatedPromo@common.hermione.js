'use strict';

const PO = require('./TranslatedPromo.page-object');

specs({
    feature: 'Переводный поиск',
    type: 'Промо переводного поиска',
}, function() {
    hermione.also.in(['iphone-dark', 'chrome-grid-480']);
    it('Внешний вид и счётчики', async function() {
        await this.browser.yaOpenSerp({
            text: 'киану ривз',
            exp_flags: [
                'translated_promo=translated',
            ],
        }, PO.translatedPromo());

        await this.browser.yaCheckBaobabServerCounter({
            path: '/$page/$main/$result/translated-promo',
        });

        await this.browser.yaAssertViewExtended('plain', PO.translatedPromo(), { horisontalOffset: 20, verticalOffset: 0 });

        const link = PO.translatedPromo.link();
        const url = await this.browser.yaParseHref(link);
        assert.equal(url.query.yatr, '1', 'Ссылка на переводную вертикаль не содержит параметр yatr=1');
    });
});
