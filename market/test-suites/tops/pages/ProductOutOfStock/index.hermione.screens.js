import {makeSuite, mergeSuites, makeCase} from '@yandex-market/ginny';

import {phoneProductRoute} from '@self/platform/spec/hermione/fixtures/product';

// mocks
import {productOutOfStock} from '@self/platform/spec/hermione2/fixtures/product/productOutOfStock';
// page-objects
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import ProductCard from '@self/platform/widgets/parts/ProductCard/__pageObject';
import MandrelDevToolsPO from '@self/platform/spec/page-objects/mandrel/DevTools';
// constants
import COOKIE_CONSTANTS from '@self/root/src/constants/cookie';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Карточка товара не в наличии', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-87761',
    defaultParams: {
        productTitle: 'Тестовый телефон',
        cookie: {},
    },
    story: mergeSuites(
        {
            async beforeEach() {
                hermione.setPageObjects.call(this, {
                    defaultOffer: () => this.browser.createPageObject(DefaultOffer),
                    productCard: () => this.browser.createPageObject(ProductCard),
                });
            },
        },
        makeSuite('По умолчанию', {
            story: {
                'отображается верно': makeCase({
                    async test() {
                        await this.browser.setState('report', productOutOfStock);

                        await this.browser.yaOpenPage(PAGE_IDS_COMMON.PRODUCT, phoneProductRoute);

                        await this.browser.allure.runStep(
                            'Дожидаемся загрузки блока с описанием товара',
                            () => this.productCard.waitForVisible()
                        );

                        await this.browser.allure.runStep(
                            'Проверяем что блок с ДО отсутствует',
                            async () => {
                                const isDOExist = await this.defaultOffer.isExisting();

                                return this.browser.expect(isDOExist).to.be.equal(false);
                            }
                        );

                        await this.browser.yaRemoveElement(MandrelDevToolsPO.root);

                        await this.browser.assertView('plain', ProductCard.root, {
                            allowViewportOverflow: true,
                        });
                    },
                }),
            },
        }),
        /**
         * @expFlag touch_out-of-stock-km_similar
         * @ticket MARKETFRONT-81951
         * @start
         */
        makeSuite('Редизайн', {
            defaultParams: {
                cookie: {
                    [COOKIE_CONSTANTS.EXP_FLAGS]: {
                        name: COOKIE_CONSTANTS.EXP_FLAGS,
                        value: 'touch_out-of-stock-km_similar',
                    },
                    [COOKIE_CONSTANTS.FORCE_AT_EXP]: {
                        name: COOKIE_CONSTANTS.FORCE_AT_EXP,
                        value: 'true',
                    },
                },
            },
            story: {
                'отображается верно': makeCase({
                    async test() {
                        await this.browser.setState('report', productOutOfStock);

                        await this.browser.yaOpenPage(PAGE_IDS_COMMON.PRODUCT, phoneProductRoute);

                        await this.browser.allure.runStep(
                            'Дожидаемся загрузки блока с описанием товара',
                            () => this.productCard.waitForVisible()
                        );

                        await this.browser.allure.runStep(
                            'Проверяем что блок с ДО отсутствует',
                            async () => {
                                const isDOExist = await this.defaultOffer.isExisting();

                                return this.browser.expect(isDOExist).to.be.equal(false);
                            }
                        );

                        await this.browser.yaRemoveElement(MandrelDevToolsPO.root);

                        await this.browser.assertView('plain', ProductCard.root, {
                            allowViewportOverflow: true,
                        });
                    },
                }),
            },
        })
        /**
         * @expFlag touch_out-of-stock-km_similar
         * @end
         */
    ),
});
