specs({ feature: 'Feature name', experiment: 'Experiment name' }, () => {
    hermione.also.in(['ie9']);
    describe('Descr 1', () => {
        beforeEach(() => {
            console.log('Inside a beforeEach');
        });

        afterEach(() => {
            console.log('Production afterEach');
        });

        it('First it', () => {});
        it('Second it', () => {});


        [
            { prop: 'It 1' },
            { prop: 'It 2' },
        ].forEach(({ prop }) => {
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
    });

    describe('For each describe', () => {
        beforeEach(() => {
            console.log('exp for each beforeEach hook')
        });

        [
            { name: 'Descr 1' },
            { name: 'Descr 2' },
        ].forEach(item => {
            hermione.also.in(['ie11']);
            describe(item.name, () => {
                hermione.also.in(['firefox']);
                hermione.also.in(['ie11']);
                it(item.name + 'Inner it', () => {
                    console.log('inner it body!');
                });
            });
        });
    });
});
