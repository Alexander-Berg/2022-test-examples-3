import { getDuration, cropFirstChars } from '../fromVideoObject';

describe('Функция getDuration', () => {
    it('должна возвращать ожидаемый результат', () => {
        expect(getDuration(1234)).toEqual('00:20:34');
        expect(getDuration(11223)).toEqual('03:07:03');
        expect(getDuration(24 * 60 * 60 - 1)).toEqual('23:59:59');
        expect(getDuration(24 * 60 * 60)).toEqual('24:00:00');
        expect(getDuration(172801)).toEqual('48:00:01');
    });
});

describe('Функция cropFirstChars', () => {
    it('должна возвращать ожидаемый результат', () => {
        expect(cropFirstChars('00:01:17')).toEqual('1:17');
        expect(cropFirstChars('00:00:17')).toEqual('0:17');
        expect(cropFirstChars('00:11:17')).toEqual('11:17');
        expect(cropFirstChars('11:11:17')).toEqual('11:11:17');
        expect(cropFirstChars('00:00:01')).toEqual('0:01');
        expect(cropFirstChars('')).toEqual('');
    });
});
