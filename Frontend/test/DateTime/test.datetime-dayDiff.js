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
    TOMORROW = new Date(2014, 2, 5, 12, 12, 12, 0), // 05.03.2014 12:12:12.0
    AFTER_10_DAYS = new Date(2014, 2, 14, 12, 12, 12, 0), // 14.03.2014 12:12:12.0
    YESTERDAY_PREV_MONTH = new Date(2018, 5, 30, 12, 12, 12, 0), // 30.06.2018 12:12:12.0
    TODAY_NEXT_MONTH = new Date(2018, 6, 1, 12, 12, 12, 0); // 01.07.2018 12:12:12.0

describe('dayDiff', function() {
    var daysDiff = DateTime.daysDiff;

    BEM.I18N.lang('ru');

    it('сегодня', function() {
        daysDiff(NOW, NOW).should.equal(0);
    });

    it('завтра', function() {
        daysDiff(NOW, TOMORROW).should.equal(-1);
    });

    it('вчера', function() {
        daysDiff(NOW, YESTERDAY).should.equal(1);
    });

    it('через 10 дней', function() {
        daysDiff(NOW, AFTER_10_DAYS).should.equal(-10);
    });

    it('вчера в прошлом месяце', function() {
        daysDiff(TODAY_NEXT_MONTH, YESTERDAY_PREV_MONTH).should.equal(1);
    });

    it('сегодня - даты в разных таймзонах', function() {
        // время отличается на одну минуту
        daysDiff('2014-03-03T01:00:00+0600', '2014-03-02T21:59:00+0300').should.equal(0);
    });

    // new Date('2014-01-01').getTimezoneOffset(); // -180 (node 8), -240 (node 12)
    it('вчера - время в таймзоне +0900', function() {
        daysDiff('2020-03-04T01:00:00+0900', '2020-03-03T23:59:00+0900').should.equal(1);
    });
});
