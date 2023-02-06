specs({ feature: 'Feature name', experiment: 'Experiment name' }, () => {
    hermione.also.in(['ie9']);
    describe('Descr 1', () => {
        beforeEach(() => {
            console.log('Inside a beforeEach');
        });

        afterEach(() => {
            console.log('Inside an afterEach');
        });

        it('Third it', () => {});


        [
            { prop: 'It 2' },
            { prop: 'It 3' },
        ].forEach(({ prop }) => {
            it(prop, async () => {
                console.log('forEach it body');

                await this.browser.yaOpenSerp({
                    text: 'text',
                    exp_flags: ['flag-1']
                });
            });
        });
    });

    describe('For each describe', () => {
        [
            { name: 'Descr 2' },
            { name: 'Descr 3' }
        ].forEach(item => {
            hermione.also.in(['ie11']);
            describe(item.name, () => {
                hermione.also.in(['firefox']);
                hermione.also.in(['ie11']);
                it(item.name + 'Inner it', () => {
                    console.log('experiment!');
                });
            });
        });

        [1, 2].forEach(item => {
            it(`Another it ${item}`, () => {});
        })
    });
});
