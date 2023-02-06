import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

// suites
import DeliveryFromWarehouse from '@self/platform/spec/hermione/test-suites/blocks/DeliveryFromWarehouse';
import DeliveryFromWarehouseTooltip from '@self/platform/spec/hermione/test-suites/blocks/DeliveryFromWarehouse/tooltip';
import ShopLogoSuite from '@self/platform/spec/hermione/test-suites/blocks/n-shop-logo';
import ShopRatingSuite from '@self/platform/spec/hermione/test-suites/blocks/ShopRating';

// page-objects
import DeliveryContent from '@self/platform/components/DeliveryInfo/DeliveryContent/__pageObject';
import DeliveryTooltipContent from '@self/platform/components/DeliveryInfo/DeliveryTooltipContent/__pageObject';
import DefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
// import ShopLogo from '@self/platform/spec/page-objects/components/ShopLogo';
import ShopInfo from '@self/platform/spec/page-objects/components/ShopInfo';
import ShopRating from '@self/platform/spec/page-objects/components/ShopRating';

import {
    productWithFirstPartyOfferState,
    productWithThirdPartyOfferState,
    cpcOfferState,
    route as placementTypesPageRoute,
} from '../fixtures/placementTypes';

export default makeSuite('Типы размещения офферов', {
    environment: 'kadavr',
    story: mergeSuites(
        makeSuite('Карточка модели. ДО. 1P оффер.', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('report', productWithFirstPartyOfferState);
                        return this.browser.yaOpenPage('market:product', placementTypesPageRoute);
                    },
                },
                prepareSuite(DeliveryFromWarehouse, {
                    meta: {
                        id: 'marketfront-4166',
                        issue: 'MARKETFRONT-17082',
                    },
                    params: {
                        expectedElementText: 'Доставка Яндекса со своего склада',
                    },
                    pageObjects: {
                        deliveryFromWarehouse() {
                            return this.createPageObject(DeliveryContent);
                        },
                    },
                }),
                prepareSuite(DeliveryFromWarehouseTooltip, {
                    meta: {
                        id: 'marketfront-4166',
                        issue: 'MARKETFRONT-17082',
                    },
                    params: {
                        expectedTooltipHeaderText: 'Товары со склада Яндекса',
                    },
                    pageObjects: {
                        deliveryFromWarehouse() {
                            return this.createPageObject(DeliveryContent);
                        },
                        tooltipContent() {
                            return this.createPageObject(DeliveryTooltipContent);
                        },
                    },
                }),
                prepareSuite(ShopLogoSuite, {
                    meta: {
                        id: 'marketfront-4160',
                        issue: 'MARKETFRONT-17082',
                    },
                    pageObjects: {
                        shopLogo() {
                            return this.createPageObject(ShopInfo, {
                                parent: DefaultOffer.root,
                            });
                        },
                    },
                })
            ),
        }),
        makeSuite('Карточка модели. ДО. 3P оффер.', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('report', productWithThirdPartyOfferState);
                        return this.browser.yaOpenPage('market:product', placementTypesPageRoute);
                    },
                },
                prepareSuite(DeliveryFromWarehouse, {
                    meta: {
                        id: 'marketfront-4167',
                        issue: 'MARKETFRONT-17082',
                    },
                    params: {
                        expectedElementText: 'Доставка Яндекса со своего склада',
                    },
                    pageObjects: {
                        deliveryFromWarehouse() {
                            return this.createPageObject(DeliveryContent);
                        },
                    },
                }),
                prepareSuite(DeliveryFromWarehouseTooltip, {
                    meta: {
                        id: 'marketfront-4167',
                        issue: 'MARKETFRONT-17082',
                    },
                    params: {
                        expectedTooltipHeaderText: 'Товары со склада Яндекса',
                    },
                    pageObjects: {
                        deliveryFromWarehouse() {
                            return this.createPageObject(DeliveryContent);
                        },
                        tooltipContent() {
                            return this.createPageObject(DeliveryTooltipContent);
                        },
                    },
                })
            ),
        }),
        makeSuite('Карточка модели. ДО. CPC оффер. Логотип и рейтинг магазина.', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('report', cpcOfferState);
                        return this.browser.yaOpenPage('market:product', placementTypesPageRoute);
                    },
                },
                /**
                 * @expFlag dsk_km-do_trust-rev
                 * @ticket MARKETFRONT-71593
                 * @start
                 */
                // Логотип отображаем только у ЯМ - после редизайна ShopInfo и добавления логотипов контентом, нужно поправить
                // prepareSuite(ShopLogoSuite, {
                //     meta: {
                //         id: 'marketfront-4164',
                //         issue: 'MARKETFRONT-17082',
                //     },
                //     pageObjects: {
                //         shopLogo() {
                //             return this.createPageObject(ShopLogo, {
                //                 parent: DefaultOffer.root,
                //             });
                //         },
                //     },
                // }),
                prepareSuite(ShopRatingSuite, {
                    meta: {
                        id: 'marketfront-4164',
                        issue: 'MARKETFRONT-17082',
                    },
                    pageObjects: {
                        shopRating() {
                            return this.createPageObject(ShopRating, {
                                parent: DefaultOffer.root,
                            });
                        },
                    },
                })
            ),
        })
    ),
});
