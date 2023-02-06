import { numberFormatting } from '@yandex-turbo/core/FormattingNumber/FormattingNumber';

describe('Модуль форматирования числа', () => {
    it('100', () => {
        expect(numberFormatting(100)).toEqual('100');
    });

    it('1 000', () => {
        expect(numberFormatting(1000)).toEqual('1 000');
    });

    it('12,3 тыс. (12 300)', () => {
        expect(numberFormatting(12300)).toEqual('12,3 тыс.');
    });

    it('12 тыс. (12 000)', () => {
        expect(numberFormatting(12000)).toEqual('12 тыс.');
    });

    it('123 тыс. (123 000)', () => {
        expect(numberFormatting(123000)).toEqual('123 тыс.');
    });

    it('123 тыс. (123 456)', () => {
        expect(numberFormatting(123456)).toEqual('123 тыс.');
    });

    it('1,3 млн. (1 300 000)', () => {
        expect(numberFormatting(1300000)).toEqual('1,3 млн');
    });

    it('1 млн. (1 000 000)', () => {
        expect(numberFormatting(1000000)).toEqual('1 млн');
    });

    it('1,3 млн. (1 345 000)', () => {
        expect(numberFormatting(1345000)).toEqual('1,3 млн');
    });
});
