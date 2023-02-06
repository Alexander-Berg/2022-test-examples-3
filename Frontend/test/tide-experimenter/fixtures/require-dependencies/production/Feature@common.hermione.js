'use strict';
const test = require('../experiment/testfile');
const {one, two} = require('../experiment/numbers');
const {beta} = require('../experiment/letters');
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
                data_filter: 'extended-snippet',
            }, PO.OrganicExtendedSnippet());
        });

        it('Внешний вид', async function() {
            await this.browser.assertView('plain', assertViewSelector);
        });

    });
});
