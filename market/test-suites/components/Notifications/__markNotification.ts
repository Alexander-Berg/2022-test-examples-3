'use strict';

import {makeCase, makeSuite} from 'ginny';

/**
 *
 * @param {PageObject.NotificationsListItem} item – элемент списка уведомлений
 * @param {PageObject.PagedList} list – список уведомлений
 * @param {PageObject.CheckboxB2b} checkbox – чекбокс уведомления
 * @param {PageObject.ButtonB2bNext} button – кнопка "Отметить как прочитанное"
 * @param {PageObject.TextB2b} text – текст "Выбрано 1 уведомление"
 */
export default makeSuite('Отметить уведомление как прочитанное', {
    story: {
        'При клике на чекбокс элемента списка': {
            'он отмечается': makeCase({
                async test() {
                    const expectedHeaderText = 'Выбрано 1 уведомление';
                    const expectedListHeaderButtonText = 'Отметить как прочитанное';

                    await this.allure.runStep('Проверяем отображение блока отметки уведомлений', () =>
                        this.header.mark.vndIsExisting().should.eventually.be.equal(false, 'Блок отсутствует'),
                    );

                    await this.browser.allure.runStep('Проверяем прочитанность уведомления', () =>
                        this.item.isRead().should.eventually.be.equal(false),
                    );

                    await this.checkbox.click();

                    await this.allure.runStep('Проверяем отображение блока отметки уведомлений', () =>
                        this.header.mark.vndIsExisting().should.eventually.be.equal(true, 'Блок присутствует'),
                    );

                    await this.browser.allure.runStep(
                        `В заголовке таблицы появились сообщение "${expectedHeaderText}"`,
                        () => this.text.root.getText().should.eventually.be.equal(expectedHeaderText),
                    );

                    await this.browser.allure.runStep(`Появилась кнопка "${expectedListHeaderButtonText}"`, () =>
                        this.button.root.getText().should.eventually.be.equal(expectedListHeaderButtonText),
                    );

                    await this.browser.allure.runStep(`Нажимаем на кнопку "${expectedListHeaderButtonText}"`, () =>
                        this.button.root.click(),
                    );

                    await this.allure.runStep('Блока отметки уведомлений пропал', () =>
                        this.browser.waitUntil(
                            () =>
                                this.header.mark
                                    .vndIsExisting()
                                    // @ts-expect-error(TS7006) найдено в рамках VNDFRONT-4580
                                    .then(isExisting => !isExisting),
                            this.browser.options.waitforTimeout,
                            'Блок отсутствует',
                        ),
                    );

                    await this.list.waitForLoading();

                    await this.browser.allure.runStep('Уведомление прочитано', () =>
                        this.item.isRead().should.eventually.be.equal(true),
                    );
                },
            }),
        },
    },
});
