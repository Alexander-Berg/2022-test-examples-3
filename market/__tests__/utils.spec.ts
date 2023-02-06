import {
    getCorrectTimeValue,
    getPositiveNumberValue,
    parseTimeValueToTimeData,
    setInTimeData,
    formatTimeDataToTimeValue,
} from '../utils';
import {DEFAULT_TIME_DATA} from '../constants';
import {TimeData} from '../types';

/** =========================== Тестовые данные =========================== */

const timeDataWithZeroFields: TimeData = {
    hours: 0,
    minutes: 0,
    seconds: 0,
};

const textTimeData1: TimeData = {
    hours: 10,
    minutes: 20,
    seconds: 30,
};

const textTimeData2: TimeData = {
    hours: 23,
    minutes: 59,
    seconds: 59,
};

/** ======================================================================= */

describe('Тестирование вспомогательных утилит', () => {
    describe('getCorrectTimeValue', () => {
        it('При передаче не строкового значения возвращает null', () => {
            expect(getCorrectTimeValue(undefined)).toEqual(null);
            expect(getCorrectTimeValue(null)).toEqual(null);
            expect(getCorrectTimeValue(1)).toEqual(null);
            expect(getCorrectTimeValue(true)).toEqual(null);
            expect(getCorrectTimeValue({})).toEqual(null);
        });

        it('При передаче строки в формате HH:mm:ss возвращает ее без изменений', () => {
            expect(getCorrectTimeValue('23:59:59')).toEqual('23:59:59');
            expect(getCorrectTimeValue('00:00:00')).toEqual('00:00:00');
            expect(getCorrectTimeValue('12:13:14')).toEqual('12:13:14');
        });

        it('При передаче строки в формате HH:mm возвращаем строку с добавленными к ней секунды', () => {
            expect(getCorrectTimeValue('23:59')).toEqual('23:59:00');
            expect(getCorrectTimeValue('00:00')).toEqual('00:00:00');
            expect(getCorrectTimeValue('10:59')).toEqual('10:59:00');
        });

        it('При передаче строки не формате HH:mm:ss или HH:mm возвращает null', () => {
            expect(getCorrectTimeValue('test')).toEqual(null);
            expect(getCorrectTimeValue('25:00:00')).toEqual(null);
            expect(getCorrectTimeValue('00:70:00')).toEqual(null);
            expect(getCorrectTimeValue('00:00:90')).toEqual(null);
            expect(getCorrectTimeValue('00:00:000')).toEqual(null);
        });
    });

    describe('getPositiveNumberValue', () => {
        it('При передаче не строк или чисел возращает 0', () => {
            expect(getPositiveNumberValue(null)).toEqual(0);
            expect(getPositiveNumberValue(false)).toEqual(0);
            expect(getPositiveNumberValue({})).toEqual(0);
            expect(getPositiveNumberValue(undefined)).toEqual(0);
            expect(getPositiveNumberValue(NaN)).toEqual(0);
        });

        it('При передаче отрицательных чисел возвращает 0', () => {
            expect(getPositiveNumberValue(-1)).toEqual(0);
            expect(getPositiveNumberValue(-65243)).toEqual(0);
        });

        it('При передаче числа возвращает его без изменений', () => {
            expect(getPositiveNumberValue(59)).toEqual(59);
            expect(getPositiveNumberValue(23)).toEqual(23);
        });

        it('При передаче неккоректной строки возвращает 0', () => {
            expect(getPositiveNumberValue('')).toEqual(0);
            expect(getPositiveNumberValue('hello')).toEqual(0);
        });

        it('При передаче строковых полей из паттерна HH:mm:ss отрабатывает корректно', () => {
            expect(getPositiveNumberValue('00')).toEqual(0);
            expect(getPositiveNumberValue('09')).toEqual(9);
            expect(getPositiveNumberValue('54')).toEqual(54);
        });
    });

    describe('parseTimeValue', () => {
        it('При передаче не строкового значения возвращает данные о времени со значением полей null', () => {
            expect(parseTimeValueToTimeData(undefined)).toEqual(DEFAULT_TIME_DATA);
            expect(parseTimeValueToTimeData(null)).toEqual(DEFAULT_TIME_DATA);
            expect(parseTimeValueToTimeData(1)).toEqual(DEFAULT_TIME_DATA);
            expect(parseTimeValueToTimeData(true)).toEqual(DEFAULT_TIME_DATA);
            expect(parseTimeValueToTimeData({})).toEqual(DEFAULT_TIME_DATA);
        });

        it('При передаче строки не формате HH:mm:ss или HH:mm возвращает данные о времени со значением полей null', () => {
            expect(parseTimeValueToTimeData('test')).toEqual(DEFAULT_TIME_DATA);
            expect(parseTimeValueToTimeData('25:00:00')).toEqual(DEFAULT_TIME_DATA);
            expect(parseTimeValueToTimeData('00:70:00')).toEqual(DEFAULT_TIME_DATA);
            expect(parseTimeValueToTimeData('00:00:90')).toEqual(DEFAULT_TIME_DATA);
            expect(parseTimeValueToTimeData('00:00:000')).toEqual(DEFAULT_TIME_DATA);
        });

        it('При передаче строки в формате HH:mm:ss возвращает правильные данные времени', () => {
            const expected1 = {hours: 23, minutes: 59, seconds: 59};
            const expected2 = {hours: 0, minutes: 0, seconds: 0};
            const expected3 = {hours: 12, minutes: 13, seconds: 14};

            expect(parseTimeValueToTimeData('23:59:59')).toEqual(expected1);
            expect(parseTimeValueToTimeData('00:00:00')).toEqual(expected2);
            expect(parseTimeValueToTimeData('12:13:14')).toEqual(expected3);
        });

        it('При передаче строки в формате HH:mm возвращает правильные данные времени', () => {
            const expected1 = {hours: 23, minutes: 59, seconds: 0};
            const expected2 = {hours: 0, minutes: 0, seconds: 0};
            const expected3 = {hours: 10, minutes: 32, seconds: 0};

            expect(parseTimeValueToTimeData('23:59')).toEqual(expected1);
            expect(parseTimeValueToTimeData('00:00')).toEqual(expected2);
            expect(parseTimeValueToTimeData('10:32')).toEqual(expected3);
        });
    });

    describe('setInTimeData', () => {
        it('При передаче null в качестве значения устанавливает его в поле', () => {
            expect(setInTimeData(textTimeData1, 'hours', null)).toHaveProperty('hours', null);
            expect(setInTimeData(textTimeData1, 'minutes', null)).toHaveProperty('minutes', null);
            expect(setInTimeData(textTimeData1, 'seconds', null)).toHaveProperty('seconds', null);
        });

        it('При передаче неккоректного значения для установки устанавливет 0 в поле', () => {
            expect(setInTimeData(textTimeData1, 'hours', undefined)).toHaveProperty('hours', 0);
            expect(setInTimeData(textTimeData1, 'seconds', NaN)).toHaveProperty('seconds', 0);
        });

        it('При передаче корректного строкового значения устанавливает в нужное поле', () => {
            expect(setInTimeData(textTimeData1, 'hours', '09')).toHaveProperty('hours', 9);
            expect(setInTimeData(textTimeData1, 'minutes', '32')).toHaveProperty('minutes', 32);
            expect(setInTimeData(textTimeData1, 'seconds', '02')).toHaveProperty('seconds', 2);
        });

        it('При передаче корректного числового значения устанавливает в нужное поле', () => {
            expect(setInTimeData(textTimeData1, 'hours', 6)).toHaveProperty('hours', 6);
            expect(setInTimeData(textTimeData1, 'minutes', 59)).toHaveProperty('minutes', 59);
            expect(setInTimeData(textTimeData1, 'seconds', 32)).toHaveProperty('seconds', 32);
        });

        it('При передаче корректного значения но большего чем максимально допустимое устанавливает максимально допустимое', () => {
            expect(setInTimeData(textTimeData1, 'hours', 25)).toHaveProperty('hours', 23);
            expect(setInTimeData(textTimeData1, 'minutes', 101)).toHaveProperty('minutes', 59);
            expect(setInTimeData(textTimeData1, 'seconds', 4291)).toHaveProperty('seconds', 59);
        });
    });

    describe('formatTimeDataToTimeValue', () => {
        it('Корректно отрабатывает наобъекте данных времени', () => {
            expect(formatTimeDataToTimeValue(timeDataWithZeroFields)).toEqual('00:00:00');
            expect(formatTimeDataToTimeValue(textTimeData1)).toEqual('10:20:30');
            expect(formatTimeDataToTimeValue(textTimeData2)).toEqual('23:59:59');
        });
    });
});
