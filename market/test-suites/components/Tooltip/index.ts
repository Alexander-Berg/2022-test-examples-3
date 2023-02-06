'use strict';

import {PageObject, makeSuite, makeCase} from 'ginny';

const DropdownB2bNext = PageObject.get('DropdownB2bNext');

/**
 * Тесты на подсказку по наведению курсора
 * @param {PageObject} targetElement - элемент, на который требуется навести курсор
 * @param {Object} params
 * @param {string} params.text - текст подсказки
 */
export default makeSuite('Тултип.', {
    environment: 'testing',
    params: {
        user: 'Пользователь',
    },
    story: {
        beforeEach() {
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            return this.browser.allure.runStep('Ожидаем появления элемента', () =>
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.targetElement.waitForExist(),
            );
        },
        'При наведении на элемент': {
            'появляется тултип с текстом': makeCase({
                async test() {
                    this.setPageObjects({
                        tooltip() {
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            return this.createPageObject('DropdownB2bNext', this.browser, DropdownB2bNext.active);
                        },
                    });

                    await this.targetElement.root.vndHoverToElement();

                    await this.browser.allure.runStep('Ожидаем появления подсказки', () =>
                        this.tooltip.waitForVisible(),
                    );

                    await this.tooltip
                        .getActiveText()
                        .should.eventually.be.equal(this.params.text, 'Текст подсказки корректный');
                },
            }),
        },
    },
});
