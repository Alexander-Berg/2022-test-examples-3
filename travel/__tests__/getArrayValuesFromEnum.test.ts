import getArrayValuesFromEnum from '../getArrayValuesFromEnum';

enum StringEnum {
    first = 'first',
    second = 'second',
}

enum DigitEnum {
    first,
    second,
}

describe('getArrayValuesFromEnum', () => {
    it('Значения принадлежит enum', () => {
        expect(getArrayValuesFromEnum(['first'], StringEnum)).toEqual([
            StringEnum.first,
        ]);
        expect(getArrayValuesFromEnum(['second'], StringEnum)).toEqual([
            StringEnum.second,
        ]);
        expect(getArrayValuesFromEnum(['second', 'first'], StringEnum)).toEqual(
            [StringEnum.second, StringEnum.first],
        );
    });

    it('Значения не принадлежит enum', () => {
        expect(getArrayValuesFromEnum(['three'], StringEnum)).toEqual([]);

        expect(getArrayValuesFromEnum(['first'], DigitEnum)).toEqual([]);
        expect(getArrayValuesFromEnum(['second'], DigitEnum)).toEqual([]);
        expect(getArrayValuesFromEnum(['three'], DigitEnum)).toEqual([]);
    });

    it('Значения частично принадлежит enum', () => {
        expect(getArrayValuesFromEnum(['three', 'second'], StringEnum)).toEqual(
            [StringEnum.second],
        );
    });
});
