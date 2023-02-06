/* eslint-disable @typescript-eslint/no-non-null-assertion */
import FormatterFactory from '../../../../src/parsers/core/formatter-factory';

describe('formatters', () => {
    const unknownType = FormatterFactory.getFormatterByType('unknownType')!;

    const int = FormatterFactory.getFormatterByType('int')!;
    const price = FormatterFactory.getFormatterByType('price')!;
    const location = FormatterFactory.getFormatterByType('location')!;
    const string = FormatterFactory.getFormatterByType('string')!;
    const year = FormatterFactory.getFormatterByType('year')!;
    const date = FormatterFactory.getFormatterByType('date')!;
    const currency = FormatterFactory.getFormatterByType('currency')!;

    describe('formatters-factory', () => {
        it('should get formatter by type', () => {
            expect(unknownType).toEqual(null);

            expect(int).not.toEqual(null);
            expect(int).not.toEqual(undefined);

            expect(price).not.toEqual(null);
            expect(price).not.toEqual(undefined);

            expect(location).not.toEqual(null);
            expect(location).not.toEqual(undefined);

            expect(string).not.toEqual(null);
            expect(string).not.toEqual(undefined);

            expect(year).not.toEqual(null);
            expect(year).not.toEqual(undefined);

            expect(date).not.toEqual(null);
            expect(date).not.toEqual(undefined);

            expect(currency).not.toEqual(null);
            expect(currency).not.toEqual(undefined);
        });
    });

    describe('regex-formatter', () => {
        it('should correct format integer to string', () => {
            expect(int.formatFunction('-290')).toEqual('-290');
            expect(int.formatFunction('19999')).toEqual('19999');

            expect(int.formatFunction('not a integer')).toEqual(null);
            expect(int.formatFunction(parseInt('not a integer', 10))).toEqual(null);
            expect(int.formatFunction({})).toEqual(null);
        });

        it('should correct format with preformatter', () => {
            expect(price.formatFunction('250 руб.')).toEqual('250');
            expect(price.formatFunction('- 100 руб.')).toEqual('100');
        });

        it('should correct format string', () => {
            expect(string.formatFunction(420)).toEqual(null);
            expect(string.formatFunction('I am string')).toEqual('I am string');
        });
    });

    describe('date-formatter', () => {
        it('should correct format date', () => {
            const formatObj = {
                re: /([\d]?[\d]?[\d]{2})-([\d]{1,2})-([\d]{1,2})/,
                year: 1,
                month: 2,
                day: 3
            };

            expect(date.formatFunction('2017-12-12', formatObj)).toEqual('2017-12-12');
            expect(date.formatFunction('17-1-1', formatObj)).toEqual('2017-01-01');
            expect(date.formatFunction('---', formatObj)).toEqual(null);
        });
    });
});
