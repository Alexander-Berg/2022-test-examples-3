import {makeCase, makeSuite, prepareSuite, mergeSuites} from 'ginny';

import DefaultOfferContentSuite from './content';

/**
 * Тесты на блок DefaultOffer.
 *
 * @property {PageObject.DefaultOffer} this.defaultOffer
 */
export default makeSuite('Новый дефолтный оффер.', {
    story: {
        'Содержимое.': mergeSuites(
            prepareSuite(DefaultOfferContentSuite, {
                suiteName: 'Кнопка перехода в магазин.',
                meta: {
                    id: 'm-touch-1785',
                    issue: 'MOBMARKET-6436',
                },
                params: {
                    showShopBtn: true,
                },
            }),
            prepareSuite(DefaultOfferContentSuite, {
                suiteName: 'Цена товарного предложения.',
                meta: {
                    id: 'm-touch-1783',
                    issue: 'MOBMARKET-6434',
                },
                params: {
                    showPrice: true,
                },
            })
        ),

        'По умолчанию': {
            'должен присутствовать': makeCase({
                id: 'm-touch-1782',
                issue: 'MOBMARKET-6433',
                test() {
                    return this.defaultOffer
                        .isExisting()
                        .should.eventually.to.equal(true, 'Проверяем, что ДО присутствует на странице');
                },
            }),
        },
    },
});
