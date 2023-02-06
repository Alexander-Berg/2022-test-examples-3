var vm = require('vm'),
    path = require('path'),
    extend = require('util')._extend,
    dirName = path.resolve(__dirname, '../../DateTime'),
    DateTime = require(path.join(dirName, 'DateTime')).DateTime,
    readFileSync = require('fs').readFileSync;

// Загружаем BEM.I18N core
vm.runInThisContext(readFileSync(path.join(__dirname, '../bemI18nForTests.js')));

// Переводы
vm.runInThisContext(readFileSync(path.join(dirName, 'DateTime.i18n/ru.js')));

var NOW = '2014-03-04T12:12:12+0500',
    SECONDS = {
        '-1': '2014-03-04T12:12:11+0500',
        '-2': '2014-03-04T12:12:10+0500',
        '-5': '2014-03-04T12:12:07+0500'
    },
    MINUTES = {
        '-1': '2014-03-04T12:11:12+0500',
        '-2': '2014-03-04T12:10:12+0500',
        '-5': '2014-03-04T12:07:12+0500'
    },
    HOURS = {
        '-1': '2014-03-04T11:12:12+0500',
        '-2': '2014-03-04T10:12:12+0500',
        '-5': '2014-03-04T07:12:12+0500'
    },
    BEFORE_YESTERDAY = {
        DAY: '2014-03-02T12:12:12+0500',
        END: '2014-03-02T23:59:59+0500'
    },
    YESTERDAY = {
        START: '2014-03-03T00:00:00+0500',
        DAY: '2014-03-03T12:12:12+0500',
        END: '2014-03-03T23:59:59+0500'
    },
    TODAY = {
        START: '2014-03-04T00:00:00+0500'
    };

describe('mtime', function() {
    var options,
        optionsShort,
        mtime = DateTime.mtime;

    BEM.I18N.lang('ru');

    beforeEach(function() {
        options = {
            style: 'mtime',
            now: NOW
        };

        optionsShort = extend({
            short: true
        }, options);
    });

    describe('1 секунду назад', function() {
        it('полное написание', function() {
            mtime(SECONDS['-1'], options).should.equal('1 секунду назад');
        });

        it('короткое написание', function() {
            mtime(SECONDS['-1'], optionsShort).should.equal('1 с. назад');
        });
    });

    describe('2 секунды назад', function() {
        it('полное написание', function() {
            mtime(SECONDS['-2'], options).should.equal('2 секунды назад');
        });

        it('короткое написание', function() {
            mtime(SECONDS['-2'], optionsShort).should.equal('2 с. назад');
        });
    });

    describe('5 секунд назад', function() {
        it('полное написание', function() {
            mtime(SECONDS['-5'], options).should.equal('5 секунд назад');
        });

        it('короткое написание', function() {
            mtime(SECONDS['-5'], optionsShort).should.equal('5 с. назад');
        });
    });

    describe('1 минуту назад', function() {
        it('полное написание', function() {
            mtime(MINUTES['-1'], options).should.equal('1 минуту назад');
        });

        it('короткое написание', function() {
            mtime(MINUTES['-1'], optionsShort).should.equal('1 мин. назад');
        });
    });

    describe('2 минуты назад', function() {
        it('полное написание', function() {
            mtime(MINUTES['-2'], options).should.equal('2 минуты назад');
        });

        it('короткое написание', function() {
            mtime(MINUTES['-2'], optionsShort).should.equal('2 мин. назад');
        });
    });

    describe('5 минут назад', function() {
        it('полное написание', function() {
            mtime(MINUTES['-5'], options).should.equal('5 минут назад');
        });

        it('короткое написание', function() {
            mtime(MINUTES['-5'], optionsShort).should.equal('5 мин. назад');
        });
    });

    describe('1 час назад', function() {
        it('полное написание', function() {
            mtime(HOURS['-1'], options).should.equal('1 час назад');
        });

        it('короткое написание', function() {
            mtime(HOURS['-1'], optionsShort).should.equal('1 ч. назад');
        });

        it('разные таймзоны', function() {
            options.now = '2014-01-01T01:00:00+0200';

            mtime('2014-01-01T01:00:00+0300', options).should.equal('1 час назад');
        });
    });

    describe('2 часа назад', function() {
        it('полное написание', function() {
            mtime(HOURS['-2'], options).should.equal('2 часа назад');
        });

        it('короткое написание', function() {
            mtime(HOURS['-2'], optionsShort).should.equal('2 ч. назад');
        });

        it('разные таймзоны', function() {
            options.now = '2014-01-01T05:55:00+0500';

            mtime('2014-01-01T05:55:00+0700', options).should.equal('2 часа назад');
        });
    });

    describe('5 часов назад', function() {
        it('полное написание', function() {
            mtime(HOURS['-5'], options).should.equal('5 часов назад');
        });

        it('короткое написание', function() {
            mtime(HOURS['-5'], optionsShort).should.equal('5 ч. назад');
        });

        it('разные таймзоны', function() {
            options.now = '2014-01-01T13:00:00+0500';

            mtime('2014-01-01T09:00:00+0600', options).should.equal('5 часов назад');
        });
    });

    describe('вчера', function() {
        it('позавчера в 23:59:59', function() {
            mtime(BEFORE_YESTERDAY.END, options).should.not.equal('вчера');
        });

        it('вчера', function() {
            mtime(YESTERDAY.DAY, options).should.equal('вчера');
        });

        it('вчера в 00:00:00', function() {
            mtime(YESTERDAY.START, options).should.equal('вчера');
        });

        it('вчера в 23:59:59', function() {
            mtime(YESTERDAY.END, options).should.equal('вчера');
        });

        it('сегодня в 00:00:00', function() {
            mtime(TODAY.START, options).should.not.equal('вчера');
        });

        it('разные таймзоны', function() {
            options.now = '2014-01-01T01:00:00+0000';

            mtime('2014-01-01T03:00:00+0500', options).should.equal('вчера');
        });
    });

    describe('позавчера', function() {
        it('позавчера', function() {
            mtime(BEFORE_YESTERDAY.DAY, options).should.equal('позавчера');
        });

        it('3 дня назад в 00:00:00', function() {
            mtime('2014-03-01T00:00:00+0500', options).should.equal('01.03.2014');
        });

        it('3 дня назад в 23:59:59', function() {
            mtime('2014-03-01T23:59:59+0500', options).should.equal('01.03.2014');
        });

        it('разные таймзоны', function() {
            options.now = '2014-01-01T01:00:00+0000';

            mtime('2013-12-31T03:00:00+0500', options).should.equal('позавчера');
        });
    });
});
