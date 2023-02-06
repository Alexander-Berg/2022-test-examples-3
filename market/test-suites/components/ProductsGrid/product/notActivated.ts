'use strict';

import {makeCase, makeSuite, mergeSuites} from 'ginny';

import PRODUCT_KEYS from 'app/constants/products/keys';

import showProductHook from './hooks/showProduct';

const PRODUCT_KEYS_WITH_COVER = [
    PRODUCT_KEYS.BRAND_ZONE,
    PRODUCT_KEYS.PAID_OPINIONS,
    PRODUCT_KEYS.MODEL_BIDS,
    PRODUCT_KEYS.RECOMMENDED,
];

/**
 * Тесты на отображение неподлкюченной услуги
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 */
export default makeSuite('Отображение неподключённой услуги.', {
    issue: 'VNDFRONT-3809',
    environment: 'kadavr',
    feature: 'Управление услугами и пользователями',
    params: {
        user: 'Пользователь',
        productName: 'Услуга',
    },
    story: mergeSuites(
        showProductHook({
            managerView: false,
            details: false,
        }),
        {
            beforeEach() {
                this.setPageObjects({
                    warning() {
                        return this.createPageObject('TextNextLevitan', this.product, this.product.setPlacementWarning);
                    },
                    details() {
                        return this.createPageObject('ProductDetails', this.product);
                    },
                });
            },
            'При наличии неподключённой услуги': {
                'у неё oтображается обложка или статус "Не подключено" и сообщение о проблемах с договором': makeCase({
                    async test() {
                        const {productKey, productName, isManager} = this.params;

                        if (PRODUCT_KEYS_WITH_COVER.includes(productKey)) {
                            await this.product.cover
                                .isVisible()
                                .should.eventually.be.equal(true, `Обложка услуги "${productName}" отображается`);
                        } else {
                            await this.product.placement
                                .getText()
                                .should.eventually.be.equal('Не подключено', 'Текст статуса услуги верный');
                        }

                        await this.warning
                            .getText()
                            .should.eventually.be.equal(
                                'Услуга не подключена: не принята оферта или не введены данные договора',
                                'Текст сообщения о проблемах с договором верный',
                            );

                        if (isManager) {
                            await this.details
                                .isVisible()
                                .should.eventually.be.equal(true, 'Менеджерский блок отображается');
                        }
                    },
                }),
            },
        },
    ),
});
