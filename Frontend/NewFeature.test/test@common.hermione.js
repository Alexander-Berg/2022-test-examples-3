'use strict';

specs({
    feature: 'Feature',
    experiment: 'New-experiment'
}, function() {
    it('Внешний вид', async function() {
        await this.browser.yaOpenSerp({
            text: 'текст',
            data_filter: '',
            rearr: ''
        }, this.PO.element());

        await this.browser.yaWaitForVisible(this.PO.element(), 'Элемент не появился');
        await this.browser.assertView('plain', this.PO.element());
    });

    /* ... */
});
