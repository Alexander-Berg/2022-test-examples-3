import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок DiscountBadge
 *
 * @param {PageObject.DiscountBadge | PageObject.DiscountBadgeReact} discountBadge
 */
export default makeSuite('Бейдж скидок.', {
    feature: 'Бейдж скидок.',
    story: {
        'По умолчанию': {
            'должен отображаться': makeCase({
                test() {
                    return this.browser.allure.runStep('Проверяем видимость бейджа', async () => {
                        await this.discountBadge.scrollToBadge();

                        const discountBadgeVisible = await this.discountBadge.root.isVisible();

                        return this.expect(discountBadgeVisible).to.equal(
                            true,
                            'Бейдж скидок отображается'
                        );
                    });
                },
            }),
        },
    },
});
