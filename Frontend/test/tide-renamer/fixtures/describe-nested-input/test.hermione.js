specs({ feature: 'Feature-one', type: 'Type-one' }, async function () {
    describe('Describe in specs', async function () {
        describe('Nested describe', async function () {
            it('The first it case', function () {
            });

            it('The second it case', async function () {
            });
        });

        it('The last it case', function () {
        });
    });
});
