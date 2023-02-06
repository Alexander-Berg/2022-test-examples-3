'use strict';

specs({
    feature: 'Feature-name'
}, function() {
    describe('First describe', function () {
        it('Should check first', async function() {
            await this.browser.someCommand();
        });
    });

    describe('Second describe', function () {
        beforeEach(() => {
            let oldBeforeEach;
            this.doSomething();
        });

        it('Should check second', async function() {
            let oldTestBody;
            await this.browser.doSomething();
        });
    });
});
