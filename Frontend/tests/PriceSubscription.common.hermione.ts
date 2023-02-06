describe('Storybook', function() {
    describe('PriceSubscription', function() {
        it('Внешний вид', async function() {
            const bro = this.browser;
            await bro.yaOpenComponent('tests-pricesubscription--plain', true);
            await bro.yaAssertViewThemeStorybook('plain', '.PriceSubscription-Modal');
        });

        it('Отсутствие подписки на снижение цены', async function() {
            const bro = this.browser;
            await bro.yaOpenComponent('tests-pricesubscription--unsubscribed', true);
            await bro.yaAssertViewThemeStorybook('unsubscribed', '.PriceSubscription');
        });

        it('Подписан на снижение цены', async function() {
            const bro = this.browser;
            await bro.yaOpenComponent('tests-pricesubscription--subscribed', true);
            await bro.yaAssertViewThemeStorybook('unsubscribed', '.PriceSubscription');
        });
    });
});
