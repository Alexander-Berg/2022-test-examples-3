'use strict';

import {mergeSuites, PageObject, makeSuite, makeCase} from 'ginny';

import PRODUCT_KEYS from 'app/constants/products/keys';

import openProductModalHook from '../hooks/openProductModal';

const ProductManagerForm = PageObject.get('ProductManagerForm');

const PRODUCT_KEYS_WITH_COVER = [
    PRODUCT_KEYS.BRAND_ZONE,
    PRODUCT_KEYS.PAID_OPINIONS,
    PRODUCT_KEYS.MODEL_BIDS,
    PRODUCT_KEYS.RECOMMENDED,
];

/**
 * Тест на отмену подключения услуги
 * @param {PageObject.Products} products - список услуг
 * @param {PageObject.Modal} modal - модальное окно изменения данных об услуге
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 * @param {string} params.pageRouteName - ключ страницы
 * @param {number} params.vendor - ID вендора
 * @param {boolean} [params.canBeActivated] - флаг того, что услугу можно запустить из интерфейса
 */
export default makeSuite('Отмена подключения услуги.', {
    feature: 'Управление услугами и пользователями',
    issue: 'VNDFRONT-3408',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
        productName: 'Услуга',
    },
    story: mergeSuites(openProductModalHook(), {
        'При отмене': {
            'форма скрывается без подключения услуги': makeCase({
                async test() {
                    this.setPageObjects({
                        cancelButton() {
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            return this.createPageObject('ButtonB2b', this.modal, ProductManagerForm.cancelButton);
                        },
                    });

                    await this.cancelButton.click();

                    await this.modal.waitForHidden();

                    const {canBeActivated, productKey, productName} = this.params;

                    await this.product
                        .isVisible()
                        .should.eventually.be.equal(true, `Карточка услуги "${productName}" отображается`);

                    if (canBeActivated) {
                        if (PRODUCT_KEYS_WITH_COVER.includes(productKey)) {
                            return this.product.cover
                                .isVisible()
                                .should.eventually.be.equal(true, `Обложка услуги "${productName}" отображается`);
                        }

                        return this.product.placement
                            .getText()
                            .should.eventually.be.equal('Не подключено', 'Текст статуса услуги верный');
                    }

                    this.setPageObjects({
                        warning() {
                            return this.createPageObject(
                                'TextNextLevitan',
                                this.product,
                                this.product.setPlacementWarning,
                            );
                        },
                    });

                    return this.warning
                        .getText()
                        .should.eventually.be.equal(
                            'Услуга не подключена: не принята оферта или не введены данные договора',
                            'Текст сообщения о проблемах с договором верный',
                        );
                },
            }),
        },
    }),
});
