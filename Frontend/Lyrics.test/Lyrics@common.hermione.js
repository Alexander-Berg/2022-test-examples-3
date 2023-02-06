'use strict';

const PO = require('./Lyrics.page-object/index@common');

specs({ feature: 'Стихолюб' }, () => {
    // eslint-disable-next-line camelcase
    const data_filter = 'poetry-lover';

    h.it('Проверка внешнего вида', async function() {
        const { browser } = this;
        const text = 'есенин не жалею не зову';

        await browser.yaOpenSerp({ text, data_filter }, PO.poetry());
        await browser.assertView('poetry', PO.poetry());

        const moreCounter = '/$page/$main/$result/more';
        await browser.yaCheckBaobabCounter(PO.poetry.moreButton(), { path: moreCounter });
        await browser.yaWaitForVisible(PO.poetry.contentExpanded(), 'Не появился полный текст колдунщика');
        await browser.assertView('poetry-expanded', PO.poetry());

        const lessCounter = '/$page/$main/$result/less';
        await browser.yaCheckBaobabCounter(PO.poetry.lessButton(), { path: lessCounter });
        await browser.yaWaitForVisible(PO.poetry.content(), 'Полный текст колдунщика не скрылся');

        const keyValueCounter = '/$page/$main/$result/key-value/url[@pos=0]';
        await browser.yaCheckLink2({
            selector: PO.poetry.keyValue.firstLink(),
            baobab: { path: keyValueCounter },
        });
    });

    hermione.also.in('iphone-dark');
    h.it('Проверка внешнего вида для коротких стихов', async function() {
        const { browser } = this;
        const text = 'да охранюся я от мушек';

        await browser.yaOpenSerp({ text, data_filter }, PO.poetry());
        await browser.assertView('poetry', PO.poetry());
    });
});
