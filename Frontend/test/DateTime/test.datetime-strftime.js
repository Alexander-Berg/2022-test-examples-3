var vm = require('vm'),
    readFileSync = require('fs').readFileSync,
    path = require('path'),
    dirName = path.resolve(__dirname, '../../DateTime'),
    DateTime = require(path.join(dirName, 'DateTime')).DateTime,
    strftime = DateTime.strftime,
    Benchmark = require('benchmark'),
    perlCDate = require('../perlCDate.js').perlCDate;

// Загружаем BEM.I18N core
vm.runInThisContext(readFileSync(path.join(__dirname, '../bemI18nForTests.js')));

// Переводы, ru и en
vm.runInThisContext(readFileSync(path.join(dirName, 'DateTime.i18n/ru.js')));

describe('strftime', function() {
    var DATE = new Date(2014, 2, 4, 12, 12, 12); // 04.03.2014 12:12:12
    var DATE2 = new Date(2014, 0, 1, 1, 2, 3);

    BEM.I18N.lang('ru');

    describe('ядро', function() {
        it('%a - сокращенное название дня недели', function() {
            strftime('%a', DATE).should.equal('вт');
        });

        it('%A - полное название дня недели', function() {
            strftime('%A', DATE).should.equal('вторник');
        });

        it('%b - аббревиатура названия месяца', function() {
            strftime('%b', DATE).should.equal('мар');

            var date = new Date(2014, 4, 4);
            strftime('%b', date).should.equal('мая');
        });

        it('%~b - аббревиатура названия месяца в ИП', function() {
            strftime('%~b', DATE).should.equal('мар');

            var date = new Date(2014, 4, 4);
            strftime('%~b', date).should.equal('май');
        });

        it('%B - полное название месяца в РП', function() {
            strftime('%B', DATE).should.equal('марта');
        });

        it('%~B - полное название месяца в ИП', function() {
            strftime('%~B', DATE).should.equal('март');
        });

        it('%c - предпочитаемое отображение даты и времени, "%Y-%m-%d %H:%M:%S', function() {
            strftime('%c', DATE).should.equal('2014-03-04 12:12:12');
            strftime('%c', DATE2).should.equal('2014-01-01 01:02:03');
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
            var date = new Date(DATE.valueOf());
            date.setHours(20);

            strftime('%H', date).should.equal('20');
        });

        it('%I - двухзначный номер часа в 12-часовом формате с ведущим нулем', function() {
            var date = new Date(DATE.valueOf());
            date.setHours(5);

            strftime('%I', date).should.equal('05');
        });

        it('%j - трехзначный номер дня в году с ведущими нулями', function() {
            strftime('%j', DATE).should.equal('063');
        });

        it('%k - двухзначное представление часа в 24-часовом формате, с пробелом перед одиночной цифрой, От " 0" до 23', function() {
            var date = new Date(DATE.valueOf());
            date.setHours(5);

            strftime('%k', date).should.equal(' 5');
        });

        it('%l - час в 12-часовом формате, с пробелом перед одиночной цифрой', function() {
            var date = new Date(DATE.valueOf());
            date.setHours(5);

            strftime('%l', date).should.equal(' 5');
        });

        it('%m - двухзначный порядковый номер месяца с ведущим нулем', function() {
            strftime('%m', DATE).should.equal('03');
        });

        it('%M - двухзначный номер минуты с ведущим нулем', function() {
            var date = new Date(DATE.valueOf());
            date.setMinutes(6);

            strftime('%M', date).should.equal('06');
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
            var date = new Date(DATE.valueOf());
            date.setMinutes(6);
            date.setSeconds(6);

            strftime('%r', date).should.equal('12:06:06 PM');
        });

        it('%R - время в 24 часовом формате HH:MM', function() {
            var date = new Date(DATE.valueOf());
            date.setHours(20);
            date.setMinutes(6);

            strftime('%R', date).should.equal('20:06');
        });

        // http://www.onlineconversion.com/unix_time.htm
        it('%s - метка времени Эпохи Unix', function() {
            var timestamp = parseInt(DATE.getTime() / 1000, 10);

            strftime('%s', DATE).should.equal(timestamp.toString());
        });

        it('%S - двухзначный номер секунды с ведущим нулем', function() {
            var date = new Date(DATE.valueOf());
            date.setSeconds(6);

            strftime('%S', date).should.equal('06');
        });

        it('%t - табуляция', function() {
            strftime('%t').should.equal('\t');
        });

        it('%T - ISO 8601 формат времени HH:MM:SS', function() {
            var date = new Date(DATE.valueOf());
            date.setHours(20);
            date.setMinutes(6);
            date.setSeconds(6);

            strftime('%T', date).should.equal('20:06:06');
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

        it('%w - вернулся правильный порядковый номер дня недели', function() {
            var dates = [
                    '2015-03-01T12:12:12Z',
                    '2015-03-02T12:12:12Z',
                    '2015-03-03T12:12:12Z',
                    '2015-03-04T12:12:12Z',
                    '2015-03-05T12:12:12Z',
                    '2015-03-06T12:12:12Z',
                    '2015-03-07T12:12:12Z'
                ],
                dayNumbers = dates.map(function(date) {
                    return strftime('%w', date);
                });

            dayNumbers.should.eql(['0', '1', '2', '3', '4', '5', '6']);
        });

        it('%W - порядковый номер недели в указанном году, начиная с первого понедельника в качестве первой недели', function() {
            strftime('%W', DATE).should.equal('09');
        });

        it('%x - предпочитаемое отображение даты, без времени "%m/%d/%y"', function() {
            strftime('%x', DATE).should.equal('03/04/14');
        });

        it('%X - предпочитаемое отображение времени "%H:%M:%S"', function() {
            strftime('%X', DATE).should.equal('12:12:12');
            strftime('%X', DATE2).should.equal('01:02:03');
        });

        it('%y - последние 2 цифры года с ведущим нулем', function() {
            var date = new Date(DATE.valueOf());
            date.setFullYear(2001);

            strftime('%y', date).should.equal('01');
        });

        it('%Y - год', function() {
            strftime('%Y', DATE).should.equal('2014');
        });

        describe('смещения временной зоны', function() {
            const originalTZ = process.env.TZ;
            before(() => process.env.TZ = 'MSK');
            after(() => process.env.TZ = originalTZ);

            // так как javascript переводит все даты (объекты Date) в локальную временную зону,
            // мы не можем сравнивать смещения с константами - тест м.б. запущен в любой зоне.
            // поэтому далее regexp'ы

            it('%z - смещение временной зоны относительно UTC (пример -0500)', function() {
                strftime('%z', DATE).should.match(/^[\-\+]\d{4}$/);
            });

            it('%Z - аббревиатура временной зоны относительно UTC', function() {
                // В идеале http://www.timeanddate.com/time/zones/ - строки из букв,
                // но иногда бывает и просто '+03'
                // результат зависит от временной зоны на машине, выполняющей тесты
                strftime('%Z', DATE).should.match(/^(([A-Z]+)|(\+\d\d))$/);
            });
        });

        it('%%', function() {
            strftime('%%', DATE).should.equal('%');
        });
    });

    describe('модификаторы', function() {
        var digitFlags = ['C', 'd', 'e', 'g', 'H', 'I', 'j', 'm', 'M', 'S', 'V', 'W', 'y', 'l'],
            letterFlags = ['a', 'A', 'b', 'h', 'B'],
            date = new Date(709, 1, 1, 6, 6, 6); // Mon Feb 01 0709 06:06:06

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

    xdescribe('перловый СDate объект', function() {
        it('%-e %B в %H:%M', function() {
            strftime('%-e %B в %H:%M', perlCDate(DATE)).should.equal('4 марта в 12:12');
        });

        it('%e %B %Y', function() {
            strftime('%e %B %Y', perlCDate(DATE)).should.equal(' 4 марта 2014');
        });

        it('%~B %Y', function() {
            strftime('%~B %Y', perlCDate(DATE)).should.equal('март 2014');
        });
    });

    // Заигнорен, чтобы запускать только при необходимости
    xit('benchmark strftime', function() {
        var DATE = new Date(2014, 2, 4, 12, 12, 12); // 04.03.2014 12:12:12
        var LOCAL_OFFSET = (function() {
            var o = new Date().getTimezoneOffset();
            var abs = Math.abs(o);
            var H = String(parseInt(abs / 60, 10)).padStart(2, '0');
            var M = String(abs % 60).padStart(2, '0');

            return (o > 0 ? '-' : '+') + H + M;
        })();

        BEM.I18N.lang('ru');

        var suite = new Benchmark.Suite;

        suite
            .add('strftime(undefined)', function() {
                strftime('%c', undefined);
            })
            .add('strftime(number)', function() {
                strftime('%c', 1539182890047);
            })
            .add('strftime(date)', function() {
                strftime('%c', DATE);
            })
            .add('strftime(string without timezone)', function() {
                strftime('%c', '2014-03-03T12:12:12');
            })
            .add('strftime(string with zulu timezone)', function() {
                strftime('%c', '2014-03-03T12:12:12Z');
            })
            .add('strftime(string with different timezone)', function() {
                strftime('%c', '2014-03-03T12:12:12-0500');
            })
            .add('strftime(string with local timezone)', function() {
                strftime('%c', '2014-03-03T12:12:12' + LOCAL_OFFSET);
            })
            .on('cycle', function(event) {
                console.log(String(event.target)); // eslint-disable-line no-console
            })
            .on('complete', function() {
                console.log('Fastest is ' + this.filter('fastest').map('name')); // eslint-disable-line no-console
            })
            .run({ async: false });
    });

    xit('benchmark strftime pad', function() {
        const DATE = new Date(2014, 2, 4, 9, 8, 7);

        new Benchmark.Suite()
            .add('strftime with padding', function() {
                strftime('%C %d %e %g %H %I %k %l %m %M %S %y', DATE);
            })
            .on('cycle', function(event) {
                console.log(String(event.target)); // eslint-disable-line no-console
            })
            .run({ async: false });
    });
});
