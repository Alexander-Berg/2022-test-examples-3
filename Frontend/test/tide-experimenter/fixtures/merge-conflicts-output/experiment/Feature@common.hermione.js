'use strict';

specs({
    feature: 'Feature-name',
    experiment: 'Exp-name'
}, function() {
    describe('Second describe', function () {
        beforeEach(() => {
            let newBeforeEach;
            this.doExperiment();
        });

        it('Should check second', async function() {
            let newTestBody;
            await this.browser.doExperiment();
        });
    });
});
