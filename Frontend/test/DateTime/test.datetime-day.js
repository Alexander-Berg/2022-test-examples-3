var vm = require('vm'),
    path = require('path'),
    readFileSync = require('fs').readFileSync,
    dirName = path.resolve(__dirname, '../../DateTime'),
    DateTime = require(path.join(dirName, 'DateTime')).DateTime;

// Загружаем BEM.I18N core
vm.runInThisContext(readFileSync(path.join(__dirname, '../bemI18nForTests.js')));

// Переводы
vm.runInThisContext(readFileSync(path.join(dirName, 'DateTime.i18n/ru.js')));

var YESTERDAY = new Date(2014, 2, 3, 12, 12, 12, 0), // 03.03.2014 12:12:12.0
    NOW = new Date(2014, 2, 4, 12, 12, 12, 0), // 04.03.2014 12:12:12.0
    TOMORROW = new Date(2014, 2, 5, 12, 12, 12, 0); // 05.03.2014 12:12:12.0

describe('day', function() {
    var options,
        day = DateTime.day,
        setTime = function(date, hours, mins, secs) {
            date.setHours(hours);
            date.setMinutes(mins);
            date.setSeconds(secs);

            return date;
        },
        beginDate = function(date) {
            return setTime(date, 0, 0, 0);
        },
        endDate = function(date) {
            return setTime(date, 23, 59, 59);
        };

    BEM.I18N.lang('ru');

    beforeEach(function() {
        options = {
            format: '%e %B',
            now: new Date(NOW)
        };
    });

    describe('сегодня', function() {
        it('в течение дня', function() {
            day(NOW, options).should.equal('сегодня');
        });

        it('вчера в 23:59:59', function() {
            var date = new Date(YESTERDAY);

            day(endDate(date), options).should.not.equal('сегодня');
        });

        it('сегодня в 00:00:00', function() {
            var date = new Date(NOW);

            day(beginDate(date), options).should.equal('сегодня');
        });

        it('сегодня в 23:59:59', function() {
            var date = new Date(NOW);

            day(endDate(date), options).should.equal('сегодня');
        });

        it('завтра в 00:00:00', function() {
            var date = new Date(TOMORROW);

            day(beginDate(date), options).should.not.equal('сегодня');
        });
    });

    describe('вчера', function() {
        it('в течение дня', function() {
            day(YESTERDAY, options).should.equal('вчера');
        });

        it('позавчера в 23:59:59', function() {
            var date = new Date(YESTERDAY);

            date.setDate(date.getDate() - 1);

            day(beginDate(date), options).should.not.equal('вчера');
        });

        it('вчера в 00:00:00', function() {
            var date = new Date(YESTERDAY);

            day(beginDate(date), options).should.equal('вчера');
        });

        it('вчера в 23:59:59', function() {
            var date = new Date(YESTERDAY);

            day(endDate(date), options).should.equal('вчера');
        });

        it('сегодня в 00:00:00', function() {
            var date = new Date(NOW);

            day(beginDate(date), options).should.not.equal('вчера');
        });
    });

    describe('завтра', function() {
        it('в течение дня', function() {
            day(TOMORROW, options).should.equal('завтра');
        });

        it('сегодня в 23:59:59', function() {
            var date = new Date(NOW);

            day(endDate(date), options).should.not.equal('завтра');
        });

        it('завтра в 00:00:00', function() {
            var date = new Date(TOMORROW);

            day(beginDate(date), options).should.equal('завтра');
        });

        it('завтра в 23:59:59', function() {
            var date = new Date(TOMORROW);

            day(endDate(date), options).should.equal('завтра');
        });

        it('поcле завтра в 00:00:00', function() {
            var date = new Date(TOMORROW);

            date.setDate(date.getDate() + 1);

            day(beginDate(date), options).should.not.equal('завтра');
        });
    });
});
