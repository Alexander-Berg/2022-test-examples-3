import Point from '../../../interfaces/Point';

import getPointId from '../getPointId';

describe('getPointId', () => {
    it('Валидное значение. Вернет число', () => {
        expect(getPointId('s120' as Point)).toBe(120);
        expect(getPointId('c10' as Point)).toBe(10);
    });

    it('Не валидные значения. Вернет NaN', () => {
        expect(getPointId('10' as Point)).toBeNaN();
        expect(getPointId('0' as Point)).toBeNaN();
        expect(getPointId(' s120 ' as Point)).toBeNaN();
        expect(getPointId('y120' as Point)).toBeNaN();
        expect(getPointId('some10' as Point)).toBeNaN();
        expect(getPointId(' ' as Point)).toBeNaN();
        expect(getPointId('s 10' as Point)).toBeNaN();
    });
});
