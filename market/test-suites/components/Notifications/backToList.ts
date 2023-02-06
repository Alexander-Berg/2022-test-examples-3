'use strict';

import url from 'url';

import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на возврат к указанной странице со страницы конкретного уведомления
 *
 * @param {PageObject} clickable – кликабельный элемент возврата на указанную страницу
 * @param {PageObject.DatePicker} datePicker - фильтр по периоду
 * @param {PageObject.SelectB2b} productSelect - фильтр по услугам
 * @param {PageObject.SelectB2b} statusSelect - фильтр по статусу
 * @param {Object} params
 * @param {string} params.backPageUrl - ссылка на страницу, на которую хотим вернуться
 * @param {string} params.elementCaption - текст кликабельного элемента возврата на указанную страницу
 * @param {boolean} [params.withFiltersCheck] - признак, что в тесте требуется изменить значение фильтров
 * @param {string} [params.expectedPeriodValue] - ожидаемое значение периода при возврате на список уведомлений
 * @param {string} [params.expectedProductValue] - ожидаемое значение услуги при возврате на список уведомлений
 * @param {string} [params.expectedStatusValue] - ожидаемое значение статуса при возврате на список уведомлений
 */
export default makeSuite('Возврат к указанной странице.', {
    story: {
        'При клике на элемент': {
            'осуществляется возврат на корректную страницу': makeCase({
                async test() {
                    const {
                        backPageUrl,
                        elementCaption,
                        withFiltersCheck,
                        expectedPeriodValue,
                        expectedProductValue,
                        expectedStatusValue,
                    } = this.params;

                    const parsedBackUrl = url.parse(backPageUrl, true, true);

                    await this.clickable
                        .isExisting()
                        .should.eventually.be.equal(true, `Элемент «${elementCaption}» отображается`);

                    await this.clickable
                        .getText()
                        .should.eventually.be.equal(elementCaption, 'У элемента корректный текст');

                    await this.allure.runStep(`Кликаем по элементу «${elementCaption}»`, () =>
                        this.browser
                            .vndWaitForChangeUrl(() => this.clickable.click())
                            .should.eventually.be.link(parsedBackUrl, {
                                skipProtocol: true,
                                skipHostname: true,
                            }),
                    );

                    if (withFiltersCheck) {
                        await this.allure.runStep('Проверяем состояние фильтров у списка уведомлений', async () => {
                            await this.datePicker.innerToggler
                                .getText()
                                .should.eventually.be.equal(expectedPeriodValue, 'Период не задан');

                            await this.productSelect
                                .getText()
                                .should.eventually.be.equal(
                                    expectedProductValue,
                                    'Текст в фильтре по услуге корректный',
                                );

                            await this.statusSelect
                                .getText()
                                .should.eventually.be.equal(
                                    expectedStatusValue,
                                    'Текст в фильтре по статусу корректный',
                                );
                        });
                    }
                },
            }),
        },
    },
});
