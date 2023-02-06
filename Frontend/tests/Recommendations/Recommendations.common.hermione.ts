describe('ProductPage', function() {
    describe('Рекомендации', function() {
        const cardSelector = '.Card';
        const recommendationsSelector = '.Recommendations';
        const firstRecommendationLink = '.Recommendations .ProductCardsList-Item:nth-child(1) .Link';

        it('Внешний вид и клик в рекомендацию', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/product/558171067/sku/101446185758?flags=goods_recommender_enabled=1');
            await bro.yaWaitForVisible(cardSelector, 3000, 'карточка товара не появилась');

            await bro.$(recommendationsSelector).scrollIntoView();

            await bro.assertView('plain', recommendationsSelector);

            if (await bro.getMeta('platform') === 'desktop') {
                const target = await bro.getAttribute(firstRecommendationLink, 'target');
                assert.strictEqual(target, '_blank');
            }

            await bro.click(firstRecommendationLink);
            await bro.yaCheckBaobabEvent({ path: '$page.$main.card.recommendations.scroller.$result.productCard' });
        });

        it('Внешний вид рекомендаций для фармы', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/product/228007281/sku/101392471728?flags=goods_recommender_enabled=1');
            await bro.yaWaitForVisible(cardSelector, 3000, 'карточка товара не появилась');

            await bro.$(recommendationsSelector).scrollIntoView();

            await bro.assertView('plain', recommendationsSelector);
        });
    });
});
