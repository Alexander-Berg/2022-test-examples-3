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
        /* <<<<<<< production */
        beforeEach(() => {
            let oldBeforeEach;
            this.doSomething();
        });

        /*
        =======
        */
        beforeEach(() => {
            let newBeforeEach;
            this.doExperiment();
        });/*
        >>>>>>> experiment */

        /* <<<<<<< production */
        it('Should check second', async function() {
            let oldTestBody;
            await this.browser.doSomething();
        });

        /*
        =======
        */
        it('Should check second', async function() {
            let newTestBody;
            await this.browser.doExperiment();
        });/*
        >>>>>>> experiment */
    });
});
