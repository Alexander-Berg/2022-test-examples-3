'use strict';

const PO = require('./Lyrics.page-object/index@common');

specs('Стихолюб', () => {
    hermione.only.notIn(['iphone'], 'orientation is not supported');
    it('Альбомная ориентация', async function() {
        await this.browser.yaOpenSerp(
            { text: 'есенин не жалею не зову', data_filter: 'poetry-lover' },
            PO.poetry(),
        );

        await this.browser.setOrientation('landscape');
        await this.browser.assertView('poetry-landscape', PO.poetry());
        await this.browser.click(PO.poetry.moreButton());
        await this.browser.yaWaitForVisible(PO.poetry.contentExpanded(), 'Не появился полный текст колдунщика');
        await this.browser.click(PO.poetry.lessButton());
        await this.browser.yaWaitForVisible(PO.poetry.content(), 'Полный текст колдунщика не скрылся');
    });
});
