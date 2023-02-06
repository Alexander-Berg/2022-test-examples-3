const EventEmitter = require('events');
const TolokaLog = require('../../../src/lib/toloka-log/src/TolokaLog');

describe('TolokaLog', () => {
    const sandbox = sinon.createSandbox();
    let window, params;

    beforeEach(() => {
        window = {
            addEventListener: sandbox.spy(),
            removeEventListener: sandbox.spy(),
            parent: { window: { postMessage: () => {} } },
        };
        params = {
            useConsoleLog: false,
        };
    });

    afterEach(() => sandbox.restore());

    describe('инициализация', () => {
        it('должен задавать дефолтное значение mode, если оно не переданно', () => {
            const logger = new TolokaLog(params, window);
            const DEFAULT_MODE = 'client';

            assert.equal(logger.params.mode, DEFAULT_MODE);
        });

        it('должен добавлять колбек на событие message, если работает в режиме hub', () => {
            params.mode = 'hub';
            new TolokaLog(params, window);

            assert.isTrue(window.addEventListener.calledOnce);
            assert.isTrue(window.addEventListener.calledWith('message'));
        });

        it('не должен добавлять обработчик на событие message, если работает в режиме client или single', () => {
            params.mode = 'single';
            new TolokaLog(params, window);

            assert.isTrue(window.addEventListener.notCalled);
        });

        it('должен сохранять переданные id', () => {
            const ids = [1, 2, 3];
            params.validId = ids;
            const logger = new TolokaLog(params, window);

            assert.deepEqual(logger.params.validId, ids);
        });

        it('должен обработать сообщения, которые были записанны в window.__tolokaLog и удалить записи', () => {
            const postMessage = sinon.spy();
            window.parent = { window: { postMessage } };
            const FIRST_MESSAGE = ['message1'];
            const SECOND_MESSAGE = ['message2'];
            window.__tolokaLog = [FIRST_MESSAGE, SECOND_MESSAGE];
            params.mode = 'client';

            new TolokaLog(params, window);

            assert.isTrue(postMessage.calledTwice);
            assert.equal(postMessage.firstCall.args[0].message, FIRST_MESSAGE[0]);
            assert.equal(postMessage.secondCall.args[0].message, SECOND_MESSAGE[0]);
            assert.notExists(window.__tolokaLog);
        });
    });

    describe('log', () => {
        it('должен передать сообщение родительскому айфрейму, если работает в режиме client', () => {
            const postMessage = sinon.spy();
            window.parent = { window: { postMessage } };
            params.mode = 'client';
            const logger = new TolokaLog(params, window);
            logger.log('message');

            assert.isTrue(postMessage.calledOnce);
        });

        it('не должен пытаться передать сообщение родительскому айфрейму, если работает в режиме hub или single', () => {
            const postMessage = sinon.spy();
            window.parent.window.postMessage = postMessage;
            params.mode = 'single';
            const logger = new TolokaLog(params, window);
            logger.log('message');

            assert.isTrue(postMessage.notCalled);
        });

        it('должен сохранять сообщение, если работает в режиме hub или single', () => {
            params.mode = 'single';
            const logger = new TolokaLog(params, window);
            logger.log('message1');
            logger.log('message2');

            assert.equal(logger.getLogs().length, 2);
        });

        it('не должен сохранять сообщение, если работает в режиме client', () => {
            params.mode = 'client';
            const logger = new TolokaLog(params, window);
            logger.log('message1');
            logger.log('message2');

            assert.isEmpty(logger.getLogs());
        });

        it('должен добавлять timeStamp к сообщению', () => {
            params.mode = 'single';
            const logger = new TolokaLog(params, window);
            logger.log('message1');
            const message = logger.getLogs()[0];

            assert.exists(message.timeStamp);
        });

        it('должен добавлять дефолтный type к сообщению, если он не передан', () => {
            const DEFAULT_TYPE = 'client-message';
            params.mode = 'single';
            const logger = new TolokaLog(params, window);
            logger.log('message1');
            const message = logger.getLogs()[0];

            assert.equal(message.type, DEFAULT_TYPE);
        });

        it('должен добавлять window.name в parentId к сообщению, если работает в режиме client', () => {
            const NAME = 'test-name';
            const postMessage = sinon.spy();

            params.mode = 'client';
            window.name = NAME;
            window.parent = { window: { postMessage } };

            const logger = new TolokaLog(params, window);
            logger.log('message1');
            const message = postMessage.firstCall.args[0];

            assert.equal(message.systemSide, NAME);
        });

        it('не должен добавлять parentId к сообщению, если работает в режиме hub или single', () => {
            params.mode = 'single';
            const logger = new TolokaLog(params, window);
            logger.log('message1');
            const message = logger.getLogs()[0];

            assert.notExists(message.parentId);
        });

        it('должен сохранять сообщение если оно пришло от айфрейма с валидным id', () => {
            const emiter = new EventEmitter();

            const validId = [1, 2, 3];
            const hubParams = Object.assign({}, params, { validId, mode: 'hub' });
            window.addEventListener = emiter.on.bind(emiter);
            const hub = new TolokaLog(hubParams, window);

            const message = {
                data: {
                    systemSide: 1,
                    message: 'test',
                },
            };

            emiter.emit('message', message);
            assert.equal(hub.getLogs().length, 1);
        });

        it('должен игнорировать сообщение если оно пришло от айфрейма с не валидным id', () => {
            const emiter = new EventEmitter();

            const validId = [1, 2, 3];
            const hubParams = Object.assign({}, params, { validId, mode: 'hub' });
            window.addEventListener = emiter.on.bind(emiter);
            const hub = new TolokaLog(hubParams, window);

            const message = {
                data: {
                    systemSide: 'unknown-id',
                    message: 'test',
                },
            };

            emiter.emit('message', message);
            assert.isEmpty(hub.getLogs());
        });
    });

    describe('destroy', () => {
        it('должен удалять обработчик события message, если работает в режиме hub', () => {
            params.mode = 'hub';
            const logger = new TolokaLog(params, window);
            logger.destroy();
            assert.isTrue(window.removeEventListener.calledOnce);
            assert.isTrue(window.removeEventListener.calledWith('message'));
        });

        it('должен обнулять список логов', () => {
            const logger = new TolokaLog(params, window);
            logger.log('message1');
            logger.log('message2');
            logger.destroy();

            assert.isEmpty(logger.getLogs());
        });
    });
});
