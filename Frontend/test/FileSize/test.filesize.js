var vm = require('vm'),
    readFileSync = require('fs').readFileSync,
    path = require('path'),
    dirName = path.resolve(__dirname, '../../FileSize'),
    fileSize = require(path.join(dirName, 'FileSize')).FileSize;

// Загружаем BEM.I18N core
vm.runInThisContext(readFileSync(path.join(__dirname, '../bemI18nForTests.js')));

// Переводы, ru и en
vm.runInThisContext(readFileSync(path.join(dirName, 'FileSize.i18n/ru.js')));
vm.runInThisContext(readFileSync(path.join(dirName, 'FileSize.i18n/en.js')));

describe('FileSize, язык ru', function() {
    beforeEach(function() {
        BEM.I18N.lang('ru');
    });

    describe('Размер файла в байтах', function() {
        it('1 байт', function() {
            fileSize(1).should.equal('1 байт');
        });

        it('2 байта', function() {
            fileSize(2).should.equal('2 байта');
        });

        it('5 байт', function() {
            fileSize(5).should.equal('5 байт');
        });
    });

    describe('Размер файла в Кбайтах', function() {
        it('1 КБ', function() {
            fileSize(1250).should.equal('1 КБ');
        });

        it('2 КБ', function() {
            fileSize(2000).should.equal('2 КБ');
        });

        it('5 КБ', function() {
            fileSize(5000).should.equal('5 КБ');
        });
    });

    describe('Размер файла в Мегабайтах', function() {
        it('1.0 МБ', function() {
            fileSize(1048576).should.equal('1.0 МБ');
        });

        it('10.0 МБ', function() {
            fileSize(10485760).should.equal('10.0 МБ');
        });
    });
});

describe('FileSize, язык en', function() {
    beforeEach(function() {
        BEM.I18N.lang('en');
    });

    describe('File size in bytes', function() {
        it('1 byte', function() {
            fileSize(1).should.equal('1 byte');
        });

        it('2 bytes', function() {
            fileSize(2).should.equal('2 bytes');
        });

        it('5 bytes', function() {
            fileSize(5).should.equal('5 bytes');
        });
    });

    describe('File size in Kilo bytes', function() {
        it('1 KB', function() {
            fileSize(1250).should.equal('1 KB');
        });

        it('2 KB', function() {
            fileSize(2000).should.equal('2 KB');
        });

        it('5 KB', function() {
            fileSize(5000).should.equal('5 KB');
        });
    });

    describe('File size in Mega bytes', function() {
        it('1.0 МБ', function() {
            fileSize(1048576).should.equal('1.0 MB');
        });

        it('10.0 МБ', function() {
            fileSize(10485760).should.equal('10.0 MB');
        });
    });
});
