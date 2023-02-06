import {
    makeSuite,
    makeCase,
} from 'ginny';

/**
 * @param {PageObject.SearchOffer} snippet
 */
export default makeSuite('Листовой сниппет оффера. Рейтинг и отзывы магазина.', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'должен отображаться рейтинг магазина': makeCase({
                id: 'm-touch-3088',
                issue: 'MARKETFRONT-6188',
                test() {
                    return this.snippet.shopRating.isExisting().should.eventually.to.equal(
                        true, 'Рейтинг магазина должен присутствовать'
                    );
                },
            }),
        },
        'При клике на N отзывов': {
            'происходит переход на страницу отзывов о магазине': makeCase({
                id: 'm-touch-3089',
                issue: 'MARKETFRONT-6188',
                async test() {
                    await this.browser.yaWaitForChangeUrl(() => this.snippet.clickShopRatingLink());

                    const currentUrl = await this.browser.getUrl();
                    const expectedUrl = await this.browser.yaBuildURL('touch:shop-reviews', {
                        shopId: this.params.shopId,
                        slug: this.params.slug,
                    });

                    await this.browser.allure.runStep(
                        'Проверяем URL открытой страницы',
                        () => this.expect(currentUrl).to.be.link(
                            expectedUrl,
                            {
                                skipProtocol: true,
                                skipHostname: true,
                            }
                        )
                    );
                },
            }),
        },
    },
});
