'use strict';

specs({
    feature: 'Feature-name'
}, function() {
    hermione.only.notIn('ie8');
    describe('Second describe', function () {
        /* <<<<<<< production */
        /*
        =======
        */
        hermione.only.in('chrome');/*
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

    /* <<<<<<< production */
    /*
    =======
    */
    hermione.only.notIn('ie');/*
    >>>>>>> experiment */

    describe('Another describe', () => {
        hermione.skip.in('ie8', 'it cannot work in this browser');
        it('Should do experiment', async function() {
            let newTestBody = 2;
            await this.browser.doAnotherExperiment();
        });
    });
});
