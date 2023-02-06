/*eslint-disable @typescript-eslint/no-magic-numbers*/
describe('formatNumber', function () {
    const OLD_ENV = process.env;
    let format;

    beforeEach(() => {
        jest.resetModules(); // Most important - it clears the cache
        process.env = { ...OLD_ENV }; // Make a copy
    });

    afterAll(() => {
        process.env = OLD_ENV; // Restore old environment
    });

    describe('ru', () => {
        beforeEach(() => {
            process.env.LANG = 'ru';
            format = require('shared/helpers/formatNumber/formatNumber').formatNumber;
        });

        it('should return delimit 9000', function () {
            expect(format(9000)).toMatchInlineSnapshot(`"9 000"`);
        });

        it("shouldn't modify 900", function () {
            expect(format(900)).toMatchInlineSnapshot(`"900"`);
        });

        it('should delimit 5689900', function () {
            expect(format(5689900)).toMatchInlineSnapshot(`"5 689 900"`);
        });

        it('should delimit minus 5689900', function () {
            expect(format(-5689900)).toMatchInlineSnapshot(`"-5 689 900"`);
        });

        it('should delimit 3200500.1001', function () {
            expect(format(3200500.1001)).toMatchInlineSnapshot(`"3 200 500,1"`);
        });

        it('should left only 2 fraction digits', function () {
            expect(format(1.234, { maximumFractionDigits: 2 })).toMatchInlineSnapshot(`"1,23"`);
        });

        it('should round fraction digits', function () {
            expect(
                format(7.899, {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                }),
            ).toMatchInlineSnapshot(`"7,90"`);
        });

        it('should left 2 zeros as fraction digits', function () {
            expect(format(42, { minimumFractionDigits: 2 })).toMatchInlineSnapshot(`"42,00"`);
        });

        it('should leave 2 zeros as fraction digits when second argument is supplied', function () {
            expect(format(0, { minimumFractionDigits: 2 })).toMatchInlineSnapshot(`"0,00"`);
        });

        it('should work with null value', function () {
            expect(format(0)).toMatchInlineSnapshot(`"0"`);
        });

        it('should hide zero fraction digits', function () {
            expect(format(7.0)).toMatchInlineSnapshot(`"7"`);
        });

        it('should show zero fraction digits', function () {
            expect(format(7.0, { minimumFractionDigits: 2 })).toMatchInlineSnapshot(`"7,00"`);
        });
    });

    describe('en', () => {
        beforeEach(() => {
            process.env.LANG = 'en';
            format = require('shared/helpers/formatNumber/formatNumber').formatNumber;
        });

        it('should return delimit 9000', function () {
            expect(format(9000)).toMatchInlineSnapshot(`"9,000"`);
        });

        it("shouldn't modify 900", function () {
            expect(format(900)).toMatchInlineSnapshot(`"900"`);
        });

        it('should delimit 5689900', function () {
            expect(format(5689900)).toMatchInlineSnapshot(`"5,689,900"`);
        });

        it('should delimit minus 5689900', function () {
            expect(format(-5689900)).toMatchInlineSnapshot(`"-5,689,900"`);
        });

        it('should delimit 3200500.1001', function () {
            expect(format(3200500.1001)).toMatchInlineSnapshot(`"3,200,500.1"`);
        });

        it('should left only 2 fraction digits', function () {
            expect(format(1.234, { maximumFractionDigits: 2 })).toMatchInlineSnapshot(`"1.23"`);
        });

        it('should round fraction digits', function () {
            expect(
                format(7.899, {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                }),
            ).toMatchInlineSnapshot(`"7.90"`);
        });

        it('should left 2 zeros as fraction digits', function () {
            expect(format(42, { minimumFractionDigits: 2 })).toMatchInlineSnapshot(`"42.00"`);
        });

        it('should leave 2 zeros as fraction digits when second argument is supplied', function () {
            expect(format(0, { minimumFractionDigits: 2 })).toMatchInlineSnapshot(`"0.00"`);
        });

        it('should work with null value', function () {
            expect(format(0)).toMatchInlineSnapshot(`"0"`);
        });

        it('should hide zero fraction digits', function () {
            expect(format(7.0)).toMatchInlineSnapshot(`"7"`);
        });

        it('should show zero fraction digits', function () {
            expect(format(7.0, { minimumFractionDigits: 2 })).toMatchInlineSnapshot(`"7.00"`);
        });
    });
});
