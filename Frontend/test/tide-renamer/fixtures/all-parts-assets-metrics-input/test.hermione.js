specs({ feature: 'Feature-one', type: 'Type-one' }, async function () {
    specs('Inner-specs', async function () {
        it('The first it case', function () {
        });

        it('The second it case', async function () {
        });

        specs('Inner-specs-2', async function () {
            it('It test case one', async function () {
            });

            it('Another it test case', async function () {
            });

            it('Test three', async function () {
            });

            it('The last case', async function () {
            });
        });

        specs('Experiment', async function () {
            it('Exp-case', async function () {
            });
        });
    });
});
