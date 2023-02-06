const {convertRawExperimentValue} = require('../../experiments');

describe('convertRawExperimentValue', () => {
    describe('Boolean', () => {
        it('Вернёт false для 0', () => {
            expect(convertRawExperimentValue('0', Boolean)).toBe(false);
        });

        it('Вернёт true для числа отличного от 0', () => {
            expect(convertRawExperimentValue('1', Boolean)).toBe(true);
            expect(convertRawExperimentValue('5', Boolean)).toBe(true);
        });

        it('Вернёт false для нечисловых значений', () => {
            expect(convertRawExperimentValue('это всё ложь!', Boolean)).toBe(
                false,
            );
        });
    });

    describe('String', () => {
        it('Вернёт строковое значение', () => {
            expect(convertRawExperimentValue('test', String)).toBe('test');
            expect(convertRawExperimentValue('5.1', String)).toBe('5.1');
            expect(convertRawExperimentValue('true', String)).toBe('true');
        });
    });

    describe('Number', () => {
        it('Вернёт числовое значение', () => {
            expect(convertRawExperimentValue('100', Number)).toBe(100);
            expect(convertRawExperimentValue('100.13', Number)).toBe(100.13);
        });

        it('Вернёт NaN для некорректного значения', () => {
            expect(convertRawExperimentValue('соточка', Number)).toEqual(NaN);
        });
    });
});
