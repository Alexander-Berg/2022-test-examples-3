var vm = require('vm'),
    path = require('path'),
    should = require('should'),
    readFileSync = require('fs').readFileSync,

    assert = require('assert'),

    dirName = path.resolve(__dirname, '../../Format'),
    Format = require(path.join(dirName, 'Format')).Format;

// Загружаем BEM.I18N core
vm.runInThisContext(readFileSync(path.join(__dirname, '../bemI18nForTests.js')));

// Переводы
vm.runInThisContext(readFileSync(path.join(dirName, 'Format.i18n/ru.js')));
vm.runInThisContext(readFileSync(path.join(dirName, 'Format.i18n/en.js')));
vm.runInThisContext(readFileSync(path.join(dirName, 'Format.i18n/tr.js')));

describe('Number', function() {
    var formatNumber;

    describe('custom format', function() {
        beforeEach(function() {
            formatNumber = Format.number;
        });

        it('should enforce precision and round values', function() {
            formatNumber(123.456789, 0).should.equal('123');
            formatNumber(123.456789, 1).should.equal('123.5');
            formatNumber(123.456789, 2).should.equal('123.46');
            formatNumber(123.456789, 3).should.equal('123.457');
            formatNumber(123.456789, 4).should.equal('123.4568');
            formatNumber(123.456789, 5).should.equal('123.45679');
        });

        it('should fix floting point rounding error', function() {
            formatNumber(0.615, 2).should.equal('0.62');
            formatNumber(0.614, 2).should.equal('0.61');
        });

        it('should add leading zeros to decimals', function() {
            formatNumber(123, 3).should.equal('123.000');
            formatNumber(123.4, 3).should.equal('123.400');
            formatNumber(123.45, 3).should.equal('123.450');
            formatNumber(123.456, 3).should.equal('123.456');
            formatNumber(123.4567, 3).should.equal('123.457');
            formatNumber(123.45678, 3).should.equal('123.457');
        });

        it('should format large numbers', function() {
            formatNumber(123456.54321, 0).should.equal('123 457');
            formatNumber(123456.54321, 1).should.equal('123 456.5');
            formatNumber(123456.54321, 2).should.equal('123 456.54');
            formatNumber(123456.54321, 3).should.equal('123 456.543');
            formatNumber(123456.54321, 4).should.equal('123 456.5432');
            formatNumber(123456.54321, 5).should.equal('123 456.54321');

            formatNumber(12345678.12, 0).should.equal('12 345 678');
            formatNumber(12345678.12, 1).should.equal('12 345 678.1');
            formatNumber(12345678.12, 2).should.equal('12 345 678.12');
            formatNumber(12345678.12, 3).should.equal('12 345 678.120');
            formatNumber(12345678.12, 4).should.equal('12 345 678.1200');
        });

        it('should format negative numbers', function() {
            formatNumber(-123456.54321, 0).should.equal('-123 457');
            formatNumber(-123456.54321, 1).should.equal('-123 456.5');
            formatNumber(-123456.54321, 2).should.equal('-123 456.54');
            formatNumber(-123456.54321, 3).should.equal('-123 456.543');
            formatNumber(-123456.54321, 4).should.equal('-123 456.5432');
            formatNumber(-123456.54321, 5).should.equal('-123 456.54321');

            formatNumber(-12345678.12, 0).should.equal('-12 345 678');
            formatNumber(-12345678.12, 1).should.equal('-12 345 678.1');
            formatNumber(-12345678.12, 2).should.equal('-12 345 678.12');
            formatNumber(-12345678.12, 3).should.equal('-12 345 678.120');
            formatNumber(-12345678.12, 4).should.equal('-12 345 678.1200');
        });

        it('should allow setting thousands separator', function() {
            formatNumber(12345678.12, 0, '|').should.equal('12|345|678');
            formatNumber(12345678.12, 1, '>').should.equal('12>345>678.1');
            formatNumber(12345678.12, 2, '*').should.equal('12*345*678.12');
            formatNumber(12345678.12, 3, '/').should.equal('12/345/678.120');
        });

        it('should allow setting decimal separator', function() {
            formatNumber(12345678.12, 0, null, '|').should.equal('12 345 678');
            formatNumber(12345678.12, 1, null, '>').should.equal('12 345 678>1');
            formatNumber(12345678.12, 2, null, '*').should.equal('12 345 678*12');
            formatNumber(12345678.12, 3, null, '/').should.equal('12 345 678/120');
        });

        it('should allow setting thousand and decimal separators', function() {
            formatNumber(12345678.12, 0, '-', '|').should.equal('12-345-678');
            formatNumber(12345678.12, 1, '_', '>').should.equal('12_345_678>1');
            formatNumber(12345678.12, 2, '&', '*').should.equal('12&345&678*12');
            formatNumber(12345678.12, 3, '#', '/').should.equal('12#345#678/120');
        });

        it('should allow setting delimiters, decimal mark and format', function() {
            formatNumber(-12345678.12, 2, ' ', ',', '%s%b%d').should.equal('-12 345 678,12');
            formatNumber(-12345678.12, 2, ' ', ',', '%b%d').should.equal('12 345 678,12');
            formatNumber(-12345678.12, 2, ' ', ',', '%s').should.equal('-');
            formatNumber(-12345678.12, 2, ' ', ',', '%b').should.equal('12 345 678');
            formatNumber(12345678.12, 2, ' ', ',', '%d').should.equal(',12');
        });
    });

    describe('Russian format', function() {
        beforeEach(function() {
            BEM.I18N.lang('ru');

            formatNumber = Format.localeNumber;
        });

        it('should format with locale settings', function() {
            formatNumber(1234.1).should.equal('1 234,10');
            formatNumber(12345.1).should.equal('12 345,10');
            formatNumber(12345678.1).should.equal('12 345 678,10');
            formatNumber(12345678.12).should.equal('12 345 678,12');
        });
    });

    describe('English format', function() {
        beforeEach(function() {
            BEM.I18N.lang('en');
            formatNumber = Format.localeNumber;
        });

        it('should format with locale settings', function() {
            formatNumber(1234.1).should.equal('1,234.10');
            formatNumber(12345.1).should.equal('12,345.10');
            formatNumber(12345678.1).should.equal('12,345,678.10');
            formatNumber(12345678.12).should.equal('12,345,678.12');
        });
    });

    describe('Turkish format', function() {
        beforeEach(function() {
            BEM.I18N.lang('tr');
            formatNumber = Format.localeNumber;
        });

        it('should format with locale settings', function() {
            formatNumber(1234.1).should.equal('1.234,10');
            formatNumber(12345.1).should.equal('12.345,10');
            formatNumber(12345678.1).should.equal('12.345.678,10');
            formatNumber(12345678.12).should.equal('12.345.678,12');
        });
    });

    describe('Params', function() {
        var localeParams;

        beforeEach(function() {
            localeParams = Format.localeParams();
        });

        it('should return number format settings', function() {
            should.exist(localeParams.number);
        });

        it('should have param settings', function() {
            var params = localeParams.number;

            should.exist(params.decimal, 'decimal');
            should.exist(params.thousand, 'thousand');
            should.exist(params.precision, 'precision');
            should.exist(params.grouping, 'grouping');
        });
    });

    describe('short format', function() {
        var shortNumber,
            OVER_THOUSAND = 1367,
            OVER_MILLION = 1e7;

        beforeEach(function() {
            shortNumber = Format.shortNumber;
        });

        it('should separate numbers by coma', function() {
            assert(shortNumber(OVER_THOUSAND).indexOf(',') !== -1);
        });

        it('should left only 1 number after coma', function() {
            assert(/,[0-9]{1}/.test(shortNumber(12300)), 'It doesnt match right pattern');
        });

        it('shouldnt left coma and any number after it without precision', function() {
            assert(!/,0/.test(shortNumber(3e6)), 'it should be "3M", without comas');
        });

        it('should end with "M" for millions', function() {
            assert(/M$/.test(shortNumber(OVER_MILLION)), 'It doesnt end with "M"');
        });

        describe('params', function() {
            it('should separate numbers if separator is given in params', function() {
                assert(shortNumber(OVER_THOUSAND, { separator: '.' }).indexOf('.') !== -1);
            });

            it('should set number of digits after coma if it was given in params', function() {
                var formated = shortNumber(OVER_THOUSAND, { precision: 2 });

                assert(/,\d{2}/.test(formated));
            });
        });

        describe('translations', function() {
            var lang = BEM.I18N.lang.bind(BEM.I18N);

            describe('RU', function() {
                beforeEach(function() {
                    lang('ru');
                });

                it('for thousands should end with "K"', function() {
                    assert(/K$/.test(shortNumber(OVER_THOUSAND)), 'it doesnt end with K');
                });
            });

            describe('TR', function() {
                beforeEach(function() {
                    lang('tr');
                });

                it('for thousands should end with "B"', function() {
                    assert(/B$/.test(shortNumber(OVER_THOUSAND)), 'it doesnt end with B');
                });
            });

            describe('EN', function() {
                beforeEach(function() {
                    lang('en');
                });

                it('for thousands should end with "K"', function() {
                    assert(/K$/.test(shortNumber(OVER_THOUSAND)), 'it doesnt end with K');
                });
            });
        });
    });
});
