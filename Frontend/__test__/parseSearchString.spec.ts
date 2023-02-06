import { parseSearchString } from '../parseSearchString';

describe('Функция parseSearchString', () => {
    it.each([
        ['', {}],
        ['?loginas=123', { loginas: '123' }],
        ['?loginas=123&adc=321', { loginas: '123', adc: '321' }],
        ['a=1&b=2', { a: '1', b: '2' }],
        ['=&a=2', { a: '2' }],
    ])('должна правильно разбирать строку запроса', (string, expectedResult) => {
        expect(parseSearchString(string)).toEqual(expectedResult);
    });
});
