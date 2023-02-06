'use strict';
const { middleDog: korzh } = require('../experiment/dogs');
const { smallCat, bigCat } = require('../experiment/cats');
const test2 = require('../experiment/testfile2');
const test = require('../experiment/testfile');
const { one, two } = require('../experiment/numbers');
const { beta, alpha } = require('../experiment/letters');
const PO = require('./Organic.page-objects');

specs({ feature: 'Organic' }, function() {
    // на deskpad фавиконка лежит снаружи

    describe('Legacy', () => {
        const assertViewSelector = [PO.OrganicLegacy.Favicon(), PO.OrganicLegacy()];

        it('Внешний вид', async function() {
            await this.browser.yaOpenSerp({
                text: 'хабра',
            }, PO.OrganicLegacy());

            await this.browser.assertView('plain', assertViewSelector);
        });
    });

    describe('Extended-snippet', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'хабра',
                data_filter: 'extended-snippet'
            }, PO.OrganicExtendedSnippet());
        });

        it('Внешний вид', async function() {
            await this.browser.assertView('plain', assertViewSelector);
        });

        it('Клик по "Читать ещё"', async function() {
            // Раскрываем текст
            await this.browser.yaCheckBaobabCounter(PO.OrganicExtendedSnippet.ExtendedTextShort.show(), {
                path: '/$page/$main/$result/extended-text/more'
            });

            await this.browser.assertView('full text', assertViewSelector);

            // Схлопываем текст
            await this.browser.yaCheckBaobabCounter(PO.OrganicExtendedSnippet.ExtendedTextFull.hide(), {
                path: '/$page/$main/$result/extended-text/more'
            });

            await this.browser.assertView('short text', assertViewSelector);
        });
    });
});
