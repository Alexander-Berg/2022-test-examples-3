'use strict';
const test = require('./testfile');
const test2 = require('./testfile2');
const {smallCat, bigCat} = require('./cats');
const {middleDog: korzh} = require('./dogs');
const {one, two} = require('./numbers');
const {alpha, beta} = require('./letters');

specs({
    feature: 'Organic',
    experiment: 'e2etest',
}, function() {
    describe('Extended-snippet', function() {
        const assertViewSelector = [PO.OrganicExtendedSnippet.Favicon(), PO.OrganicExtendedSnippet()];

        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'хабра',
                data_filter: 'extended-snippet',
            }, PO.OrganicExtendedSnippet());
        });

        it('Клик по "Читать ещё"', async function() {
            // Раскрываем текст
            await this.browser.yaCheckBaobabCounter(PO.OrganicExtendedSnippet.ExtendedTextShort.show(), {
                path: '/$page/$main/$result/extended-text/more',
            });

            await this.browser.assertView('full text', assertViewSelector);

            // Схлопываем текст
            await this.browser.yaCheckBaobabCounter(PO.OrganicExtendedSnippet.ExtendedTextFull.hide(), {
                path: '/$page/$main/$result/extended-text/more',
            });

            await this.browser.assertView('short text', assertViewSelector);
        });
    });
});
