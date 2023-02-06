specs({ feature: 'New feature', experiment: 'Another experiment' }, async function () {
    specs('New specs', async function () {
        describe('Some other describe', function () {
            it('The first it case', function () {
            });

            it('Renamed it', async function () {
            });

            it('The last it case', function () {
            });
        });
    });
});
