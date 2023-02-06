specs({ feature: 'Feature name', experiment: 'Some experiment' }, async function () {
    specs('New specs', async function () {
        describe('Describe in specs', function () {
            it('The first it case', function () {
            });

            it('Renamed it', async function () {
            });

            it('The last it case', function () {
            });
        });
    });
});
