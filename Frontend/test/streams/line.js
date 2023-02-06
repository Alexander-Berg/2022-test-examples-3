/* eslint-env mocha */
'use strict';

const sinon = require('sinon');

const yandexLogger = require('../..');
const lineStream = require('../../streams/line');

describe('YandexLogger. Streams. Line', () => {
    let stream;
    let clock;

    beforeEach(() => {
        stream = { write: sinon.spy() };

        clock = sinon.useFakeTimers(1000);
    });

    afterEach(() => {
        clock.restore();
    });

    it('должен логировать записи в дефолтном формате в переданный стрим', () => {
        let logger = createLogger({ stream });
        logger.info('Simple "%s"', 'message');

        sinon.assert.calledWithExactly(
            stream.write,
            '1970-01-01T00:00:01.000Z INFO: Simple "message"\n'
        );
        sinon.assert.calledOnce(stream.write);
    });

    it('должен сериализовать объекты', () => {
        let logger = createLogger({
            stream,
            template: '{{levelName}} {{user}} ({{msg}})',
        });
        logger.error({ user: { login: 'yndx' } }, 'Simple "%s"', 'message');

        sinon.assert.calledWithExactly(
            stream.write,
            'ERROR {"login":"yndx"} (Simple "message")\n'
        );
        sinon.assert.calledOnce(stream.write);
    });

    it('должен сериализовать вложенные объекты', () => {
        let logger = createLogger({
            stream,
            template: '{{levelName}} {{data.user}} ({{msg}})',
        });
        logger.error({ data: { user: { login: 'yndx' } } }, 'Simple "%s"', 'message');

        sinon.assert.calledWithExactly(
            stream.write,
            'ERROR {"login":"yndx"} (Simple "message")\n'
        );
        sinon.assert.calledOnce(stream.write);
    });

    it('должен принимать функции для резолва полей', () => {
        let logger = createLogger({
            stream,
            template: '{{date}} {{levelName}} {{user}} ({{msg}})',
            resolvers: {
                levelName: record => record.levelName.toLowerCase(),
            },
        });
        logger.error({ user: { login: 'yndx' } }, 'Simple "%s"', 'message');

        sinon.assert.calledWithExactly(
            stream.write,
            '"1970-01-01T00:00:01.000Z" error {"login":"yndx"} (Simple "message")\n'
        );
        sinon.assert.calledOnce(stream.write);
    });

    it('должен принимать функции для резолва вложенных полей', () => {
        let logger = createLogger({
            stream,
            template: '{{date}} {{levelName}} {{data.user}}',
            resolvers: {
                'data.user': record => `login=${record.data.user.login}`,
            },
        });
        logger.error({ data: { user: { login: 'yndx' } } }, 'Simple "%s"', 'message');

        sinon.assert.calledWithExactly(
            stream.write,
            '"1970-01-01T00:00:01.000Z" ERROR login=yndx\n'
        );
        sinon.assert.calledOnce(stream.write);
    });

    it('должен поддерживать условные конструкции', () => {
        let logger = createLogger({
            stream,
            template: '{{date}} {{levelName}}: {{msg}}' +
                '{{#user}} User: {{userData}}{{/user}};' +
                '{{#err}} Stack: {{err.stack}}{{/err}}',
            resolvers: {
                date: record => record.date.toISOString(),
                userData: record => {
                    let user = record.user;
                    return `login=${user.login}`;
                },
            },
        });
        logger.fatal({ err: { stack: {} }, user: { login: 'yndx' } }, 'Simple "%s"', 'message');

        sinon.assert.calledWithExactly(
            stream.write,
            '1970-01-01T00:00:01.000Z FATAL: Simple "message" ' +
            'User: login=yndx; ' +
            'Stack: {}\n'
        );
        sinon.assert.calledOnce(stream.write);
    });

    describe('Шаблоны', () => {
        let tests = [
            ['{{uid}}', 'Robot', { uid: 'Robot' }],
            ['{{uid}} {{login}}', 'Robot yndx-robot', { uid: 'Robot', login: 'yndx-robot' }],
            [
                '{{#uid}}{{uid}} {{/uid}}{{login}}',
                '#89625 yndx-robot',
                { uid: '#89625', login: 'yndx-robot' },
            ],
            [
                '{{#uid}}{{uid}} {{/uid}}{{login}}',
                'yndx-robot',
                { login: 'yndx-robot' },
            ],
            [
                '{{#uid}}{{uid}}{{#login}} {{login}}{{/login}}{{/uid}}',
                '#89625 yndx-robot',
                { uid: '#89625', login: 'yndx-robot' },
            ],
            [
                '{{#uid}}{{uid}}{{#login}} {{login}}{{/login}}{{/uid}}',
                '',
                { login: 'yndx-robot' },
            ],
            [
                '{{#uid}}{{uid}}{{#login}} {{login}}{{/login}}{{/uid}}',
                '#89625',
                { uid: '#89625' },
            ],
            [
                '{{>uid}} {{!uid}} {{{uid}}} {{uid}}',
                'uid uid uid #89625',
                { uid: '#89625' },
            ],
        ];

        while (tests.length) {
            let test = tests.shift();
            it(`"${test[0]}" -> "${test[1]}"`, () => {
                let logger = createLogger({ stream, template: test[0] });
                logger.fatal(test[2]);
                sinon.assert.calledWithExactly(stream.write, `${test[1]}\n`);
                sinon.assert.calledOnce(stream.write);
            });
        }
    });
});

function createLogger(config) {
    return yandexLogger({
        streams: [
            {
                stream: lineStream(config),
            },
        ],
    });
}
