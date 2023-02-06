import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на n-snippet-cell2 при наличии скидки
 * @param {PageObject.SnippetCell2} snippetCell2
 */
export default makeSuite('Гридовый сниппет продукта с бейджем скидки.', {
    feature: 'Сниппет ко/км',
    story: {
        'При наличии скидки': {
            'должен отображаться бейдж': makeCase({
                id: 'marketfront-15',
                issue: 'MARKETVERSTKA-26059',
                test() {
                    return this.browser.allure.runStep('Проверяем наличие бейджа скидки', async () => {
                        const isDiscountBadgeExisting = await this.snippetCell2.discountBadge.isExisting();

                        return this.expect(isDiscountBadgeExisting).to.equal(true, 'Бейдж скидки присутствует');
                    });
                },
            }),
        },
    },
});
