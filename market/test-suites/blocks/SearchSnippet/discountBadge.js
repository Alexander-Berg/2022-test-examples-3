import {makeCase, makeSuite} from 'ginny';

import SearchSnippetPrice from '@self/platform/spec/page-objects/containers/SearchSnippet/Price';

/**
 * Тест на блок DiscountBadge
 *
 * @param {PageObject.SearchSnippetPrice} snippetPrice
 */
export default makeSuite('Бейдж скидок.', {
    feature: 'Бейдж скидок.',
    story: {
        'По умолчанию': {
            'должен отображаться': makeCase({
                test() {
                    return this.browser.allure.runStep('Проверяем видимость бейджа', async () => {
                        await this.browser.scroll(SearchSnippetPrice.discountBadge);

                        const discountBadgeVisible = await this.snippetPrice.discountBadge.isVisible();

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
