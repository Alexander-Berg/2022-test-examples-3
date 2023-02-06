const EventEmitter = require('events');
const UserEventLogger = require('../../../src/lib/toloka-log/src/UserEventLogger');

describe('UserEventLogger', () => {
    const sandbox = sinon.createSandbox();
    let window;

    beforeEach(() => {
        window = {
            addEventListener: sandbox.spy(),
            tolokaLog: sandbox.spy(),
            location: { href: 'https://www.google.com' },
        };
    });

    afterEach(() => sandbox.restore());

    describe('инициализация', () => {
        it('должен добавлять обработчик на событие click', () => {
            new UserEventLogger(window);

            assert.isTrue(window.addEventListener.calledWith('click'));
        });

        it('должен добавлять обработчик на событие beforeunload', () => {
            new UserEventLogger(window);

            assert.isTrue(window.addEventListener.calledWith('beforeunload'));
        });

        it('должен отправять сигнал page-load-start', () => {
            new UserEventLogger(window);
            const EVENT = 'page-load-start';

            assert.isTrue(window.tolokaLog.calledWith(EVENT));
        });
    });

    describe('формат сообщений', () => {
        it('должен передавать тип сообщения system-message', () => {
            const TYPE = 'system-message';
            new UserEventLogger(window);

            const args = window.tolokaLog.firstCall.args;
            const type = args[1];

            assert.equal(type, TYPE);
        });

        it('должен записывать url в meta данные сообщения', () => {
            new UserEventLogger(window);

            const args = window.tolokaLog.firstCall.args;
            const meta = args[2];

            assert.equal(meta.url, window.location.href);
        });

        it('должен записывать eventTimeStampMs в meta данные сообщения', () => {
            new UserEventLogger(window);

            const args = window.tolokaLog.firstCall.args;
            const meta = args[2];

            assert.exists(meta.eventTimeStampMs);
        });

        it('должен записывать координаты x и y в meta данные сообщения для события click', () => {
            const emiter = new EventEmitter();
            const position = { x: 123, y: 321 };
            window.addEventListener = emiter.on.bind(emiter);
            new UserEventLogger(window);

            emiter.emit('click', position);

            const args = window.tolokaLog.secondCall.args;
            const meta = args[2];

            assert.equal(meta.x, position.x);
            assert.equal(meta.y, position.y);
        });
    });
});
