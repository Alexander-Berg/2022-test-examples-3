'use strict';

specs({
    feature: 'Feature-name'
}, function() {
    hermione.only.notIn('ie8');
    describe('Second describe', function () {
        it('Should check second', async function() {
            let oldTestBody;
            await this.browser.doSomething();
        });
    });

    describe('Another describe', () => {});
});
