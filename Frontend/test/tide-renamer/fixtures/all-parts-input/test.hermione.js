specs({ feature: 'Feature name', experiment: 'Some experiment' }, async function () {
    specs('Nested specs', async function () {
        describe('Describe in specs', function () {
            it('The first it case', function () {
            });

            it('The second it case', async function () {
            });

            it('The last it case', function () {
            });
        });
    });
});
