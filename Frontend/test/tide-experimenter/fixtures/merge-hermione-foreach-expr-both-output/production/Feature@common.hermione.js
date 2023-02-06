specs({
    feature: 'Feature name'
}, () => {
    hermione.also.in(['ie9']);
    describe('Descr 1', () => {
        beforeEach(() => {
            console.log('Inside a beforeEach');
        });

        /* <<<<<<< production */
        afterEach(() => {
            console.log('Production afterEach');
        });

        /*
        =======
        */
        afterEach(() => {
            console.log('Inside an afterEach');
        });/*
        >>>>>>> experiment */

        it('First it', () => {});
        it('Second it', () => {});


        [{ prop: 'It 1' }, { prop: 'It 2' }, {
            prop: 'It 3'
        }].forEach(({ prop }) => {
            it(prop, async () => {
                console.log('forEach it body');

                await this.browser.yaOpenSerp({
                    text: 'text',
                    exp_flags: ['flag-1']
                });
            });
        });

        hermione.only.in(['firefox']);
        it('Test case', () => {
            console.log('hello world!');
        });

        it('Third it', () => {});
    });

    describe('For each describe', () => {
        beforeEach(() => {
            console.log('exp for each beforeEach hook')
        });

        [{ name: 'Descr 1' }, { name: 'Descr 2' }, {
            name: 'Descr 3'
        }].forEach(item => {
            hermione.also.in(['ie11']);
            describe(item.name, () => {
                hermione.also.in(['firefox']);
                hermione.also.in(['ie11']);
                it(item.name + 'Inner it', () => {
                    console.log('inner it body!');
                });
                hermione.also.in(['firefox']);
                hermione.also.in(['ie11']);
                it(item.name + 'Inner it', () => {
                    console.log('experiment!');
                });
            });
        });

        [1, 2].forEach(item => {
            it(`Another it ${item}`, () => {});
        });
    });
});
