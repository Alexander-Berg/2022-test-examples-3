'use strict';

specs({
    feature: 'Feature-name',
    experiment: 'Exp-name'
}, function() {
    describe('Second describe', function () {
        beforeEach(() => {
            this.doSomething()
        });
        // exp: af4c03a, prod: 202f4ae
        it('Should check experimental', async function() {
            await this.browser.yaOpenSerp({
                text: 'an interesting text',
                exp_flags: 'flag-name=1',
            }, this.PO.selector());
            await this.browser.assertView('ex_plain', PO.item2());
        });
    });
    describe('New describe', function () {
        // exp: 3388c8d, prod: 12f5d35
        it('Should check something new', async function() {
            await this.browser.assertView('plain', PO.item());
        });
    });
});
