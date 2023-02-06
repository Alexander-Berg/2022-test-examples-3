'use strict';

import {makeCase, makeSuite} from 'ginny';
import moment from 'moment';

/**
 * Тест на валидацию корректности периода проведения кампании
 * @param {PageObject.DatePicker} datePicker - календарь
 * @param {Object} params
 * @param {string} params.label – текстовое название поля
 */
export default makeSuite('Валидация корректности периода проведения кампании.', {
    params: {
        user: 'Пользователь',
    },
    story: {
        beforeEach() {
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            return this.datePicker
                .isExisting()
                .should.eventually.be.equal(true, 'Кнопка открытия календаря отображается');
        },
        'При выборе периода с датами начала и конца в разных месяцах': {
            'появляется валидационное сообщение': {
                'о необходимости выбрать дату окончания в том же месяце, что и дата начала': makeCase({
                    async test() {
                        await this.datePicker.open();

                        await this.datePicker.selectDate(moment());
                        await this.datePicker.selectDate(moment().add(1, 'months'));

                        const calendarLabel = 'Период проведения';
                        const endOfMonth = moment().endOf('month').format('DD MMMM');

                        await this.form
                            .getFieldErrorMessageByLabelText(this.params.label)
                            .should.eventually.be.equal(
                                `Выберите дату окончания не позже ${endOfMonth}`,
                                `Текст ошибки у поля "${calendarLabel}" корректный`,
                            );
                    },
                }),
            },
        },
    },
});
