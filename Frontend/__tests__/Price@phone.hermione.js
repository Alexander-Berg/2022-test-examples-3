describe('Price', () => {
    it('Внешний вид', async function() {
        const { browser } = this;
        await browser.yaOpenEcomStory('price', 'default');
        await browser.assertView('plain', '.story');
    });
});
