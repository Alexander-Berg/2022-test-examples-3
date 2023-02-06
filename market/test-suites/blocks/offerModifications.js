import {makeSuite, makeCase} from 'ginny';
// TODO: сьют использовался на странице карточки модели в блоке топ6 ,
// можно переписать для тестирования аналогичного функционала в списке офферов n-page-product-offers
// https://st.yandex-team.ru/MARKETFRONT-19373

export default makeSuite('Eщё N вариантов', {
    story: {
        'При клике': {
            'N > 48': {
                'Происходит переход в таб цен': makeCase({
                    async test() {
                        await this.top6.waitForVisible();
                        const expectedPathname = await this.browser.yaBuildURL(
                            'market:product-offers',
                            {
                                productId: this.params.productId,
                                slug: this.params.slug,
                            }
                        );

                        await this.browser.yaWaitForChangeUrl(
                            () => this.topOfferSecond.moreOffersLink.click()
                        );

                        const {pathname} = await this.browser.yaParseUrl();

                        await this.expect(
                            pathname
                        ).to.be.equal(expectedPathname, 'произошел переход в таб цен');
                    },
                }),
            },
            'N < 48': {
                'Попап': {
                    'Открывается': makeCase({
                        async test() {
                            await this.top6.waitForVisible();
                            await this.topOfferFirst.moreOffersLink.click();
                            await this.offerModifications.waitForVisible();

                            await this.expect(this.offerModifications.isVisible()).to.be.equal(true, 'попап открылся');
                        },
                    }),
                    'Открыт': {
                        'При клике на паранджу закрывается': makeCase({
                            async test() {
                                await this.top6.waitForVisible();
                                await this.topOfferFirst.moreOffersLink.click();
                                await this.offerModifications.waitForVisible();
                                await this.modalFloat.closeOnParanja();
                                await this.browser.yaDelay(100);
                                await this.expect(
                                    this.offerModifications.isVisible()
                                ).to.be.equal(false, 'попап закрылся');
                            },
                        }),
                        'Кнопка "В магазин"': {
                            'При клике': makeCase({
                                async test() {
                                    await this.top6.waitForVisible();
                                    await this.topOfferFirst.moreOffersLink.click();
                                    await this.offerModificationsSnippet.waitForVisible();


                                    const currentTabId = await this.browser.allure.runStep(
                                        'Получаем идентификатор текущей вкладки',
                                        () => this.browser.getCurrentTabId()
                                    );

                                    await this.offerModificationsSnippet.clickoutLink.click();
                                    const newTabId = await this.browser
                                        .yaWaitForNewTab({startTabIds: [currentTabId], timeout: 2000});

                                    await this.browser.allure.runStep(
                                        'Переключаем вкладку на только что открытую',
                                        () => this.browser.switchTab(newTabId)
                                    );
                                    await this.browser.yaWaitForPageLoaded();
                                    const {hostname} = await this.browser.yaParseUrl();


                                    await this.expect(
                                        hostname.includes('market')
                                    ).to.be.equal(false, 'произошел переход в магазин');
                                },
                            }),
                        },
                        'Информация о продавце присутствует': makeCase({
                            id: 'marketfront-4054',
                            issue: 'MARKETFRONT-10895',
                            async test() {
                                await this.top6.waitForVisible();
                                await this.topOfferFirst.moreOffersLink.click();
                                await this.offerModificationsSnippet.waitForVisible();

                                const isSupplierNameExisting =
                                    await this.offerModificationsSnippet.supplierName.isVisible();

                                await this.expect(isSupplierNameExisting).to.be.equal(
                                    true, 'Информация о продавце присутствует.'
                                );
                            },
                        }),
                    },
                },
            },
        },
    },
});
