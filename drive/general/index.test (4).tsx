import { ONE_SECOND } from '../../../constants';
import { matchWelcomeTime, stringHasNumber } from '../utils';
import moment = require('moment/moment');

const BORDER_VALUE = 4;
const mockedData = [
    {
        ignore_time: 'true',
        since: BORDER_VALUE,
        until: '11',
        welcome: 'Доброе утро',
    },
    {
        ignore_time: 'true',
        since: '23',
        until: BORDER_VALUE,
        welcome: 'Доброй ночи',
    }];

describe('matchWelcomeTime', () => {
    it('define correct Time', () => {
        const morningTimeNum = 8;
        const timeNow = Math.round(+moment().set('hour', morningTimeNum) / ONE_SECOND);
        const expectedResult = mockedData[0]?.welcome;
        expect(matchWelcomeTime(mockedData, timeNow)).toEqual(expectedResult);
    });

    it('correct define border value', () => {
        const timeNow = Math.round(+moment().set('hour', BORDER_VALUE) / ONE_SECOND);
        const expectedResult = mockedData[0]?.welcome;
        expect(matchWelcomeTime(mockedData, timeNow)).toEqual(expectedResult);
    });

    it('correct work without variants', () => {
        const timeNum = 15;
        const timeNow = Math.round(+moment().set('hour', timeNum) / ONE_SECOND);
        expect(matchWelcomeTime(mockedData, timeNow)).toEqual('');
    });

    it('correct work with night hours', () => {
        const timeNum = 23;
        const timeNow = Math.round(+moment().set('hour', timeNum) / ONE_SECOND);
        expect(matchWelcomeTime(mockedData, timeNow)).toEqual(mockedData[1].welcome);
    });

    it('correct work with night hours 2', () => {
        const timeNum = 2;
        const timeNow = Math.round(+moment().set('hour', timeNum) / ONE_SECOND);
        expect(matchWelcomeTime(mockedData, timeNow)).toEqual(mockedData[1].welcome);
    });

    it('correct work with midnight', () => {
        const timeNum = 0;
        const timeNow = Math.round(+moment().set('hour', timeNum) / ONE_SECOND);
        expect(matchWelcomeTime(mockedData, timeNow)).toEqual(mockedData[1].welcome);
    });
});

describe('stringHasNumber function', () => {
    it('correct work with plus', () => {
        const validString = '+79955556677';
        expect(stringHasNumber(validString)).toBeTruthy();
    });

    it('correct work with another text', () => {
        const validString = 'blabla+79955556677bla';
        expect(stringHasNumber(validString)).toBeTruthy();
    });

    it('correct work without plus', () => {
        const validString = '79955556677';
        expect(stringHasNumber(validString)).toBeTruthy();
    });

    it('correct numbers count', () => {
        const notValidString = '+799555566';
        expect(stringHasNumber(notValidString)).toBeFalsy();
    });

    it('check number validation', () => {
        const notValidString = '+bla799555566';
        expect(stringHasNumber(notValidString)).toBeFalsy();
    });
});
