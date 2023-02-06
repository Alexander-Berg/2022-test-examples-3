'use strict';

specs({
    feature: 'Feature-name'
}, function() {
    it('Should check something', async function() {
        await this.browser.yaOpenSerp({
            text: 'some-text',
        }, this.PO.selector());
    });
});
