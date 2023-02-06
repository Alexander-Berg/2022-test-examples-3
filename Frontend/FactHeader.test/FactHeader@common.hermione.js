'use strict';

specs('Факт', function() {
    hermione.also.in('iphone-dark');
    it('Брендинг с рекламой', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3984107319,
            text: 'foreverdata',
            exp_flags: 'fact_disable_branding=null',
        }, '.serp-list');

        await this.browser.assertView('plain', '.serp-list', {
            ignoreElements: ['.serp-item:not(.has-branding)'],
        });
    });
});
