import {prepareSuite, makeSuite} from 'ginny';

import {createPrice} from '@yandex-market/kadavr/mocks/Report/helpers/price';
import {createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
import {phoneProductRoute, productWithPicture} from '@self/platform/spec/hermione/fixtures/product';
import LinkSellersInfoSuite from '@self/platform/spec/hermione/test-suites/blocks/CreditDisclaimer/linkSellersInfo';
import CreditDisclaimer from '@self/platform/widgets/parts/CreditDisclaimer/__pageObject';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';

import {anotherTestShop, offerUrls, testShop} from './kadavrMocks';

export default makeSuite('Ссылка "Информация о продавцах"', {
    environment: 'kadavr',
    story:
        prepareSuite(LinkSellersInfoSuite, {
            params: {
                shopInfo: [testShop, anotherTestShop],
                shopIds: [testShop, anotherTestShop].map(shop => shop.id.toString()),
                offersCount: 6,
            },
            pageObjects: {
                creditDisclaimer() {
                    return this.createPageObject(CreditDisclaimer);
                },
            },
            hooks: {
                async beforeEach() {
                    const offers = [];
                    const {offersCount, shopInfo} = this.params;

                    for (let i = 0; i < offersCount; i++) {
                        offers.push(createOffer({
                            shop: shopInfo[i % 2],
                            urls: offerUrls,
                            price: createPrice(1000),
                            filters: [{
                                id: 1,
                                type: 'enum',
                                name: 'Цвет товара',
                                subType: 'image_picker',
                                kind: 2,
                                position: 1,
                                values: [{
                                    checked: true,
                                    found: 1,
                                    value: 'золотой',
                                    id: 1,
                                }],
                            }],
                            filterState: {
                                1: ['1'],
                            },
                        }));
                    }

                    const state = mergeReportState([productWithPicture, ...offers, {
                        data: {
                            search: {
                                total: offersCount,
                                totalOffers: offersCount,
                                totalOffersBeforeFilters: offersCount,
                                totalModels: offersCount,
                            },
                        },
                    }]);

                    await this.browser.setState('report', state);

                    await this.browser.yaOpenPage('touch:product', Object.assign({}, phoneProductRoute, {'hideSeoContent': 1}));
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));

                    await this.browser.allure.runStep(
                        'Ждём загрузки блока c юридическим дисклеймером о покупке в кредит',
                        () => this.creditDisclaimer.waitForVisible()
                    );
                },
            },
        }),
});
