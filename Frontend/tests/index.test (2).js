/* globals Ya */
/* eslint-disable no-new */
describe('xiva', function() {
    let sandbox;
    let clock;
    let ws;

    beforeEach(function() {
        sandbox = sinon.createSandbox();
        clock = sinon.useFakeTimers();
        ws = {
            addEventListener: sinon.stub(),
            removeEventListener: sinon.stub(),
            send: sinon.stub(),
            close: sinon.stub(),
        };

        window.WebSocket = sinon.stub().returns(ws);
        sandbox.stub(window.Math, 'random').returns(0.1);
    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('должна открывать WebSocket', function() {
        it('с урлом по умолчанию', function() {
            new window.Ya.XIVA();

            sinon.assert.calledOnce(window.WebSocket);
            sinon.assert.calledWith(window.WebSocket, 'wss://push.yandex.ru/v2/subscribe/websocket');
        });

        it('с урлом из параметров если был undefined', function() {
            new window.Ya.XIVA({
                api: undefined,
            });

            sinon.assert.calledOnce(window.WebSocket);
            sinon.assert.calledWith(window.WebSocket, 'wss://push.yandex.ru/v2/subscribe/websocket');
        });

        it('с урлом из параметров', function() {
            new window.Ya.XIVA({
                api: 'wss://url-from-params.ru',
            });

            sinon.assert.calledOnce(window.WebSocket);
            sinon.assert.calledWith(window.WebSocket, 'wss://url-from-params.ru');
        });

        it('с урлом из параметров (с добавленными в него параметрами)', function() {
            new window.Ya.XIVA({
                api: 'wss://url-from-params.ru?test=123',
                urlParams: {
                    p1: 'value-1',
                },
            });

            sinon.assert.calledOnce(window.WebSocket);
            sinon.assert.calledWith(window.WebSocket, 'wss://url-from-params.ru?test=123&p1=value-1');
        });

        it('с параметрами', function() {
            new window.Ya.XIVA({
                api: 'wss://url-from-params.ru',
                urlParams: {
                    p1: 'value-1',
                    p2: 'value-2',
                },
            });

            sinon.assert.calledOnce(window.WebSocket);
            sinon.assert.calledWith(window.WebSocket, 'wss://url-from-params.ru?p1=value-1&p2=value-2');
        });
    });

    describe('должна уметь переподключаться к WebSocket', function() {
        it('с другими параметрами', function() {
            let xiva = new window.Ya.XIVA({
                api: 'wss://url-from-params.ru',
                urlParams: {
                    p1: 'value-1',
                    p2: 'value-2',
                    p3: 'value-3',
                },
            });

            xiva.reconnect({ p1: 'another-1', p2: null });

            sinon.assert.calledTwice(window.WebSocket);
            sinon.assert.calledWith(window.WebSocket.getCall(0), 'wss://url-from-params.ru?p1=value-1&p2=value-2&p3=value-3');
            sinon.assert.calledWith(window.WebSocket.getCall(1), 'wss://url-from-params.ru?p1=another-1&p3=value-3');
        });

        it('с другими параметрами (с полной перезаписью)', function() {
            let xiva = new window.Ya.XIVA({
                api: 'wss://url-from-params.ru',
                urlParams: {
                    p1: 'value-1',
                    p2: 'value-2',
                },
            });

            xiva.reconnect({ p3: 'value-3' }, true);

            sinon.assert.calledTwice(window.WebSocket);
            sinon.assert.calledWith(window.WebSocket.getCall(0), 'wss://url-from-params.ru?p1=value-1&p2=value-2');
            sinon.assert.calledWith(window.WebSocket.getCall(1), 'wss://url-from-params.ru?p3=value-3');
        });
    });

    describe('в случае провала подключения к WebSocket', function() {
        it('должна переподключаться через случайный промежуток времени (застаблен рандум на 0.1)', function() {
            new window.Ya.XIVA({
                api: 'wss://url-from-params.ru',
                urlParams: {
                    p1: 'value-1',
                    p2: 'value-2',
                },
            });

            sinon.assert.calledOnce(window.WebSocket);

            ws.addEventListener.getCall(1).args[1]('close message');

            clock.tick(250);

            sinon.assert.calledTwice(window.WebSocket);
            sinon.assert.calledWith(window.WebSocket.getCall(0), 'wss://url-from-params.ru?p1=value-1&p2=value-2');
            sinon.assert.calledWith(window.WebSocket.getCall(1), 'wss://url-from-params.ru?p1=value-1&p2=value-2');
        });

        it('должна менять интервал переподключения с шагом в 500ms', function() {
            new window.Ya.XIVA({
                api: 'wss://url-from-params.ru',
                urlParams: {
                    p1: 'value-1',
                    p2: 'value-2',
                },
            });

            // первое переподключение происходит через случайный промежуток (Math random === 0.1)
            ws.addEventListener.getCall(1).args[1]('close message');
            clock.tick(250);
            sinon.assert.callCount(window.WebSocket, 2);

            // второе переподключение происходит спустя 500мс
            ws.addEventListener.getCall(5).args[1]('close message');
            clock.tick(600);
            sinon.assert.callCount(window.WebSocket, 2);
            clock.tick(200);
            sinon.assert.callCount(window.WebSocket, 3);

            // третье переподключение происходит спустя 1000мс
            ws.addEventListener.getCall(9).args[1]('close message');
            clock.tick(1100);
            sinon.assert.callCount(window.WebSocket, 3);
            clock.tick(200);
            sinon.assert.callCount(window.WebSocket, 4);
        });

        it('должна сбрасывать интервал переподключения спустя 10 секунд после успешного подключения', function() {
            let xiva = new window.Ya.XIVA({
                api: 'wss://url-from-params.ru',
                urlParams: {
                    p1: 'value-1',
                    p2: 'value-2',
                },
            });

            ws.addEventListener.getCall(1).args[1]('close message');
            clock.tick(250);
            ws.addEventListener.getCall(5).args[1]('close message');
            clock.tick(800);
            ws.addEventListener.getCall(9).args[1]('close message');

            ws.addEventListener.getCall(8).args[1]('open message');

            assert.strictEqual(xiva._reconnectTimeout, 1700);
            clock.tick(10100);
            assert.strictEqual(xiva._reconnectTimeout, 200);
        });
    });

    it('должна подписаться на четыре события open, close, message, error', function() {
        new window.Ya.XIVA();

        sinon.assert.calledWith(ws.addEventListener.getCall(0), 'open');
        sinon.assert.calledWith(ws.addEventListener.getCall(1), 'close');
        sinon.assert.calledWith(ws.addEventListener.getCall(2), 'message');
        sinon.assert.calledWith(ws.addEventListener.getCall(3), 'error');
    });

    it('должна закрыть соединение и отписаться при destruct', function() {
        let xiva = new window.Ya.XIVA();

        xiva.destruct();
        sinon.assert.calledOnce(ws.close);
        sinon.assert.calledWith(ws.removeEventListener.getCall(0), 'open');
        sinon.assert.calledWith(ws.removeEventListener.getCall(1), 'close');
        sinon.assert.calledWith(ws.removeEventListener.getCall(2), 'message');
        sinon.assert.calledWith(ws.removeEventListener.getCall(3), 'error');
    });

    it('должна звать событие open при открытии канала', function() {
        let xiva = new window.Ya.XIVA();
        let openCb = sinon.stub();

        xiva.addEventListener('open', openCb);

        ws.addEventListener.getCall(0).args[1]({ data: 'open message' });

        sinon.assert.calledOnce(openCb);
        assert.deepEqual(openCb.getCall(0).args[0].detail, { data: 'open message' });
    });

    it('должна звать событие close при закрытии канала', function() {
        let xiva = new window.Ya.XIVA();
        let openCb = sinon.stub();

        xiva.addEventListener('close', openCb);

        ws.addEventListener.getCall(1).args[1]({ data: 'close message' });

        sinon.assert.calledOnce(openCb);
        assert.deepEqual(openCb.getCall(0).args[0].detail, { data: 'close message' });
    });

    it('должна звать событие message при получении сообщения из канала', function() {
        let xiva = new window.Ya.XIVA();
        let openCb = sinon.stub();
        let dataObj = { p: 1 };

        xiva.addEventListener('message', openCb);

        ws.addEventListener.getCall(2).args[1]({
            data: JSON.stringify(dataObj),
        });

        sinon.assert.calledOnce(openCb);
        assert.deepEqual(openCb.getCall(0).args[0].detail, dataObj);
    });

    it('должна звать событие error при ошибке канала', function() {
        let xiva = new window.Ya.XIVA();
        let openCb = sinon.stub();

        xiva.addEventListener('error', openCb);

        ws.addEventListener.getCall(3).args[1]({ data: 'close message' });

        sinon.assert.calledOnce(openCb);
        assert.deepEqual(openCb.getCall(0).args[0].detail, { data: 'close message' });
    });

    it('должна уметь отписывать события', function() {
        let xiva = new window.Ya.XIVA();
        let openCb = sinon.stub();

        xiva.addEventListener('open', openCb);
        ws.addEventListener.getCall(0).args[1]({ data: 'some message' });

        xiva.removeEventListener('open', openCb);
        ws.addEventListener.getCall(0).args[1]();

        sinon.assert.calledOnce(openCb);
        assert.deepEqual(openCb.getCall(0).args[0].detail, { data: 'some message' });
    });

    it('должна уметь отправлять сообщения', function() {
        let xiva = new Ya.XIVA();

        xiva.send('send message');

        sinon.assert.calledOnce(ws.send);
        sinon.assert.calledWith(ws.send, 'send message');
    });
});
