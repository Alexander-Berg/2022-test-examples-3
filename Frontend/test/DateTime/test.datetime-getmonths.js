var vm = require('vm'),
    readFileSync = require('fs').readFileSync,
    path = require('path'),
    dirName = path.resolve(__dirname, '../../DateTime'),
    DateTime = require(path.join(dirName, 'DateTime')).DateTime,
    getMonths = DateTime.getMonths,
    getGenitiveMonths = DateTime.getGenitiveMonths;

// Загружаем BEM.I18N core
vm.runInThisContext(readFileSync(path.join(__dirname, '../bemI18nForTests.js')));

// Переводы
vm.runInThisContext(readFileSync(path.join(dirName, 'DateTime.i18n/ru.js')));

describe('getMonths', function() {
    BEM.I18N.lang('ru');

    it('месяцы', function() {
        getMonths().should.eql([
            'январь',
            'февраль',
            'март',
            'апрель',
            'май',
            'июнь',
            'июль',
            'август',
            'сентябрь',
            'октябрь',
            'ноябрь',
            'декабрь'
        ]);
    });

    it('месяцы, сокращённое написание', function() {
        getMonths(true).should.eql([
            'янв',
            'фев',
            'мар',
            'апр',
            'май',
            'июн',
            'июл',
            'авг',
            'сен',
            'окт',
            'ноя',
            'дек'
        ]);
    });

    it('месяцы, в родительном падеже', function() {
        getGenitiveMonths().should.eql([
            'января',
            'февраля',
            'марта',
            'апреля',
            'мая',
            'июня',
            'июля',
            'августа',
            'сентября',
            'октября',
            'ноября',
            'декабря'
        ]);
    });
});
