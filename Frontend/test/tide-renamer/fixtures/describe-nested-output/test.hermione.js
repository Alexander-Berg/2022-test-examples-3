specs({ feature: 'Feature-one', type: 'Type-one' }, async function () {
    describe('The new describe title', async function () {
        describe('New nested describe', async function () {
            it('The first it case', function () {
            });

            it('The second it case', async function () {
            });
        });

        it('The last it case', function () {
        });
    });
});
