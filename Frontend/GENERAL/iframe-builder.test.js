const { assert } = require('chai');
var _ = require('./iframe-builder.vanilla.js');

describe('iframe-builder', function() {
    var Ya;
    var Iframe;

    beforeEach(function() {
        Ya = window.Yandex;
        Iframe = Ya.VH.Iframe;
        jest.restoreAllMocks();
    });

    describe('parse()', function() {
        it('должен распарсить iframe', function() {
            var iframe = [
                '<iframe',
                'name="id712620843"',
                'class="VideoPlayer-Video"',
                'src="https://frontend.vh.yandex.ru/player/6435419990504370954"',
                'frameborder="0"',
                'scrolling="no"',
                'allowfullscreen',
                'aria-label="Video"',
                'width="100%"',
                'height="100%"',
                'allow="autoplay; fullscreen;"',
                '></iframe>'
            ].join(' ');

            assert.deepEqual(Iframe.parse(iframe), {
                name: 'id712620843',
                src: 'https://frontend.vh.yandex.ru/player/6435419990504370954',
                class: 'VideoPlayer-Video',
                allow: 'autoplay; fullscreen;',
                allowfullscreen: true,
                height: '100%',
                width: '100%',
                style: '',
                'aria-label': 'Video',
                frameborder: '0',
                scrolling: 'no'
            });
        });

        it('должен корректно распарсить iframe c одинарным тегом', function() {
            assert.deepEqual(Iframe.parse('<iframe key1="123" key2>'), {
                key1: '123',
                key2: true,
                allow: '',
                class: '',
                style: '',
                src: ''
            });
        });
    });

    describe('конструктор', function() {
        beforeEach(function() {
            jest.spyOn(Iframe, 'parse');

            jest.spyOn(Iframe.ATTRS.src, 'parse').mockReturnValue({ path: 'frontend.vh.yandex.ru/player', query: { autoplay: 1, from: 'yavideo' } });
            jest.spyOn(Iframe.ATTRS.allow, 'parse').mockReturnValue(['autoplay', 'fullscreen']);
            jest.spyOn(Iframe.ATTRS.class, 'parse').mockReturnValue(['sandbox', 'sandbox_no-required']);
        });

        it('корректно парсит html', function() {
            Iframe.parse.mockReturnValueOnce({
                id: 'vh-player',
                src: 'frontend.vh.yandex.ru/player?autoplay=1&from=yavideo',
                class: 'sandbox sandbox_no-required',
                allow: 'autoplay; fullscreen'
            });

            var iframe = new Iframe('<iframe></iframe>');

            // При сравнивании iframe напрямую с объектом deepEqual падает с ошибкой,
            // из-за того что iframe инстанс Iframe, а не Object
            assert.deepEqual(Object.keys(iframe), ['id', 'src', 'class', 'allow']);

            assert.strictEqual(iframe.id, 'vh-player');

            assert.deepEqual(iframe.src, { path: 'frontend.vh.yandex.ru/player', query: { autoplay: 1, from: 'yavideo' } });
            assert.deepEqual(iframe.class, ['sandbox', 'sandbox_no-required']);
            assert.deepEqual(iframe.allow, ['autoplay', 'fullscreen']);
        });
    });

    describe('toString()', function() {
        var iframe;

        beforeEach(function() {
            iframe = {
                id: 'vh-player',
                src: '//yandex.ru/iframe/...',
                class: 'sandbox',
                style: 'width: 100%',
                allow: 'fullscreen'
            };

            jest.spyOn(Iframe.ATTRS.src, 'build').mockReturnValue('//yandex.ru/iframe/...');
            jest.spyOn(Iframe.ATTRS.allow, 'build').mockReturnValue('fullscreen');
            jest.spyOn(Iframe.ATTRS.class, 'build').mockReturnValue('sandbox');
            jest.spyOn(Iframe.ATTRS.style, 'build').mockReturnValue('width: 100%');
        });

        it('должен корректно построить iframe', function() {
            assert.strictEqual(
                Iframe.prototype.toString.call(iframe),
                '<iframe id="vh-player" src="//yandex.ru/iframe/..." class="sandbox" style="width: 100%" allow="fullscreen"></iframe>'
            );
        });

        it('должен корректно обрабатывать булевы атрибуты', function() {
            iframe = {
                class: 'sandbox',
                allowfullscreen: true,
                customattr: true
            };

            assert.strictEqual(
                Iframe.prototype.toString.call(iframe),
                '<iframe class="sandbox" allowfullscreen customattr></iframe>'
            );
        });

        it('должен пропускать пустые атрибуты', function() {
            iframe.class = '';
            iframe.style = '';

            Iframe.ATTRS.class.build.mockReturnValueOnce('');
            Iframe.ATTRS.style.build.mockReturnValueOnce('');

            assert.strictEqual(
                Iframe.prototype.toString.call(iframe),
                '<iframe id="vh-player" src="//yandex.ru/iframe/..." allow="fullscreen"></iframe>'
            );
        });
    });

    describe('обработка сложных атрибутов', function() {
        describe('атрибут src', function() {
            it('parse() возвращает объект src', function() {
                assert.deepEqual(Iframe.ATTRS.src.parse('https://frontend.vh.yandex.ru/player/15705825753033535710?autoplay=0&amp;from=yavideo'), {
                    path: 'https://frontend.vh.yandex.ru/player/15705825753033535710',
                    query: {
                        autoplay: '0',
                        from: 'yavideo'
                    }
                });
            });

            it('parse() корректно распарсит строку с несколькими &amp;', function() {
                assert.deepEqual(Iframe.ATTRS.src.parse(
                    'https://frontend.vh.yandex.ru/player/15705825753033535710?autoplay=0&amp;from=yavideo&amp;playsinline=1&amp;enablejsapi=1&amp;wmode=opaque'), {
                    path: 'https://frontend.vh.yandex.ru/player/15705825753033535710',
                    query: {
                        autoplay: '0',
                        from: 'yavideo',
                        playsinline: '1',
                        enablejsapi: '1',
                        wmode: 'opaque'
                    }
                });
            });

            it('build() с query корректно собирает строку src', function() {
                assert.strictEqual(Iframe.ATTRS.src.build({
                    path: 'https://frontend.vh.yandex.ru/player/15705825753033535710',
                    query: {
                        autoplay: '0',
                        from: 'yavideo',
                        event_prefix: 'sandbox:'
                    }
                }), 'https://frontend.vh.yandex.ru/player/15705825753033535710?autoplay=0&from=yavideo&event_prefix=sandbox%3A');
            });

            it('build() без query корректно собирает строку без ?', function() {
                assert.strictEqual(Iframe.ATTRS.src.build({ path: 'https://frontend.vh.yandex.ru/player/15705825753033535710', query: {} }),
                    'https://frontend.vh.yandex.ru/player/15705825753033535710');
            });
        });

        describe('атрибут class', function() {
            var originalSet;

            beforeEach(function() {
                originalSet = Ya.VH.Set;
                Ya.VH.Set = jest.fn().mockImplementation(a => a);
            });

            afterEach(function() {
                Ya.VH.Set = originalSet;
            });

            it('parse() возвращает массив классов', function() {
                assert.deepEqual(Iframe.ATTRS.class.parse('sanbox sandbox_no-required'), ['sanbox', 'sandbox_no-required']);
            });

            it('parse() пустой строки возвращает пустой массив', function() {
                assert.deepEqual(Iframe.ATTRS.class.parse(''), []);
            });

            it('build() собирает строку классов через корректный сепаратор', function() {
                var arr = { join: jest.fn() };
                Iframe.ATTRS.class.build(arr);
                assert.deepEqual(arr.join.mock.calls[0], [' ']);
            });
        });

        describe('атрибут allow', function() {
            var originalSet;

            beforeEach(function() {
                originalSet = Ya.VH.Set;
                Ya.VH.Set = jest.fn().mockImplementation(a => a);
            });

            afterEach(function() {
                Ya.VH.Set = originalSet;
            });

            it('parse() возвращает массив разрешений', function() {
                assert.deepEqual(Iframe.ATTRS.allow.parse('autoplay; fullscreen'), ['autoplay', 'fullscreen']);
            });

            it('parse() пустой строки возвращает пустой массив', function() {
                assert.deepEqual(Iframe.ATTRS.allow.parse(''), []);
            });

            it('build() собирает строку разрешений через корректный сепаратор', function() {
                var arr = { join: jest.fn() };
                Iframe.ATTRS.allow.build(arr);
                assert.deepEqual(arr.join.mock.calls[0], ['; ']);
            });
        });

        describe('атрибут style', function() {
            it('parse() возвращает объект со значениями', function() {
                var style = 'width: 100px; height : 30%; color:white; border; ;;;';

                assert.deepEqual(Iframe.ATTRS.style.parse(style), {
                    width: '100px',
                    height: '30%',
                    color: 'white'
                });
            });

            it('parse() пустой строки возвращает пустой объект', function() {
                assert.deepEqual(Iframe.ATTRS.style.parse(''), {});
            });

            it('build() корректно собирает строку стилей', function() {
                var style = {
                    width: 100,
                    height: '50%',
                    border: 0,
                    margin: '',
                    visibility: null,
                    display: false
                };

                assert.strictEqual(Iframe.ATTRS.style.build(style), 'width: 100px; height: 50%; border: 0');
            });

            it('build() возвращает пустую строку, если переданный аргумент не является объектом', function() {
                assert.strictEqual(Iframe.ATTRS.style.build(null), '');
            });
        });
    });
});
