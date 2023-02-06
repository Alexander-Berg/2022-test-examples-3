/* eslint-disable no-unreachable */

import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на фильтры уценки на вкладке "Цены"
 * @property {PageObject.ProductOffersFilter} cutpriceFilter - Фильтр уценки на вкладке "Цены"
 * @property {PageObject.ProductTabs} productTabs - табы навигации на КМ
 */
export default makeSuite('Блок фильтров', {
    story: {
        'Фильтр состояния товара': {
            'cохраняет правильное значение': makeCase({
                id: 'marketfront-3085',
                issue: 'MARKETVERSTKA-32612',
                async test() {
                    // eslint-disable-next-line market/ginny/no-skip
                    return this.skip('MARKETFRONT-40812 скипаем упавшие тесты ' +
                        'т к были оторваны табы КМ и потерялась точка входа');

                    await this.cutpriceFilter.waitForVisible();

                    return this.cutpriceFilter.getActiveInputId()
                        .should.eventually.to.include(
                            'good-state_cutprice',
                            'Должен быть выбран фильтр с правильным id'
                        );
                },
            }),
        },
    },
});
