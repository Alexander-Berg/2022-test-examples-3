import {makeSuite, makeCase} from 'ginny';
import TopOfferSnippet from '@self/platform/spec/page-objects/components/TopOfferSnippet';
import ShopInfo from '@self/project/src/components/ShopInfo/__pageObject';
import OfferDetailsPopup from '@self/platform/widgets/content/OfferDetailsPopup/__pageObject';
import Price from '@self/platform/components/Price/__pageObject';

export default makeSuite('DSBS полный офер.', {
    environment: 'kadavr',
    params: {
        urls: 'Урлы офера',
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                topOfferSnippet: () => this.createPageObject(TopOfferSnippet, {
                    root: `${TopOfferSnippet.root}:nth-child(1)`,
                }),
                shopInfo: () => this.createPageObject(ShopInfo, {parent: this.topOfferSnippet}),
                price: () => this.createPageObject(Price, {parent: this.topOfferSnippet}),
                offerInfo: () => this.createPageObject(OfferDetailsPopup),
            });
        },

        'При клике': {
            'по цене должен открывать попап': makeCase({
                id: 'marketfront-4353',
                issue: 'MARKETFRONT-35406',

                async test() {
                    await this.offerInfo.isExisting()
                        .should.eventually.be.equal(
                            false,
                            'До клика попап должен быть скрыт'
                        );

                    await this.price.clickPrice();

                    await this.offerInfo.waitForVisible();
                },
            }),
            'по названию магазина должна открываться новая вкладка': makeCase({
                id: 'marketfront-4353',
                issue: 'MARKETFRONT-35406',

                async test() {
                    const {urls} = this.params;

                    const newTabIdPromise = this.browser.yaWaitForNewTab({
                        timeout: 2000,
                    });
                    await this.shopInfo.dsbsShopNameclick();
                    const newTabId = await newTabIdPromise;

                    await this.browser.allure.runStep(
                        'Переключаем вкладку на только что открытую вкладку карточки офера',
                        () => this.browser.switchTab(newTabId)
                    );

                    const url = await this.browser.getUrl();

                    await this.browser.allure.runStep(
                        'Проверяем, что url открытой вкладки соответствовал url офера',
                        () => Promise.resolve(url).should.eventually.be.link({
                            pathname: urls.offercard,
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        })
                    );
                },
            }),
        },
    },
});
