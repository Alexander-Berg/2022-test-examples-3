import {WHEN_SPECIAL_VALUE} from 'types/common/When';

import {isWhenSpecialValue} from '../index';

describe('isWhenSpecialValue(when)', () => {
    test('Должен вернуть true для today', () => {
        expect(isWhenSpecialValue(WHEN_SPECIAL_VALUE.TODAY)).toBe(true);
    });

    test('Должен вернуть true для tomorrow', () => {
        expect(isWhenSpecialValue(WHEN_SPECIAL_VALUE.TOMORROW)).toBe(true);
    });

    test('Должен вернуть true для all-days', () => {
        expect(isWhenSpecialValue(WHEN_SPECIAL_VALUE.ALL_DAYS)).toBe(true);
    });

    test('Должен вернуть false для произвольной строки', () => {
        expect(isWhenSpecialValue('not-valid')).toBe(false);
    });

    test('Должен вернуть false для произвольного типа', () => {
        expect(isWhenSpecialValue(new Date('2019-11-25'))).toBe(false);
    });
});
