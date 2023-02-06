import {get} from 'lodash/fp';
import {
    makeSuite,
    makeCase,
} from 'ginny';

/**
 * @param {PageObject.SearchSnippetShopInfo} snippetShopInfo
 * @param {PageObject.SearchSnippetClickoutButton} snippetClickoutButton
 */
export default makeSuite('Сниппет оффера. Рейтинг и отзывы магазина.', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'должен отображаться рейтинг магазина': makeCase({
                id: 'm-touch-3090',
                issue: 'MARKETFRONT-6188',
                test() {
                    return this.snippetShopInfo.shopRating.isExisting().should.eventually.to.equal(
                        true, 'Рейтинг магазина должен присутствовать'
                    );
                },
            }),
            'должна отображаться кнопка "В магазин"': makeCase({
                id: 'm-touch-3090',
                issue: 'MARKETFRONT-6188',
                test() {
                    return this.snippetClickoutButton.isExisting().should.eventually.to.equal(
                        true, 'Кнопка "В магазин" должна присутствовать'
                    );
                },
            }),
        },
        'При клике на кнопку "В магазин"': {
            'должен открываться сайт магазина в другой вкладке': makeCase({
                id: 'm-touch-3092',
                issue: 'MARKETFRONT-6188',
                async test() {
                    const currentTabId = await this.browser.allure.runStep(
                        'Получаем идентификатор текущей вкладки',
                        () => this.browser.getCurrentTabId()
                    );

                    const hostnameBeforeClick = await this.browser.yaParseUrl().then(get('hostname'));

                    await this.snippetClickoutButton.click();

                    const newTabId = await this.browser.yaWaitForNewTab({startTabIds: [currentTabId], timeout: 2000});

                    await this.browser.allure.runStep(
                        'Переключаем вкладку на только что открытую вкладку карточки продукта',
                        () => this.browser.switchTab(newTabId)
                    );

                    const hostnameAfterClick = await this.browser.yaParseUrl().then(get('hostname'));

                    await this.browser.close();
                    await this.browser.allure.runStep(
                        'Переключаем вкладку на начальную',
                        () => this.browser.switchTab(currentTabId)
                    );

                    return this.expect(hostnameAfterClick)
                        .to.not.be.equal(hostnameBeforeClick, 'Успешно перешли в магазин');
                },
            }),
        },
    },
});
