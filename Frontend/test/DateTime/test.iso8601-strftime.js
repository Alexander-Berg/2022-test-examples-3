var vm = require('vm'),
    readFileSync = require('fs').readFileSync,
    path = require('path'),
    dirName = path.resolve(__dirname, '../../DateTime'),
    strftime = require(path.join(dirName, 'DateTime')).DateTime.strftime;

// Загружаем BEM.I18N core
vm.runInThisContext(readFileSync(path.join(__dirname, '../bemI18nForTests.js')));

// Переводы, ru и en
vm.runInThisContext(readFileSync(path.join(dirName, 'DateTime.i18n/ru.js')));

describe('strftime', function() {
    var DATE = '2014-03-04T12:12:12+0400';

    describe('ядро', function() {
        it('%a - сокращенное название дня недели', function() {
            strftime('%a', DATE).should.equal('вт');
        });

        it('%A - полное название дня недели', function() {
            strftime('%A', DATE).should.equal('вторник');
        });

        it('%b - аббревиатура названия месяца', function() {
            strftime('%b', DATE).should.equal('мар');
        });

        it('%B - полное название месяца в РП', function() {
            strftime('%B', DATE).should.equal('марта');
        });

        it('%~B - полное название месяца в ИП', function() {
            strftime('%~B', DATE).should.equal('март');
        });

        it('%c - предпочитаемое отображение даты и времени, "%Y-%m-%d %H:%M:%S', function() {
            strftime('%c', DATE).should.equal('2014-03-04 12:12:12');
        });

        it('%C - двухзначный порядковый номер столетия с ведущим нулем (год, деленный на 100, усеченный до целого)', function() {
            strftime('%C', DATE).should.equal('20');
        });

        it('%d - двухзначное представление дня месяца с ведущим нулем', function() {
            strftime('%d', DATE).should.equal('04');
        });

        it('%D - дата в формате MM/DD/YY', function() {
            strftime('%D', DATE).should.equal('03/04/14');
        });

        it('%e - день месяца, с ведущим пробелом, если он состоит из одной цифры', function() {
            strftime('%e', DATE).should.equal(' 4');
        });

        it('%F - дата в формате YYYY-MM-DD', function() {
            strftime('%F', DATE).should.equal('2014-03-04');
        });

        it('%g - двухзначный номер года в соответствии со стандартом ISO-8601:1988', function() {
            strftime('%g', DATE).should.equal('14');
        });

        it('%G - полная четырехзначная версия %g', function() {
            strftime('%G', DATE).should.equal('2014');
        });

        it('%h - аббревиатура названия месяца (псевдоним %b)', function() {
            strftime('%h', DATE).should.equal('мар');
        });

        it('%H - двухзначный номер часа в 24-часовом формате с ведущим нулем', function() {
            strftime('%H', '2014-03-04T05:12:12+0400').should.equal('05');
            strftime('%H', '2014-03-04T23:12:12+0400').should.equal('23');
        });

        it('%I - двухзначный номер часа в 12-часовом формате с ведущим нулем', function() {
            strftime('%I', '2014-03-04T05:12:12+0400').should.equal('05');
            strftime('%I', '2014-03-04T23:12:12+0400').should.equal('11');
        });

        it('%j - трехзначный номер дня в году с ведущими нулями', function() {
            strftime('%j', DATE).should.equal('063');
        });

        it('%k - двухзначное представление часа в 24-часовом формате, с пробелом перед одиночной цифрой, От " 0" до 23', function() {
            strftime('%k', '2014-03-04T05:12:12+0400').should.equal(' 5');
            strftime('%k', '2014-03-04T23:12:12+0400').should.equal('23');
        });

        it('%l - час в 12-часовом формате, с пробелом перед одиночной цифрой', function() {
            strftime('%l', '2014-03-04T05:12:12+0400').should.equal(' 5');
            strftime('%l', '2014-03-04T23:12:12+0400').should.equal('11');
        });

        it('%m - двухзначный порядковый номер месяца с ведущим нулем', function() {
            strftime('%m', '2014-05-12T12:12:12+0400').should.equal('05');
            strftime('%m', '2014-12-12T12:12:12+0400').should.equal('12');
        });

        it('%M - двухзначный номер минуты с ведущим нулем', function() {
            strftime('%M', '2014-12-03T12:05:12+0400').should.equal('05');
            strftime('%M', '2014-12-03T12:12:12+0400').should.equal('12');
        });

        it('%n - перенос строки', function() {
            strftime('%n').should.equal('\n');
        });

        it('%p - "AM" или "PM" в верхнем регистре, в зависимости от указанного времени и локали', function() {
            strftime('%p', DATE).should.equal('PM');
        });

        it('%P - "am" или "pm" в нижнем регистре, в зависимости от указанного времени и локали', function() {
            strftime('%P', DATE).should.equal('pm');
        });

        it('%r - время в 12 часовом формате - 02:55:02 PM', function() {
            strftime('%r', '2014-12-03T02:55:02+0400').should.equal('02:55:02 AM');
            strftime('%r', '2014-12-03T12:06:02+0400').should.equal('12:06:02 PM');
            strftime('%r', '2014-12-03T20:06:02+0400').should.equal('08:06:02 PM');
        });

        it('%R - время в 24 часовом формате HH:MM', function() {
            strftime('%R', '2014-12-03T01:06:12+0400').should.equal('01:06');
            strftime('%R', '2014-12-03T20:06:12+0400').should.equal('20:06');
        });

        // http://www.onlineconversion.com/unix_time.htm
        it('%s - метка времени Эпохи Unix', function() {
            // 2014-03-04T12:12:12+0400 === Tue, 04 Mar 2014 08:12:12 GMT
            // Tue, 04 Mar 2014 08:12:12 GMT === 1393920732
            strftime('%s', '2014-03-04T12:12:12+0400').should.equal('1393920732');

            // 2014-03-04T12:12:12+0500 === Tue, 04 Mar 2014 07:12:12 GMT
            // Tue, 04 Mar 2014 07:12:12 GMT === 1393917132
            strftime('%s', '2014-03-04T12:12:12+0500').should.equal('1393917132');
        });

        it('%S - двухзначный номер секунды с ведущим нулем', function() {
            strftime('%S', '2014-03-04T12:12:06+0400').should.equal('06');
        });

        it('%t - табуляция', function() {
            strftime('%t').should.equal('\t');
        });

        it('%T - ISO 8601 формат времени HH:MM:SS', function() {
            strftime('%T', '2014-03-04T08:06:06+0400').should.equal('08:06:06');
            strftime('%T', '2014-03-04T20:06:06+0400').should.equal('20:06:06');
        });

        it('%u - порядковый номер дня недели согласно стандарту ISO-8601 (с 1 - пн. по 7 - вс.)', function() {
            strftime('%u', DATE).should.equal('2');
        });

        // http://strftime.onlinephpfunctions.com/
        it('%U - порядковый номер недели в указанном году, начиная с первого воскресенья в качестве первой недели', function() {
            strftime('%U', DATE).should.equal('09');
        });

        // http://strftime.onlinephpfunctions.com/
        it('%V - порядковый номер недели в указанном году в соответствии со стандартом ISO-8601:1988', function() {
            strftime('%V', DATE).should.equal('10');
        });

        it('%w - день недели, с вс - 0', function() {
            strftime('%w', DATE).should.equal('2');
        });

        it('%W - порядковый номер недели в указанном году, начиная с первого понедельника в качестве первой недели', function() {
            strftime('%W', DATE).should.equal('09');
        });

        it('%x - предпочитаемое отображение даты, без времени "%m/%d/%y"', function() {
            strftime('%x', DATE).should.equal('03/04/14');
        });

        it('%X - предпочитаемое отображение времени "%H:%M:%S"', function() {
            strftime('%X', DATE).should.equal('12:12:12');
        });

        it('%y - последние 2 цифры года с ведущим нулем', function() {
            strftime('%y', '2001-03-04T12:12:12+0400').should.equal('01');
            strftime('%y', '2014-03-04T12:12:12+0400').should.equal('14');
        });

        it('%Y - год', function() {
            strftime('%Y', DATE).should.equal('2014');
        });

        it('%z - смещение временной зоны относительно UTC', function() {
            strftime('%z', '2014-03-04T12:12:12+0300').should.equal('+0300');
            strftime('%z', '2014-03-04T12:12:12+0345').should.equal('+0345');

            strftime('%z', '2014-03-04T12:12:12-0500').should.equal('-0500');
            strftime('%z', '2014-03-04T12:12:12-0545').should.equal('-0545');
        });

        it('%Z - аббревиатура временной зоны относительно UTC', function() {
            // В идеале http://www.timeanddate.com/time/zones/ - строки из букв,
            // но иногда бывает и просто '+03'
            // результат зависит от временной зоны на машине, выполняющей тесты
            strftime('%Z', '2014-03-04T12:12:12+0400').should.match(/^(([A-Z]+)|(\+\d\d))$/);
        });

        it('%%', function() {
            strftime('%%', DATE).should.equal('%');
        });
    });

    describe('модификаторы', function() {
        var digitFlags = ['C', 'd', 'e', 'g', 'H', 'I', 'j', 'm', 'M', 'S', 'V', 'W', 'y', 'l'],
            letterFlags = ['a', 'A', 'b', 'h', 'B'],
            date = '0709-02-01T06:06:06+0400';

        it('"0" модификатор должен подставлять ведущий ноль для флагов %C, %d, %e, %g, %H, %I, %j, %m, %M, %S, %V, %W, %y, %l', function() {
            var data = digitFlags.map(function(flag) {
                return strftime('%0' + flag, date);
            });

            data.should.eql(['07', '01', '01', '09', '06', '06', '032', '02', '06', '06', '05', '05', '09', '06']);
        });

        it('"_" модификатор должен подставлять ведущий пробел для флагов %C, %d, %e, %g, %H, %I, %j, %m, %M, %S, %V, %W, %y, %l', function() {
            var data = digitFlags.map(function(flag) {
                return strftime('%_' + flag, date);
            });

            data.should.eql([' 7', ' 1', ' 1', ' 9', ' 6', ' 6', ' 32', ' 2', ' 6', ' 6', ' 5', ' 5', ' 9', ' 6']);
        });

        it('"-" модификатор должен удалять ведущий пробел или ноль для флагов %C, %d, %e, %g, %H, %I, %j, %m, %M, %S, %V, %W, %y, %l', function() {
            var data = digitFlags.map(function(flag) {
                return strftime('%-' + flag, date);
            });

            data.should.eql(['7', '1', '1', '9', '6', '6', '32', '2', '6', '6', '5', '5', '9', '6']);
        });

        it('"^" модификатор должен переводить значения флагов %a, %A, %b, %B, %h в верхний регистр', function() {
            var data = letterFlags.map(function(flag) {
                return strftime('%^' + flag, date);
            });

            data.should.eql(['ПН', 'ПОНЕДЕЛЬНИК', 'ФЕВ', 'ФЕВ', 'ФЕВРАЛЯ']);
        });

        it('"#" модификатор должен переводить значения флагов %a, %A, %b, %B, %h в нижний регистр', function() {
            var data = letterFlags.map(function(flag) {
                return strftime('%#' + flag, date);
            });

            data.should.eql(['пн', 'понедельник', 'фев', 'фев', 'февраля']);
        });

        it('"~" модификатор должен переводить значения флага %B в ИП', function() {
            strftime('%~B', date).should.equal('февраль');
        });
    });

    describe('комбинация', function() {
        it('%-e %B в %H:%M', function() {
            strftime('%-e %B в %H:%M', DATE).should.equal('4 марта в 12:12');
        });

        it('%e %B %Y', function() {
            strftime('%e %B %Y', DATE).should.equal(' 4 марта 2014');
        });

        it('%~B %Y', function() {
            strftime('%~B %Y', DATE).should.equal('март 2014');
        });
    });
});
