'use strict';

specs({
    feature: 'Feature-name'
}, function() {
    describe('First describe', function () {
        // 4634c1a
        it('Should check first', async function() {
            await this.browser.yaOpenSerp({
                text: 'some-text',
            }, this.PO.selector());
        });
    });

    describe('Second describe', function () {
        beforeEach(() => {
            this.doSomething();
        });
        // 9046501
        it('Should check second', async function() {
            await this.browser.yaOpenSerp({
                text: 'some-text 2',
            }, this.PO.selector());
            await this.browser.assertView('plain', PO.item());
        });
        // exp: af4c03a, prod: 202f4ae
        it('Should check experimental', async function() {
            await this.browser.yaOpenSerp({ text: 'an interesting text' }, this.PO.selector());
            await this.browser.assertView('ex_plain', PO.item2());
        });
    });
    describe('New describe', function() {
        // exp: 3388c8d, prod: 12f5d35
        it('Should check something new', async function() {
            await this.browser.assertView('plain', PO.item());
        });
    });
});
