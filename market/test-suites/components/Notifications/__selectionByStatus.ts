'use strict';

import {makeCase, makeSuite} from 'ginny';

import CheckboxB2b from 'spec/page-objects/CheckboxB2b';

const checkedCheckboxSelector = `${CheckboxB2b.root} > ${CheckboxB2b.icon}`;

/**
 * Тест на переход на страницу уведомления
 * @param {PageObject.NotificationsListHeader} header – шапка таблицы со списком
 * @param {PageObject.TextB2b} text – текст блока отметки уведомлений
 * @param {PageObject.SelectB2b} select – меню выбора статуса
 * @param {PageObject.PopupB2b} popup – попап меню выбора статуса
 * @param {PageObject.PagedList} list – список уведомлений
 * @param {Object} params
 * @param {string} params.optionTitle - заголовок выбираемого элемента меню
 * @param {string} params.expectedText - ожидаемый текст блока отметки уведомлений
 */
export default makeSuite('Выбор по статусу.', {
    environment: 'kadavr',
    story: {
        'При выборе статуса': {
            'маркируются соответствующие элементы списка': makeCase({
                id: 'vendor_auto-786',
                issue: 'VNDFRONT-2411',
                async test() {
                    const {selectOptionTitle, resetOptionTitle, initialCount, selectedText, selectedCount} =
                        this.params;

                    await this.allure.runStep('Проверяем отображение блока отметки уведомлений', () =>
                        this.header.mark.vndIsExisting().should.eventually.be.equal(false, 'Блок отсутствует'),
                    );
                    await this.allure.runStep('Проверяем количество отмеченных элементов', () =>
                        this.list
                            .getItemElementsBySelector(checkedCheckboxSelector)
                            // @ts-expect-error(TS7031) найдено в рамках VNDFRONT-4580
                            .then(({value}) => value.length)
                            .should.eventually.be.equal(initialCount, `Отмечено "${initialCount}" элементов`),
                    );

                    await this.select.click();

                    await this.popup.waitForPopupShown();

                    await this.select.selectItem(selectOptionTitle);

                    await this.allure.runStep('Проверяем отображение блока отметки уведомлений', () =>
                        this.header.mark.vndIsExisting().should.eventually.be.equal(true, 'Блок присутствует'),
                    );
                    await this.allure.runStep('Получаем текст блока отметки уведомлений', () =>
                        this.text.root
                            .getText()
                            .should.eventually.be.equal(selectedText, `Текст соответствует "${selectedText}"`),
                    );
                    await this.allure.runStep('Проверяем количество отмеченных элементов', () =>
                        this.list
                            .getItemElementsBySelector(checkedCheckboxSelector)
                            // @ts-expect-error(TS7031) найдено в рамках VNDFRONT-4580
                            .then(({value}) => value.length)
                            .should.eventually.be.equal(selectedCount, `Отмечено "${selectedCount}" элементов`),
                    );

                    await this.select.click();

                    await this.popup.waitForPopupShown();

                    await this.select.selectItem(resetOptionTitle);

                    await this.allure.runStep('Проверяем количество отмеченных элементов', () =>
                        this.list
                            .getItemElementsBySelector(checkedCheckboxSelector)
                            // @ts-expect-error(TS7031) найдено в рамках VNDFRONT-4580
                            .then(({value}) => value.length)
                            .should.eventually.be.equal(initialCount, `Отмечено "${initialCount}" элементов`),
                    );
                },
            }),
        },
    },
});
