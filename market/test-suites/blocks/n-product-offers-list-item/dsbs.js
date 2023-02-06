import {makeSuite, makeCase} from 'ginny';
import OfferPhotoThumbnail from '@self/platform/components/OfferPhotoThumbnail/__pageObject';
import ShopInfo from '@self/project/src/components/ShopInfo/__pageObject';
import Price from '@self/platform/components/Price/__pageObject';
import OfferDetailsPopup from '@self/platform/widgets/content/OfferDetailsPopup/__pageObject';


export default makeSuite('Снипет DSBS товара.', {
    environment: 'kadavr',
    params: {
        urls: 'Урлы офера',
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                shopInfo: () => this.createPageObject(ShopInfo, {parent: this.snippet}),
                image: () => this.createPageObject(OfferPhotoThumbnail, {parent: this.snippet}),
                price: () => this.createPageObject(Price, {parent: this.snippet}),
                offerInfo: () => this.createPageObject(OfferDetailsPopup),
            });
        },

        'При клике': {
            'по заголовку открывается попап': makeCase({
                id: 'marketfront-4353',
                issue: 'MARKETFRONT-35406',

                async test() {
                    await this.offerInfo.isExisting()
                        .should.eventually.be.equal(
                            false,
                            'До клика попап должен быть скрыт'
                        );

                    await this.snippet.clickTitle();

                    await this.offerInfo.waitForVisible();
                },
            }),
            'по цене открывается попап': makeCase({
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
            'по картинке открывается попап': makeCase({
                id: 'marketfront-4353',
                issue: 'MARKETFRONT-35406',

                async test() {
                    await this.offerInfo.isExisting()
                        .should.eventually.be.equal(
                            false,
                            'До клика попап должен быть скрыт'
                        );

                    await this.image.clickImage();

                    await this.offerInfo.waitForVisible();
                },
            }),
            'по названию магазина переход на КО': makeCase({
                id: 'marketfront-4353',
                issue: 'MARKETFRONT-35406',

                async test() {
                    const {urls} = this.params;

                    const newTabIdPromise = this.browser.yaWaitForNewTab({timeout: 2000});
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
