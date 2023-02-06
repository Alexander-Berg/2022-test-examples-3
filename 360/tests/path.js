const path = require('../lib/path');
const expect = require('expect');

describe('path', () => {
    /**
     * Вспомогательная функция для запуска теста
     *
     * @param {string} method название тестируемого метода хелпера
     * @param {string} pathGiven входной параметр (путь до файла)
     * @param {string|Array<string>} expectedResult ожидаемый результат
     */
    const runTest = (method, pathGiven, expectedResult) => {
        const testName = `${pathGiven} should return "${expectedResult}"`;

        it(testName, () => {
            expect(path[method](pathGiven)).toEqual(expectedResult);
        });
    };

    /**
     * Вспомогательная функция для тестирования метода хелпера
     *
     * @param {string} method название тестируемого метода хелпера
     * @param {Object.<string, string|Array<string>>} tests объект с тестами вида <вход, выход>
     */
    const runTests = (method, tests) => {
        Object.keys(tests).forEach((input) => {
            runTest(method, input, tests[input]);
        });
    };

    describe('extname', () => {
        const tests = {
            'file.ext': 'ext',
            'file.ext.jpg': 'jpg',
            file: '',
            'file.': '',
            '': '',
            '/': '',
            '/disk/path/to/folder/file.jpg': 'jpg'
        };

        runTests('extname', tests);
    });

    describe('extname with param withDot', () => {
        expect(path.extname('file.ext', true)).toEqual('.ext');
        expect(path.extname('file.ext.jpg', true)).toEqual('.jpg');
        expect(path.extname('file', true)).toEqual('');
        expect(path.extname('file.', true)).toEqual('');
        expect(path.extname('', true)).toEqual('');
        expect(path.extname('/disk/path/to/folder/file.jpg', true)).toEqual('.jpg');
    });

    describe('split', () => {
        const tests = {
            'file.ext': ['file.ext'],
            '/disk/path/to/folder/file.jpg': ['disk', 'path', 'to', 'folder', 'file.jpg'],
            '//disk/path/to////folder/file.jpg/': ['disk', 'path', 'to', 'folder', 'file.jpg'],
            '': [],
            '/': []
        };

        runTests('split', tests);
    });

    describe('basename', () => {
        const tests = {
            'file.ext': 'file.ext',
            '/disk/path/to/folder/file.jpg': 'file.jpg',
            '//disk/path/to////folder/file.jpg/': 'file.jpg',
            '': '',
            '/': ''
        };

        runTests('basename', tests);
    });

    describe('normalize', () => {
        const tests = {
            'file.ext': '/file.ext',
            '/disk/path/to/folder/file.jpg': '/disk/path/to/folder/file.jpg',
            '//disk/path/to////folder/file.jpg/': '/disk/path/to/folder/file.jpg',
            '': '/',
            '/': '/'
        };

        runTests('normalize', tests);
    });

    describe('escape', () => {
        const tests = {
            '/files/folder/file.jpg': '/files/folder/file.jpg',
            '/files/ололо/файл': '/files/%D0%BE%D0%BB%D0%BE%D0%BB%D0%BE/%D1%84%D0%B0%D0%B9%D0%BB',
            '/files/%%<>': '/files/%25%25%3C%3E'
        };

        runTests('escape', tests);
    });

    describe('unescape', () => {
        const tests = {
            '/files/folder/file.jpg': '/files/folder/file.jpg',
            '/files/%D0%BE%D0%BB%D0%BE%D0%BB%D0%BE/%D1%84%D0%B0%D0%B9%D0%BB': '/files/ололо/файл',
            '/files/%25%25%3C%3E': '/files/%%<>'
        };

        runTests('unescape', tests);
    });

    describe('dirname', () => {
        const tests = {
            '/disk/folder/path/to/parent/file.jpg': '/disk/folder/path/to/parent',
            '/disk/folder/path/to/parent/file.jpg///': '/disk/folder/path/to/parent',
            '/disk/folder/path/to/parent////file.jpg': '/disk/folder/path/to/parent',
            'disk////folder////path//to/parent/file.jpg': '/disk/folder/path/to/parent',
            // eslint-disable-next-line max-len
            '/public/OK7booQs1TJM6ugySfA1dNGoqcNVLyyUR02CdavJJb8=:/1.jpeg': '/public/OK7booQs1TJM6ugySfA1dNGoqcNVLyyUR02CdavJJb8=:',
            '/disk/parent/file/jpg/': '/disk/parent/file',
            '////': '/',
            '/': '/',
            '': '/'
        };

        runTests('dirname', tests);
    });
});
