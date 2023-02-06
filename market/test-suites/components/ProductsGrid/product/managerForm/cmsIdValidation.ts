'use strict';

import {mergeSuites, makeSuite, makeCase} from 'ginny';

import openProductModalHook from '../hooks/openProductModal';

/**
 * Тест на редактирование ID страницы услуги
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 * @param {string} params.pageRouteName - ключ страницы
 * @param {number} params.vendor - ID вендора
 */
export default makeSuite('Бренд-зона. Редактирование услуги. Валидация ID страницы.', {
    feature: 'Управление услугами и пользователями',
    issue: 'VNDFRONT-4157',
    environment: 'kadavr',
    id: 'vendor_auto-942',
    params: {
        user: 'Пользователь',
        productName: 'Услуга',
    },
    story: mergeSuites(openProductModalHook(), {
        'При попытке сохранении ID страницы другого вендора': {
            'появляется сообщение об ошибке': makeCase({
                async test() {
                    this.setPageObjects({
                        tooltip() {
                            return this.createPageObject('PopupB2b');
                        },
                    });

                    const cmsPageId = '666';

                    await this.form.setFieldValueByName('cmsPageId', cmsPageId, 'ID страницы CMS');

                    await this.form.submit('Сохранить');

                    await this.browser.allure.runStep('Ожидаем появления хинта с ошибкой', () =>
                        this.tooltip.waitForPopupShown(),
                    );

                    await this.tooltip
                        .getActiveText()
                        .should.eventually.be.equal(
                            'Идентификатор этой бренд-зоны уже указан в кабинете другого производителя',
                            'Текст ошибки корректный',
                        );
                },
            }),
        },
    }),
});
