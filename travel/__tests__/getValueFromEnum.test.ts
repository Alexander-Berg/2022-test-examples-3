import getValueFromEnum from '../getValueFromEnum';

enum StringEnum {
    first = 'first',
    second = 'second',
}

enum DigitEnum {
    first,
    second,
}

describe('getValueFromEnum', () => {
    it('Значение принадлежит enum', () => {
        expect(getValueFromEnum('first', StringEnum)).toBe(StringEnum.first);
        expect(getValueFromEnum('second', StringEnum)).toBe(StringEnum.second);
    });

    it('Значение не принадлежит enum', () => {
        expect(getValueFromEnum('three', StringEnum)).toBeUndefined();

        expect(getValueFromEnum('first', DigitEnum)).toBeUndefined();
        expect(getValueFromEnum('second', DigitEnum)).toBeUndefined();
        expect(getValueFromEnum('three', DigitEnum)).toBeUndefined();
    });
});
