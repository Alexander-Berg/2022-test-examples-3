'use strict';

const PO = require('../Lyrics.page-object/index@common');

specs({
    feature: 'Стихолюб',
    type: 'Коран',
}, () => {
    it('Проверка ссылок', async function() {
        await this.browser.yaOpenSerp({
            text: 'فَقَدْ كَذَّبُوا بِالْحَقِّ لَمَّا',
            lr: '11508',
            tld: 'com.tr',
            data_filter: 'poetry-lover' }, PO.poetry());

        await this.browser.assertView('plain', PO.poetry());

        await this.browser.yaCheckBaobabCounter(PO.poetry.keyValue.firstLink(), {
            path: '/$page/$main/$result/key-value/url[@pos=0]',
        });
    });
});
