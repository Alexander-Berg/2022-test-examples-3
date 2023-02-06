'use strict';

import {makeCase, makeSuite, mergeSuites, importSuite} from 'ginny';

import showProductHook from './hooks/showProduct';

/**
 * Тесты катоффы в подсказке услуги
 * @param {PageObject.Products} products - список услуг
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 * @param {string[]} params.cutoffList - тексты с описанием катоффов в модальном окне
 */
export default makeSuite('Катоффы в подсказке услуги.', {
    issue: 'VNDFRONT-3807',
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
                    hint() {
                        return this.createPageObject('ClickableLevitan', this.product.placement);
                    },
                    icon() {
                        return this.createPageObject('IconLevitan', this.hint);
                    },
                    modal() {
                        return this.createPageObject('Modal');
                    },
                    closeButton() {
                        return this.createPageObject('ButtonLevitan', this.modal.footer);
                    },
                    closeIcon() {
                        return this.createPageObject('IconB2b', this.modal);
                    },
                });

                return this.browser.allure.runStep('Ожидаем появления значка подсказки', () =>
                    this.icon.waitForExist(),
                );
            },
        },
        importSuite('Hint', {
            suiteName: 'Хинт с подсказкой при наличии менеджерского катоффа.',
            meta: {
                id: 'vendor_auto-31',
                environment: 'kadavr',
            },
            params: {
                text: 'Менеджер Маркета отключил услугу. Свяжитесь с менеджером или службой поддержки.',
            },
        }),
        importSuite('Hint', {
            suiteName: 'Хинт с подсказкой при наличии катоффа по документам.',
            meta: {
                id: 'vendor_auto-518',
                environment: 'kadavr',
            },
            params: {
                text: 'Закончился срок действия документа на товарный знак. Обновите подтверждающие документы в Настройках.',
            },
        }),
        {
            'При клике на значок': {
                'открывается модальное окно с описанием катоффов': makeCase({
                    id: 'vendor_auto-517',
                    async test() {
                        await this.icon.click();

                        await this.browser.allure.runStep('Ожидаем появления модального окна', () =>
                            this.modal.waitForVisible(),
                        );

                        await this.modal.content
                            .getText()
                            .should.eventually.be.equal(
                                this.params.cutoffList.join('\n'),
                                'Текст подсказки корректный',
                            );

                        await this.browser.allure.runStep('Нажимаем на кнопку [Понятно]', () =>
                            this.closeButton.click(),
                        );

                        await this.modal.waitForHidden();

                        await this.icon.click();

                        await this.browser.allure.runStep('Ожидаем появления модального окна', () =>
                            this.modal.waitForVisible(),
                        );

                        await this.browser.allure.runStep('Закрываем модалку по крестику в заголовке', () =>
                            this.closeIcon.click(),
                        );

                        return this.modal.waitForHidden();
                    },
                }),
            },
        },
    ),
});
