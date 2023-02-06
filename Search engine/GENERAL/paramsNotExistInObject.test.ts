import {paramsNotExistInObject} from './paramsNotExistInObject';

describe('paramsNotExistInObject', () => {
    test('should return expected result', () => {
        expect(paramsNotExistInObject({b: ''}, {a: ''})).toBe(true);
        expect(paramsNotExistInObject({b: ''}, {})).toBe(false);
        expect(paramsNotExistInObject({b: ''}, {b: 'asd'})).toBe(false);
        expect(paramsNotExistInObject({}, {b: ''})).toBe(true);
    });
});
