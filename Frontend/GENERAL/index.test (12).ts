import { assert } from 'chai';
import Query from './index';

describe('VH-Sandbox-Query', function() {
    describe('parseJSON()', function() {
        it('должен распарсить валидный json', function() {
            assert.deepEqual(Query.parseJSON('{"key1":123,"key2":"value"}'), {
                key1: 123,
                key2: 'value'
            });
        });

        it('должен вернуть строку в первоначальном состоянии, если она не json', function() {
            assert.strictEqual(Query.parseJSON('<iframe>'), '<iframe>');
        });
    });

    describe('parse()', function() {
        it('должен вернуть пустой объект, если запрос не является строкой', function() {
            assert.deepEqual(Query.parse(null), {});
        });

        it('должен распарсить строку с параметрами запроса', function() {
            assert.deepEqual(Query.parse('#html=iframe&duration=100&autoplay=0&loop=true'), {
                html: 'iframe',
                duration: 100,
                autoplay: 0,
                loop: true
            });
        });

        it('должен декодировать закодированные параметры', function() {
            const query = 'param=%D0%B7%D0%BD%D0%B0%D1%87%D0%B5%D0%BD%D0%B8%D0%B5';

            assert.deepEqual(Query.parse(query), {
                param: 'значение'
            });
        });

        it('должен использовать последнее значение если указано два одинаковых ключа', function() {
            assert.deepEqual(Query.parse('?a=true&b=1&b=2&b=3'), {
                a: true,
                b: 3
            });
        });

        it('должен корректно распарсить строку с кастромным сепаратором', function() {
            assert.deepEqual(Query.parse('a=true;b=1', ';'), {
                a: true,
                b: 1
            });
        });

        it('должен корректно распарсить ключи без значений, если это разрешено', function() {
            assert.deepEqual(Query.parse('a;b;c=1', ';', true), {
                a: true,
                b: true,
                c: 1
            });
        });
    });

    describe('конструктор', function() {
        let originalQueryParse: typeof Query.parse;
        beforeEach(function() {
            originalQueryParse = Query.parse;
            Query.parse = jest.fn() as typeof Query.parse;
        });

        afterEach(function() {
            Query.parse = originalQueryParse;
        });

        it('должен распарсить запрос, переданный строкой', function() {
            (Query.parse as jest.Mock).mockReturnValue({ a: 1, b: false });

            const query = new Query('a=1&b=false');

            // При сравнивании query напрямую с объектом deepEqual падает с ошибкой,
            // из-за того что query инстанс Query, а не Object
            assert.deepEqual(Object.keys(query), ['a', 'b']);
            assert.strictEqual(query.a, 1);
            assert.strictEqual(query.b, false);
        });

        it('должен скопировать значения из запроса, переданного объектом', function() {
            const query = new Query({ a: 1, b: false, c: 'iframe' });

            // При сравнивании query напрямую с объектом deepEqual падает с ошибкой,
            // из-за того что query инстанс Query, а не Object
            assert.deepEqual(Object.keys(query), ['a', 'b', 'c']);
            assert.strictEqual(query.a, 1);
            assert.strictEqual(query.b, false);
            assert.strictEqual(query.c, 'iframe');
        });

        it('должен использовать значение по умолчанию для параметра, отсутствующего в запросе', function() {
            const query = new Query({ a: 1 }, {
                b: false,
                c: {
                    value: null
                }
            });

            // При сравнивании query напрямую с объектом deepEqual падает с ошибкой,
            // из-за того что query инстанс Query, а не Object
            assert.deepEqual(Object.keys(query), ['b', 'c', 'a']);

            assert.strictEqual(query.b, false);
            assert.strictEqual(query.c, null);
        });

        it('не должен использовать значение по умолчанию, равное undefined', function() {
            const query = new Query({ a: 1 }, {
                b: undefined,
                c: {
                    value: undefined
                }
            });

            // При сравнивании query напрямую с объектом deepEqual падает с ошибкой,
            // из-за того что query инстанс Query, а не Object
            assert.deepEqual(Object.keys(query), ['a']);
        });

        it('должен использовать сеттер для парсинга значения параметра при его наличии', function() {
            const query = new Query({ autoplay: 1 }, {
                autoplay: {
                    set: function(this: Query, value: unknown, key: string) {
                        this[key] = Boolean(value);
                        this.calledAutoplaySetter = true;
                    }
                }
            });

            assert.isTrue(query.autoplay);
            assert.isTrue(query.calledAutoplaySetter);
        });

        it('должен передать в обработчик значения небезопасных параметров при их изменении', function() {
            const unsafeParamsHandler = jest.fn();
            const query = new Query({
                safe: 123,
                unsafe2: 'ololo'
            }, {
                unsafe1: {
                    value: 'default',
                    unsafe: true
                },
                unsafe2: {
                    value: 'default',
                    unsafe: true
                }
            }, unsafeParamsHandler);

            assert.deepEqual(unsafeParamsHandler.mock.calls[0], [{ unsafe2: 'ololo' }, query]);
        });
    });

    describe('toString()', function() {
        let query: Query;

        beforeEach(function() {
            // Выключаем конструктор
            query = Object.create(Query.prototype);

            query.html = 'iframe';
            query.tv = 1;
            query.loop = true;
            query.counters = {
                reqid: 123,
                duration: 20
            };
        });

        it('должен построить строку запроса из текущих параметров', function() {
            assert.strictEqual(
                query.toString(),
                'html=iframe&tv=1&loop=true&counters=%7B%22reqid%22%3A123%2C%22duration%22%3A20%7D'
            );
        });

        it('не должен добавить в строку запроса параметр, равный undefined', function() {
            query['test-id'] = undefined;
            assert.strictEqual(
                query.toString(),
                'html=iframe&tv=1&loop=true&counters=%7B%22reqid%22%3A123%2C%22duration%22%3A20%7D'
            );
        });

        it('должен добавить сепаратор в конец строки запросов при наличии параметра addSeparatorToEnd', function() {
            assert.strictEqual(
                query.toString('/', true),
                'html=iframe/tv=1/loop=true/counters=%7B%22reqid%22%3A123%2C%22duration%22%3A20%7D/'
            );
        });

        it('не должен кодировать параметры, добавленные в исключение', function() {
            query.yuid = '${yuid}';

            assert.strictEqual(
                query.toString('/', false, ['yuid']),
                'html=iframe/tv=1/loop=true/counters=%7B%22reqid%22%3A123%2C%22duration%22%3A20%7D/yuid=${yuid}'
            );
        });
    });

    describe('appendTo()', function() {
        let query: Query;

        beforeEach(function() {
            // Выключаем конструктор
            query = Object.create(Query.prototype);
            query.toString = jest.fn().mockReturnValue('a=1&b=2');
        });

        it('должен добавить параметры запроса в url', function() {
            const url = query.appendTo('//youtube.com/embed/365?some=true');
            assert.strictEqual(url, '//youtube.com/embed/365?some=true&a=1&b=2');
        });

        it('должен корректно отработать, если в query ничего нет', function() {
            query.toString = jest.fn().mockReturnValue('');

            const url = query.appendTo('//youtube.com/embed/365?some=true');
            assert.strictEqual(url, '//youtube.com/embed/365?some=true');
        });
    });
});
