'use strict';

import {mergeSuites, importSuite, makeSuite} from 'ginny';

import buildUrl from 'spec/lib/helpers/buildUrl';

import PRODUCTS from 'app/constants/products';

import openProductModalHook from '../hooks/openProductModal';

/**
 * Тест перехода по ссылке на заказ в Балансе
 * @param {PageObject.Products} products - список услуг
 * @param {PageObject.Modal} modal - модальное окно изменения данных об услуге
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 * @param {string} params.pageRouteName - ключ страницы
 * @param {number} params.vendor - ID вендора
 */
export default makeSuite('Переход на заказ в Балансе.', {
    feature: 'Управление услугами и пользователями',
    issue: 'VNDFRONT-3429',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
        productName: 'Услуга',
    },
    story: mergeSuites(
        openProductModalHook(),
        importSuite('Link', {
            meta: {
                environment: 'kadavr',
            },
            params: {
                target: '_blank',
                external: true,
            },
            pageObjects: {
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.form.getReadonlyFieldByName('balanceLink'));
                },
            },
            hooks: {
                // @ts-expect-error(TS2322) найдено в рамках VNDFRONT-4580
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.url = buildUrl('external:balance-admin-order', {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        serviceId: PRODUCTS[this.params.productKey].serviceId,
                        orderId: 724,
                    });

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.caption = this.params.url;
                },
            },
        }),
    ),
});
