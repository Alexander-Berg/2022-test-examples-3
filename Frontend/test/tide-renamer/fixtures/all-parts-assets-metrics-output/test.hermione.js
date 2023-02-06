specs({ feature: 'Feature-one N', type: 'Type-one Type' }, async function () {
    specs('Inner-specs Y', async function () {
        it('The first it case', function () {
        });

        it('The second it case mod', async function () {
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
