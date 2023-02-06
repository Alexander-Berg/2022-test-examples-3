'use strict';

import {PageObject, makeSuite, makeCase} from 'ginny';

const DropdownB2bNext = PageObject.get('DropdownB2bNext');

/**
 * Тесты на подсказку
 * @param {PageObject.Hint} hint - значок подсказки
 * @param {Object} params
 * @param {string} params.text - текст подсказки
 */
export default makeSuite('Подсказка.', {
    environment: 'testing',
    params: {
        user: 'Пользователь',
    },
    story: {
        beforeEach() {
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            this.setPageObjects({
                icon() {
                    return this.createPageObject('IconLevitan', this.hint);
                },
                dropdown() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('DropdownB2bNext', this.browser, DropdownB2bNext.active);
                },
            });

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            return this.browser.allure.runStep('Ожидаем появления значка подсказки', () =>
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.icon.waitForExist(),
            );
        },
        'При нажатии на значок': {
            'появляется хинт с текстом': makeCase({
                async test() {
                    await this.icon.click();

                    await this.browser.allure.runStep('Ожидаем появления подсказки', () =>
                        this.dropdown.waitForExist(),
                    );

                    await this.dropdown
                        .getActiveText()
                        .should.eventually.be.equal(this.params.text, 'Текст подсказки корректный');

                    await this.browser.allure.runStep('Нажимаем на подсказку', () => this.dropdown.click());

                    await this.browser.allure.runStep('Ожидаем скрытия подсказки', () =>
                        this.browser.waitUntil(
                            async () => {
                                const existing = await this.dropdown.isExisting();

                                return existing === false;
                            },
                            this.browser.options.waitforTimeout,
                            'Подсказка отображается',
                        ),
                    );
                },
            }),
        },
    },
});
