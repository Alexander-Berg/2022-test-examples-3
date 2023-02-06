import { getDuration } from '../entity';

describe('Функция getDuration', () => {
    it('должна возвращать ожидаемый результат', () => {
        expect(getDuration(1234)).toEqual('20 мин');
        expect(getDuration(11223)).toEqual('3 ч 7 мин');
        expect(getDuration(undefined)).toEqual(undefined);
        expect(getDuration(Infinity)).toEqual(undefined);
    });
});
