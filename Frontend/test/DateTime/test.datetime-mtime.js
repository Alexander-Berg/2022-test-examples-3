var vm = require('vm'),
    path = require('path'),
    dirName = path.resolve(__dirname, '../../DateTime'),
    DateTime = require(path.join(dirName, 'DateTime')).DateTime,
    readFileSync = require('fs').readFileSync,
    Benchmark = require('benchmark');

// Загружаем BEM.I18N core
vm.runInThisContext(readFileSync(path.join(__dirname, '../bemI18nForTests.js')));

// Переводы
vm.runInThisContext(readFileSync(path.join(dirName, 'DateTime.i18n/ru.js')));

var SECOND = 1e3,
    MINUTE = 60 * SECOND,
    HOUR = 60 * MINUTE,

    BEFORE_YESTERDAY = new Date(2014, 2, 2, 12, 12, 12, 0), // 02.03.2014 12:12:12.0
    YESTERDAY = new Date(2014, 2, 3, 12, 12, 12, 0), // 03.03.2014 12:12:12.0
    NOW = new Date(2014, 2, 4, 12, 12, 12, 0), // 04.03.2014 12:12:12.0

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

describe('mtime', function() {
    var options,
        optionsShort,
        mtime = DateTime.mtime;

    BEM.I18N.lang('ru');

    beforeEach(function() {
        options = {
            style: 'mtime',
            format: '%e %B',
            now: new Date(NOW)
        };

        optionsShort = {
            style: 'mtime',
            format: '%e %B',
            now: new Date(NOW),
            short: true
        };
    });

    describe('1 секунду назад', function() {
        var oneSecAgo = NOW - SECOND;

        it('полное написание', function() {
            mtime(oneSecAgo, options).should.equal('1 секунду назад');
        });

        it('короткое написание', function() {
            mtime(oneSecAgo, optionsShort).should.equal('1 с. назад');
        });
    });

    describe('2 секунды назад', function() {
        var twoSecAgo = NOW - 2 * SECOND;

        it('полное написание', function() {
            mtime(twoSecAgo, options).should.equal('2 секунды назад');
        });

        it('короткое написание', function() {
            mtime(twoSecAgo, optionsShort).should.equal('2 с. назад');
        });
    });

    describe('5 секунд назад', function() {
        var fiveSecAgo = NOW - 5 * SECOND;

        it('полное написание', function() {
            mtime(fiveSecAgo, options).should.equal('5 секунд назад');
        });

        it('короткое написание', function() {
            mtime(fiveSecAgo, optionsShort).should.equal('5 с. назад');
        });
    });

    describe('1 минуту назад', function() {
        var oneMinAgo = NOW - MINUTE;

        it('полное написание', function() {
            mtime(oneMinAgo, options).should.equal('1 минуту назад');
        });

        it('короткое написание', function() {
            mtime(oneMinAgo, optionsShort).should.equal('1 мин. назад');
        });
    });

    describe('2 минуты назад', function() {
        var twoMinAgo = NOW - 2 * MINUTE;

        it('полное написание', function() {
            mtime(twoMinAgo, options).should.equal('2 минуты назад');
        });

        it('короткое написание', function() {
            mtime(twoMinAgo, optionsShort).should.equal('2 мин. назад');
        });
    });

    describe('5 минут назад', function() {
        var fiveMinAgo = NOW - 5 * MINUTE;

        it('полное написание', function() {
            mtime(fiveMinAgo, options).should.equal('5 минут назад');
        });

        it('короткое написание', function() {
            mtime(fiveMinAgo, optionsShort).should.equal('5 мин. назад');
        });
    });

    describe('1 час назад', function() {
        var oneHourAgo = NOW - HOUR;

        it('полное написание', function() {
            mtime(oneHourAgo, options).should.equal('1 час назад');
        });

        it('короткое написание', function() {
            mtime(oneHourAgo, optionsShort).should.equal('1 ч. назад');
        });
    });

    describe('2 часа назад', function() {
        var twoHoursAgo = NOW - 2 * HOUR;

        it('полное написание', function() {
            mtime(twoHoursAgo, options).should.equal('2 часа назад');
        });

        it('короткое написание', function() {
            mtime(twoHoursAgo, optionsShort).should.equal('2 ч. назад');
        });
    });

    describe('5 часов назад', function() {
        var fiveHoursAgo = NOW - 5 * HOUR;

        it('полное написание', function() {
            mtime(fiveHoursAgo, options).should.equal('5 часов назад');
        });

        it('короткое написание', function() {
            mtime(fiveHoursAgo, optionsShort).should.equal('5 ч. назад');
        });
    });

    describe('вчера', function() {
        it('вчера', function() {
            mtime(YESTERDAY, options).should.equal('вчера');
        });

        it('позавчера в 23:59:59', function() {
            var date = new Date(BEFORE_YESTERDAY);

            mtime(endDate(date), options).should.not.equal('вчера');
        });

        it('вчера в 00:00:00', function() {
            var date = new Date(YESTERDAY);

            mtime(beginDate(date), options).should.equal('вчера');
        });

        it('вчера в 23:59:59', function() {
            var date = new Date(YESTERDAY);

            mtime(endDate(date), options).should.equal('вчера');
        });

        it('сегодня в 00:00:00', function() {
            var date = new Date(NOW);

            mtime(beginDate(date), options).should.not.equal('вчера');
        });
    });

    describe('позавчера', function() {
        it('позавчера', function() {
            mtime(BEFORE_YESTERDAY, options).should.equal('позавчера');
        });

        it('3 дня назад в 23:59:59', function() {
            var date = new Date(BEFORE_YESTERDAY);

            date.setDate(date.getDate() - 1);

            options.format = null;
            mtime(endDate(date), options).should.equal('01.03.2014');
        });
    });

    // Заигнорен, чтобы запускать только при необходимости
    xit('benchmark', function() {
        var oneSecAgo = NOW - SECOND;
        var twoSecAgo = NOW - 2 * SECOND;
        var fiveSecAgo = NOW - 5 * SECOND;
        var oneMinAgo = NOW - MINUTE;
        var twoMinAgo = NOW - 2 * MINUTE;
        var fiveMinAgo = NOW - 5 * MINUTE;
        var oneHourAgo = NOW - HOUR;
        var twoHoursAgo = NOW - 2 * HOUR;
        var fiveHoursAgo = NOW - 5 * HOUR;
        var date = new Date(BEFORE_YESTERDAY);

        options = {
            style: 'mtime',
            format: '%e %B',
            now: new Date(NOW)
        };

        optionsShort = {
            style: 'mtime',
            format: '%e %B',
            now: new Date(NOW),
            short: true
        };

        var suite = new Benchmark.Suite;

        suite
            .add('mtime', function() {
                mtime(oneSecAgo, options);
                mtime(oneSecAgo, optionsShort);
                mtime(twoSecAgo, options);
                mtime(twoSecAgo, optionsShort);
                mtime(fiveSecAgo, options);
                mtime(fiveSecAgo, optionsShort);
                mtime(oneMinAgo, options);
                mtime(oneMinAgo, optionsShort);
                mtime(twoMinAgo, options);
                mtime(twoMinAgo, optionsShort);
                mtime(fiveMinAgo, options);
                mtime(fiveMinAgo, optionsShort);
                mtime(oneHourAgo, options);
                mtime(oneHourAgo, optionsShort);
                mtime(twoHoursAgo, options);
                mtime(twoHoursAgo, optionsShort);
                mtime(fiveHoursAgo, options);
                mtime(fiveHoursAgo, optionsShort);
                mtime(YESTERDAY, options);
                mtime(endDate(date), options);
                mtime(beginDate(date), options);
                mtime(BEFORE_YESTERDAY, options);
            })
            .on('cycle', function(event) {
                console.log(String(event.target)); // eslint-disable-line no-console
            })
            .run({ async: false });
    });

    // @TODO:
    //  - Добавить restriction
    //  - формат
    //
});
