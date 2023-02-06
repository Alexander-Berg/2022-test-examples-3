import {makeCase} from 'ginny';
import dayjs from 'dayjs';
import 'dayjs/locale/ru';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import common, {ORDER_ID, LAST_DATE_TO_PROLONGATE_PICKUP, LAST_DATE_TO_PROLONGATE_POST_TERM}
    from './common';

function additionalCases(type) {
    return {
        'Наличие у заказа ссылки "продлить".': makeCase({
            // ПВЗ 'marketfront-5060'
            // Постамат 'marketfront-5061'
            id: type === 'PICKUP' ? 'marketfront-5060' : 'marketfront-5061',
            issue: 'MARKETFRONT-51587',

            async test() {
                await this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDERS);
                await this.allure.runStep(
                    'Проверяем наличие ссылки на странице "Мои заказы"',
                    () => this.prolongateLink.isVisible().should.eventually.to.be.equal(
                        false,
                        'У заказа не должно быть ссылки'
                    )
                );

                await this.browser.yaOpenPage(this.params.pageId, {
                    orderId: ORDER_ID,
                });

                await this.allure.runStep(
                    'Проверяем наличие ссылки на странице заказа',
                    () => this.prolongateLink.isVisible().should.eventually.to.be.equal(
                        true,
                        'У заказа есть ссылка'
                    )
                );
            },
        }),
        'Успешное продление даты доставки.': makeCase({
            // ПВЗ 'marketfront-5064'
            // Постамат 'marketfront-5065'
            id: type === 'PICKUP' ? 'marketfront-5064' : 'marketfront-5065',
            issue: 'MARKETFRONT-51587',

            async test() {
                await this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDERS);
                await this.allure.runStep(
                    'Проверяем наличие ссылки на странице "Мои заказы"',
                    () => this.prolongateLink.isVisible().should.eventually.to.be.equal(
                        false,
                        'У заказа не должно быть ссылки'
                    )
                );

                await this.browser.yaOpenPage(this.params.pageId, {
                    orderId: ORDER_ID,
                });

                await this.prolongateLink.click();
                await this.allure.runStep('Попап открылся', () => {
                    this.prolongationSuggestion.waitForVisible(1000, true);
                });

                await this.prolongationSuggestion.submitClick();
                try {
                    await this.preloader.waitForVisible(100, false);
                    await this.preloader.waitForVisible(1000, true);
                } catch (e) {
                    // бывает, что это работает так быстро, что один из шагов падает ошибкой.
                }

                await this.prolongationSuggestion.title.getText().should.eventually.be.equal(
                    `Срок хранения продлён до ${dayjs(
                        type === 'PICKUP'
                            ? LAST_DATE_TO_PROLONGATE_PICKUP
                            : LAST_DATE_TO_PROLONGATE_POST_TERM
                    ).format('D MMMM')}`
                );
                await this.prolongationSuggestion.exitClick();
                await this.prolongationSuggestion.waitForVisible(100, true);
            },
        }),

    };
}

export default common(additionalCases);
