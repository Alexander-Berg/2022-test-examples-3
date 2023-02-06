import { valueGreaterThan, valueLessThan, valueIn, valueNotIn } from '../../src/utils/operators';

describe('valueGreaterThan', () => {
    test('возвращает true, если значение больше заданного', () => {
        expect(valueGreaterThan(5, 3)).toBe(true);
    });

    test('возвращает false, если значение не больше заданного', () => {
        expect(valueGreaterThan(2, 2)).toBe(false);
    });
});

describe('valueLessThan', () => {
    test('возвращает true, если значение меньше заданного', () => {
        expect(valueLessThan(1, 4)).toBe(true);
    });

    test('возвращает false, если значение не меньше заданного', () => {
        expect(valueLessThan(2, 2)).toBe(false);
    });
});

describe('valueIn', () => {
    test('возвращает true, если передано одно значение и оно входит в массив допустимых значений', () => {
        expect(valueIn('ok', ['ok', 'approved'])).toBe(true);
    });

    test('возвращает false, если передано одно значение и оно не входит в массив допустимых значений', () => {
        expect(valueIn('not ok', ['ok', 'approved'])).toBe(false);
    });

    test('возвращает true, если передан массив значений и каждое из них входит в массив допустимых значений', () => {
        expect(valueIn(['ok', 'approved'], ['ok', 'approved', 'good'])).toBe(true);
    });

    test('возвращает false, если передан массив значений и не все из них входят в массив допустимых значений', () => {
        expect(valueIn(['ok', 'approved', 'not ok'], ['ok', 'approved', 'good'])).toBe(false);
    });
});

describe('valueNotIn', () => {
    test('возвращает true, если передано одно значение и оно не входит в массив недопустимых значений', () => {
        expect(valueNotIn('ok', ['not ok', 'not approved'])).toBe(true);
    });

    test('возвращает false, если передано одно значение и оно входит в массив недопустимых значений', () => {
        expect(valueNotIn('not ok', ['not ok', 'approved'])).toBe(false);
    });

    test('возвращает true, если передан массив значений и ни одно из них не входит в массив недопустимых значений', () => {
        expect(valueNotIn(['ok', 'approved'], ['not ok', 'not approved', 'not good'])).toBe(true);
    });

    test('возвращает false, если передан массив значений и какое-то из них входит в массив недопустимых значений', () => {
        expect(valueNotIn(['ok', 'approved', 'not ok'], ['not ok', 'good'])).toBe(false);
    });
});
