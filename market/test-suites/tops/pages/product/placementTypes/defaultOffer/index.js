import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import {PAGE_IDS_TOUCH} from '@self/root/src/constants/pageIds';

// suites
import DeliveryFromWarehouseSuite from '@self/platform/spec/hermione/test-suites/blocks/DeliveryFromWarehouse';
import DeliveryFromWarehousePopupSuite from '@self/platform/spec/hermione/test-suites/blocks/DeliveryFromWarehouse/popup';
import ShopRatingSuite from '@self/platform/spec/hermione/test-suites/blocks/DefaultOffer/shopRating';

// page-objects
import DeliveryFromWarehousePO from '@self/platform/components/DeliveryFromWarehouse/__pageObject';
import DefaultOfferPO from '@self/platform/spec/page-objects/components/DefaultOffer';

import {
    route as placementTypesPageRoute,
    productWithFirstPartyOfferState,
    productWithThirdPartyOfferState,
    cpcOfferState,
} from '../fixtures/placementTypes';

const EXPECTED = {
    POPUP_HEADER_TEXT: 'Товары со склада Яндекса',
};

export default makeSuite('Типы размещения офферов.', {
    environment: 'kadavr',
    story: mergeSuites(
        makeSuite('Карточка модели. ДО. 1P оффер.', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('report', productWithFirstPartyOfferState);
                        return this.browser.yaOpenPage(
                            PAGE_IDS_TOUCH.YANDEX_MARKET_PRODUCT,
                            placementTypesPageRoute
                        );
                    },
                },
                prepareSuite(DeliveryFromWarehouseSuite, {
                    meta: {
                        id: 'marketfront-4166',
                        issue: 'MARKETFRONT-18602',
                    },
                    params: {
                        expectedElementText: '',
                    },
                    pageObjects: {
                        deliveryFromWarehouse() {
                            return this.createPageObject(DeliveryFromWarehousePO);
                        },
                    },
                }),
                prepareSuite(DeliveryFromWarehousePopupSuite, {
                    meta: {
                        id: 'marketfront-4166',
                        issue: 'MARKETFRONT-18602',
                    },
                    params: {
                        expectedPopupHeaderText: EXPECTED.POPUP_HEADER_TEXT,
                    },
                    pageObjects: {
                        deliveryFromWarehouse() {
                            return this.createPageObject(DeliveryFromWarehousePO);
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
                        return this.browser.yaOpenPage(
                            PAGE_IDS_TOUCH.YANDEX_MARKET_PRODUCT,
                            placementTypesPageRoute
                        );
                    },
                },
                prepareSuite(DeliveryFromWarehouseSuite, {
                    meta: {
                        id: 'marketfront-4167',
                        issue: 'MARKETFRONT-18602',
                    },
                    params: {
                        expectedElementText: '',
                    },
                    pageObjects: {
                        deliveryFromWarehouse() {
                            return this.createPageObject(DeliveryFromWarehousePO);
                        },
                    },
                }),
                prepareSuite(DeliveryFromWarehousePopupSuite, {
                    meta: {
                        id: 'marketfront-4167',
                        issue: 'MARKETFRONT-18602',
                    },
                    params: {
                        expectedPopupHeaderText: EXPECTED.POPUP_HEADER_TEXT,
                    },
                    pageObjects: {
                        deliveryFromWarehouse() {
                            return this.createPageObject(DeliveryFromWarehousePO);
                        },
                    },
                })
            ),
        }),
        makeSuite('Карточка модели. ДО. CPC оффер. Рейтинг магазина.', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('report', cpcOfferState);
                        return this.browser.yaOpenPage(
                            PAGE_IDS_TOUCH.YANDEX_MARKET_PRODUCT,
                            placementTypesPageRoute
                        );
                    },
                },
                prepareSuite(ShopRatingSuite, {
                    meta: {
                        id: 'marketfront-4164',
                        issue: 'MARKETFRONT-18602',
                    },
                    pageObjects: {
                        defaultOffer() {
                            return this.createPageObject(DefaultOfferPO);
                        },
                    },
                })
            ),
        })
    ),
});
