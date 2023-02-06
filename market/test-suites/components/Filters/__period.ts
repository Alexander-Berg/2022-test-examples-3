'use strict';

import {makeSuite, makeCase} from 'ginny';
import moment from 'moment';

/**
 * @param {PageObject.DatePicker} datePicker — компонент выбора диапазона дат
 * @param {PageObject.ListContainer} list - список элементов
 * @param {Object} params
 * @param {number} params.initialItemsCount - количесто без фильтрации
 * @param {number} params.filteredItemsCount - количество элементов после фильтрации
 * @param {string} params.fromParamName - имя устанавливаемого query-параметра начала периода
 * @param {string} params.fromParamValue - значение устанавливаемого query-параметра начала периода
 * @param {string} params.toParamName - имя устанавливаемого query-параметра конца периода
 * @param {string} params.toParamValue - значение устанавливаемого query-параметра конца периода
 * @param {string} params.pageRouteName - имя роута для перехода при фильтрации
 * @param {Object} params.routeParams - параметры роута
 */
export default makeSuite('Фильтр по периоду.', {
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При выборе дат': {
            'фильтрует список': makeCase({
                async test() {
                    const {
                        fromParamName,
                        fromParamValue,
                        toParamName,
                        toParamValue,
                        pageRouteName,
                        initialItemsCount,
                        filteredItemsCount,
                        routeParams,
                    } = this.params;

                    this.setPageObjects({
                        popup() {
                            return this.createPageObject('PopupB2b');
                        },
                    });

                    const currentMonth = moment();
                    const prevMonth = moment().startOf('month').subtract(1, 'months');

                    // Ожидаем загрузки списка
                    await this.list.waitForLoading();
                    await this.list
                        .getItemsCount()
                        .should.eventually.be.equal(initialItemsCount, `Отображается элементов: ${initialItemsCount}`);

                    // Открываем попап
                    await this.datePicker.open();

                    await this.browser.allure.runStep('Дожидаемся появления попапа', () =>
                        this.popup.waitForPopupShown(),
                    );

                    // Выбираем предыдущий месяц
                    await this.datePicker.selectPrevMonth();

                    // Выбираем диапазон дат с первого числа предыдущего месяца по текущий день
                    await this.browser
                        .vndWaitForChangeUrl(async () => {
                            await this.datePicker.selectDate(prevMonth);
                            await this.datePicker.selectDate(currentMonth);
                        }, true)
                        .should.eventually.be.link(
                            {
                                query: {
                                    [fromParamName]: prevMonth.format('YYYY-MM-DD'),
                                    [toParamName]: currentMonth.format('YYYY-MM-DD'),
                                },
                            },
                            {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                                skipPathname: true,
                            },
                        );

                    // Открываем страницу с предустановленными фильтрами по периоду
                    await this.browser.vndOpenPage(pageRouteName, {
                        ...routeParams,
                        [fromParamName]: fromParamValue,
                        [toParamName]: toParamValue,
                    });

                    // Ожидаем загрузки списка
                    await this.list.waitForLoading();
                    await this.list
                        .getItemsCount()
                        .should.eventually.be.equal(filteredItemsCount, 'Список отфильтрован');
                },
            }),
        },
    },
});
