'use strict';

specs({
    feature: 'Feature-name'
}, () => {
    describe('Describe 1', () => {
        beforeEach(async function () {
            await this.browser.yaOpenSerp({ text: 'text' }, this.PO.serpList());
        });

        it('It 1', async function () {
            await this.browser.assertView('plain', this.PO.serpList());
            await this.browser.assertView('plain 2', this.PO.serpList());
        });

        it('It 2', async function () {
            await this.browser.assertView('plain', this.PO.serpList());
            await this.browser.assertView('plain 2', this.PO.serpList());
        });
    });

    it('It 3', async function () {
        await this.browser.yaOpenSerp({ text: 'text' }, this.PO.serpList());
        await this.browser.assertView('plain', this.PO.serpList());
    });
});
