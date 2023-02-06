'use strict';

specs({
    feature: 'Органика',
    experiment: 'Сниппет с сайтлинками',
}, function() {
    it('Сниппет с сайтлинками', async function() {
        await this.browser.yaOpenSerp({
            text: 'test',
            foreverdata: '4111324260',
        }, this.PO.firstSnippet());

        await this.browser.assertView('plain', this.PO.firstSnippet());
    });
});
