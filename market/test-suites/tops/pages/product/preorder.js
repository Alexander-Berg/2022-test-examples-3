import {makeSuite, prepareSuite} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

// mocks
import {
    PRODUCT_ROUTE,
} from '@self/platform/spec/hermione/test-suites/tops/pages/product/fixtures/productWithCutPrices';
// page-objects
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import ProductOffers from '@self/platform/spec/page-objects/widgets/parts/ProductOffers';
// suites
import PreorderSuite from '@self/project/src/spec/hermione/test-suites/blocks/preorder';

import {
    productWithPreorder,
} from '@self/platform/spec/hermione/fixtures/product';

const buildProductWithDefaultOfferPreorder = () => {
    const dataMixin = {
        data: {
            search: {
                total: 1,
                totalOffers: 1,
            },
        },
    };

    return mergeState([
        productWithPreorder,
        dataMixin,
    ]);
};

export default makeSuite('Дефолтный оффер.', {
    environment: 'kadavr',
    feature: 'предзаказ',
    story: prepareSuite(PreorderSuite, {
        meta: {
            id: 'm-touch-3686',
            issue: 'MARKETFRONT-51879',
        },
        hooks: {
            async beforeEach() {
                await this.browser.setState('report', buildProductWithDefaultOfferPreorder());
                await this.browser.yaOpenPage('touch:product', PRODUCT_ROUTE);
                await this.browser.allure.runStep(
                    'Дожидаемся загрузки ДО',
                    () => this.defaultOffer.waitForVisible()
                );

                await this.browser.allure.runStep(
                    'Скроллим до кнопки "Предзаказ" на ДО',
                    () => this.cartButton
                        .getSelector()
                        .then(selector => this.browser.scroll(selector, 0, -200))
                );
            },
        },
        pageObjects: {
            productOffers() {
                return this.createPageObject(ProductOffers);
            },
            defaultOffer() {
                return this.createPageObject(DefaultOffer, {
                    parent: this.productOffers,
                });
            },
            cartButton() {
                return this.createPageObject(CartButton, {
                    parent: this.defaultOffer,
                });
            },
        },
        params: {
            expectedText: 'Оформить предзаказ',
            expectedLink: 'my/checkout',
        },
    }),
});
