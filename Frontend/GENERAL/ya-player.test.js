const { assert } = require('chai');
var _ = require('./ya-player.vanilla.js');

describe('ya-player', function() {
    var Ya;
    var Player;
    var node;
    var originalInstances;

    beforeEach(function() {
        Ya = window.Yandex;
        Player = Ya.VH.Player;

        //cоздаем контейнер для вставки
        document.body.insertAdjacentHTML('beforeend', '<div id="player1"></div>');
        node = document.getElementById('player1');

        jest.restoreAllMocks();
        originalInstances = Player.instances;
        Player.instances = [];
    });

    afterEach(function() {
        Player.instances = originalInstances;
        node.parentNode.removeChild(node);
    });

    describe('конструктор', function() {
        var originalEventEmitter;

        beforeEach(function() {
            originalEventEmitter = Ya.VH.EventEmitter;

            jest.spyOn(Player, '_makeIframe'). mockReturnValue('<iframe id="player1_player"></iframe>');
            jest.spyOn(Player, '_makeAMPIframe');
            Ya.VH.EventEmitter = jest.fn().mockReturnValue({});
        });

        afterEach(function() {
            Ya.VH.EventEmitter = originalEventEmitter;
        });

        it('соберет корректный объект плеера', function() {
            var player = new Player('player1', {
                contentId: 'id123',
                params: {}
            });

            var iframe = document.getElementById('player1_player');

            assert.deepEqual(Object.keys(player), ['_prefix', '_amp', '_emitter', '_iframe']);
            assert.strictEqual(player._prefix, 'player1_player:');
            assert.deepEqual(player._emitter, {});
            assert.strictEqual(player._iframe, iframe);
            assert.include(Player.instances, player);
        });

        it('бросает ошибку, если не передан id', function() {
            // eslint-disable-next-line no-new
            assert.throws(function() { new Player() }, Ya.VH.Error);
        });

        it('для AMP вызовет функцию _makeAMPIframe', function() {
            // eslint-disable-next-line no-new
            new Player('player1', {
                contentId: 'id123',
                params: {},
                amp: true
            });

            assert.strictEqual(Player._makeAMPIframe.mock.calls.length, 1);
            assert.deepEqual(Player._makeAMPIframe.mock.calls[0], ['<iframe id="player1_player"></iframe>', {
                contentId: 'id123',
                params: {
                    event_prefix: ''
                },
                amp: true,
                iframeId: 'player1_player'
            }]);
        });
    });

    describe('_getPageRequestParams', function() {
        var getPageRequestParamsStub;
        var originalYa;

        beforeEach(function() {
            getPageRequestParamsStub = jest.fn();

            originalYa = window.Ya;
            window.Ya = {
                adfoxCode: {
                    getPageRequestParams: getPageRequestParamsStub
                }
            };
        });

        afterEach(function() {
            window.Ya = originalYa;
        });

        it('возвращает результат вызова getPageRequestParams, если он вернул объект', function() {
            getPageRequestParamsStub.mockReturnValueOnce({ location: '#' });

            assert.deepEqual(Player._getPageRequestParams(), {
                location: '#'
            });
        });

        it('возвращает пустой объект, если getPageRequestParams вернул что-то кроме объекта', function() {
            getPageRequestParamsStub.mockReturnValueOnce(null);
            assert.deepEqual(Player._getPageRequestParams(), {});
        });

        it('возвращает пустой объект, если getPageRequestParams отсутствует', function() {
            window.Ya.adfoxCode.getPageRequestParams = undefined;
            assert.deepEqual(Player._getPageRequestParams(), {});
        });

        it('возвращает пустой объект, если adfoxCode отсутствует', function() {
            window.Ya = {};
            assert.deepEqual(Player._getPageRequestParams(), {});
        });

        it('возвращает пустой объект, если Ya отсутствует', function() {
            window.Ya = undefined;
            assert.deepEqual(Player._getPageRequestParams(), {});
        });
    });

    describe('_prepareAdConfig()', function() {
        beforeEach(function() {
            jest.spyOn(Player, '_getPageRequestParams').mockReturnValue({
                adSessionId: 'ad123'
            });
        });

        it('проклеивает id контейнера и параметры страницы в параметры рекламы', function() {
            var adConfig = {
                adBreaks: [
                    {
                        adFoxParameters: {}
                    },
                    {
                        test: 'test'
                    }
                ]
            };

            assert.deepEqual(Player._prepareAdConfig(adConfig, 'id123'), {
                adBreaks: [
                    {
                        adFoxParameters: {
                            containerId: 'id123',
                            adSessionId: 'ad123'
                        }
                    },
                    {
                        test: 'test'
                    }
                ]
            });
        });

        it('не проклеивает id контейнера в параметры рекламы, если нет поля adFoxParameters', function() {
            var adConfig = {
                adBreaks: [
                    {
                        partnerId: 123456,
                        category: 2017,
                        videoContentId: '14900627828921839597'
                    },
                    {
                        test: 'test'
                    }
                ]
            };

            assert.deepEqual(Player._prepareAdConfig(adConfig, 'id123'), {
                adBreaks: [
                    {
                        partnerId: 123456,
                        category: 2017,
                        videoContentId: '14900627828921839597'
                    },
                    {
                        test: 'test'
                    }
                ]
            });
        });

        it('корректно обрабатывает ситуацию с пустым adConfig', function() {
            assert.deepEqual(Player._prepareAdConfig({}, 'id123'), {});
        });
    });

    describe('_makeIframe()', function() {
        var originalIframe;

        beforeEach(function() {
            originalIframe = Ya.VH.Iframe;
            Ya.VH.Iframe = function() {
                this.src = {};
            };
        });

        afterEach(function() {
            Ya.VH.Iframe = originalIframe;
        });

        it('корректно формирует объект Iframe', function() {
            var config = {
                params: {
                    event_prefix: 'player1_player:',
                    autoplay: 1
                },
                contentId: 'id123',
                iframeId: 'player1_player',
                height: 300,
                width: 300
            };

            assert.deepEqual(Player._makeIframe(config, 'player1'), {
                id: 'player1_player',
                src: {
                    path: 'http://frontend.vh.yandex.ru/player/id123',
                    query: {
                        autoplay: 1,
                        event_prefix: 'player1_player:'
                    }
                },
                allow: [
                    'autoplay',
                    'gyroscope',
                    'accelerometer',
                    'picture-in-picture',
                    'encrypted-media'
                ],
                style: {
                    display: 'block',
                    width: '300px',
                    height: '300px',
                    margin: 0,
                    padding: 0,
                    border: 0
                },
                allowfullscreen: true
            });
        });

        it('корректно формирует объект Iframe с adConfig', function() {
            var config = {
                params: {
                    event_prefix: 'player1_player:',
                    autoplay: 1
                },
                contentId: 'id123',
                iframeId: 'player1_player',
                adConfig: {
                    partnerId: 123456,
                    category: 2017,
                    videoContentId: '14900627828921839597',
                    adBreaks: [
                        {
                            adFoxParameters: {}
                        },
                        {
                            adFoxParameters: {}
                        }
                    ]
                }
            };

            assert.deepEqual(Player._makeIframe(config, 'player1'), {
                id: 'player1_player',
                src: {
                    path: 'http://frontend.vh.yandex.ru/player/id123',
                    query: {
                        autoplay: 1,
                        event_prefix: 'player1_player:',
                        adConfig: '{"partnerId":123456,"category":2017,"videoContentId":"14900627828921839597","adBreaks":[{"adFoxParameters":{"containerId":"player1"}},{"adFoxParameters":{"containerId":"player1"}}]}'
                    }
                },
                allow: [
                    'autoplay',
                    'gyroscope',
                    'accelerometer',
                    'picture-in-picture',
                    'encrypted-media'
                ],
                style: {
                    display: 'block',
                    margin: 0,
                    padding: 0,
                    border: 0,
                    height: '100%',
                    width: '100%'
                },
                allowfullscreen: true
            });
        });
    });

    describe('_makeAMPIframe()', function() {
        var originalUrlAmpVhPlayer;

        beforeEach(function() {
            originalUrlAmpVhPlayer = Player.URL_AMP_VH_PLAYER;
        });

        afterEach(function() {
            Player.URL_AMP_VH_PLAYER = originalUrlAmpVhPlayer;
        });

        it('корректно формирует iframe песочницы amp-vh-player', function() {
            assert.strictEqual(Player._makeAMPIframe('<iframe></iframe>', {
                width: '100px',
                height: '300px'
            }), '<amp-video-iframe ' +
                    'src="http://yastatic.net/video-player/0x4b93bdfb3cf/pages-common/amp-vh-player/amp-vh-player.html#html=%3Ciframe%3E%3C%2Fiframe%3E" ' +
                    'width="100px" ' +
                    'height="300px" ' +
                    'layout="responsive" ' +
                    'poster="">' +
                '</amp-video-iframe>');
        });

        it('вернет пустую строку, если не была задана высота', function() {
            assert.strictEqual(Player._makeAMPIframe('<iframe></iframe>', {
                width: '100px',
                height: ''
            }), '');
        });

        it('вернет пустую строку, если не была задана ширина', function() {
            assert.strictEqual(Player._makeAMPIframe('<iframe></iframe>', {
                width: '',
                height: '300px'
            }), '');
        });

        it('подставит версию песочницы для дебага', function() {
            jest.spyOn(localStorage.__proto__, 'getItem').mockImplementation(() => '0x11111111');

            assert.strictEqual(Player._makeAMPIframe('<iframe></iframe>', {
                width: '100px',
                height: '300px'
            }), '<amp-video-iframe ' +
                'src="http://yastatic.net/video-player/0x11111111/pages-common/amp-vh-player/amp-vh-player.html#html=%3Ciframe%3E%3C%2Fiframe%3E" ' +
                'width="100px" ' +
                'height="300px" ' +
                'layout="responsive" ' +
                'poster="">' +
                '</amp-video-iframe>');
        });
    });

    describe('методы управления плеером', function() {
        var instance;

        beforeEach(function() {
            instance = Object.create(Player.prototype);

            instance._sendMessage = jest.fn();
        });

        it('play()', function() {
            instance.play();
            assert.strictEqual(instance._sendMessage.mock.calls[0][0], 'play');
        });

        it('pause()', function() {
            instance.pause();
            assert.strictEqual(instance._sendMessage.mock.calls[0][0], 'pause');
        });

        it('mute()', function() {
            instance.mute();
            assert.strictEqual(instance._sendMessage.mock.calls[0][0], 'mute');
        });

        it('unmute()', function() {
            instance.unmute();
            assert.strictEqual(instance._sendMessage.mock.calls[0][0], 'unmute');
        });

        it('setQuality()', function() {
            instance.setQuality('small');
            assert.deepEqual(instance._sendMessage.mock.calls[0], ['setQuality', { quality: 'small' }]);
        });

        it('seek()', function() {
            instance.seek(10);
            assert.deepEqual(instance._sendMessage.mock.calls[0], ['seek', { time: 10 }]);
        });

        it('setVolume()', function() {
            instance.setVolume(0.5);
            assert.deepEqual(instance._sendMessage.mock.calls[0], ['setVolume', { volume: 0.5 }]);
        });

        it('setSource()', function() {
            instance.setSource('id123', {
                autoplay: true
            });

            assert.deepEqual(instance._sendMessage.mock.calls[0], ['updateSource', {
                id: 'id123',
                params: {
                    autoplay: true
                }
            }]);
        });

        it('destroy()', function() {
            document.body.insertAdjacentHTML('beforeend', '<div id="player2"></div>');

            jest.spyOn(Player, '_makeIframe').mockImplementation(function(config, id) {
                return '<iframe id="' + id + '_player" style="width: 360px; height: 600px; border: none"></iframe>';
            });
            jest.spyOn(Player, '_makeAMPIframe');
            Ya.VH.EventEmitter = jest.fn().mockReturnValue({});

            var player1 = new Player('player1', {
                contentId: 'id123',
                params: {}
            });

            // eslint-disable-next-line no-new
            new Player('player2', {
                contentId: 'id456',
                params: {}
            });

            player1.destroy();
            assert.strictEqual(node.innerHTML, '');
            assert.strictEqual(Player.instances.length, 1);

            player1.destroy();
            assert.strictEqual(Player.instances.length, 1);
        });
    });

    describe('miniPLayer', function() {
        var originalEventEmitter;
        var originalSetTimeout;
        var player;

        beforeEach(function() {
            originalEventEmitter = Ya.VH.EventEmitter;

            jest.spyOn(Player, '_makeIframe').mockReturnValue('<iframe id="player1_player" style="width: 360px; height: 600px; border: none"></iframe>');
            Ya.VH.EventEmitter = jest.fn().mockReturnValue({});

            player = new Player('player1', {
                contentId: 'id123',
                params: {}
            });

            originalSetTimeout = window.setTimeout;
            window.setTimeout = function(callback) {
                callback();
            };
        });

        afterEach(function() {
            Ya.VH.EventEmitter = originalEventEmitter;
            window.setTimeout = originalSetTimeout;
        });

        describe('enableMiniPlayer()', function() {
            it('проигнорирует вызов, если плеер уже был вынесен', function() {
                jest.spyOn(window, 'setTimeout');
                player.miniPlayerEnabled = true;

                player.enableMiniPlayer({});
                assert.strictEqual(window.setTimeout.mock.calls.length, 0);
            });

            it('вынесет плеер и запомнит текущее положение', function() {
                player.miniPlayerEnabled = false;

                player.enableMiniPlayer({
                    bottom: 10,
                    right: 20
                }, {
                    width: 100,
                    height: 50
                });

                assert.isTrue(player.miniPlayerEnabled);

                assert.strictEqual(player._iframe.style.transition, 'opacity 0.7s linear 0s');
                assert.strictEqual(player._iframe.style.position, 'fixed');
                assert.strictEqual(player._iframe.style.bottom, '10px');
                assert.strictEqual(player._iframe.style.right, '20px');
                assert.strictEqual(player._iframe.style.width, '100px');
                assert.strictEqual(player._iframe.style.height, '50px');
            });
        });
    });

    describe('_sendMessage()', function() {
        var originalEventEmitter;
        var player;

        beforeEach(function() {
            originalEventEmitter = Ya.VH.EventEmitter;

            jest.spyOn(Player, '_makeIframe').mockReturnValue('<iframe id="player1_player"></iframe>');
            Ya.VH.EventEmitter = jest.fn().mockReturnValue({});

            player = new Player('player1', {
                contentId: 'id123',
                params: {}
            });

            jest.spyOn(player._iframe.contentWindow, 'postMessage');
        });

        afterEach(function() {
            Ya.VH.EventEmitter = originalEventEmitter;
        });

        it('вызывает postMessage для метода без дополнительных параметров', function() {
            player._sendMessage('play');
            assert.deepEqual(player._iframe.contentWindow.postMessage.mock.calls[0], [JSON.stringify({ method: 'player1_player:play' }), '*']);
        });

        it('вызывает postMessage для метода с дополнительныч параметров', function() {
            player._sendMessage('setQuality', { quality: 'small' });
            assert.deepEqual(player._iframe.contentWindow.postMessage.mock.calls[0], [JSON.stringify({ quality: 'small', method: 'player1_player:setQuality' }), '*']);
        });
    });

    describe('методы работы с событиями', function() {
        var originalEventEmitter;
        var player;

        beforeEach(function() {
            originalEventEmitter = Ya.VH.EventEmitter;

            jest.spyOn(Player, '_makeIframe').mockReturnValue('<iframe id="player1_player"></iframe>');
            Ya.VH.EventEmitter = jest.fn().mockReturnValue({
                on: jest.fn(),
                once: jest.fn(),
                removeListener: jest.fn()
            });

            player = new Player('player1', {
                contentId: 'id123',
                params: {}
            });
        });

        afterEach(function() {
            Ya.VH.EventEmitter = originalEventEmitter;
        });

        it('on()', function() {
            var listener = function() {};
            player.on('paused', listener);
            assert.deepEqual(player._emitter.on.mock.calls[0], ['paused', listener]);
        });

        it('once()', function() {
            var listener = function() {};
            player.once('paused', listener);
            assert.deepEqual(player._emitter.once.mock.calls[0], ['paused', listener]);
        });

        it('off()', function() {
            var listener = function() {};
            player.off('paused', listener);

            assert.deepEqual(player._emitter.removeListener.mock.calls[0], ['paused', listener, player._emitter, false]);
        });
    });

    describe('_onMessage()', function() {
        var originalEventEmitter;
        var player;

        beforeEach(function() {
            originalEventEmitter = Ya.VH.EventEmitter;

            jest.spyOn(Player, '_makeIframe').mockReturnValue('<iframe id="player1_player"></iframe>');
            Ya.VH.EventEmitter = jest.fn().mockReturnValue({
                emit: jest.fn()
            });

            player = new Player('player1', {
                contentId: 'id123',
                params: {}
            });
        });

        afterEach(function() {
            Ya.VH.EventEmitter = originalEventEmitter;
        });

        it('прослушает сообщение, посланное от плеера в виде объекта', function() {
            Player._onMessage({
                data: { event: 'player1_player:play', value: 'value' }
            });

            var index = Player.instances.indexOf(player);

            assert.strictEqual(Player.instances[index]._emitter.emit.mock.calls[0][0], 'play');
            assert.deepEqual(Player.instances[index]._emitter.emit.mock.calls[0][1], {
                event: 'player1_player:play',
                value: 'value'
            });
        });

        it('прослушает сообщение, посланное от плеера в виде строки', function() {
            Player._onMessage({
                data: '{"event":"player1_player:play"}'
            });

            var index = Player.instances.indexOf(player);

            assert.strictEqual(Player.instances[index]._emitter.emit.mock.calls.length, 1);
            assert.strictEqual(Player.instances[index]._emitter.emit.mock.calls[0][0], 'play');
            assert.deepEqual(Player.instances[index]._emitter.emit.mock.calls[0][1], {
                event: 'player1_player:play'
            });
        });

        it('ничего не сделает, если передан невалидный объект', function() {
            Player._onMessage({
                data: { qwerty: 'player1_player:play' }
            });

            var index = Player.instances.indexOf(player);

            assert.strictEqual(Player.instances[index]._emitter.emit.mock.calls.length, 0);
        });

        it('ничего не сделает, если iframe инстанса уже удален', function() {
            player._iframe = undefined;
            Player._onMessage({
                data: { qwerty: 'player1_player:play' }
            });

            var index = Player.instances.indexOf(player);

            assert.strictEqual(Player.instances[index]._emitter.emit.mock.calls.length, 0);
        });
    });
});
