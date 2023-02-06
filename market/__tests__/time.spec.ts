import moment from 'moment';

import {Time} from '../time';

describe('time', () => {
    const d1 = moment('2018-09-20T11:15:10.779Z');

    describe('ru locale', () => {
        it('should localize dates', () => {
            const result = d1
                .clone()
                .utc()
                .locale('ru')
                .format('LLLL');

            expect(result).toEqual('четверг, 20 сентября 2018 г., 11:15');
        });
    });

    describe('Default Time Zone', () => {
        it('should use MSK by default', () => {
            expect(Time.simpleFormat(d1)).toEqual('20.09.2018 14:15');
        });
    });

    describe('humanDuration', () => {
        it('should return human duration', () => {
            expect(Time.humanDuration('PT25H1M1S')).toEqual('1 день 1 час 1 минута 1 секунда');
            expect(Time.humanDuration('PT23H59M59S')).toEqual('23 часа 59 минут 59 секунд');
            expect(Time.humanDuration('PT123H59M59S')).toEqual('5 дней 3 часа 59 минут 59 секунд');
            expect(Time.humanDuration('PT123H59M59S')).toEqual('5 дней 3 часа 59 минут 59 секунд');
        });
    });
});
