import { assert } from 'chai';
import { suite, test } from 'mocha-typescript';
import nock from 'nock';

import YaNotifyClient from 'index';

@suite('YaNotifyClient')
export class YaNotifyClientSuite {
    @test
    async 'should send message to user'() {
        const client = new YaNotifyClient({
            endpoint: 'http://localhost',
            token: 'TOKEN',
        });

        const data = {
            message: 'Hi!',
            users: ['savichev'],
        };

        const request = nock(
            'http://localhost', {
                reqheaders: {
                    Authorization: 'OAuth TOKEN',
                },
            })
            .post('/notify', {
                ...data,
                parse_mode: null,
            })
            .reply(201, '"Message send to savichev@."');

        const expected = 'Message send to savichev@.';
        const actual = await client.sendMessage(data);

        request.done();

        assert.strictEqual(actual, expected);
    }

    @test
    'should throw error when not passed endpoint'() {
        const initClient = () => {
            // @ts-ignore
            const client = new YaNotifyClient({ // eslint-disable-line @typescript-eslint/no-unused-vars,no-unused-vars
                token: 'TOKEN',
            });
        };

        assert.throws(initClient, 'Endpoint option is required.');
    }

    @test
    'should throw error when endpoint is not string'() {
        const initClient = () => {
            // @ts-ignore
            const client = new YaNotifyClient({ // eslint-disable-line @typescript-eslint/no-unused-vars,no-unused-vars
                // @ts-ignore
                endpoint: true,
                token: 'TOKEN',
            });
        };

        assert.throws(initClient, 'Endpoint option must be a string.');
    }

    @test
    'should throw error when not passed token'() {
        const initClient = () => {
            // @ts-ignore
            const client = new YaNotifyClient({ // eslint-disable-line @typescript-eslint/no-unused-vars,no-unused-vars
                endpoint: 'http://localhost',
            });
        };

        assert.throws(initClient, 'Token option is required.');
    }

    @test
    'should throw error when token is not string'() {
        const initClient = () => {
            // @ts-ignore
            const client = new YaNotifyClient({ // eslint-disable-line @typescript-eslint/no-unused-vars,no-unused-vars
                endpoint: 'http://localhost',
                // @ts-ignore
                token: true,
            });
        };

        assert.throws(initClient, 'Token option must be a string.');
    }

    after() {
        nock.cleanAll();
    }
}
