'use strict';

specs({
    feature: 'Сниппеты',
    type: 'Метка свежести',
}, function() {
    hermione.also.in(['chrome-desktop-dark']);
    it('Проверка внешнего вида - с датой', async function() {
        const PO = this.PO;

        await this.browser.yaOpenSerp({
            text: ' ',
            user_time: 20180910,
            foreverdata: 1703155694,
            data_filter: 'special-dates' }, PO.serpList.snippetWithLabel());

        await this.browser.assertView('with-date', PO.serpList.snippetWithLabel());
    });

    hermione.also.in(['chrome-desktop-dark']);
    it('Проверка внешнего вида - длинный текст', async function() {
        const PO = this.PO;

        await this.browser.yaOpenSerp({
            text: ' ',
            user_time: 20180910,
            foreverdata: 1175373586,
            data_filter: 'special-dates' }, PO.serpList.snippetWithLabel());

        await this.browser.assertView('long-text', PO.serpList.snippetWithLabel());
    });

    hermione.also.in(['chrome-desktop-dark']);
    it('Проверка внешнего вида - с текстом', async function() {
        const PO = this.PO;

        await this.browser.yaOpenSerp({
            text: ' ',
            user_time: 20170503,
            foreverdata: 1703155694,
            data_filter: 'special-dates' }, PO.serpList.snippetWithLabel());

        await this.browser.assertView('with-text', PO.serpList.snippetWithLabel());
    });

    it('Проверка ссылок и счетчиков', async function() {
        const PO = this.PO;

        await this.browser.yaOpenSerp({
            text: '11 сентября',
            foreverdata: 2924813483,
            data_filter: 'special-dates' }, PO.serpList.snippetWithLabel());

        await this.browser.yaCheckBaobabCounter(PO.serpList.snippetWithLabel.title.link(), {
            path: '/$page/$main/$result/title',
        });

        await this.browser.yaCheckBaobabCounter(PO.serpList.snippetWithLabel.greenurl.link(), {
            path: '/$page/$main/$result/path/urlnav',
        });
    });

    it('Расширенный сниппет с особой датой', async function() {
        const { PO, browser } = this;

        await browser.yaOpenSerp({
            text: 'test',
            user_time: 20180910,
            foreverdata: 1703155694,
            data_filter: { values: ['extended-snippet', 'special-dates'], operation: 'AND' },
        }, PO.extendedSnippet());

        await browser.yaCheckBaobabCounter(PO.extendedSnippet.show(), {
            path: '/$page/$main/$result/extended-text/more[@behaviour@type="dynamic"]',
        });

        await browser.yaWaitForVisible(PO.extendedSnippet.hide());

        await browser.yaCheckBaobabCounter(PO.extendedSnippet.hide(), {
            path: '/$page/$main/$result/extended-text/close[@behaviour@type="dynamic"]',
        });
    });
});
