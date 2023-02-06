/*eslint-disable no-magic-numbers*/
import { shallow } from 'enzyme';
import * as React from 'react';

import { EMPTY_DATA, ONE_HOUR, ONE_SECOND } from '../../constants';
import FormatDate, {
    dateLag,
    durationBetween,
    FormatDateInString,
    getAge,
    isEqualDate,
    numberToTimeFormat,
    prettySeconds,
    reverseDate,
    stringToScheduleTime,
    timeDurationNow,
} from './index';

const testDates = [
    new Date('2018-02-25T18:00:00').getTime(),
    new Date('2018-02-25T17:59:50').getTime(),
    new Date('2018-08-25T17:59:01').getTime(),
];

const testDatesSecondIndex = 2;

describe('FormatDate', () => {
    it('default', () => {
        expect(shallow(<FormatDate value={testDates[0]}/>).text()).toEqual('25 ФЕВ 2018, 18:00');
        expect(shallow(<FormatDate value={testDates[1]}/>).text()).toEqual('25 ФЕВ 2018, 17:59');
        expect(shallow(<FormatDate value={testDates[testDatesSecondIndex]}/>).text()).toEqual('25 АВГ 2018, 17:59');
    });

    it('FormatDateInString', () => {
        expect(FormatDateInString({ value: testDates[0] })).toEqual('25 ФЕВ 2018, 18:00');
        expect(FormatDateInString({ value: testDates[1] })).toEqual('25 ФЕВ 2018, 17:59');
        expect(FormatDateInString({ value: testDates[testDatesSecondIndex] })).toEqual('25 АВГ 2018, 17:59');
    });

    it('dateLag', () => {
        expect(dateLag(testDates[0], testDates[1])).toEqual('-10.0 cек.');
        expect(dateLag(testDates[0], testDates[testDatesSecondIndex])).toEqual('181.0 дн.');
    });

    it('reverseDate', () => {
        expect(reverseDate('28.11.2019')).toEqual('11.28.2019');
        expect(reverseDate('02.14.2019')).toEqual('14.02.2019');
    });

    it('isEqualDate', () => {
        expect(isEqualDate([testDates[0] / ONE_SECOND, testDates[1]])).toEqual(false);
        expect(isEqualDate([testDates[testDatesSecondIndex] / ONE_SECOND, testDates[testDatesSecondIndex]]))
            .toEqual(true);
    });

    it('timeDurationNow', () => {
        expect(timeDurationNow(Date.now() / ONE_SECOND)).toEqual('00:00:00');
        expect(timeDurationNow(Date.now() / ONE_SECOND - ONE_HOUR / ONE_SECOND)).toEqual('01:00:00');
    });

    it('durationBetween', () => {
        expect(durationBetween([testDates[0] / ONE_SECOND, testDates[1] / ONE_SECOND])).toEqual('00:00:10');
        expect(durationBetween([testDates[testDatesSecondIndex] / ONE_SECOND, testDates[1] / ONE_SECOND]))
            .toEqual('180дн. 23:59:11');
    });

    it('getAge', () => {
        expect(getAge('1996-11-19', new Date('2019-07-05'))).toEqual('22.62');
    });

    it('stringToScheduleTime', () => {
        expect(stringToScheduleTime('1000')).toEqual('10:00');
        expect(stringToScheduleTime('100')).toEqual('01:00');
    });

    it('prettySeconds', () => {
        expect(prettySeconds(0)).toEqual('0.0 cек.');
        expect(prettySeconds(1.5)).toEqual('1.5 cек.');
        expect(prettySeconds(2)).toEqual('2.0 cек.');
        expect(prettySeconds(2 * 60)).toEqual('2.0 мин.');
        expect(prettySeconds(2 * 60 * 60)).toEqual('2.0 час.');
        expect(prettySeconds(2 * 60 * 60 * 24)).toEqual('2.0 дн.');
    });

    it('numberToTimeFormat', () => {
        expect(numberToTimeFormat(0)).toEqual('00:00');
        expect(numberToTimeFormat(5)).toEqual('05:00');
        expect(numberToTimeFormat(10)).toEqual('10:00');
        expect(numberToTimeFormat(24)).toEqual(EMPTY_DATA);
        expect(numberToTimeFormat(100)).toEqual(EMPTY_DATA);
        expect(numberToTimeFormat(-1)).toEqual(EMPTY_DATA);
        expect(numberToTimeFormat(NaN)).toEqual(EMPTY_DATA);
    });
});
