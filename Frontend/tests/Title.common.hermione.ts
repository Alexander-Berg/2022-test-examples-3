describe('Storybook', function() {
    describe('Title', function() {
        it('default', async function() {
            await this.browser.yaOpenComponent('tests-title', 'plain', true);

            await this.browser.assertView('plain', this.PO.Title());
        });
    });
});
