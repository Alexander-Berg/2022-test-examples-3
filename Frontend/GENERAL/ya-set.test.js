const { assert } = require('chai');
var _ = require('./ya-set.vanilla.js');

describe('ya-set', function() {
    var Ya;
    var Set;

    beforeEach(function() {
        Ya = window.Yandex;
        Set = Ya.VH.Set;
    });

    describe('конструктор', function() {
        it('корректно инициализирует коллекцию', function() {
            var set = new Set(['autoplay', 'fullscreen']);

            assert.deepEqual(Object.keys(set), ['_arr', 'length']);
            assert.strictEqual(set.length, 2);
            assert.deepEqual(set._arr, ['autoplay', 'fullscreen']);
        });

        it('инициализирует коллекцию пустой при отсутствии аргументов', function() {
            var set = new Set();

            assert.strictEqual(set.length, 0);
            assert.deepEqual(set._arr, []);
        });

        it('инициализирует коллекцию пустой при получении некорректного аргумента', function() {
            var set = new Set('invalid');

            assert.strictEqual(set.length, 0);
            assert.deepEqual(set._arr, []);
        });
    });

    describe('join()', function() {
        it('корректно преобразует в строку', function() {
            var set = new Set(['autoplay', 'fullscreen']);

            assert.strictEqual(set.join('; '), 'autoplay; fullscreen');
        });

        it('корректно преобразует в строку без сепаратора', function() {
            var set = new Set(['autoplay', 'fullscreen']);

            assert.strictEqual(set.join(), 'autoplay fullscreen');
        });
    });

    describe('add()', function() {
        it('корректно добавляет элемент', function() {
            var set = new Set(['autoplay', 'fullscreen']);
            set.add('accelerometer');

            assert.strictEqual(set.length, 3);
            assert.deepEqual(set._arr, ['autoplay', 'fullscreen', 'accelerometer']);
        });

        it('не добавляет элемент, который уже есть в коллекции', function() {
            var set = new Set(['autoplay', 'fullscreen']);
            set.add('fullscreen');

            assert.strictEqual(set.length, 2);
            assert.deepEqual(set._arr, ['autoplay', 'fullscreen']);
        });
    });

    describe('delete()', function() {
        beforeEach(function() {});

        it('корректно удаляет элемент', function() {
            var set = new Set(['autoplay', 'fullscreen']);
            set.delete('autoplay');

            assert.strictEqual(set.length, 1);
            assert.deepEqual(set._arr, ['fullscreen']);
        });

        it('корректно работает с пустой коллекцией', function() {
            var set = new Set([]);
            set.delete('autoplay');

            assert.strictEqual(set.length, 0);
            assert.deepEqual(set._arr, []);
        });
    });
});
