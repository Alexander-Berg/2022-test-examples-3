hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('ProductCard', function() {
        it('currentPrice', async function() {
            await this.browser.yaOpenComponent('tests-productcard--currentprice', true);
            await this.browser.yaAssertViewThemeStorybook('current-price', '.ProductCard');
        });

        it('rangePrice', async function() {
            await this.browser.yaOpenComponent('tests-productcard--rangeprice', true);
            await this.browser.yaAssertViewThemeStorybook('range-price', '.ProductCard');
        });

        it('withImageProps', async function() {
            await this.browser.yaOpenComponent('tests-productcard--withimageprops', true);
            await this.browser.yaAssertViewThemeStorybook('image-props', '.ProductCard');
        });

        it('withoutOffer', async function() {
            await this.browser.yaOpenComponent('tests-productcard--withoutoffer', true);
            await this.browser.yaAssertViewThemeStorybook('without-offer', '.ProductCard');
        });

        it('product', async function() {
            await this.browser.yaOpenComponent('tests-productcard--product', true);
            await this.browser.yaAssertViewThemeStorybook('product', '.ProductCard');
        });

        it('sku', async function() {
            await this.browser.yaOpenComponent('tests-productcard--sku', true);
            await this.browser.yaAssertViewThemeStorybook('sku', '.ProductCard');
        });

        it('с лейблом у цены', async function() {
            await this.browser.yaOpenComponent('tests-productcard--sku', true, [{
                name: 'priceLabel',
                value: 'Хорошая цена',
            }]);
            await this.browser.yaAssertViewThemeStorybook('price-label', '.ProductCard');
        });

        it('withZeroPrice', async function() {
            await this.browser.yaOpenComponent('tests-productcard--withzeroprice', true);
            await this.browser.yaAssertViewThemeStorybook('with-zero-price', '.ProductCard');
        });

        it('expTitleThumb', async function() {
            await this.browser.yaOpenComponent('tests-productcard--exptitlethumb', true);
            await this.browser.yaAssertViewThemeStorybook('exp-title-thumb', '.ProductCard');
        });

        it('usedOffer', async function() {
            await this.browser.yaOpenComponent('tests-productcard--usedoffer', true);
            await this.browser.yaAssertViewThemeStorybook('used-npmoffer', '.ProductCard');
        });

        it('expAloneOfferSku', async function() {
            await this.browser.yaOpenComponent('tests-productcard--expaloneoffersku', true);
            await this.browser.yaAssertViewThemeStorybook('exp-alone-offer-sku', '.ProductCard');
        });

        it('expOfferAsSku', async function() {
            await this.browser.yaOpenComponent('tests-productcard--expofferassku', true);
            await this.browser.yaAssertViewThemeStorybook('exp-alone-offer-sku', '.ProductCard');
        });
    });
});
