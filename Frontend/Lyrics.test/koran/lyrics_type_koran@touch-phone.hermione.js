'use strict';

const PO = require('../Lyrics.page-object/index@common');

specs('Стихолюб / Коран', () => {
    hermione.only.notIn(['iphone'], 'orientation is not supported');
    it('Альбомная ориентация', async function() {
        await this.browser.yaOpenSerp(
            { text: 'فَقَدْ كَذَّبُوا بِالْحَقِّ لَمَّا', data_filter: 'poetry-lover' },
            PO.poetry(),
        );

        await this.browser.setOrientation('landscape');
        await this.browser.assertView('koran-landscape', PO.poetry());
    });
});
