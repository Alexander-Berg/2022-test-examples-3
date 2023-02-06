var vm = require('vm'),
    path = require('path'),
    readFileSync = require('fs').readFileSync,
    should = require('should'),
    dirName = path.resolve(__dirname, '../../Format'),
    Format = require(path.join(dirName, 'Format')).Format;

// Загружаем BEM.I18N core
vm.runInThisContext(readFileSync(path.join(__dirname, '../bemI18nForTests.js')));

// Переводы
vm.runInThisContext(readFileSync(path.join(dirName, 'Format.i18n/ru.js')));
vm.runInThisContext(readFileSync(path.join(dirName, 'Format.i18n/en.js')));
vm.runInThisContext(readFileSync(path.join(dirName, 'Format.i18n/tr.js')));

describe('Currency', function() {
    describe('Custom format', function() {
        var formatCurrency;

        beforeEach(function() {
            formatCurrency = Format.currency;
        });

        it('should work for small numbers', function() {
            formatCurrency(123).should.equal('123.00');
            formatCurrency(123.4).should.equal('123.40');
            formatCurrency(123.45).should.equal('123.45');
            formatCurrency(12345.67).should.equal('12 345.67');
        });

        it('should use format placeholder', function() {
            formatCurrency(123, null, null, null, '$%v').should.equal('$123.00');
            formatCurrency(123.4, null, null, null, '$%v').should.equal('$123.40');
            formatCurrency(123.45, null, null, null, '$%v').should.equal('$123.45');
            formatCurrency(12345.67, null, null, null, '$%v').should.equal('$12 345.67');
        });

        it('should replace symbol in format', function() {
            formatCurrency(123, null, null, null, '%v %s', 'руб.').should.equal('123.00 руб.');
            formatCurrency(123.4, null, null, null, '%v %s', 'руб.').should.equal('123.40 руб.');
            formatCurrency(123.45, null, null, null, '%v %s', 'руб.').should.equal('123.45 руб.');
            formatCurrency(12345.67, null, null, null, '%v %s', 'руб.').should.equal('12 345.67 руб.');
        });
    });

    describe('Params', function() {
        var localeParams;

        beforeEach(function() {
            localeParams = Format.localeParams();
        });

        it('should return currency format settings', function() {
            should.exist(localeParams.currency);
        });

        it('should have param settings', function() {
            var params = localeParams.currency;

            should.exist(params.format, 'format');

            should.exist(params.decimal, 'decimal');
            should.exist(params.thousand, 'thousand');
            should.exist(params.precision, 'precision');
            should.exist(params.grouping, 'grouping');
        });
    });
});
