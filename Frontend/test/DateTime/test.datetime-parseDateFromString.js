var vm = require('vm');
var readFileSync = require('fs').readFileSync;
var path = require('path');
var dirName = path.resolve(__dirname, '../../DateTime');
var DateTime = require(path.join(dirName, 'DateTime')).DateTime;
var parseDateFromString = DateTime.internal.parseDateFromString;
var strftime = DateTime.strftime;

// Загружаем BEM.I18N core
vm.runInThisContext(readFileSync(path.join(__dirname, '../bemI18nForTests.js')));

// Переводы, ru и en
vm.runInThisContext(readFileSync(path.join(dirName, 'DateTime.i18n/ru.js')));

describe('parseDateFromString', function() {
    var LOCAL_OFFSET = (function() {
        var o = new Date().getTimezoneOffset();
        var abs = Math.abs(o);
        var H = String(parseInt(abs / 60, 10)).padStart(2, '0');
        var M = String(abs % 60).padStart(2, '0');

        return (o > 0 ? '-' : '+') + H + M;
    })();

    it('парсит дату из строки без таймзоны как для локальной таймзоны', function() {
        var date = parseDateFromString('2018-10-10T23:40:05');
        // date.getTime() разный в разных таймзонах
        date.getTime().should.equal(new Date('2018-10-10T23:40:05' + LOCAL_OFFSET).getTime(), '1/3 .getTime()');
        date.getTimezoneOffset().should.equal(0, '2/3 .getTimezoneOffset()');
        strftime('%F %T', date).should.equal('2018-10-10 23:40:05', '3/3 strftime(\'%F %T\')');
    });

    it('парсит дату из строки с локальной таймзоной', function() {
        var date = parseDateFromString('2018-10-10T23:40:05' + LOCAL_OFFSET);
        // date.getTime() разный в разных таймзонах
        date.getTime().should.equal(new Date('2018-10-10T23:40:05' + LOCAL_OFFSET).getTime(), '1/3 .getTime()');
        date.getTimezoneOffset().should.equal(new Date().getTimezoneOffset(), '2/3 .getTimezoneOffset()');
        strftime('%F %T', date).should.equal('2018-10-10 23:40:05', '3/3 strftime(\'%F %T\')');
    });

    it('парсит дату из строки с таймзоной -10', function() {
        // var d1 = new Date('2018-10-10T00:10:05-1000'); // в таймзоне +0300
        // d1.toISOString(); // '2018-10-10T10:10:05.000Z'
        // d1.toString();    // 'Wed Oct 10 2018 13:10:05 GMT+0300 (+03)', или '… (MSK)', '… (Moscow Standard Time)'
        // d1.getHours();    // 13
        // d1.getDate();     // 10
        var date = parseDateFromString('2018-10-10T23:40:05-1000');
        date.getTime().should.equal(new Date('2018-10-11T09:40:05.000Z').getTime(), '1/3 .getTime()');
        date.getTimezoneOffset().should.equal(600, '2/3 .getTimezoneOffset()');
        strftime('%F %T', date).should.equal('2018-10-10 23:40:05', '3/3 strftime(\'%F %T\')');
    });

    it('парсит дату из строки с таймзоной +10', function() {
        // var d2 = new Date('2018-10-10T00:10:05+1000'); // в таймзоне +0300
        // d2.toISOString() // '2018-10-09T14:10:05.000Z'
        // d2.toString()    // 'Tue Oct 09 2018 17:10:05 GMT+0300 (+03)', или '… (MSK)', '… (Moscow Standard Time)'
        // d2.getHours()    // 17
        // d2.getDate()     // 9
        var date = parseDateFromString('2018-10-10T00:10:05+1000');
        date.getTime().should.equal(new Date('2018-10-09T14:10:05.000Z').getTime(), '1/3 .getTime()');
        date.getTimezoneOffset().should.equal(-600, '2/3 .getTimezoneOffset()');
        strftime('%F %T', date).should.equal('2018-10-10 00:10:05', '3/3 strftime(\'%F %T\')');
    });
});
