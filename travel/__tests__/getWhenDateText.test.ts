import {ROBOT} from '../../../../lib/date/formats';

import StationDateSpecialValue from '../../../../interfaces/date/StationDateSpecialValue';
import StationType from '../../../../interfaces/state/station/StationType';
import StationEventList from '../../../../interfaces/state/station/StationEventList';
import DateRobot from '../../../../interfaces/date/DateRobot';

import getWhenDateText from '../../getWhenDateText';

import moment from 'moment';

moment.locale('ru');

const whenDate = moment('2020-07-27T17:33:43+05:00').format(ROBOT) as DateRobot;

describe('getWhenDateText', () => {
    it('Вернет null, если это не страница аэропорта или жд', () => {
        expect(getWhenDateText(StationType.bus, StationEventList.arrival)).toBe(
            null,
        );
    });

    it('Вернет null, если нет whenSpecial и whenDate', () => {
        expect(getWhenDateText(StationType.bus, StationEventList.arrival)).toBe(
            null,
        );
    });

    it('Вернет строку null для страницы аэропорта прибытие поиск на все дни', () => {
        expect(
            getWhenDateText(
                StationType.plane,
                StationEventList.arrival,
                whenDate,
                StationDateSpecialValue.allDays,
            ),
        ).toBe(null);
    });

    it('Вернет строку null для страницы аэропорта отправление поиск на все дни', () => {
        expect(
            getWhenDateText(
                StationType.plane,
                StationEventList.departure,
                whenDate,
                StationDateSpecialValue.allDays,
            ),
        ).toBe(null);
    });

    it('Вернет строку "27 июля, понедельник" для страницы аэропорта прбытие поиск на конкретный день', () => {
        expect(
            getWhenDateText(
                StationType.plane,
                StationEventList.arrival,
                whenDate,
            ),
        ).toBe('27 июля, понедельник');
    });

    it('Вернет строку "27 июля, понедельник" для страницы аэропорта отправление поиск на конкретный день', () => {
        expect(
            getWhenDateText(
                StationType.plane,
                StationEventList.departure,
                whenDate,
            ),
        ).toBe('27 июля, понедельник');
    });

    it('Вернет строку null для страницы аэропорта прибытие поиск на все дни, с мобильной версии', () => {
        expect(
            getWhenDateText(
                StationType.plane,
                StationEventList.arrival,
                whenDate,
                StationDateSpecialValue.allDays,
                true,
            ),
        ).toBe(null);
    });

    it('Вернет строку null для страницы аэропорта отправление поиск на все дни, с мобильной версии', () => {
        expect(
            getWhenDateText(
                StationType.plane,
                StationEventList.departure,
                whenDate,
                StationDateSpecialValue.allDays,
                true,
            ),
        ).toBe(null);
    });

    it('Вернет строку "Прилет 27 июля, понедельник" для страницы аэропорта прибытие поиск на дату, с мобильной версии', () => {
        expect(
            getWhenDateText(
                StationType.plane,
                StationEventList.arrival,
                whenDate,
                undefined,
                true,
            ),
        ).toBe('Прилет 27 июля, понедельник');
    });

    it('Вернет строку "Вылет 27 июля, понедельник" для страницы аэропорта отправление поиск на дату, с мобильной версии', () => {
        expect(
            getWhenDateText(
                StationType.plane,
                StationEventList.departure,
                whenDate,
                undefined,
                true,
            ),
        ).toBe('Вылет 27 июля, понедельник');
    });

    it('Вернет строку null для страницы жд прибытие поиск на все дни', () => {
        expect(
            getWhenDateText(
                StationType.railroad,
                StationEventList.arrival,
                whenDate,
                StationDateSpecialValue.allDays,
            ),
        ).toBe(null);
    });

    it('Вернет строку null для страницы жд отправление поиск на все дни', () => {
        expect(
            getWhenDateText(
                StationType.railroad,
                StationEventList.departure,
                whenDate,
                StationDateSpecialValue.allDays,
            ),
        ).toBe(null);
    });

    it('Вернет строку "27 июля, понедельник" для страницы жд прбытие поиск на конкретный день', () => {
        expect(
            getWhenDateText(
                StationType.railroad,
                StationEventList.arrival,
                whenDate,
            ),
        ).toBe('27 июля, понедельник');
    });

    it('Вернет строку "27 июля, понедельник" для страницы жд отправление поиск на конкретный день', () => {
        expect(
            getWhenDateText(
                StationType.railroad,
                StationEventList.departure,
                whenDate,
            ),
        ).toBe('27 июля, понедельник');
    });

    it('Вернет строку null для страницы жд прибытие поиск на все дни, с мобильной версии', () => {
        expect(
            getWhenDateText(
                StationType.railroad,
                StationEventList.arrival,
                whenDate,
                StationDateSpecialValue.allDays,
                true,
            ),
        ).toBe(null);
    });

    it('Вернет строку null для страницы жд отправление поиск на все дни, с мобильной версии', () => {
        expect(
            getWhenDateText(
                StationType.railroad,
                StationEventList.departure,
                whenDate,
                StationDateSpecialValue.allDays,
                true,
            ),
        ).toBe(null);
    });

    it('Вернет строку "Прибытие 27 июля, понедельник" для страницы жд прибытие поиск на дату, с мобильной версии', () => {
        expect(
            getWhenDateText(
                StationType.railroad,
                StationEventList.arrival,
                whenDate,
                undefined,
                true,
            ),
        ).toBe('Прибытие 27 июля, понедельник');
    });

    it('Вернет строку "Отправление 27 июля, понедельник" для страницы жд отправление поиск на дату, с мобильной версии', () => {
        expect(
            getWhenDateText(
                StationType.railroad,
                StationEventList.departure,
                whenDate,
                undefined,
                true,
            ),
        ).toBe('Отправление 27 июля, понедельник');
    });
});
