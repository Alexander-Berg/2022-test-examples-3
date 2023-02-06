var vm = require('vm');
var readFileSync = require('fs').readFileSync;
var path = require('path');
var dirName = path.resolve(__dirname, '../../DateTime');
var DateTime = require(path.join(dirName, 'DateTime')).DateTime;
var subtract = DateTime.subtract;
var parseDateFromString = DateTime.internal.parseDateFromString;
var Benchmark = require('benchmark');

// Загружаем BEM.I18N core
vm.runInThisContext(readFileSync(path.join(__dirname, '../bemI18nForTests.js')));

// Переводы, ru и en
vm.runInThisContext(readFileSync(path.join(dirName, 'DateTime.i18n/ru.js')));

function tzOffsetToString(tzOffset) {
    var abs = Math.abs(tzOffset);
    var H = String(parseInt(abs / 60, 10)).padStart(2, '0');
    var M = String(abs % 60).padStart(2, '0');

    return (tzOffset > 0 ? '-' : '+') + H + M;
}

describe('subtract', function() {
    var MS_IN_DAY = 24 * 60 * 60 * 1000;
    var LOCAL_OFFSET = tzOffsetToString(new Date().getTimezoneOffset());
    var NEXT_OFFSET = tzOffsetToString(new Date().getTimezoneOffset() - 60);
    var PREV_OFFSET = tzOffsetToString(new Date().getTimezoneOffset() + 60);

    describe('возвращает 0 для дат с одинаковым днём', function() {
        it('в таймзоне по умолчанию (локальной) - Date', function() {
            subtract(
                new Date('2018-10-10T00:00:01'),
                new Date('2018-10-10T23:59:59')
            ).should.equal(0);
        });

        it('в таймзоне по умолчанию (локальной) - наш parse', function() {
            subtract(
                parseDateFromString('2018-10-10T00:00:01'),
                parseDateFromString('2018-10-10T23:59:59')
            ).should.equal(0);
        });

        // если парсить строку через new Date() или Date.parse(), то будет проходить максимум один
        // из последующих тестов - тот, в котором часовой пояс совпадёт с часовым поясом машины,
        // на которой выполняются тесты, т.к. при переводе во все другие таймзоны кроме оригинальной
        // дни будут различаться
        it('в таймзоне Z', function() {
            subtract(
                parseDateFromString('2018-10-10T00:00:01Z'),
                parseDateFromString('2018-10-10T23:59:59Z')
            ).should.equal(0);
        });

        it('в таймзоне +0300', function() {
            subtract(
                parseDateFromString('2018-10-10T00:00:01+0300'),
                parseDateFromString('2018-10-10T23:59:59+0300')
            ).should.equal(0);
        });

        it('в таймзоне +0900', function() {
            subtract(
                parseDateFromString('2018-10-10T00:00:01+0900'),
                parseDateFromString('2018-10-10T23:59:59+0900')
            ).should.equal(0);
        });

        describe('приводит вторую дату к таймзоне первой даты', function() {
            // здесь везде вторая дата - этот тот же день в первой таймзоне
            it('первая дата в Zulu time, вторая - в +0900', function() {
                subtract(
                    parseDateFromString('2018-10-10T00:00:01Z'),
                    parseDateFromString('2018-10-11T08:59:59+0900')
                ).should.equal(0);
            });

            it('первая дата в локальной таймзоне, вторая - в локальной +1 час', function() {
                subtract(
                    parseDateFromString('2018-10-10T00:00:01' + LOCAL_OFFSET),
                    parseDateFromString('2018-10-11T00:59:59' + NEXT_OFFSET)
                ).should.equal(0);
            });

            it('первая дата в таймзоне +0300, вторая - в +0900', function() {
                subtract(
                    parseDateFromString('2018-10-10T00:00:01+0300'),
                    parseDateFromString('2018-10-11T05:59:59+0900')
                ).should.equal(0);
            });

            it('первая дата в таймзоне +0900, вторая - в Zulu time', function() {
                subtract(
                    parseDateFromString('2018-10-10T00:00:01+0900'),
                    parseDateFromString('2018-10-09T23:59:59Z')
                ).should.equal(0);
            });
        });
    });

    describe('возвращает разницу в миллисекундах, кратную суткам, для разных дат', function() {
        it('в таймзоне по умолчанию (локальной) - Date', function() {
            subtract(
                new Date('2018-10-10T00:00:01'),
                new Date('2018-10-09T23:59:59')
            ).should.equal(MS_IN_DAY);
            subtract(
                new Date('2018-10-10T12:23:34'),
                new Date('2018-09-20T23:40:05')
            ).should.equal(20 * MS_IN_DAY);
            subtract(
                new Date('2018-09-20T23:40:05'),
                new Date('2018-10-10T12:23:34')
            ).should.equal(-20 * MS_IN_DAY);
        });

        // если парсить строку через new Date() или Date.parse(), то будет проходить максимум один
        // из последующих тестов - тот, в котором часовой пояс совпадёт с часовым поясом машины,
        // на которой выполняются тесты, т.к. при переводе во все другие таймзоны кроме оригинальной
        // дни будут одинаковыми
        it('в таймзоне Z', function() {
            subtract(
                parseDateFromString('2018-10-10T00:00:01Z'),
                parseDateFromString('2018-10-09T23:59:59Z')
            ).should.equal(MS_IN_DAY);
        });

        it('в таймзоне +0300', function() {
            subtract(
                parseDateFromString('2018-10-10T00:00:01+0300'),
                parseDateFromString('2018-10-09T23:59:59+0300')
            ).should.equal(MS_IN_DAY);
        });

        it('в таймзоне +0900', function() {
            subtract(
                parseDateFromString('2018-10-10T00:00:01+0900'),
                parseDateFromString('2018-10-09T23:59:59+0900')
            ).should.equal(MS_IN_DAY);
        });

        describe('приводит вторую дату к таймзоне первой даты', function() {
            // здесь везде вторая дата - этот другой день в первой таймзоне,
            // несмотря на то, что обе даты начинаются с "2018-10-10"
            it('первая дата в Zulu time, вторая - в +0900', function() {
                subtract(
                    parseDateFromString('2018-10-10T00:00:01Z'),
                    parseDateFromString('2018-10-10T08:59:59+0900')
                ).should.equal(MS_IN_DAY);
            });

            it('первая дата в локальной таймзоне, вторая - в локальной +1 час', function() {
                subtract(
                    parseDateFromString('2018-10-10T00:00:01' + LOCAL_OFFSET),
                    parseDateFromString('2018-10-10T00:59:59' + NEXT_OFFSET)
                ).should.equal(MS_IN_DAY);
            });

            it('первая дата в таймзоне +0300, вторая - в +0900', function() {
                subtract(
                    parseDateFromString('2018-10-10T00:00:01+0300'),
                    parseDateFromString('2018-10-10T05:59:59+0900')
                ).should.equal(MS_IN_DAY);
            });

            it('первая дата в таймзоне +0900, вторая - в Zulu time', function() {
                subtract(
                    parseDateFromString('2018-10-10T00:00:01+0900'),
                    parseDateFromString('2018-10-10T15:59:59Z')
                ).should.equal(-MS_IN_DAY);
            });
        });
    });

    // Заигнорен, чтобы запускать только при необходимости
    xit('benchmark', function() {
        var localDate1 = new Date('2018-10-10T12:23:34');
        var localDate2 = new Date('2018-10-10T23:59:59');

        var localParsedDate1 = parseDateFromString('2018-10-10T00:00:01' + LOCAL_OFFSET);
        var parsedDate2 = parseDateFromString('2018-10-10T00:59:59' + NEXT_OFFSET);
        var parsedDate3 = parseDateFromString('2018-10-10T00:59:59' + PREV_OFFSET);

        var suite = new Benchmark.Suite;

        suite
            .add('subtract(local TZ, local TZ)', function() {
                subtract(localDate1, localDate2);
            })
            .add('subtract(local TZ, non-local TZ)', function() {
                subtract(localParsedDate1, parsedDate2);
            })
            .add('subtract(non-local TZ 1, non-local TZ 2)', function() {
                subtract(parsedDate2, parsedDate3);
            })
            .on('cycle', function(event) {
                console.log(String(event.target)); // eslint-disable-line no-console
            })
            .on('complete', function() {
                console.log('Fastest is ' + this.filter('fastest').map('name')); // eslint-disable-line no-console
            })
            .run({ async: false });
    });
});
