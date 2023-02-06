var vm = require('vm'),
    readFileSync = require('fs').readFileSync,
    path = require('path'),
    dirName = path.resolve(__dirname, '../../DateTime'),
    DateTime = require(path.join(dirName, 'DateTime')).DateTime,
    getDays = DateTime.getDays;

// Загружаем BEM.I18N core
vm.runInThisContext(readFileSync(path.join(__dirname, '../bemI18nForTests.js')));

// Переводы
vm.runInThisContext(readFileSync(path.join(dirName, 'DateTime.i18n/ru.js')));

describe('getDays', function() {
    BEM.I18N.lang('ru');

    it('дни недели', function() {
        getDays().should.eql([
            'понедельник',
            'вторник',
            'среда',
            'четверг',
            'пятница',
            'суббота',
            'воскресенье'
        ]);
    });

    it('дни недели, сокращённое написание', function() {
        getDays(true).should.eql([
            'пн',
            'вт',
            'ср',
            'чт',
            'пт',
            'сб',
            'вс'
        ]);
    });
});
