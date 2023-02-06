import {makeCase, makeSuite} from 'ginny';
import SearchSnippetPrice from '@self/platform/spec/page-objects/containers/SearchSnippet/Price';

/**
 * Тест на бейдж с промокодом
 *
 * @param {PageObject.SearchSnippetPrice} snippetPrice
 * @param {PageObject.OfferDealPopup} dealsDescriptionPopup
 */
export default makeSuite('Бейдж акции.', {
    feature: 'Бейдж акции.',
    story: {
        'По умолчанию': {
            'должен содержать ожидаемый текст': makeCase({
                test() {
                    return this.browser.allure.runStep('Проверяем текст бейджа', async () => {
                        await this.browser.scroll(SearchSnippetPrice.promocode);

                        const dealsBadgeText = await this.snippetPrice.promocode.getText();

                        return this.expect(dealsBadgeText).to.equal(
                            this.params.expectedText,
                            'Бейдж акции содержит ожидаемый текст'
                        );
                    });
                },
            }),
        },
        'По наведению': {
            'должен открывать попап описания акции': makeCase({
                test() {
                    return this.browser.allure.runStep('Проверяем открытие попапа по наведению на бейдж', async () => {
                        await this.browser.scroll(SearchSnippetPrice.promocode);
                        await this.snippetPrice.promocode.click();
                        const isPopupVisible = await this.dealsDescriptionPopup.isExisting();
                        return this.expect(isPopupVisible).to.be.equal(true, 'Попап описания акции отображается');
                    });
                },
            }),
        },
    },
});
