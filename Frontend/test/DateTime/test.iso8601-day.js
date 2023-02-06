var vm = require('vm'),
    path = require('path'),
    readFileSync = require('fs').readFileSync,
    dirName = path.resolve(__dirname, '../../DateTime'),
    day = require(path.join(dirName, 'DateTime')).DateTime.day;

// Загружаем BEM.I18N core
vm.runInThisContext(readFileSync(path.join(__dirname, '../bemI18nForTests.js')));

// Переводы
vm.runInThisContext(readFileSync(path.join(dirName, 'DateTime.i18n/ru.js')));

var NOW = '2014-03-04T12:12:12+0500',
    BEFORE_YESTERDAY = {
        END: '2014-03-02T23:59:59+0500'
    },
    YESTERDAY = {
        START: '2014-03-03T00:00:00+0500',
        DAY: '2014-03-03T12:12:12+0500',
        END: '2014-03-03T23:59:59+0500'
    },
    TODAY = {
        START: '2014-03-04T00:00:00+0500',
        DAY: '2014-03-04T12:12:12+0500',
        END: '2014-03-04T23:59:59+0500'
    },
    TOMORROW = {
        START: '2014-03-05T00:00:00+0500',
        DAY: '2014-03-05T12:12:12+0500',
        END: '2014-03-05T23:59:59+0500'
    },
    AFTER_TOMORROW = {
        START: '2014-03-06T00:00:00+0500'
    };

describe('day', function() {
    var options;

    beforeEach(function() {
        options = {
            now: NOW
        };
    });

    describe('сегодня', function() {
        it('вчера в 23:59:59', function() {
            day(YESTERDAY.END, options).should.not.equal('сегодня');
        });

        it('в течение дня', function() {
            day(TODAY.DAY, options).should.equal('сегодня');
        });

        it('в 00:00:00', function() {
            day(TODAY.START, options).should.equal('сегодня');
        });

        it('в 23:59:59', function() {
            day(TODAY.END, options).should.equal('сегодня');
        });

        it('завтра в 00:00:00', function() {
            day(TOMORROW.BEGIN, options).should.not.equal('сегодня');
        });

        it('разные таймзоны', function() {
            options.now = '2014-01-01T05:00:00+0500';
            day('2014-01-01T05:00:00+0500', options).should.equal('сегодня');
        });
    });

    describe('вчера', function() {
        it('позавчера в 23:59:59', function() {
            day(BEFORE_YESTERDAY.END, options).should.not.equal('вчера');
        });

        it('в течение дня', function() {
            day(YESTERDAY.DAY, options).should.equal('вчера');
        });

        it('в 00:00:00', function() {
            day(YESTERDAY.START, options).should.equal('вчера');
        });

        it('в 23:59:59', function() {
            day(YESTERDAY.END, options).should.equal('вчера');
        });

        it('сегодня в 00:00:00', function() {
            day(TODAY.START, options).should.not.equal('вчера');
        });

        it('разные таймзоны', function() {
            options.now = '2014-01-01T01:00:00Z';
            day('2014-01-01T03:00:00+0500', options).should.equal('вчера');
        });
    });

    describe('завтра', function() {
        it('сегодня в 23:59:59', function() {
            day(TODAY.END, options).should.not.equal('завтра');
        });

        it('в течение дня', function() {
            day(TOMORROW.DAY, options).should.equal('завтра');
        });

        it('в 00:00:00', function() {
            day(TOMORROW.START, options).should.equal('завтра');
        });

        it('в 23:59:59', function() {
            day(TOMORROW.END, options).should.equal('завтра');
        });

        it('поcле завтра в 00:00:00', function() {
            day(AFTER_TOMORROW.START, options).should.not.equal('завтра');
        });

        it('разные таймзоны', function() {
            options.now = '2014-01-01T03:00:00+0500';
            day('2014-01-01T01:00:00+0000', options).should.equal('сегодня');
        });
    });
});
