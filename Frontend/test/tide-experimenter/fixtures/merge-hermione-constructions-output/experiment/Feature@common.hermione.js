'use strict';

specs({
    feature: 'Feature-name',
    experiment: 'Exp-name'
}, function() {
    hermione.only.notIn('ie8');
    describe('Second describe', function () {
        hermione.only.in('chrome');
        it('Should check second', async function() {
            let newTestBody;
            await this.browser.doExperiment();
        });
    });

    hermione.only.notIn('ie');
    describe('Another describe', () => {
        hermione.skip.in('ie8', 'it cannot work in this browser');
        it('Should do experiment', async function() {
            let newTestBody = 2;
            await this.browser.doAnotherExperiment();
        });
    });
});
