/* eslint-disable no-unreachable */

import _ from 'lodash';
import {makeSuite, makeCase} from 'ginny';
import {createProduct, createOfferForProduct, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {
    getPromoFilterState,
    promoFilterValue1,
    promoFilterValue1Checked,
    promoFilterValue2,
} from '@self/platform/spec/hermione/fixtures/filters/promo';

import {guruMock} from '@self/platform/spec/hermione/fixtures/priceFilter/product';
import dataFixture from '@self/platform/spec/hermione/test-suites/tops/pages/n-page-product-offers/fixtures/data';
import productOptionsFixture from
    '@self/platform/spec/hermione/test-suites/tops/pages/n-page-product-offers/fixtures/productOptions';
import offerFixture from '@self/platform/spec/hermione/test-suites/tops/pages/n-page-product-offers/fixtures/offer';


const getState = filterValues => {
    const {mock} = guruMock;
    const product = createProduct(_.assign({}, productOptionsFixture, mock), mock.id);
    const offer = createOfferForProduct(offerFixture, mock.id, '3');

    return mergeState([
        offer,
        product,
        dataFixture,
        getPromoFilterState(filterValues),
    ]);
};

const initialState = getState([promoFilterValue1, promoFilterValue2]);
const checkedState = getState([promoFilterValue1Checked, promoFilterValue2]);

/**
 * Тесты на блок фильтров КМ вкладки "Цены"
 * @param {PageObject.FilterList} filterList
 * @param {PageObject.ProductTabs} productTabs
 */
export default makeSuite('Фильтр третьего порядка.', {
    feature: 'Фильтры',
    environment: 'kadavr',
    story: {
        'При переходе на главную КМ и обратно на цены': {
            async beforeEach() {
                const {mock} = guruMock;
                await this.browser.setState('report', initialState);
                return this.browser.yaOpenPage('market:product-offers', {
                    'productId': mock.id,
                    'slug': mock.slug,
                });
            },

            'правильно обнуляется': makeCase({
                id: 'marketfront-2712',
                issue: 'MARKETVERSTKA-30532',
                async test() {
                    // eslint-disable-next-line market/ginny/no-skip
                    return this.skip('MARKETFRONT-40812 скипаем упавшие тесты ' +
                        'т к были оторваны табы КМ и потерялась точка входа');

                    await this.browser.allure.runStep(
                        'Устанавливаем фильтр по первому промо-предложению',
                        async () => {
                            await this.filterList.waitForVisible();
                            await this.browser.setState('report', checkedState);
                            await this.filterList.clickItemByIndex(1);
                        }
                    );

                    await this.browser.allure.runStep(
                        'Переходим на главную КМ',
                        () => this.miniCard.backToProductLinkClick()
                    );

                    await this.browser.allure.runStep(
                        'Возвращаемся на Цены',
                        () => this.morePricesLink.morePricesLinkClick()
                    );

                    await this.browser.allure.runStep(
                        'Снимаем фильтр по первому промо-предложению',
                        async () => {
                            await this.filterList.waitForVisible();
                            await this.browser.setState('report', initialState);
                            await this.filterList.clickItemByIndex(1);
                        }
                    );

                    await this.browser.allure.runStep(
                        'Снова переходим на главную КМ',
                        () => this.miniCard.backToProductLinkClick()
                    );

                    // В урле нет фильтрации по промо-предложению
                    const {query} = await this.browser.yaParseUrl();
                    return this.expect(query, 'Нет параметра promo-type')
                        .to.not.have.property('promo-type');
                },
            }),
        },
    },
});
