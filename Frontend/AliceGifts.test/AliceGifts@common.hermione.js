'use strict';

const PO = require('./AliceGifts.page-object');

hermione.only.notIn('searchapp-phone');
specs('Генератор подарков с Алисой', () => {
    it('Форма ввода', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ foreverdata: '486334836', text: 'что подарить', data_filter: 'alice-gifts' }, PO.aliceGifts());
        await browser.assertView('plain', PO.aliceGifts());
    });

    it('Показ подарков при загрузке', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({ foreverdata: '2340592378', text: 'подарок', data_filter: 'alice-gifts' }, PO.aliceGifts());
        await browser.assertView('plain', PO.aliceGifts());
    });
});
