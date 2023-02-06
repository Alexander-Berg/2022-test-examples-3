'use strict';

import url from 'url';

import moment from 'moment';

import buildUrl from 'spec/lib/helpers/buildUrl';
// @ts-expect-error(TS2306) найдено в рамках VNDFRONT-4532
import ROUTE_NAMES from 'app/constants/routeNames';

/**
 * Хук открытия страницы уведомления через список уведомлений
 *
 * @param {Object} params
 * @param {number} params.vendor - id текущего вендора
 * @param {string} params.statusValue - значение статуса для выбора в фильтре по статусу
 * @param {string} params.productValue - значение услуги для выбора в фильтре по услуге
 * @param {object} params.periodValue - значение начала и конца периода (moment) для выбора в фильтре по периоду
 * @param {number} params.notificationId - id уведомления, на которое будем переходить
 * @param {boolean} [params.withFiltersCheck] - признак, что в тесте требуется изменить значение фильтров
 * @param {string} [params.expectedPeriodValue] - ожидаемое значение периода при возврате на список уведомлений
 * @param {PageObject.DatePicker} datePicker - фильтр по периоду
 * @param {PageObject.PopupB2b} popup - активный попап
 * @param {PageObject.PagedList} list - список уведомлений
 * @param {PageObject.SelectB2b} productSelect - фильтр по услугам
 * @param {PageObject.SelectB2b} statusSelect - фильтр по статусу
 * @param {PageObject.Bell} bell - колокольчик с уведомлениями
 */
export default () => ({
    async beforeEach() {
        const {
            vendor,
            statusValue,
            productValue,
            periodValue,
            notificationId,
            withFiltersCheck,
            expectedPeriodValue,
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
        } = this.params;

        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
        this.params.backPageUrl = buildUrl(ROUTE_NAMES.NOTIFICATIONS, {
            vendor,
        });

        const pageUrl = buildUrl(ROUTE_NAMES.NOTIFICATION, {
            vendor,
            notificationId,
        });
        const parsedUrl = url.parse(pageUrl, true, true);
        const prevMonth = moment().startOf('month').subtract(1, 'months');

        if (withFiltersCheck) {
            if (expectedPeriodValue === undefined) {
                const DATE_FORMAT = 'DD.MM.YY';

                const formattedValue = periodValue.format(DATE_FORMAT);

                const prevMonthFormattedValue = prevMonth.format(DATE_FORMAT);

                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.params.expectedPeriodValue = `${prevMonthFormattedValue} - ${formattedValue}`;
            }

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.allure.runStep('Изменяем фильтры у списка уведомлений', async () => {
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                await this.allure.runStep('Задаём период', async () => {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.datePicker.open();

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.popup.waitForPopupShown();

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.browser.vndWaitForChangeUrl(async () => {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        await this.datePicker.selectDate(periodValue);

                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        await this.datePicker.selectPrevMonth();

                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        await this.datePicker.selectDate(prevMonth);
                    }, true);

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.datePicker.innerToggler.click();
                });

                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                await this.allure.runStep('Задаём услугу', async () => {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.productSelect.click();

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.popup.waitForPopupShown();

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.productSelect.selectItem(productValue);

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.productSelect.click();
                });

                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                await this.allure.runStep('Задаём статус', async () => {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.statusSelect.click();

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.popup.waitForPopupShown();

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.statusSelect.selectItem(statusValue);

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.statusSelect.click();
                });
            });

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.list.waitForLoading();
        }

        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
        await this.allure.runStep('Переходим на страницу одного уведомления', () =>
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            this.browser
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                .vndWaitForChangeUrl(() => this.list.getItemByIndex(0).click())
                .should.eventually.be.link(parsedUrl, {
                    skipProtocol: true,
                    skipHostname: true,
                }),
        );
    },
});
