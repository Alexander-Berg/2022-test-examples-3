describe('ProductPage', function() {
    describe('Рекомендации', function() {
        const recommendationsSelector = '.Recommendations';
        const firstRecommendationSelector = '.Recommendations .ProductCardsList-Item:nth-child(1)';
        const firstRecommendationOffersSelector = '.Recommendations .ProductCardsList-Item:nth-child(1) .ProductCard-OffersCount';

        it('Проверка кол-ва офферов', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/product/186853207/sku/101612053795?flags=goods_recommender_enabled=1');
            await bro.yaWaitForPageLoad();

            await bro.yaWaitForVisible(recommendationsSelector, 3000, 'рекомендации не появились');
            await bro.click(firstRecommendationSelector);

            await bro.yaWaitForPageLoad();
            await bro.yaWaitForVisible(recommendationsSelector, 3000, 'рекомендации не появились');

            const initialCount = await (
                await bro.$(firstRecommendationOffersSelector)
            ).getText();

            await bro.refresh();
            await bro.yaWaitForPageLoad();
            await bro.yaWaitForVisible(recommendationsSelector, 3000, 'рекомендации не появились');

            const resultCount = await (
                await bro.$(firstRecommendationOffersSelector)
            ).getText();

            assert.strictEqual(initialCount, resultCount, 'некорректное кол-во офферов в рекомендациях');
        });
    });
});
