import {makeSuite, makeCase} from 'ginny';
import {get} from 'lodash/fp';

export default makeSuite('Поп-ап', {
    feature: 'Поп-ап разгруппировки по магазину',
    story: {
        'При клике на “Ещё N варианта”': {
            'открывается поп-ап с ценами от одного и того же магазина': makeCase({
                id: 'm-touch-3239',
                issue: 'MARKETFRONT-7546',
                async test() {
                    const isOpenerExisting = await this.offerSnippet.moreOffersOpener.isExisting();

                    await this.expect(isOpenerExisting).to.be.equal(true, '"Ещё N варианта" отображается на сниппете');

                    await this.offerSnippet.clickMoreOffersOpener();

                    await this.offerModifications.waitForAppearance();

                    const offerSnippetPrice = await this.offerSnippet.price.getText();
                    const offerModificationsPrice = await this.offerModifications.price.getText();

                    return this.expect(offerSnippetPrice).to.be.equal(offerModificationsPrice, 'Цены совпадают');
                },
            }),
        },
        'В открывшемся поп-апе при клике на кнопку "В магазин"': {
            'должен открываться сайт магазина в другой вкладке': makeCase({
                id: 'm-touch-3240',
                issue: 'MARKETFRONT-7546',
                async test() {
                    const currentTabId = await this.browser.allure.runStep(
                        'Получаем идентификатор текущей вкладки',
                        () => this.browser.getCurrentTabId()
                    );

                    const hostnameBeforeClick = await this.browser.yaParseUrl().then(get('hostname'));

                    await this.offerSnippet.clickMoreOffersOpener();

                    await this.offerModifications.waitForAppearance();

                    await this.offerModifications.shopLinkClick();

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
        'При клике на N отзывов': {
            'происходит переход на страницу отзывов о магазине': makeCase({
                id: 'm-touch-3241',
                issue: 'MARKETFRONT-7546',
                async test() {
                    await this.offerSnippet.clickMoreOffersOpener();

                    await this.offerModifications.waitForAppearance();

                    await this.offerModifications.shopReviewsLinkClick();

                    const changedUrl = await this.browser.getUrl();

                    const expectedUrl = await this.browser.yaBuildURL('touch:shop-reviews', {
                        shopId: this.params.shopId,
                        slug: this.params.slug,
                    });

                    await this.browser.allure.runStep(
                        'Проверяем URL открытой страницы',
                        () => this.expect(changedUrl).to.be.link(
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
        'При клике на паранжу': {
            'происходит закрытие поп-апа': makeCase({
                id: 'm-touch-3241',
                issue: 'MARKETFRONT-7546',
                async test() {
                    await this.offerSnippet.clickMoreOffersOpener();

                    await this.offerModifications.waitForAppearance();

                    return this.offerModifications.paranjaClick();
                },
            }),
        },
    },
});

