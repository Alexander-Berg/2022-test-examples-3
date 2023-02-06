describe('Storybook', function() {
    describe('Title', function() {
        it('default', async function() {
            const { browser, PO } = this;

            await browser.yaOpenComponent('tests-title--plain', true);

            await browser.assertView('plain', PO.Title());
        });
    });
});
